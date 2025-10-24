package com.campusmarket.messaging;

import com.campusmarket.messaging.schema.SchemaValidationUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

class OrderEventSchemaTest {

    @Test
    void orderEventMatchesSchema() throws IOException {
        OrderEvent event = new OrderEvent();
        event.setEventType(OrderEventType.ORDER_CREATED);
        event.setOrderId(1L);
        event.setGoodsId(2L);
        event.setBuyerId(3L);
        event.setCurrentStatus("PENDING_PAYMENT");
        event.setPreviousStatus(null);
        event.setNote("integration-test");
        event.setEventTime(LocalDateTime.now());

        SchemaValidationUtil.assertValid(
                "/schemas/order-event-schema.json",
                event
        );
    }
}
