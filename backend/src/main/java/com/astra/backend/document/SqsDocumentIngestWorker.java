package com.astra.backend.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ingest.async.enabled", havingValue = "true")
public class SqsDocumentIngestWorker {

    private final SqsClient sqsClient;
    private final DocumentIngestService ingestService;
    private final String queueUrl;
    private final int maxMessages;
    private final int waitSeconds;

    public SqsDocumentIngestWorker(
            SqsClient sqsClient,
            DocumentIngestService ingestService,
            @Value("${app.ingest.sqs.queue-url}") String queueUrl,
            @Value("${app.ingest.sqs.max-messages:5}") int maxMessages,
            @Value("${app.ingest.sqs.wait-seconds:10}") int waitSeconds
    ) {
        this.sqsClient = sqsClient;
        this.ingestService = ingestService;
        this.queueUrl = queueUrl;
        this.maxMessages = maxMessages;
        this.waitSeconds = waitSeconds;
    }

    @Scheduled(fixedDelayString = "${app.ingest.sqs.poll-delay-ms:1500}")
    public void poll() {
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(maxMessages)
                .waitTimeSeconds(waitSeconds)
                .build()).messages();

        for (Message message : messages) {
            if (processSafely(message)) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        }
    }

    private boolean processSafely(Message message) {
        try {
            Long documentId = Long.parseLong(message.body());
            ingestService.ingest(documentId);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
