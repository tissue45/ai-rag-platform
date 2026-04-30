package com.astra.backend.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, Long> {
    @Modifying
    @Query("delete from DocumentChunkEntity c where c.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);
}

