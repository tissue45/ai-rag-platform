package com.astra.backend.document;

import com.astra.backend.user.UserEntity;
import com.astra.backend.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documents;
    private final UserRepository users;
    private final DocumentIngestService ingestService;

    public DocumentController(DocumentRepository documents, UserRepository users, DocumentIngestService ingestService) {
        this.documents = documents;
        this.users = users;
        this.ingestService = ingestService;
    }

    public record CreateDocumentRequest(
            @NotBlank(message = "title은 필수입니다.")
            @Size(max = 255, message = "title은 255자 이하여야 합니다.")
            String title,

            @NotBlank(message = "content는 필수입니다.")
            String content
    ) {}

    public record DocumentSummaryResponse(
            Long id,
            String title,
            String sourceType,
            String ingestStatus,
            Instant createdAt
    ) {}

    public record DocumentDetailResponse(
            Long id,
            String title,
            String sourceType,
            String ingestStatus,
            String ingestError,
            String content,
            Instant createdAt
    ) {}

    @PostMapping
    public DocumentDetailResponse create(@Valid @RequestBody CreateDocumentRequest req, Authentication auth) {
        UserEntity owner = currentUser(auth);

        DocumentEntity doc = new DocumentEntity();
        doc.setOwner(owner);
        doc.setTitle(req.title().trim());
        doc.setContent(req.content());
        doc.setSourceType(DocumentSourceType.TEXT);
        doc.setIngestStatus(DocumentIngestStatus.PENDING);
        DocumentEntity saved = documents.save(doc);
        try {
            ingestService.ingest(saved.getId());
            saved = documents.findById(saved.getId()).orElse(saved);
        } catch (Exception ignored) {
            saved = documents.findById(saved.getId()).orElse(saved);
        }

        return toDetail(saved);
    }

    @GetMapping
    public List<DocumentSummaryResponse> list(Authentication auth) {
        String email = currentEmail(auth);
        return documents.findByOwnerEmailOrderByCreatedAtDesc(email).stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentDetailResponse getOne(@PathVariable Long id, Authentication auth) {
        String email = currentEmail(auth);
        DocumentEntity doc = documents.findByIdAndOwnerEmail(id, email)
                .orElseThrow(() -> new DocumentNotFoundException("문서를 찾을 수 없습니다."));
        return toDetail(doc);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        String email = currentEmail(auth);
        DocumentEntity doc = documents.findByIdAndOwnerEmail(id, email)
                .orElseThrow(() -> new DocumentNotFoundException("문서를 찾을 수 없습니다."));
        documents.delete(doc);
    }

    private UserEntity currentUser(Authentication auth) {
        String email = currentEmail(auth);
        return users.findByEmail(email).orElseThrow(() -> new IllegalStateException("로그인 사용자를 찾을 수 없습니다."));
    }

    private String currentEmail(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return auth.getName();
    }

    private DocumentSummaryResponse toSummary(DocumentEntity d) {
        return new DocumentSummaryResponse(
                d.getId(),
                d.getTitle(),
                d.getSourceType().name(),
                d.getIngestStatus().name(),
                d.getCreatedAt()
        );
    }

    private DocumentDetailResponse toDetail(DocumentEntity d) {
        return new DocumentDetailResponse(
                d.getId(),
                d.getTitle(),
                d.getSourceType().name(),
                d.getIngestStatus().name(),
                d.getIngestError(),
                d.getContent(),
                d.getCreatedAt()
        );
    }
}

