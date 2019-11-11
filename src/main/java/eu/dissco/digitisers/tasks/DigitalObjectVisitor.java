package eu.dissco.digitisers.tasks;

import net.dona.doip.client.DigitalObject;

public interface DigitalObjectVisitor {

    /**
     * Function that has the logic of what to do when visiting a digital specimen
     * @param ds
     * @return Digital object returned as result of the visit
     */
    public DigitalObject visitDigitalSpecimen(DigitalObject ds);


    /**
     * Release any resource taken by the visitor
     */
    public void close();
}
