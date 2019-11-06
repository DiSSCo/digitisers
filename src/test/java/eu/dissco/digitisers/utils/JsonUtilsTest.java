package eu.dissco.digitisers.utils;

import com.google.common.collect.MapDifference;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class JsonUtilsTest {

    private final static Logger logger = LoggerFactory.getLogger(JsonUtilsTest.class);

    @Test
    public void compareJsonObjects_different() {
        String leftJson = "{\n" +
                "  \"name\": {\n" +
                "    \"first\": \"John\",\n" +
                "    \"last\": \"Doe\"\n" +
                "  },\n" +
                "  \"address\": null,\n" +
                "  \"birthday\": \"1980-01-01\",\n" +
                "  \"company\": \"Acme\",\n" +
                "  \"occupation\": \"Software engineer\",\n" +
                "  \"phones\": [\n" +
                "    {\n" +
                "      \"number\": \"000000000\",\n" +
                "      \"type\": \"home\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"number\": \"999999999\",\n" +
                "      \"type\": \"mobile\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String rightJson = "{\n" +
                "  \"name\": {\n" +
                "    \"first\": \"Jane\",\n" +
                "    \"last\": \"Doe\",\n" +
                "    \"nickname\": \"Jenny\"\n" +
                "  },\n" +
                "  \"birthday\": \"1990-01-01\",\n" +
                "  \"occupation\": null,\n" +
                "  \"phones\": [\n" +
                "    {\n" +
                "      \"number\": \"111111111\",\n" +
                "      \"type\": \"mobile\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"favorite\": true,\n" +
                "  \"groups\": [\n" +
                "    \"close-friends\",\n" +
                "    \"gym\"\n" +
                "  ]\n" +
                "}";

        Gson gson = new Gson();
        JsonElement leftJsonElem = gson.fromJson(leftJson,JsonElement.class);
        JsonElement rightJsonElem = gson.fromJson(rightJson,JsonElement.class);
        MapDifference<String, Object> difference = JsonUtils.compareJsonElements(leftJsonElem,rightJsonElem);
        assertFalse("Json elements should be different", difference.areEqual());

        System.out.println("Entries only on the left\n--------------------------");
        difference.entriesOnlyOnLeft()
                .forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("\n\nEntries only on the right\n--------------------------");
        difference.entriesOnlyOnRight()
                .forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("\n\nEntries differing\n--------------------------");
        difference.entriesDiffering()
                .forEach((key, value) -> System.out.println(key + ": " + value));
    }

    @Test
    public void compareJsonObjects_same() {
        String leftJson = "{\n" +
                "  \"name\": {\n" +
                "    \"first\": \"John\",\n" +
                "    \"last\": \"Doe\"\n" +
                "  },\n" +
                "  \"address\": null,\n" +
                "  \"birthday\": \"1980-01-01\",\n" +
                "  \"company\": \"Acme\",\n" +
                "  \"occupation\": \"Software engineer\",\n" +
                "  \"phones\": [\n" +
                "    {\n" +
                "      \"number\": \"000000000\",\n" +
                "      \"type\": \"home\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"number\": \"999999999\",\n" +
                "      \"type\": \"mobile\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String rightJson = leftJson;
        Gson gson = new Gson();
        JsonElement leftJsonElem = gson.fromJson(leftJson,JsonElement.class);
        JsonElement rightJsonElem = gson.fromJson(rightJson,JsonElement.class);
        MapDifference<String, Object> difference = JsonUtils.compareJsonElements(leftJsonElem,rightJsonElem);
        assertTrue("Json elements should be equal", difference.areEqual());

        System.out.println("Entries only on the left\n--------------------------");
        difference.entriesOnlyOnLeft()
                .forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("\n\nEntries only on the right\n--------------------------");
        difference.entriesOnlyOnRight()
                .forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("\n\nEntries differing\n--------------------------");
        difference.entriesDiffering()
                .forEach((key, value) -> System.out.println(key + ": " + value));
    }
}