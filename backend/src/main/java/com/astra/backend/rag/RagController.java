package com.astra.backend.rag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    public record AskRequest(
            @NotBlank(message = "question은 필수입니다.")
            String question,
            @NotEmpty(message = "documentIds는 최소 1개 이상 필요합니다.")
            List<Long> documentIds
    ) {}

    public record SourceResponse(
            Long documentId,
            String documentTitle,
            Long chunkId,
            Integer chunkIndex,
            String content,
            Double score
    ) {}

    public record AskResponse(
            String answer,
            List<SourceResponse> sources
    ) {}

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest req, Authentication auth) {
        String ownerEmail = currentEmail(auth);
        RagService.AskResult result = ragService.ask(ownerEmail, req.question().trim(), req.documentIds());
        List<SourceResponse> sources = result.sources().stream()
                .map(s -> new SourceResponse(
                        s.documentId(),
                        s.documentTitle(),
                        s.chunkId(),
                        s.chunkIndex(),
                        s.content(),
                        s.score()
                ))
                .toList();
        return new AskResponse(result.answer(), sources);
    }

    private String currentEmail(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return auth.getName();
    }
}

