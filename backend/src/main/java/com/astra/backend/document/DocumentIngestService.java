package com.astra.backend.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentIngestService {

    private final DocumentRepository documents;
    private final DocumentChunkRepository chunks;
    private final OpenAiEmbeddingClient embeddingClient;
    private final TextChunker textChunker;
    private final JdbcTemplate jdbcTemplate;

    public DocumentIngestService(
            DocumentRepository documents,
            DocumentChunkRepository chunks,
            OpenAiEmbeddingClient embeddingClient,
            TextChunker textChunker,
            JdbcTemplate jdbcTemplate
    ) {
        this.documents = documents;
        this.chunks = chunks;
        this.embeddingClient = embeddingClient;
        this.textChunker = textChunker;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void ingest(Long documentId) {
        DocumentEntity doc = documents.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("인제스트 대상 문서를 찾을 수 없습니다."));

        try {
            List<String> pieces = textChunker.split(doc.getContent());
            List<DocumentChunkEntity> rows = new ArrayList<>(pieces.size());
            for (int i = 0; i < pieces.size(); i++) {
                String piece = pieces.get(i);
                DocumentChunkEntity chunk = new DocumentChunkEntity();
                chunk.setDocument(doc);
                chunk.setChunkIndex(i);
                chunk.setContent(piece);
                chunk.setCharCount(piece.length());
                rows.add(chunk);
            }

            List<DocumentChunkEntity> savedChunks = chunks.saveAll(rows);
            for (DocumentChunkEntity chunk : savedChunks) {
                List<Double> embedding = embeddingClient.embed(chunk.getContent());
                insertEmbedding(chunk.getId(), embedding, embeddingClient.model());
            }

            doc.setIngestStatus(DocumentIngestStatus.COMPLETED);
            doc.setIngestError(null);
            doc.setIngestedAt(Instant.now());
            documents.save(doc);
        } catch (Exception e) {
            doc.setIngestStatus(DocumentIngestStatus.FAILED);
            doc.setIngestError(e.getMessage());
            documents.save(doc);
            throw e;
        }
    }

    private void insertEmbedding(Long chunkId, List<Double> embedding, String model) {
        String vectorLiteral = toVectorLiteral(embedding);
        jdbcTemplate.update(
                "INSERT INTO chunk_embeddings (chunk_id, embedding, model) VALUES (?, CAST(? AS vector), ?)",
                chunkId,
                vectorLiteral,
                model
        );
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
}

