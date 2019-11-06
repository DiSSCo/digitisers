package eu.dissco.digitisers;

import com.google.common.io.Resources;
import net.dona.doip.client.DigitalObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@Ignore
public class GbifDigitiserTest {

    private final static Logger logger = LoggerFactory.getLogger(GbifDigitiserTest.class);

    private static GbifDigitiser digitiser;

    @BeforeClass
    public static void init() throws Exception {
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        digitiser = new GbifDigitiser(configPropertiesFilePath);
    }

    @AfterClass
    public static void setup() {
        digitiser.getDigitalObjectRepositoryClient().close();
    }

    @Test
    public void testImportDataToRepository_scientificName() throws Exception{
        String canonicalName = "Agathis montana";
        String kingdom = "Plantae";

        List<DigitalObject> listDsSaved = digitiser.digitiseDigitalSpecimensByCanonicalNameAndKindgdom(canonicalName,kingdom);
        assertNotNull("The importation should completed without raising any unhandled exception",listDsSaved);
    }
}