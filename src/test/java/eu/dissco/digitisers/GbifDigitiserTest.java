package eu.dissco.digitisers;

import com.google.common.io.Resources;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class GbifDigitiserTest {

    private final static Logger logger = LoggerFactory.getLogger(GbifDigitiserTest.class);

    private static GbifDigitiser digitiser;

    @BeforeClass
    public static void setup() throws Exception {
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        digitiser = new GbifDigitiser(configPropertiesFilePath);
    }

    @AfterClass
    public static void tearDown() {

    }

    @Before
    public void init() {
    }

    @After
    public void finalize() {
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testImportDataToRepository_scientificName() throws Exception{
        String canonicalName = "Agathis montana";
        String kingdom = "Plantae";
        digitiser.digitiseDigitalSpecimensByCanonicalNameAndKingdom(canonicalName,kingdom);
    }
}