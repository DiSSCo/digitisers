package eu.dissco.digitisers.utils;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.FileBasedBuilderParameters;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    private final static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static Configuration loadConfigurationFromResourceFile(String filename) throws ConfigurationException {
        URL configFileUrl = Resources.getResource(filename);

        FileBasedBuilderParameters params = new Parameters().fileBased();
        params.setListDelimiterHandler(new DefaultListDelimiterHandler(';'));
        params.setFile(new File(configFileUrl.getPath()));
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class).configure(params);
        return builder.getConfiguration();
    }

    public static Configuration loadConfigurationFromFilePath(String filepath) throws ConfigurationException {
        FileBasedBuilderParameters params = new Parameters().fileBased();
        params.setListDelimiterHandler(new DefaultListDelimiterHandler(';'));
        params.setFile(new File(filepath));
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class).configure(params);
        return builder.getConfiguration();
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
