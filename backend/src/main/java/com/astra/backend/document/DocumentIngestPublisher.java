package com.astra.backend.document;

public interface DocumentIngestPublisher {
    void publish(Long documentId);
}
