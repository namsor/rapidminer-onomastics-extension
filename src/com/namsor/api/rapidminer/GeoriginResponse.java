package com.namsor.api.rapidminer;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author elian
 */
public class GeoriginResponse implements Serializable {

    private String firstName;
    private String lastName;
    private String id;
    private String country;
    private String countryAlt;
    private Double score;
    private String script;
    private String countryFirstName;
    private String countryLastName;
    private Double scoreFirstName;
    private Double scoreLastName;
    private String subRegion;
    private String region;
    private String topRegion;
    private String countryName;

    public GeoriginResponse() {
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
     * @param lastName the lastName to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
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
     * @return the country
     */
    @XmlElement
    public String getCountry() {
        return country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @return the countryAlt
     */
    @XmlElement
    public String getCountryAlt() {
        return countryAlt;
    }

    /**
     * @param countryAlt the countryAlt to set
     */
    public void setCountryAlt(String countryAlt) {
        this.countryAlt = countryAlt;
    }

    /**
     * @return the score
     */
    @XmlElement
    public double getScore() {
        return score;
    }

    /**
     * @param scoreInConfidenceLn the score to set
     */
    public void setScore(double score) {
        this.score = score;
    }

    /**
     * @return the script
     */
    @XmlElement
    public String getScript() {
        return script;
    }

    /**
     * @param script the script to set
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * @return the countryFirstName
     */
    @XmlElement
    public String getCountryFirstName() {
        return countryFirstName;
    }

    /**
     * @param countryFirstName the countryFirstName to set
     */
    public void setCountryFirstName(String countryFirstName) {
        this.countryFirstName = countryFirstName;
    }

    /**
     * @return the countryLastName
     */
    @XmlElement
    public String getCountryLastName() {
        return countryLastName;
    }

    /**
     * @param countryLastName the countryLastName to set
     */
    public void setCountryLastName(String countryLastName) {
        this.countryLastName = countryLastName;
    }

    /**
     * @return the scoreFirstName
     */
    @XmlElement
    public Double getScoreFirstName() {
        return scoreFirstName;
    }

    /**
     * @param scoreFirstName the scoreFirstName to set
     */
    public void setScoreFirstName(Double scoreFirstName) {
        this.scoreFirstName = scoreFirstName;
    }

    /**
     * @return the scoreLastName
     */
    @XmlElement
    public Double getScoreLastName() {
        return scoreLastName;
    }

    /**
     * @param scoreLastName the scoreLastName to set
     */
    public void setScoreLastName(Double scoreLastName) {
        this.scoreLastName = scoreLastName;
    }

    /**
     * @return the subRegion
     */
    @XmlElement
    public String getSubRegion() {
        return subRegion;
    }

    /**
     * @param subRegion the subRegion to set
     */
    public void setSubRegion(String subRegion) {
        this.subRegion = subRegion;
    }

    /**
     * @return the region
     */
    @XmlElement
    public String getRegion() {
        return region;
    }

    /**
     * @param region the region to set
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * @return the topRegion
     */
    @XmlElement
    public String getTopRegion() {
        return topRegion;
    }

    /**
     * @param topRegion the topRegion to set
     */
    public void setTopRegion(String topRegion) {
        this.topRegion = topRegion;
    }

    /**
     * @return the countryName
     */
    @XmlElement
    public String getCountryName() {
        return countryName;
    }

    /**
     * @param countryName the countryName to set
     */
    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

}
