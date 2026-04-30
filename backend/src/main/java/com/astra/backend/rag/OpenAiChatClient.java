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
                너는 문서 기반 RAG 어시스턴트다.

                [목표]
                - 사용자의 질문에 대해 제공된 sources를 최우선 근거로 한국어로 답한다.
                - sources에 명확한 근거가 있으면 그 범위 안에서 정확하게 답한다.
                - sources만으로 부족한 경우, 일반 지식으로 보완할 수 있다.
                  단, 이때는 반드시 "문서 근거 외 일반 정보"라고 명시한다.
                - 근거가 불충분하면 단정하지 말고, 필요한 추가 정보를 짧게 요청한다.
                - 사용자가 "기존 내용 외", "추가할 내용", "추가 정보"를 요청하면
                  source 요약을 반복하지 말고 새로운 보완 정보 위주로 답한다.
                - 특히 개수 요청(예: 3개)이 있으면 개수를 맞춰 항목형으로 제시한다.

                [출력 형식]
                - "답변" 본문만 출력한다.
                - "근거 요약", "sources", "문서 근거 외 일반 정보" 같은 섹션 제목은 출력하지 않는다.
                - 사용자가 "기존 내용 외"를 요청하면 기존 source 문장 반복을 최소화하고
                  새로운 정보 중심으로 바로 항목을 제시한다.

                [작성 규칙]
                - 과장/추측/허위 사실 금지
                - 숫자/스펙은 source에 있을 때만 단정
                - source와 충돌하는 일반 지식은 쓰지 않음
                - 너무 길지 않게, 실무적으로 바로 쓸 수 있게 작성
                - 사용자가 "기존 내용 외"를 요청한 경우, source에 이미 있는 문장을 반복하지 않는다.

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

