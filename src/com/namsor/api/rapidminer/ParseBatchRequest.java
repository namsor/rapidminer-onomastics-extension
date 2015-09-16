/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.namsor.api.rapidminer;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author elian
 */
@XmlRootElement
public class ParseBatchRequest  implements Serializable {
    private ParseResponse[] names;

    /**
     * @return the names
     */
    @XmlElement
    public ParseResponse[] getNames() {
        return names;
    }

    public ParseBatchRequest() {
    }

    /**
     * @param names the names to set
     */
    public void setNames(ParseResponse[] names) {
        this.names = names;
    }

    private String nameFormatTip;

    /**
     * Get the value of nameFormatTip
     *
     * @return the value of nameFormatTip
     */
    @XmlElement
    public String getNameFormatTip() {
        return nameFormatTip;
    }

    /**
     * Set the value of nameFormatTip
     *
     * @param nameFormatTip new value of nameFormatTip
     */
    public void setNameFormatTip(String nameFormatTip) {
        this.nameFormatTip = nameFormatTip;
    }

    private String countryIso2Default;

    /**
     * @return the countryIso2
     */
    @XmlElement
    public String getCountryIso2Default() {
        return countryIso2Default;
    }

    /**
     * @param countryIso2 the countryIso2 to set
     */
    public void setCountryIso2Default(String countryIso2Default) {
        this.countryIso2Default = countryIso2Default;
    }    
}
