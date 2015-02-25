package testgrp.srv;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author circlespainter
 */
@XmlRootElement
public class Data {
    String f1;
    String f2;

    /**
     * Default constructor (needed by JAXB/Jackson)
     */
    public Data() {}

    /**
     * Value constructor
     */
    public Data(String f1, String f2) {
        this.f1 = f1;
        this.f2 = f2;
    }

    public String getF2() {
        return f2;
    }

    public String getF1() {
        return f1;
    }
}