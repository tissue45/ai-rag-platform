package com.astra.backend.document;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

@Component
public class OpenAiEmbeddingClient {

    private final String apiKey;
    private final String baseUrl;
    private final String embeddingModel;
    private WebClient webClient;

    public OpenAiEmbeddingClient(
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.openai.embedding-model:text-embedding-3-small}") String embeddingModel
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    void init() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String model() {
        if (apiKey == null || apiKey.isBlank()) {
            return "local-fallback-1536";
        }
        return embeddingModel;
    }

    public List<Double> embed(String input) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackEmbedding(input);
        }

        EmbeddingResponse response = webClient.post()
                .uri("/embeddings")
                .bodyValue(new EmbeddingRequest(embeddingModel, input))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("임베딩 응답이 비어 있습니다.");
        }
        return response.data().getFirst().embedding();
    }

    private List<Double> fallbackEmbedding(String input) {
        final int dim = 1536;
        byte[] bytes = (input == null ? "" : input).getBytes(StandardCharsets.UTF_8);
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        long seed = crc32.getValue();

        Double[] values = new Double[dim];
        long state = seed == 0 ? 1 : seed;
        for (int i = 0; i < dim; i++) {
            state = (state * 6364136223846793005L + 1442695040888963407L);
            double normalized = ((state >>> 11) & 0xFFFF) / 65535.0;
            values[i] = (normalized * 2.0) - 1.0;
        }
        return List.of(values);
    }

    private record EmbeddingRequest(String model, String input) {}
    private record EmbeddingData(List<Double> embedding) {}
    private record EmbeddingResponse(List<EmbeddingData> data) {}
}

