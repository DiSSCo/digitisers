package eu.dissco.digitisers.readers;

import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import eu.dissco.digitisers.processors.DigitalObjectVisitor;
import eu.dissco.digitisers.utils.FileUtils;
import net.dona.doip.client.DigitalObject;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bh2020MappingReaderTest {

    private final static Logger logger = LoggerFactory.getLogger(Bh2020MappingReaderTest.class);

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
    public void testParseContentBh2020DemoMappings() throws Exception {
        String bh2020MappingFilePath = "bh2020/test_mapping.json";
        String dwcaFilePath = "GBIF_DwC-a/small/bh2020DemoMappingOccurrencesIds.zip";

        File dwcaFile = new File(Resources.getResource(dwcaFilePath).toURI());

        Map<String, List<String>> mapGbifIdsEnaIds = new HashMap<>();
        JsonArray bh2020GbifEnaMappingsObj = (JsonArray) FileUtils.loadJsonElementFromFilePath(Resources.getResource(bh2020MappingFilePath).getPath());
        for (JsonElement bh2020GbifEnaMappingObj : bh2020GbifEnaMappingsObj) {
            String gbifId = bh2020GbifEnaMappingObj.getAsJsonObject().get("gbifID").getAsString();
            JsonArray enaIdsObj =  bh2020GbifEnaMappingObj.getAsJsonObject().get("enaIDs").getAsJsonArray();
            List<String> enaIds = new ArrayList<>();
            for (JsonElement enaIdObj : enaIdsObj) {
                enaIds.add(enaIdObj.getAsString());
            }
            mapGbifIdsEnaIds.put(gbifId,enaIds);
        }

        Bh2020MappingReader bh2020MappingReader = new Bh2020MappingReader();
        bh2020MappingReader.readDigitalSpecimensFromBh2020MappingFile(mapGbifIdsEnaIds,dwcaFile,digitalObjectVisitor);
    }

}