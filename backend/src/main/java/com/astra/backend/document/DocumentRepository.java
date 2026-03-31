package com.astra.backend.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
    Optional<DocumentEntity> findByIdAndOwnerEmail(Long id, String ownerEmail);
    List<DocumentEntity> findByIdInAndOwnerEmail(List<Long> ids, String ownerEmail);
}

