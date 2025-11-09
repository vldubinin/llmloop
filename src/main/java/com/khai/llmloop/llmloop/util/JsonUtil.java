package com.khai.llmloop.llmloop.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import java.util.ArrayList;
import java.util.List;

public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchemaGenerator SCHEMA_GENERATOR = new JsonSchemaGenerator(MAPPER);

    public static String getPropertyDescription(Class clazz) {
        try {
            String jsonSchema = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(SCHEMA_GENERATOR.generateSchema(clazz));
            return MAPPER.readValue(jsonSchema, JsonNode.class).get("properties").toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> toJsonList(List objects) throws JsonProcessingException {
        List<String> jsonList = new ArrayList<>();
        for (Object obj : objects) {
            jsonList.add(MAPPER.writeValueAsString(obj));
        }
        return jsonList;
    }

    public static String toJson(Object obj) throws JsonProcessingException {
        return MAPPER.writeValueAsString(obj);
    }

    public static <DataType> DataType toObject(String json, Class<DataType> clazz) {
        try {
            return MAPPER.readValue(sanitizeJson(json), clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static <DataType> DataType toObject(String json, TypeReference<DataType> typeReference) {
        try {
            return MAPPER.readValue(sanitizeJson(json), typeReference);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String sanitizeJson(String json) {
        return json.replace("```json", "");
    }
}
