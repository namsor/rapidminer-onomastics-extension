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
 * @author ELC201203
 */
@XmlRootElement
public class GenderBatchRequest  implements Serializable {
    private GenderResponse[] names;

    /**
     * @return the names
     */
    @XmlElement
    public GenderResponse[] getNames() {
        return names;
    }

    /**
     * @param names the names to set
     */
    public void setNames(GenderResponse[] names) {
        this.names = names;
    }
}
