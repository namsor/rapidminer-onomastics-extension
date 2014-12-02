package com.namsor.api.rapidminer;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author ELC201203
 */
@XmlRootElement
public class GeoriginBatchRequest  implements Serializable {
    private GeoriginResponse[] names;

    /**
     * @return the names
     */
    @XmlElement
    public GeoriginResponse[] getNames() {
        return names;
    }

    public GeoriginBatchRequest() {
    }

    /**
     * @param names the names to set
     */
    public void setNames(GeoriginResponse[] names) {
        this.names = names;
    }
}
