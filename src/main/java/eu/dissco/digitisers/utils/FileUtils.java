package eu.dissco.digitisers.utils;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    public static Configuration loadConfigurationFromResourceFile(String filename) throws ConfigurationException {
        URL configFileUrl = Resources.getResource(filename);
        Configurations configs = new Configurations();
        Configuration config = configs.properties(configFileUrl);
        return config;
    }

    public static Configuration loadConfigurationFromFilePath(String filepath) throws ConfigurationException {
        Configurations configs = new Configurations();
        Configuration config = configs.properties(filepath);
        return config;
    }

    public static JsonElement loadJsonElementFromResourceFile(String filename) throws IOException, URISyntaxException {
        Gson gson = new Gson();
        URL url = Resources.getResource(filename);
        Path path = Paths.get(url.toURI());

        return gson.fromJson(new FileReader(path.toFile()), JsonElement.class);
    }

    public static JsonElement loadJsonElementFromFilePath(String filepath) throws IOException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(filepath));
        return gson.fromJson(reader, JsonElement.class);
    }
}
