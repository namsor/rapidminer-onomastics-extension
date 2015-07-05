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
public class ParseResponse  implements Serializable {
    private Double score;
    private String title;
    private String nameFormat;
    private String firstName;
    private String lastName;
    private String midName;
    private String countryIso2;
    private String fullName;
    private String id;

    public ParseResponse() {        
    }


    /**
     * @return the score
     */
    @XmlElement
    public Double getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(Double score) {
        this.score = score;
    }

    /**
     * @return the nameFormat
     */
    @XmlElement
    public String getNameFormat() {
        return nameFormat;
    }

    /**
     * @param nameFormat the nameFormat to set
     */
    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
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
    public String getLastName() {
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
    @XmlElement
    public String getCountryIso2() {
        return countryIso2;
    }

    /**
     * @param countryIso2 the countryIso2 to set
     */
    public void setCountryIso2(String countryIso2) {
        this.countryIso2 = countryIso2;
    }

    /**
     * @return the fullName
     */
    @XmlElement
    public String getFullName() {
        return fullName;
    }

    /**
     * @param fullName the fullName to set
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * @return the midName
     */
    @XmlElement
    public String getMidName() {
        return midName;
    }

    /**
     * @param midName the midName to set
     */
    public void setMidName(String midName) {
        this.midName = midName;
    }

    /**
     * @return the title
     */
    @XmlElement
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

        
}
