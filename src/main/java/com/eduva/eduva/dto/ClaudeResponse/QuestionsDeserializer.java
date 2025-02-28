package com.eduva.eduva.dto.ClaudeResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;

public class QuestionsDeserializer
        extends StdDeserializer<List<ClaudeQuestionInfo>> {

    private final ObjectMapper mapper = new ObjectMapper();

    public QuestionsDeserializer() {
        super(List.class);
    }

    @Override
    public List<ClaudeQuestionInfo> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = p.readValueAsTree();

        if (node.isArray()) {
            // Already an array
            return mapper.convertValue(node, new TypeReference<List<ClaudeQuestionInfo>>() {});
        } else if (node.isTextual()) {
            // It's a string; parse it as JSON
            String text = node.asText();
            JsonNode parsedNode = mapper.readTree(text);
            return mapper.convertValue(parsedNode, new TypeReference<List<ClaudeQuestionInfo>>() {});
        }

        // Otherwise handle error case
        throw new JsonParseException(p, "Unexpected format for questions");
    }
}

