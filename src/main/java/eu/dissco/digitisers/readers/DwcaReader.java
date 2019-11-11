package eu.dissco.digitisers.readers;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import eu.dissco.digitisers.tasks.DigitalObjectVisitor;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.gbif.dwc.Archive;
import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DwcaReader extends DigitalObjectReader {

    /****************/
    /* CONSTRUCTORS */
    /****************/

    public DwcaReader(DigitalObjectVisitor digitalObjectVisitor){
        super(digitalObjectVisitor);
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /***
     * Function that parses the information found in the dwca file using the design pattern "visitor"
     * Note: If there is any row that can not be parsed into a digital specimen, the system will report it in the log
     * file but the function will continue to process the following lines.
     * @return Number of digital specimens parsed from the information found in the dwca file
     */
    public int readDigitalSpecimensFromDwcaFile(File dwcaFile){
        int numDsSerialized=0;
        int numDsProcessed=0;
        try{
            this.getLogger().info("Parsing Dwc-A file " + dwcaFile.toURI() + " into digital specimens ");

            Path archiveFile = Paths.get(dwcaFile.toURI());
            File tempDir = Files.createTempDir();
            Path extractToFolder = Paths.get(tempDir.getPath());

            Archive dwcArchive = DwcFiles.fromCompressed(archiveFile, extractToFolder);

            //Currently we only process dwc-a files that its core file is Occurrences
            if (dwcArchive.getCore().getRowType().prefixedName().equalsIgnoreCase("dwc:Occurrence")){
                int rows = 1;
                //Iterate through the records in the dwc-a
                for (StarRecord rec : dwcArchive) {
                    this.getLogger().info("Parsing row " + rows + " (core id " + rec.core().id() + ") ...");
                    try{
                        //Only parse the information from the dwc-a, if the current record is for a specimen
                        //and at least has the minimum data required for a digital specimen
                        if (checkIfDwcaRecordCanBeParsedAsDigitalSpecimen(rec)){
                            //Read the data of the digital specimen from the dwca record
                            JsonObject dsContent = getDigitalSpecimenContentFromDwcaRecord(rec);

                            //Create object for digital specimen
                            DigitalObject ds = new DigitalObject();
                            ds.type = "DigitalSpecimen";
                            ds.setAttribute("content", dsContent);

                            numDsSerialized = numDsSerialized + 1;
                            this.getLogger().debug("Row " + rows + " (core id " + rec.core().id() + ") has been serialized correctly into a Digital Specimen");

                            //Call function that use the visitor to process the digital object
                            DigitalObject dsProcessed = this.processDigitalSpecimen(ds);
                            if (dsProcessed!=null){
                                numDsProcessed = numDsProcessed + 1;
                                this.getLogger().info("Digital specimen for row " + rows + " (core id " + rec.core().id() + ") has been processed correctly");
                            } else{
                                this.getLogger().warn("Digital specimen for row " + rows + " (core id " + rec.core().id() + ") has NOT been processed correctly");
                            }
                        } else{
                            this.getLogger().warn("Row " + rows + " (core id " + rec.core().id() + ") hasn't been serialized into a Digital Specimen" );
                        }
                    } catch (Exception e){
                        this.getLogger().error("Unexpected error parsing row " + rows,e);
                    }
                    rows++;
                }
                this.getLogger().info("Dwc-A file " + dwcaFile.toURI() + " parsed. Result: " + (rows-1) + " row(s) were found in " +
                        "the core file. " + numDsSerialized + " of those rows were serialized as Digital Specimens and " +
                        numDsProcessed + " of those Digital Speciemens were processed correctly.");
            } else{
                this.getLogger().error("Only dwca files that its core file is Occurrences can be processed into digital specimens");
            }
        }catch (Exception e){
            this.getLogger().error("Unexpected error parsing dwca-file to digital specimens " + dwcaFile.toURI(),e);
        }
        return numDsSerialized;
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    private boolean checkIfDwcaRecordCanBeParsedAsDigitalSpecimen(StarRecord rec){
        //Check if the dwca record is for a specimen (living, preserved or fossil) and it has enough information to populate a Digital Specimen object
        String basisOfRecord=this.getValueFromDwcaRecord(rec,DwcTerm.basisOfRecord);
        String scientificName=this.getValueFromDwcaRecord(rec,GbifTerm.acceptedScientificName);
        String physicalSpecimenId=this.getPhysicalSpecimenId(rec,true);
        String institutionCode=this.getValueFromDwcaRecord(rec,DwcTerm.institutionCode);

        boolean canDwcaRecordBeParsedAsDigitalSpecimen=StringUtils.containsIgnoreCase(basisOfRecord,"specimen") &&
                StringUtils.isNotBlank(scientificName) && StringUtils.isNotBlank(physicalSpecimenId) &&
                StringUtils.isNotBlank(institutionCode);

        if (!canDwcaRecordBeParsedAsDigitalSpecimen){
            StringBuilder sb = new StringBuilder();
            sb.append("Darwin core record can not be parsed as DS because: ");
            if (!StringUtils.containsIgnoreCase(basisOfRecord,"specimen")) sb.append(" basisOfRecord is not for a specimen");
            if (StringUtils.isBlank(scientificName)) sb.append(" scientificName is blank or can't be found");
            if (StringUtils.isBlank(institutionCode)) sb.append(" institutionCode is blank or can't be found");
            if (StringUtils.isBlank(physicalSpecimenId)) sb.append(" physicalSpecimenId is blank or can't be found");
            this.getLogger().warn(sb.toString());
        }

        return canDwcaRecordBeParsedAsDigitalSpecimen;
    }

    private JsonObject getDigitalSpecimenContentFromDwcaRecord(StarRecord rec){
        JsonObject dsContent = new JsonObject();

        //physicalSpecimenId
        addPropertyToJsonObject(dsContent,"physicalSpecimenId", this.getPhysicalSpecimenId(rec,false));

        //scientific name.
        addPropertyToJsonObject(dsContent,"scientificName",this.getValueFromDwcaRecord(rec, GbifTerm.acceptedScientificName));

        //catalogNumber
        addPropertyToJsonObject(dsContent,"catalogNumber",this.getValueFromDwcaRecord(rec,DwcTerm.catalogNumber));

        //otherCatalogNumbers
        addPropertyToJsonObject(dsContent,"otherCatalogNumbers",this.getValueFromDwcaRecord(rec,DwcTerm.otherCatalogNumbers));

        //institutionCode
        addPropertyToJsonObject(dsContent,"institutionCode",this.getValueFromDwcaRecord(rec,DwcTerm.institutionCode));

        //collectionCode
        addPropertyToJsonObject(dsContent,"collectionCode",this.getValueFromDwcaRecord(rec,DwcTerm.collectionCode));

        //recordedBy
        addPropertyToJsonObject(dsContent,"recordedBy",this.getValueFromDwcaRecord(rec,DwcTerm.recordedBy));

        //gbifId
        String gbifPrefix="";
        if (rec.core().rowType().prefixedName().equalsIgnoreCase("dwc:Occurrence")){
            gbifPrefix="https://www.gbif.org/occurrence/";
        }
        addPropertyToJsonObject(dsContent,"gbifId",gbifPrefix+getValueFromDwcaRecord(rec,GbifTerm.gbifID));

        //author reference
        addPropertyToJsonObject(dsContent,"authorReference",this.getValueFromDwcaRecord(rec, DwcTerm.scientificNameAuthorship));

        //country code
        String countryCode = getValueFromDwcaRecord(rec,DwcTerm.countryCode);
        addPropertyToJsonObject(dsContent,"countryCode",countryCode);

        //locality
        addPropertyToJsonObject(dsContent,"locality",this.getValueFromDwcaRecord(rec,DwcTerm.locality));

        //latitude and longitude
        String latitude = getValueFromDwcaRecord(rec,DwcTerm.decimalLatitude);
        String longitude = getValueFromDwcaRecord(rec,DwcTerm.decimalLongitude);
        if (NumberUtils.isCreatable(latitude) && NumberUtils.isCreatable(longitude)){
            JsonArray coordinates = new JsonArray();
            coordinates.add(Double.parseDouble(latitude));
            coordinates.add(Double.parseDouble(longitude));
            dsContent.add("decimalLatLon",coordinates);
        }

        //Collection date. TODO: Is the eventDate field?
        addPropertyToJsonObject(dsContent,"collectionDate",this.getValueFromDwcaRecord(rec, DwcTerm.eventDate));

        //commonName TODO: what to do if dwc-a has extension vernacular names. Should it be an array in the schema?
        addPropertyToJsonObject(dsContent,"commonName",this.getValueFromDwcaRecord(rec,DwcTerm.vernacularName));

        //literatureReference TODO: is the identificationReferences field or is it references? In the schema, should it be an array?
        addPropertyToJsonObject(dsContent,"literatureReference",this.getValueFromDwcaRecord(rec,DwcTerm.identificationReferences));

        //comment. TODO: is it field fieldNotes
        addPropertyToJsonObject(dsContent,"comment",this.getValueFromDwcaRecord(rec,DwcTerm.fieldNotes));

        //availableImages from multimedia extension?
        if(rec.hasExtension(GbifTerm.Multimedia)){
            JsonArray images = new JsonArray();
            for (Record extRec : rec.extension(GbifTerm.Multimedia)) {
                JsonArray image = new JsonArray();
                //Creator
                image.add(extRec.value(DcTerm.creator));

                //Format
                image.add(extRec.value(DcTerm.format));

                //Resolution. There isn't a field in the multimedia
                image.add(JsonNull.INSTANCE);

                //rightsHolder
                image.add(extRec.value(DcTerm.rightsHolder));

                //rights
                image.add(extRec.value(DcTerm.rights));

                //Image
                image.add(extRec.value(DcTerm.identifier));

                images.add(image);
            }
            if (images.size()>0){
                dsContent.add("availableImages",images);
            }
        }

        //TODO: From where to get the following fields
        //stableIdentifier
        //interpretations. Should it be an array in the DS schema?
        //annotations. Is it the field "fieldNotes"? Should it be an array in the DS schema?
        //other Keywords. In the schema, what is this field for? Why the name is in 2 words?
        //publications. Is it the DwC-a extensions references? Should it be an array in the DS schema?
        //bhlPages
        //imageID. Why do we have imageID and availableImages

        //TODO: do we want to add extra data in the dwca?
        JsonObject darwinCoreRecordJsonObj = this.getDarwinCoreRecordAsJsonObject(rec);
        dsContent.add("dwcaContent", darwinCoreRecordJsonObj);
        
        return dsContent;
    }


    private String getPhysicalSpecimenId(StarRecord rec, boolean logResults){
        String physicalSpecimenId=null;
        String identifier = getValueFromDwcaRecord(rec,DcTerm.identifier);
        String occurrenceID = getValueFromDwcaRecord(rec,DwcTerm.occurrenceID);
        String catalogNumber = getValueFromDwcaRecord(rec,DwcTerm.catalogNumber);
        String otherCatalogNumbers = getValueFromDwcaRecord(rec,DwcTerm.otherCatalogNumbers);
        String gbifID = getValueFromDwcaRecord(rec,GbifTerm.gbifID);

        String source=null;
        if (StringUtils.isNotBlank(identifier)){
            physicalSpecimenId=identifier;
            source="identifier";
        } else if(StringUtils.isNotBlank(occurrenceID)){
            physicalSpecimenId=occurrenceID;
            source="occurrenceID";
        } else if(StringUtils.isNotBlank(catalogNumber)){
            physicalSpecimenId=catalogNumber;
            source="catalogNumber";
        } else if(StringUtils.isNotBlank(otherCatalogNumbers)){
            physicalSpecimenId=otherCatalogNumbers;
            source="otherCatalogNumbers";
        } else if(StringUtils.isNotBlank(gbifID)){
            physicalSpecimenId=gbifID;
            source="gbifID";
        } else{
            this.getLogger().warn("PhysicalSpecimenId can not be obtained for this record");
        }
        if (source!=null && logResults) this.getLogger().info("PhysicalSpecimenId obtained from '"+ source + "'");
        return physicalSpecimenId;
    }

    private JsonObject getDarwinCoreRecordAsJsonObject(StarRecord rec){
        JsonObject recContentJsonObj = new JsonObject();

        //Load information from core
        JsonObject coreJsonObj = new JsonObject();
        coreJsonObj.addProperty("type",rec.core().rowType().prefixedName());
        JsonObject coreContentJsonObj = new JsonObject();
        Set<Term> terms = rec.core().terms();
        for (Term term:terms) {
            addPropertyToJsonObject(coreContentJsonObj,term.prefixedName(),rec.core().value(term));
        }
        coreJsonObj.add("content",coreContentJsonObj);
        recContentJsonObj.add("core",coreJsonObj);

        //Load information from extensions
        JsonArray extensionsJsonArray = new JsonArray();
        Map<Term, List<Record>> extensions = rec.extensions();
        for (Term term:extensions.keySet()) {
            JsonArray extensionContentJsonArr = new JsonArray();
            List<Record> extensionRecords = extensions.get(term);
            for (Record extensionRecord:extensionRecords) {
                JsonObject extensionContentJsonObj = new JsonObject();
                Set<Term> extensionTerms = extensionRecord.terms();
                for (Term extensionTerm:extensionTerms) {
                    addPropertyToJsonObject(extensionContentJsonObj,extensionTerm.prefixedName(),extensionRecord.value(extensionTerm));
                }
                extensionContentJsonArr.add(extensionContentJsonObj);
            }
            if (extensionContentJsonArr.size()>0){
                JsonObject extensionJsonObj = new JsonObject();
                extensionJsonObj.addProperty("type",term.prefixedName());
                extensionJsonObj.add("content",extensionContentJsonArr);
                extensionsJsonArray.add(extensionJsonObj);
            }
        }

        if (extensionsJsonArray.size()>0){
            recContentJsonObj.add("extensions",extensionsJsonArray);
        }

        return recContentJsonObj;
    }

    private String getValueFromDwcaRecord(StarRecord rec, Term term){
        String valueInDwca=null;
        String valueInCore = rec.core().value(term);
        String source = null;
        if((StringUtils.isBlank(valueInCore)) && rec.hasExtension(DwcTerm.Occurrence)){
            //If valueInCore is not defined try to get it from verbatim file (relation in our case should be 1 to 1 with core)
            for (Record extRec : rec.extension(DwcTerm.Occurrence)) {
                valueInDwca=extRec.value(term);
                source= "extension: " + extRec.rowType().prefixedName();
                break;
            }
        } else {
            valueInDwca=valueInCore;
            source = "core: " + rec.core().rowType().prefixedName();
        }

        if (StringUtils.isNotBlank(valueInDwca)) this.getLogger().debug(term.prefixedName() + " read from " + source);

        return valueInDwca;
    }

    private void addPropertyToJsonObject(JsonObject dsContent,String property, String value){
        if(StringUtils.isNotBlank(value)){
            //Only add to the json object properties that their values are not empty
            dsContent.addProperty(property,value);
        }
    }


}
