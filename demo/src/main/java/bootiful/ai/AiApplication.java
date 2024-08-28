package bootiful.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Function;

/**
 * Sample code from our presentation at SpringOne 2024 
 * 
 * @author Rod Johnson
 * @author Mark Pollack
 * @author Josh Long
 */
@SpringBootApplication
public class AiApplication {
	
	@Bean
	ChatClient chatClient(ChatClient.Builder builder) {
		return builder.build();
	}

	//	@Bean
	ApplicationRunner basics(ChatClient chatClient) {
		return args -> {

			record Joke(String prompt, String punchline) {
			}

			var joke = chatClient
					.prompt()
					.user("tell me a joke")
					.call()
					.entity(Joke.class);
			System.out.println("joke: " + joke);
		};
	}

	@Bean
	JdbcClient jdbcClient(DataSource dataSource) {
		return JdbcClient.create(dataSource);
	}

	//	@Bean
	ApplicationRunner rag(ChatClient chatClient,
						  JdbcClient jdbcClient,
						  VectorStore vectorStore,
						  DogRepository dogRepository) {
		return args -> {

			jdbcClient.sql("delete from vector_store").update();
			//
			dogRepository.findAll().forEach(dog -> {
				var document = new Document("id: %s, name: %s, description: %s".formatted(
						dog.id(), dog.name(), dog.description()
				));
				vectorStore.add(List.of(document));
			});

			var content = chatClient
					.prompt()
					.system("""
							you are an assistant at a dog adoption agency named Pooch's Palace.
							If someone asks for help, and you don't have any information available, respond
							with a dissapointed response suggesting you don't have any dogs at the moment, but
							to please try again soon or to consult one of our many locations in Las Vegas, 
							San Francisco, Singapore, and London.
														
							""")
					.user("do you have any neurotic dogs?")
					.advisors(new QuestionAnswerAdvisor(vectorStore))
					.call()
					.content();
			System.out.println(content);

		};
	}


	//	@Bean
	ApplicationRunner memory(ChatClient chatClient) {
		return args -> {
			var memory = new InMemoryChatMemory();
			// if you're using OpenAI you should be using the MessageChatMemoryAdvisor 
			var promptChatMemoryAdvisor = new PromptChatMemoryAdvisor(memory);
			System.out.println( chatClient
					.prompt()
					.user("hi, my name is Rod. How are you?")
					.advisors(promptChatMemoryAdvisor)
					.call()
					.content() 
			);
			System.out.println( chatClient
					.prompt()
					.user("what's my name?")
					.advisors(promptChatMemoryAdvisor)
					.call()
					.content()
			);
		};
	}
	
//	@Bean
	ApplicationRunner functions(ChatClient chatClient) {
		return args -> {
			var reply = chatClient
					.prompt()
					.user("you are an assistant helping people answer " +
							"questions about the status of their orders.")
					.functions("checkAccountStatus")
					.user("what is the status of my order (order number #23232)?")
					.call()
					.content();
			System.out.println("reply: " + reply);
		};
	}

	@Bean
	ApplicationRunner observability(ChatClient chatClient) {
		return args -> {
			this.memory(chatClient);
			// then login to zipkin and actuator metrics
		};

	}

	@Bean
	@Description("checkAccountStatus")
	Function<CheckAccountStatusRequest, CheckAccountStatusResponse> checkAccountStatus() {
		return request -> {
			var status = new CheckAccountStatusResponse(request.orderNumber(), Math.random() > .5);
			System.out.println("status: [" + status + "]");
			return status;
		};
	}

	record CheckAccountStatusRequest(String orderNumber) {
	}

	record CheckAccountStatusResponse(String orderNumber,
									  boolean completed) {
	}


	public static void main(String[] args) {
		SpringApplication.run(AiApplication.class, args);
	}

}

record Dog(@Id Integer id, String name, String description) {
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}