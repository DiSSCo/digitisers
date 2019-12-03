package eu.dissco.digitisers.clients.col;

import com.google.gson.JsonObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CoLClientTest {

    private static final Logger logger = LoggerFactory.getLogger(CoLClientTest.class);

    private static CoLClient colClient;

    @BeforeClass
    public static void setup() throws Exception {
        colClient = CoLClient.getInstance();
    }

    @AfterClass
    public static void tearDown() {

    }

    @Test
    public void getTaxonInformation_species() throws Exception {
        String canonicalName="Canis lupus";
        String rank="Species";
        String kingdomName="Animalia";
        JsonObject taxonInfoObj = colClient.getTaxonInformation(canonicalName,rank,kingdomName);
        assertEquals("The author should be ","Linnaeus, 1758",taxonInfoObj.get("author").getAsString());
    }

    @Test
    public void getTaxonInformation_hemihomonym() throws Exception {
        String canonicalName="Agathis montana";
        String rank="Species";
        String kingdomName="Plantae";
        JsonObject taxonInfoObj = colClient.getTaxonInformation(canonicalName,rank,kingdomName);
        assertEquals("The author should be ","de Laub.",taxonInfoObj.get("author").getAsString());
    }

    @Test
    public void getTaxonInformation_nonExist() throws Exception {
        String canonicalName="Fran test";
        String rank="Species";
        String kingdomName="Plantae";
        JsonObject taxonInfoObj = colClient.getTaxonInformation(canonicalName,rank,kingdomName);
        assertNull("The species shouldn't be found in CoL ",taxonInfoObj);
    }

}