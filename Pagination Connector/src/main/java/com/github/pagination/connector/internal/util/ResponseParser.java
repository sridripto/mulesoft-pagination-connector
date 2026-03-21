package com.github.pagination.connector.internal.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseParser.class);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final XmlMapper XML_MAPPER = new XmlMapper();
    private static final Pattern LINK_NEXT_PATTERN =
            Pattern.compile("<([^>]+)>\\s*;[^,]*rel=\"next\"", Pattern.CASE_INSENSITIVE);

    private ResponseParser() {}


    private static boolean isXml(String payload) {
        if (payload == null) return false;
        String trimmed = payload.stripLeading();
        return trimmed.startsWith("<");
    }

    public static JsonNode parsePayload(String payload) {
        if (payload == null || payload.isBlank()) return JSON_MAPPER.missingNode();
        try {
            if (isXml(payload)) {
                return XML_MAPPER.readTree(payload);
            }
            return JSON_MAPPER.readTree(payload);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse payload: {}", e.getMessage());
            return JSON_MAPPER.missingNode();
        }
    }

    public static JsonNode resolveField(JsonNode root, String fieldPath) {
        if (root == null) return JSON_MAPPER.missingNode();
        if (fieldPath == null || fieldPath.isBlank()) return root;

        JsonNode current = root;
        for (String segment : fieldPath.split("\\.")) {
            if (current == null || current.isMissingNode()) break;
            if (current.isArray()) {
                try {
                    current = current.get(Integer.parseInt(segment));
                } catch (NumberFormatException e) {
                    current = JSON_MAPPER.missingNode();
                }
            } else {
                current = current.path(segment);
            }
        }
        return current == null ? JSON_MAPPER.missingNode() : current;
    }

    public static List<Object> extractRecords(String payload, String dataFieldPath) {
        if (payload == null || payload.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = parsePayload(payload);
            JsonNode dataNode = resolveField(root, dataFieldPath);

            if (dataNode.isArray()) {
                List<Object> records = new ArrayList<>();
                for (JsonNode item : dataNode) {
                    records.add(JSON_MAPPER.treeToValue(item, Object.class));
                }
                return records;
            } else if (dataNode.isMissingNode() || dataNode.isNull()) {
                LOGGER.debug("Data field '{}' not found or null in response", dataFieldPath);
                return Collections.emptyList();
            } else {
                return Collections.singletonList(JSON_MAPPER.treeToValue(dataNode, Object.class));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to extract records: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public static Long extractTotalCount(String payload, String totalCountFieldPath) {
        if (totalCountFieldPath == null || totalCountFieldPath.isBlank()) return null;
        if (payload == null || payload.isBlank()) return null;
        try {
            JsonNode root = parsePayload(payload);
            JsonNode countNode = resolveField(root, totalCountFieldPath);
            if (countNode.isNumber()) return countNode.asLong();
            if (countNode.isTextual()) {
                try { return Long.parseLong(countNode.asText()); } catch (NumberFormatException ignored) {}
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to extract total count: {}", e.getMessage());
        }
        return null;
    }

    public static String extractCursor(String payload, String cursorResponseFieldPath) {
        if (cursorResponseFieldPath == null || cursorResponseFieldPath.isBlank()) return null;
        if (payload == null || payload.isBlank()) return null;
        try {
            JsonNode root = parsePayload(payload);
            JsonNode cursorNode = resolveField(root, cursorResponseFieldPath);
            if (cursorNode.isNull() || cursorNode.isMissingNode()) return null;
            String cursor = cursorNode.asText(null);
            return (cursor == null || cursor.isBlank() || "null".equalsIgnoreCase(cursor)) ? null : cursor;
        } catch (Exception e) {
            LOGGER.warn("Failed to extract cursor: {}", e.getMessage());
            return null;
        }
    }

    public static String extractNextLinkFromHeader(String linkHeaderValue) {
        if (linkHeaderValue == null || linkHeaderValue.isBlank()) return null;
        Matcher matcher = LINK_NEXT_PATTERN.matcher(linkHeaderValue);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    public static boolean isEmpty(List<Object> records) {
        return records == null || records.isEmpty();
    }
}