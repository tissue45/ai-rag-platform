package com.astra.backend.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@ConditionalOnProperty(name = "app.ingest.async.enabled", havingValue = "true")
public class SqsDocumentIngestPublisher implements DocumentIngestPublisher {

    private final SqsClient sqsClient;
    private final String queueUrl;

    public SqsDocumentIngestPublisher(
            SqsClient sqsClient,
            @Value("${app.ingest.sqs.queue-url}") String queueUrl
    ) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publish(Long documentId) {
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(String.valueOf(documentId))
                .build());
    }
}
