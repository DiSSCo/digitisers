package eu.dissco.digitisers.processors.enrichers;

import com.google.gson.JsonElement;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;

public abstract class Enricher implements Callable<Map<String, Object>> {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DigitalObject ds;
    private Configuration config;


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }

    public DigitalObject getDs() {
        return ds;
    }

    public void setDs(DigitalObject ds) {
        this.ds = ds;
    }

    public Configuration getConfig() {
        return config;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }


    /****************/
    /* CONSTRUCTORS */
    /****************/

    public Enricher(DigitalObject ds, Configuration config){
        this.ds = ds;
        this.config = config;
    }


    /********************/
    /* ABSTRACT METHODS */
    /********************/

    protected abstract Map<String,Object> enrichData();


    /******************/
    /* PUBLIC METHODS */
    /******************/

    @Override
    public Map<String,Object> call() throws Exception {
        return this.enrichData();
    }
}
