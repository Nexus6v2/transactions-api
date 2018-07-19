package io.bank.api.transactions.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class Converter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @SneakyThrows
    public static String convertToJson(Object object) {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return OBJECT_MAPPER.writeValueAsString(object);
    }
    
    @SneakyThrows
    public static <T>T convertFromJson(String json, Class<T> klass) {
        return OBJECT_MAPPER.readValue(json, klass);
    }
}
