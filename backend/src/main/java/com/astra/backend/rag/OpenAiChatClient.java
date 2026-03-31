package com.astra.backend.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class OpenAiChatClient {

    private final String apiKey;
    private final String baseUrl;
    private final String chatModel;
    private WebClient webClient;

    public OpenAiChatClient(
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.openai.chat-model:gpt-4o-mini}") String chatModel
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.chatModel = chatModel;
    }

    @PostConstruct
    void init() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String answer(String question, List<RagService.SourceItem> sources) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackAnswer(question, sources);
        }

        String sourceContext = sources.stream()
                .map(s -> "- [" + s.documentTitle() + " #" + s.chunkIndex() + "] " + s.content())
                .reduce("", (a, b) -> a + "\n" + b);

        String prompt = """
                너는 RAG 어시스턴트다.
                아래 source에 근거해서만 한국어로 답변하라.
                모르면 모른다고 답하라.

                질문:
                %s

                sources:
                %s
                """.formatted(question, sourceContext);

        ChatResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(new ChatRequest(chatModel, List.of(new Message("user", prompt)), 0.2))
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("챗 응답이 비어 있습니다.");
        }
        return response.choices().getFirst().message().content();
    }

    private String fallbackAnswer(String question, List<RagService.SourceItem> sources) {
        if (sources.isEmpty()) {
            return "관련 근거를 찾지 못했습니다. 질문을 더 구체적으로 입력해 주세요.";
        }
        String joined = sources.stream()
                .limit(3)
                .map(s -> "[" + s.documentTitle() + "] " + s.content())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        return "질문: " + question + "\n\n관련 근거 요약:\n" + joined;
    }

    private record Message(String role, String content) {}
    private record ChatRequest(String model, List<Message> messages, double temperature) {}
    private record ChatChoice(Message message) {}
    private record ChatResponse(List<ChatChoice> choices) {}
}

