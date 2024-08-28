package com.example.ai.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@SpringBootApplication
public class AiObservabilityDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiObservabilityDemoApplication.class, args);
	}

	private final Logger logger = LoggerFactory.getLogger(AiObservabilityDemoApplication.class);

	private final String multiballUserPrompt = "What is the suggested strategy to play this game?";

	private final Resource txtResource;


	AiObservabilityDemoApplication(@Value("classpath:/data/congo.txt") Resource txtResource) {
		this.txtResource = txtResource;
	}

	@Bean
	CommandLineRunner test(
			ChatClient.Builder builder, EmbeddingModel embeddingModel, DataLoader loader,
			VectorStore vectorStore) {
		var chatMemory = new InMemoryChatMemory();
		var chatClient = builder.build();
		return args -> {
			var MAX_RUNS = 1; // todo be careful of this precision foot gun! make sure to lower this for simpler runs.
			for (int i = 0; i < MAX_RUNS; i++) {

				// functionCalling(chatClient, false);

				questionAnswerNoVectorStore(chatClient);
//				questionAnswer(chatClient, loader, vectorStore);

//				questionAnswerNoVectorStore(chatClient);
				// questionAnswerWithChatMemoryStreaming(chatClient, chatMemory, vectorStore);

				Thread.sleep(5_000);
			}
		};

	}

	private void questionAnswer(ChatClient chatClient, DataLoader dataLoader,
			VectorStore vectorStore) {

		dataLoader.load(new TextReader(txtResource), txtResource.getFilename());

		var response = chatClient
				.prompt()
				.user(this.multiballUserPrompt)
				.advisors(new QuestionAnswerAdvisor(vectorStore))
				.call()
				.content();
		logger.info("Response: {}", response);

	}

	private void questionAnswerNoVectorStore(ChatClient chatClient) throws IOException {

		var systemText = """
				
				Given the context and provided history information and not prior knowledge, 
				reply to the user comment. If the answer is not in the context, inform 
				the user that you can't answer the question.
				
				Context: 
				
				
				""";


		var response = chatClient
				.prompt()
				.system(systemText + this.txtResource.getContentAsString(Charset.defaultCharset()))
				.user(multiballUserPrompt)
				.call()
				.content();
		logger.info("Response: {}", response);

	}

	private void questionAnswerWithChatMemoryStreaming(ChatClient chatClient, InMemoryChatMemory chatMemory,
			VectorStore vectorStore) {

		var response = chatClient.prompt()
			.user("How does Carina work?")
			.advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
			.advisors(new PromptChatMemoryAdvisor(chatMemory))
			.stream()
			.chatResponse();

		Objects.requireNonNull(response.collectList().block()).forEach(s -> logger.info("Stream Response: {}", s));

	}

	// Function calling

	private void functionCalling(ChatClient chatClient, boolean parallelCalls) {

		var response = chatClient
				.prompt()
			.options(OpenAiChatOptions.builder().withParallelToolCalls(parallelCalls).build())
			.user("What is the status of my payment transactions 001, 002 and 003?")
			.functions("paymentStatus")
			.call()
			.content();

		logger.info("\n\n Response: {} \n\n", response);

	}

	record Transaction(String id) {
	}

	record Status(String name) {
	}

	@Bean
	@Description("Get the status of a single payment transaction")
	Function<Transaction, Status> paymentStatus() {
		return transaction -> {
			logger.info("Single transaction: " + transaction);
			return DATASET.get(transaction);
		};
	}

	static final Map<Transaction, Status> DATASET = Map.of(new Transaction("001"), new Status("pending"),
			new Transaction("002"), new Status("approved"), new Transaction("003"), new Status("rejected"));

}
