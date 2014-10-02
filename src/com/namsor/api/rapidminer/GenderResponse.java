/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.namsor.api.rapidminer;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;

/**
 * Gender Result
 * @author ELC201203
 */
public class GenderResponse  implements Serializable {
    private Double scale;
    private String gender;
    private String firstName;
    private String lastName;
    private String countryIso2;
    private String id;

    public GenderResponse() {        
    }


    /**
     * @return the scale
     */
    @XmlElement
    public Double getScale() {
        return scale;
    }

    /**
     * @param scale the scale to set
     */
    public void setScale(Double scale) {
        this.scale = scale;
    }

    /**
     * @return the gender
     */
    @XmlElement
    public String getGender() {
        return gender;
    }

    /**
     * @param gender the gender to set
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * @return the firstName
     */
    @XmlElement
    public String getFirstName() {
        return firstName;
    }

    /**
     * @param firstName the firstName to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * @return the lastName
     */
    @XmlElement
    public String getLastname() {
        return lastName;
    }

    /**
     * @param lastname the lastName to set
     */
    public void setLastname(String lastname) {
        this.lastName = lastname;
    }

    /**
     * @return the id
     */
    @XmlElement
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the countryIso2
     */
    public String getCountryIso2() {
        return countryIso2;
    }

    /**
     * @param countryIso2 the countryIso2 to set
     */
    public void setCountryIso2(String countryIso2) {
        this.countryIso2 = countryIso2;
    }

        
}
