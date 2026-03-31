package com.astra.backend.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final int chunkSize;
    private final int overlap;

    public TextChunker(
            @Value("${app.rag.chunk-size:800}") int chunkSize,
            @Value("${app.rag.chunk-overlap:120}") int overlap
    ) {
        if (chunkSize < 100) {
            throw new IllegalArgumentException("chunk-size는 100 이상이어야 합니다.");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("chunk-overlap은 0 이상, chunk-size 미만이어야 합니다.");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public List<String> split(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int hardEnd = Math.min(start + chunkSize, normalized.length());
            int end = chooseBoundary(normalized, start, hardEnd);
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end >= normalized.length()) {
                break;
            }

            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private int chooseBoundary(String text, int start, int hardEnd) {
        if (hardEnd == text.length()) {
            return hardEnd;
        }

        int minBoundary = Math.max(start + (int) (chunkSize * 0.6), start + 1);
        for (int i = hardEnd; i >= minBoundary; i--) {
            char c = text.charAt(i - 1);
            if (Character.isWhitespace(c) || c == '\n' || c == '\r') {
                return i;
            }
        }
        return hardEnd;
    }
}

