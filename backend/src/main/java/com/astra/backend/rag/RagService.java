package com.astra.backend.rag;

import com.astra.backend.document.DocumentEntity;
import com.astra.backend.document.DocumentRepository;
import com.astra.backend.document.OpenAiEmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class RagService {

    private final DocumentRepository documents;
    private final OpenAiEmbeddingClient embeddingClient;
    private final OpenAiChatClient chatClient;
    private final NamedParameterJdbcTemplate jdbc;
    private final int topK;

    public RagService(
            DocumentRepository documents,
            OpenAiEmbeddingClient embeddingClient,
            OpenAiChatClient chatClient,
            NamedParameterJdbcTemplate jdbc,
            @Value("${app.rag.search-top-k:5}") int topK
    ) {
        this.documents = documents;
        this.embeddingClient = embeddingClient;
        this.chatClient = chatClient;
        this.jdbc = jdbc;
        this.topK = topK;
    }

    public AskResult ask(String ownerEmail, String question, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "documentIds는 최소 1개 이상 필요합니다.");
        }

        List<Long> dedupedIds = documentIds.stream().distinct().toList();
        List<DocumentEntity> owned = documents.findByIdInAndOwnerEmail(dedupedIds, ownerEmail);
        if (owned.size() != dedupedIds.size()) {
            throw new ResponseStatusException(FORBIDDEN, "선택한 문서에 접근 권한이 없습니다.");
        }

        Set<Long> ownedIds = new LinkedHashSet<>();
        for (DocumentEntity d : owned) {
            ownedIds.add(d.getId());
        }

        List<Double> queryEmbedding = embeddingClient.embed(question);
        List<SourceItem> sources = searchTopChunks(queryEmbedding, List.copyOf(ownedIds));
        String answer = chatClient.answer(question, sources);

        return new AskResult(answer, sources);
    }

    private List<SourceItem> searchTopChunks(List<Double> queryEmbedding, List<Long> documentIds) {
        String vector = toVectorLiteral(queryEmbedding);
        String sql = """
                SELECT d.id AS document_id,
                       d.title AS document_title,
                       c.id AS chunk_id,
                       c.chunk_index AS chunk_index,
                       c.content AS content,
                       (1 - (e.embedding <=> CAST(:queryVector AS vector))) AS score
                FROM chunk_embeddings e
                JOIN document_chunks c ON c.id = e.chunk_id
                JOIN documents d ON d.id = c.document_id
                WHERE d.id IN (:documentIds)
                ORDER BY e.embedding <=> CAST(:queryVector AS vector)
                LIMIT :topK
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("documentIds", documentIds)
                .addValue("queryVector", vector)
                .addValue("topK", topK);

        return jdbc.query(sql, params, (rs, rowNum) -> new SourceItem(
                rs.getLong("document_id"),
                rs.getString("document_title"),
                rs.getLong("chunk_id"),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getDouble("score")
        ));
    }

    private String toVectorLiteral(List<Double> values) {
        StringBuilder sb = new StringBuilder(values.size() * 8 + 2);
        sb.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(values.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    public record SourceItem(
            Long documentId,
            String documentTitle,
            Long chunkId,
            Integer chunkIndex,
            String content,
            Double score
    ) {}

    public record AskResult(String answer, List<SourceItem> sources) {}
}

