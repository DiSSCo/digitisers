package eu.dissco.digitisers.clients.wiki;

import com.google.gson.JsonObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class WikiDataClientTest {

    private static final Logger logger = LoggerFactory.getLogger(WikiDataClientTest.class);

    private static WikiClient wikiClient;

    @BeforeClass
    public static void setup() throws Exception {
        wikiClient = WikiClient.getInstance("wikidata");
    }

    @AfterClass
    public static void tearDown() {

    }

    @Test
    public void getWikiInformation_species_simple_found() throws Exception {
        String canonicalName="Profundiconus profundorum";
        String kingdom="Animalia";
        JsonObject wikiInfo = wikiClient.getWikiInformation(canonicalName,kingdom);
        assertEquals("The wiki page ulr should be ", "http://www.wikidata.org/entity/Q60452413",wikiClient.getPageURL(wikiInfo));
    }

    @Test
    public void getWikiInformation_species_hemihomonym_found() throws Exception {
        String canonicalName="Agathis montana";
        String kingdom="Plantae";
        JsonObject wikiInfoInPlants = wikiClient.getWikiInformation(canonicalName,kingdom);
        assertEquals("The wiki page ulr should be ", "http://www.wikidata.org/entity/Q2599422",wikiClient.getPageURL(wikiInfoInPlants));

        kingdom="Animalia";
        JsonObject wikiInfoInAnimals = wikiClient.getWikiInformation(canonicalName,kingdom);
        assertEquals("The wiki page ulr should be ", "http://www.wikidata.org/entity/Q17405678",wikiClient.getPageURL(wikiInfoInAnimals));
    }

    @Test
    public void getWikiInformation_species_notFound() throws Exception {
        String canonicalName="Fran test";
        String kingdom="Plantae";
        JsonObject wikiInfo = wikiClient.getWikiInformation(canonicalName,kingdom);
        assertNull("The wiki page shouldn't exist", wikiInfo);
    }
}