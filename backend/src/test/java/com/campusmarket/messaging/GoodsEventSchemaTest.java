package com.campusmarket.messaging;

import com.campusmarket.messaging.schema.SchemaValidationUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

class GoodsEventSchemaTest {

    @Test
    void goodsEventMatchesSchema() throws IOException {
        GoodsEvent event = new GoodsEvent(
                GoodsEventType.GOODS_UPDATED,
                10L,
                20L,
                30L,
                "APPROVED",
                5,
                "integration-test",
                LocalDateTime.now()
        );

        SchemaValidationUtil.assertValid(
                "/schemas/goods-event-schema.json",
                event
        );
    }
}
