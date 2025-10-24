package com.campusmarket.messaging;

import com.campusmarket.kafka.KafkaIntegrationTestBase;
import com.campusmarket.service.ChatCacheService;
import com.campusmarket.service.ChatMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ChatEventPublisherIT extends KafkaIntegrationTestBase {

    @Autowired
    private ChatEventPublisher chatEventPublisher;

    @MockBean
    private ChatMetricsService chatMetricsService;

    @MockBean
    private ChatCacheService chatCacheService;

    @Test
    void publishMessageCreated_shouldUpdateMetrics() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(chatMetricsService).recordMessageCreated(100L, 200L);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(chatCacheService).incrementUnread(200L, 100L, 500L);

        chatEventPublisher.publishMessageCreated(500L, 100L, 200L, "TEXT", "hello preview");

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        verify(chatMetricsService).recordMessageCreated(100L, 200L);
        verify(chatCacheService).incrementUnread(200L, 100L, 500L);
    }
}
