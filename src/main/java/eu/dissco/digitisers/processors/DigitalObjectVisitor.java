package eu.dissco.digitisers.processors;

import net.dona.doip.client.DigitalObject;

/***
 * DiSSCo digitiser uses the Visitor design pattern as it allows us to process a digital object as soon as it is read
 * and at the same time separates the logic for reading a digital object and the logic for processing it.
 */
public interface DigitalObjectVisitor {

    /**
     * Function that has the logic for what to do when visiting a digital specimen
     * @param ds Digital specimen to be visited
     * @return Digital object as a result of visiting the digital specimen
     */
    public DigitalObject visitDigitalSpecimen(DigitalObject ds);
}
