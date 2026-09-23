package com.hercey.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.security.SecureRandom;

public class AppUtil {

    public static final Gson GSON = new GsonBuilder()

            .registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
                @Override
                public JsonElement serialize(LocalDate date, Type type, JsonSerializationContext jsonSerializationContext) {
                    return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
            })
            .registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
                @Override
                public LocalDate deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                    return LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
                }
            })

            .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public JsonElement serialize(LocalDateTime date, Type type, JsonSerializationContext jsonSerializationContext) {
                    return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
            })
            .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                    return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            })
            .create();

    public static final int DEFAULT_SELECTOR_VALUE = 0;
    public static final int FIRST_RESULT_VALUE = 0;
    public static final int MAX_RESULT_VALUE = 10;
    public static final int DEFAULT_RATING_VALUE = 0;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateCode() {

        int randomNumber = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", randomNumber);

    }

}
