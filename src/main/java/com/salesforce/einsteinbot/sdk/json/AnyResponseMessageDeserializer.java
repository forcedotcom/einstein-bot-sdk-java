package com.salesforce.einsteinbot.sdk.json;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class AnyResponseMessageDeserializer<T> extends StdDeserializer<T> {

    private final static String KEY_DELIMITER = ":";
    public static final String TYPE_FIELD_NAME = "type";
    public static final String MESSAGE_TYPE_FIELD_NAME = "messageType";
    public static final String ERROR_MESSAGE = "Invalid type or message type value. Please check value of @JsonSubTypes annotation in AnyResponseMessage.";
    LinkedHashMap<String, ? extends Class<?>> typeMessageTypeToSubTypeMapping;

    public AnyResponseMessageDeserializer(Class<T> clazz) {
        super(clazz);
        typeMessageTypeToSubTypeMapping = Arrays
                .stream(clazz.getAnnotation(JsonSubTypes.class).value())
                .collect(Collectors.toMap(this::getTypeNameKey, JsonSubTypes.Type::value,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private String getTypeNameKey(JsonSubTypes.Type type) {
        return Arrays.stream(type.names()).collect(Collectors.joining(KEY_DELIMITER));
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        ObjectMapper objectMapper = (ObjectMapper) p.getCodec();
        ObjectNode object = objectMapper.readTree(p);

        Optional<String> typeValue =  Optional.of(object.get(TYPE_FIELD_NAME))
                .map(v -> v.asText());
        Optional<String> messageTypeValue = Optional.of(object.get(MESSAGE_TYPE_FIELD_NAME))
                .map(v -> v.asText());
        return deserialize(objectMapper, typeValue, messageTypeValue, object);
    }

    @SuppressWarnings("unchecked")
    private T deserialize(ObjectMapper objectMapper,
                          Optional<String> typeValue,
                          Optional<String> messageTypeValue,
                          ObjectNode object) throws IOException {

        typeValue.orElseThrow(() -> new IllegalArgumentException(ERROR_MESSAGE));

        String keyName = messageTypeValue.isPresent() ?
                String.format("%s%s%s", typeValue, KEY_DELIMITER, messageTypeValue.get()): typeValue.get();

        if (typeMessageTypeToSubTypeMapping.containsKey(keyName)){
            return (T) objectMapper.treeToValue(object, typeMessageTypeToSubTypeMapping.get(keyName));
        }
        throw new IllegalArgumentException(ERROR_MESSAGE);
    }
}