package eu.dissco.digitisers.readers;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.dissco.digitisers.processors.DigitalObjectVisitor;
import net.dona.doip.client.DigitalObject;
import org.gbif.dwc.Archive;
import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.GbifTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Bh2020MappingReader extends DwcaReader {

    /**************/
    /* ATTRIBUTES */
    /**************/

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /***
     * Function that parses the information found in the BH 2020 mapping file using the design pattern "visitor"
     * Note: If there is any row that can not be parsed into a digital specimen, the system will report it in the log
     * file but the function will continue to process the following lines.
     */
    public void readDigitalSpecimensFromBh2020MappingFile(Map<String, List<String>> mapGbifIdsEnaIds, File dwcaFile, DigitalObjectVisitor digitalObjectVisitor){
        try{
            this.getLogger().info("Parsing bh2020 mapping associated dwca file  " + dwcaFile.toURI() + " into digital specimens ");

            Path archiveFile = Paths.get(dwcaFile.toURI());
            File tempDir = Files.createTempDir();
            Path extractToFolder = Paths.get(tempDir.getPath());

            Archive dwcArchive = DwcFiles.fromCompressed(archiveFile, extractToFolder);

            //Currently we only process dwc-a files that its core file is Occurrences
            if (dwcArchive.getCore().getRowType().prefixedName().equalsIgnoreCase("dwc:Occurrence")){
                int rows = 1;
                //Iterate through the records in the dwc-a
                for (StarRecord rec : dwcArchive) {
                    this.getLogger().info("File " + dwcaFile.getName() + " Parsing row " + rows + " (core id " + rec.core().id() + ") ...");
                    try{
                        if (mapGbifIdsEnaIds.containsKey(this.getValueFromDwcaRecord(rec,GbifTerm.gbifID))){
                            //Only parse the information from the dwc-a, if the current record is for a specimen
                            //and at least has the minimum data required for a digital specimen
                            if (this.checkIfDwcaRecordCanBeParsedAsDigitalSpecimen(rec)){
                                //Read the data of the digital specimen from the dwca record
                                JsonObject dsContent = this.getDigitalSpecimenContentFromDwcaRecord(rec);

                                JsonArray enaIdsObj = new JsonArray();
                                List<String> enaIds = mapGbifIdsEnaIds.get(this.getValueFromDwcaRecord(rec,GbifTerm.gbifID));
                                for (String enaId:enaIds) {
                                    enaIdsObj.add(enaId);
                                }
                                JsonObject bh2020MappingsObj = new JsonObject();
                                bh2020MappingsObj.add("enaIds",enaIdsObj);
                                dsContent.add("bh2020Mappings",bh2020MappingsObj);

                                //Create object for digital specimen
                                DigitalObject ds = new DigitalObject();
                                ds.type = "DigitalSpecimen";
                                ds.setAttribute("content", dsContent);

                                this.getLogger().debug("File " + dwcaFile.getName() + " Row " + rows + " (core id " + rec.core().id() + ") has been serialized correctly into a Digital Specimen");

                                //Call the visitor to process the digital object
                                DigitalObject dsSaved = digitalObjectVisitor.visitDigitalSpecimen(ds);
                                if (dsSaved!=null) this.getLogger().debug("File " + dwcaFile.getName() + " Row " + rows + " (core id " + rec.core().id() + ") has been saved correctly in the repository");
                            } else{
                                this.getLogger().warn("File " + dwcaFile.getName() + " Row " + rows + " (core id " + rec.core().id() + ") hasn't been serialized into a Digital Specimen" );
                            }
                        }
                    } catch (Exception e){
                        this.getLogger().error("File " + dwcaFile.getName() + " Unexpected error parsing row " + rows,e);
                    }
                    rows++;
                }
                this.getLogger().info("Dwc-A file " + dwcaFile.toURI() + " parsed. Result: " + (rows-1) + " row(s) were found in the core file.");
            } else{
                this.getLogger().error("File " + dwcaFile.getName() + " Only dwca files that its core file is Occurrences can be processed into digital specimens");
            }

        }catch (Exception e){
            this.getLogger().error("Unexpected error parsing bh2020 mapping file to digital specimens " + dwcaFile.toURI(),e);
        }
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/


}
