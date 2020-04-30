package eu.dissco.digitisers.readers;

import com.google.common.io.Resources;
import eu.dissco.digitisers.processors.DigitalObjectVisitor;
import net.dona.doip.client.DigitalObject;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DwcaReaderTest {

    private final static Logger logger = LoggerFactory.getLogger(DwcaReaderTest.class);

    private static DigitalObjectVisitor digitalObjectVisitor;

    private long testStartTime,testEndTime;
    private int numDsParsed;

    @BeforeClass
    public static void setup() throws Exception {
        digitalObjectVisitor = new DigitalObjectVisitor() {
            private final Logger logger = LoggerFactory.getLogger(this.getClass());
            @Override
            public DigitalObject visitDigitalSpecimen(DigitalObject ds) {
                logger.debug("Digital Specimen read: " +  ds);
                return null;
            }
        };
    }

    @AfterClass
    public static void tearDown() {
    }


    @Before
    public void init() {
        this.testStartTime = System.nanoTime();
    }

    @After
    public void finalize() {
        this.testEndTime = System.nanoTime();
        long timeElapsed = this.testEndTime - this.testStartTime;
        if (this.numDsParsed >0) logger.info("Average time per record " + (timeElapsed/1000000)/this.numDsParsed + " miliseconds ");
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testParseContentDwcaToDigitalSpecimens_small() throws Exception {
        String dwcaFilePath = "GBIF_DwC-a/small/0031773-190918142434337_Agathis_montana.zip";
        File dwcaFile = new File(Resources.getResource(dwcaFilePath).toURI());

        DwcaReader dwcaReader = new DwcaReader();
        dwcaReader.readDigitalSpecimensFromDwcaFile(dwcaFile,digitalObjectVisitor);
    }

}