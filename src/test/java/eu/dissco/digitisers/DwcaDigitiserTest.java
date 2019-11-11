package eu.dissco.digitisers;

import com.google.common.io.Resources;
import net.dona.doip.client.DigitalObject;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

import static org.junit.Assert.*;

@Ignore
public class DwcaDigitiserTest {

    private final static Logger logger = LoggerFactory.getLogger(DwcaDigitiserTest.class);
    private static DwcaDigitiser digitiser;


    @BeforeClass
    public static void setup() throws Exception {
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        digitiser = new DwcaDigitiser(configPropertiesFilePath);
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
    public void testImportDwcaFileToDigitalSpecimens_file_small() throws Exception{
        String dwcaFilePath = Resources.getResource("GBIF_DwC-a/small/0022116-190918142434337_Profundiconus_profundorum.zip").getPath();
        digitiser.digitiseDigitalSpecimensFromDwcaFile(dwcaFilePath);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testImportDwcaFileToDigitalSpecimens_file_big() throws Exception{
        String dwcaFilePath = Resources.getResource("GBIF_DwC-a/big/0029199-190918142434337_Canis_lupus.zip").getPath();
        digitiser.digitiseDigitalSpecimensFromDwcaFile(dwcaFilePath);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testImportDwcaFileToDigitalSpecimens_folder() throws Exception{
        String folderPath = Resources.getResource("GBIF_DwC-a/small").getPath();
        digitiser.digitiseDigitalSpecimensFromFolder(folderPath);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testImportDwcaFileToDigitalSpecimens_url() throws Exception{
        String sDwcaURL = "https://www.gbif.org/occurrence/download/0032443-190918142434337";
        digitiser.digitiseDigitalSpecimensFromUrl(sDwcaURL);
    }

}
