package eu.dissco.digitisers.readers;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import eu.dissco.digitisers.processors.DigitalObjectVisitor;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DwcaReader{

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
     * Function that parses the information found in the dwca file using the design pattern "visitor"
     * Note: If there is any row that can not be parsed into a digital specimen, the system will report it in the log
     * file but the function will continue to process the following lines.
     */
    public void readDigitalSpecimensFromDwcaFile(File dwcaFile, DigitalObjectVisitor digitalObjectVisitor){
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
                    this.getLogger().info("File " + dwcaFile.getName() + " Parsing row " + rows + " (core id " + rec.core().id() + ") ...");
                    try{
                        //Only parse the information from the dwc-a, if the current record is for a specimen
                        //and at least has the minimum data required for a digital specimen
                        if (this.checkIfDwcaRecordCanBeParsedAsDigitalSpecimen(rec)){
                            //Read the data of the digital specimen from the dwca record
                            JsonObject dsContent = this.getDigitalSpecimenContentFromDwcaRecord(rec);

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
            this.getLogger().error("Unexpected error parsing dwca-file to digital specimens " + dwcaFile.toURI(),e);
        }
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    /**
     * Function that check if the record in the darwin core file can be parsed into a Digital specimen by checking if
     * it has at least the minimum fields requiered
     * @param rec Record in the darwin core file to check if it can be parsed into a Digital specimen
     * @return true if darwin core record can be parsed, false otherwise
     */
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
            sb.append("Darwin core record (coreID="+rec.core().id() + ") can not be parsed as DS because: ");
            if (!StringUtils.containsIgnoreCase(basisOfRecord,"specimen")) sb.append(" basisOfRecord is not for a specimen");
            if (StringUtils.isBlank(scientificName)) sb.append(" scientificName is blank or can't be found");
            if (StringUtils.isBlank(institutionCode)) sb.append(" institutionCode is blank or can't be found");
            if (StringUtils.isBlank(physicalSpecimenId)) sb.append(" physicalSpecimenId is blank or can't be found");
            this.getLogger().warn(sb.toString());
        }

        return canDwcaRecordBeParsedAsDigitalSpecimen;
    }

    /**
     * Read data from the darwin core record and saved in a jsonObject that will be the content of the digital specimen
     * @param rec Darwin core record
     * @return jsonObject with information obtained from the darwin core record
     */
    private JsonObject getDigitalSpecimenContentFromDwcaRecord(StarRecord rec){
        JsonObject dsContent = new JsonObject();

        //physicalSpecimenId
        this.addPropertyToJsonObject(dsContent,"physicalSpecimenId", this.getPhysicalSpecimenId(rec,false));

        //scientific name.
        this.addPropertyToJsonObject(dsContent,"scientificName",this.getValueFromDwcaRecord(rec, GbifTerm.acceptedScientificName));

        //catalogNumber
        this.addPropertyToJsonObject(dsContent,"catalogNumber",this.getValueFromDwcaRecord(rec,DwcTerm.catalogNumber));

        //otherCatalogNumbers
        this.addPropertyToJsonObject(dsContent,"otherCatalogNumbers",this.getValueFromDwcaRecord(rec,DwcTerm.otherCatalogNumbers));

        //institutionCode
        this.addPropertyToJsonObject(dsContent,"institutionCode",this.getValueFromDwcaRecord(rec,DwcTerm.institutionCode));

        //collectionCode
        this.addPropertyToJsonObject(dsContent,"collectionCode",this.getValueFromDwcaRecord(rec,DwcTerm.collectionCode));

        //recordedBy
        this.addPropertyToJsonObject(dsContent,"recordedBy",this.getValueFromDwcaRecord(rec,DwcTerm.recordedBy));

        //gbifId
        String gbifPrefix="";
        if (rec.core().rowType().prefixedName().equalsIgnoreCase("dwc:Occurrence")){
            gbifPrefix="https://www.gbif.org/occurrence/";
        }
        this.addPropertyToJsonObject(dsContent,"gbifId",gbifPrefix+this.getValueFromDwcaRecord(rec,GbifTerm.gbifID));

        //author reference
        this.addPropertyToJsonObject(dsContent,"authorReference",this.getValueFromDwcaRecord(rec, DwcTerm.scientificNameAuthorship));

        //country code
        String countryCode = this.getValueFromDwcaRecord(rec,DwcTerm.countryCode);
        this.addPropertyToJsonObject(dsContent,"countryCode",countryCode);

        //locality
        this.addPropertyToJsonObject(dsContent,"locality",this.getValueFromDwcaRecord(rec,DwcTerm.locality));

        //latitude and longitude
        String latitude = this.getValueFromDwcaRecord(rec,DwcTerm.decimalLatitude);
        String longitude = this.getValueFromDwcaRecord(rec,DwcTerm.decimalLongitude);
        if (NumberUtils.isCreatable(latitude) && NumberUtils.isCreatable(longitude)){
            JsonArray coordinates = new JsonArray();
            coordinates.add(Double.parseDouble(latitude));
            coordinates.add(Double.parseDouble(longitude));
            dsContent.add("decimalLatLon",coordinates);
        }

        this.addPropertyToJsonObject(dsContent,"collectionDate",this.getValueFromDwcaRecord(rec, DwcTerm.eventDate));

        //commonName
        this.addPropertyToJsonObject(dsContent,"commonName",this.getValueFromDwcaRecord(rec,DwcTerm.vernacularName));

        //literatureReference
        this.addPropertyToJsonObject(dsContent,"literatureReference",this.getValueFromDwcaRecord(rec,DwcTerm.identificationReferences));

        //comment
        this.addPropertyToJsonObject(dsContent,"comment",this.getValueFromDwcaRecord(rec,DwcTerm.fieldNotes));

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

        //TODO: From where to get the following fields. Also review existing ones
        //stableIdentifier
        //interpretations. Should it be an array in the DS schema?
        //annotations. Is it the field "fieldNotes"? Should it be an array in the DS schema?
        //other Keywords. In the schema, what is this field for? Why the name is in 2 words?
        //publications. Is it the DwC-a extensions references? Should it be an array in the DS schema?
        //bhlPages
        //imageID. Why do we have imageID and availableImages

        JsonObject darwinCoreRecordJsonObj = this.getDarwinCoreRecordAsJsonObject(rec);
        dsContent.add("dwcaContent", darwinCoreRecordJsonObj);
        
        return dsContent;
    }

    /**
     * Function that gets the PhysicalSpecimenId to be used for the darwin core record.
     * It tries to read it firstly from identifier, if empty, then look occurrenceID, then catalogNumber,
     * after that otherCatalogNumbers and finally if all the previous ones are empty it tries gbifID
     * @param rec
     * @param logResults
     * @return
     */
    private String getPhysicalSpecimenId(StarRecord rec, boolean logResults){
        String physicalSpecimenId=null;
        String identifier = this.getValueFromDwcaRecord(rec,DcTerm.identifier);
        String occurrenceID = this.getValueFromDwcaRecord(rec,DwcTerm.occurrenceID);
        String catalogNumber = this.getValueFromDwcaRecord(rec,DwcTerm.catalogNumber);
        String otherCatalogNumbers = this.getValueFromDwcaRecord(rec,DwcTerm.otherCatalogNumbers);
        String gbifID = this.getValueFromDwcaRecord(rec,GbifTerm.gbifID);

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
            this.getLogger().warn("PhysicalSpecimenId can not be obtained for this record (coreID="+rec.core().id() + ")");
        }
        if (source!=null && logResults) this.getLogger().info("PhysicalSpecimenId obtained from '"+ source + "' for record (coreID="+rec.core().id() + ")");
        return physicalSpecimenId;
    }

    /**
     * Function that serialize the darwin core record into a Json Object
     * @param rec Darwin core record to be serialized into a json object
     * @return Json object resulting of the serialization of the darwin core record
     */
    private JsonObject getDarwinCoreRecordAsJsonObject(StarRecord rec){
        JsonObject recContentJsonObj = new JsonObject();

        //Load information from core
        JsonObject coreJsonObj = new JsonObject();
        coreJsonObj.addProperty("type",rec.core().rowType().prefixedName());
        JsonObject coreContentJsonObj = new JsonObject();
        Set<Term> terms = rec.core().terms();
        for (Term term:terms) {
            this.addPropertyToJsonObject(coreContentJsonObj,term.prefixedName(),rec.core().value(term));
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
                    this.addPropertyToJsonObject(extensionContentJsonObj,extensionTerm.prefixedName(),extensionRecord.value(extensionTerm));
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

    /**
     * Function that gets the value of a term in the darwin core record.
     * It first try to find it in the core file, if it doesn't exist or if its empty,
     * it tries to get the value from the verbatim file.
     * @param rec Darwin core record to obtain the value of one of its properties
     * @param term Term from which we want to obtain its value
     * @return Value, as string, of the term in the darwin core record, when the term is found and it is not empty, or
     * it returns null otherwise
     */
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

        if (StringUtils.isNotBlank(valueInDwca)) this.getLogger().debug(term.prefixedName() + " read from " + source + " for this record (coreID="+rec.core().id() + ")");

        return valueInDwca;
    }

    /**
     * Function to ad a property to a json object.
     * It will be added if the value is not empty or null
     * @param dsContent Json object on which to add the property
     * @param property name of the property to be added
     * @param value value of the property to be added
     */
    private void addPropertyToJsonObject(JsonObject dsContent,String property, String value){
        if(StringUtils.isNotBlank(value)){
            //Only add to the json object properties that their values are not empty
            dsContent.addProperty(property,value);
        }
    }

}
