package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class PgVectorVectorStoreConfig {

    @Value("${app.pgvector.datasource.url}")
    private String url;

    @Value("${app.pgvector.datasource.username}")
    private String username;

    @Value("${app.pgvector.datasource.password}")
    private String password;

    @Value("${app.pgvector.datasource.driver-class-name}")
    private String driverClassName;

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Bean
    public VectorStore pgVectorVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        DataSource dataSource = DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store")
                .maxDocumentBatchSize(10000)
                .build();
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
        Integer count = 0;
        try {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vector_store", Integer.class);
        } catch (Exception e) {
            // 表不存在（首次启动），忽略，由下面的 add() 自动建表
        }
        if (count != null && count > 0) {
            jdbcTemplate.execute("DELETE FROM vector_store");
        }
        vectorStore.add(documents);
        return vectorStore;
    }
}
