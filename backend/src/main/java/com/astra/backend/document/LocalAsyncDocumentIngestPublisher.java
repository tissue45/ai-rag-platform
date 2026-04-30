package com.astra.backend.document;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ingest.async.enabled", havingValue = "false", matchIfMissing = true)
public class LocalAsyncDocumentIngestPublisher implements DocumentIngestPublisher {

    private final DocumentIngestService ingestService;
    private final TaskExecutor taskExecutor;

    public LocalAsyncDocumentIngestPublisher(
            DocumentIngestService ingestService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.ingestService = ingestService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void publish(Long documentId) {
        taskExecutor.execute(() -> ingestService.ingest(documentId));
    }
}
