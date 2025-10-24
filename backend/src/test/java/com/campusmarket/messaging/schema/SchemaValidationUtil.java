package com.campusmarket.messaging.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public final class SchemaValidationUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMA_FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    private SchemaValidationUtil() {
    }

    static {
        OBJECT_MAPPER.findAndRegisterModules();
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static void assertValid(String schemaPath, Object payload) throws IOException {
        try (InputStream schemaStream =
                     SchemaValidationUtil.class.getResourceAsStream(schemaPath)) {
            if (schemaStream == null) {
                throw new IllegalArgumentException("Schema not found: " + schemaPath);
            }
            JsonSchema schema = SCHEMA_FACTORY.getSchema(schemaStream);
            JsonNode node = OBJECT_MAPPER.valueToTree(payload);
            Set<ValidationMessage> validationResult = schema.validate(node);
            if (!validationResult.isEmpty()) {
                throw new AssertionError("Schema validation failed: " + validationResult);
            }
        }
    }
}
