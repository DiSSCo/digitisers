package eu.dissco.digitisers.clients.misc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class CountryClientTest {

    private static final Logger logger = LoggerFactory.getLogger(CountryClientTest.class);

    private static CountryClient countryClient;

    @BeforeClass
    public static void init() throws Exception {
        countryClient = CountryClient.getInstance();
    }

    @AfterClass
    public static void setup() {

    }

    @Test
    public void getCountryByCountryCode_valid() {
        String countryCode = "es";
        String country = countryClient.getCountryNameByCountryCode(countryCode);
        assertEquals("The country should be ", "Spain",country);
    }

    @Test
    public void getCountryByCountryCode_invalid(){
        String countryCode = "zz";
        String country = countryClient.getCountryNameByCountryCode(countryCode);
        assertNull("The country shouldn't exist", country);
    }

}