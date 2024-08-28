package com.example.ai.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.Map;

@Component
class DataLoader {

	private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

	private final VectorStore vectorStore;

	private final JdbcClient db;

	DataLoader(VectorStore vectorStore, DataSource dataSource) {
		this.db = JdbcClient.create(dataSource);
		Assert.notNull(vectorStore, "VectorStore must not be null.");
		this.vectorStore = vectorStore;
	}


	public void load(DocumentReader reader, String fileName) {
		this.db.sql("delete from vector_store").update();
		// Transform
		var tokenTextSplitter = new TokenTextSplitter();
		logger.info("Parsing document, splitting, creating embeddings and storing in vector store...");

		var splitDocuments = tokenTextSplitter.split(reader.read());

		// tag as external knowledge in the vector store's metadata
		for (var splitDocument : splitDocuments) {
			splitDocument.getMetadata().putAll(Map.of(
					"filename", fileName,
					"version", 1
			));
		}

		// Load
		this.vectorStore.write(splitDocuments);

		logger.info("Done parsing document, splitting, creating embeddings and storing in vector store");

	}

}
