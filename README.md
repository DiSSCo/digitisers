# Disco Digitiser #

## Version 1.0.0 ##

This java project provides a simple mechanism to create Digital Specimens in the Disco Repository

## 1. Getting Started

### 1.1. Generate the jar file of the project with dependencies
mvn package

### 1.2. Create a config properties file to be used during the execution
Use the file src/test/resources/config_template.properties as template to create your own config.properties file to be 
used during the execution

### 1.3 Run the program with the desired parameters
<pre><code>
java -jar digitisers-1.0-jar-with-dependencies.jar -c <b>PATH_TO_CONFIGURATION_FILE</b> -m <b>METHOD</b> <b>[ADDITIONAL PARAMETERS ACCORDING TO DIGITISER METHOD SELECTED]</b>```
</code></pre>

#### Dwca digitiser:
* **Single DwC-A File**: 
<pre><code>
java -jar digitisers-1.0-jar-with-dependencies.jar -c <b>PATH_TO_CONFIGURATION_FILE</b> -m dwca -f <b>PATH_TO_DWCA_FILE</b>
</code></pre> 

* **Directory with DwC-A Files**: 
<pre><code>
java -jar digitisers-1.0-jar-with-dependencies.jar -c <b>PATH_TO_CONFIGURATION_FILE</b> -m dwca -d <b>PATH_TO_DIRECTORY_WITH_DWCA_FILES</b>
</code></pre> 

* **DwC-A URL**: 
<pre><code>
java -jar digitisers-1.0-jar-with-dependencies.jar -c <b>PATH_TO_CONFIGURATION_FILE</b> -m dwca -u <b>DWCA_FILES_URL</b>
</code></pre>

 #### GBIF digitiser:
<pre><code>
java -jar digitisers-1.0-jar-with-dependencies.jar -c <b>PATH_TO_CONFIGURATION_FILE</b> -m gbif -n <b>CANONICAL_NAME</b> -k <b>KINGDOM</b>
</code></pre>


## 2. Features to be added in the future
* Support different dwc-a formats. Currently the system is only working with dwc-a files obtained from downloaded 
occurrence data from GBIF. Add support for dwc-a obtained from IPT (https://ipt.gbif.org/) and from NHMUK (https://data.nhm.ac.uk/dataset/collection-specimens/)
* Review fields used to populate the Digital Specimen's content from the DwC-a file (core vs verbatim, fields not clear, etc.) 
* Review how to calculate MIDS level