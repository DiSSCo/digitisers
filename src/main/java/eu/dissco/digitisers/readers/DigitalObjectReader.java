package eu.dissco.digitisers.readers;

import eu.dissco.digitisers.tasks.DigitalObjectVisitor;
import net.dona.doip.client.DigitalObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * DiSSCo digitiser uses the design pattern Visitor as it allows us to process a digital object as soon as it is read
 * and at the same time separates the logic for reading a digital object and the logic for processing it.
 * All DiSSCo digitiser readers should extends this class that enables the specific readers to set the visitor
 */
public abstract class DigitalObjectReader {

    /**************/
    /* ATTRIBUTES */
    /**************/

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DigitalObjectVisitor digitalObjectVisitor;


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }


    /********************/
    /* ABSTRACT METHODS */
    /********************/

    protected DigitalObjectVisitor getDigitalObjectVisitor() {
        return digitalObjectVisitor;
    }

    protected void setDigitalObjectVisitor(DigitalObjectVisitor digitalObjectVisitor) {
        this.digitalObjectVisitor = digitalObjectVisitor;
    }


    public DigitalObjectReader(DigitalObjectVisitor digitalObjectVisitor){
        this.digitalObjectVisitor = digitalObjectVisitor;
    }

    protected DigitalObject processDigitalSpecimen(DigitalObject ds) {
        return this.getDigitalObjectVisitor().visitDigitalSpecimen(ds);
    }
}
