/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.fastforward.magnolia.ocm.ext;

import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;

/**
 * If you want to use a MgnlUUIDListCollectionConverter and your node contains a
 * list of uuids of nodes from an other repository, you need to specify the 
 * repository name of the target nodes so the converter class knows where to
 * look for the nodes.
 * 
 * @author will
 */
public class MgnlTargetRepositoryAwareCollectionDescriptor extends CollectionDescriptor {
    private String targetRepositoryName;

    /**
     * @return the targetRepositoryName
     */
    public String getTargetRepositoryName() {
        return targetRepositoryName;
    }

    /**
     * @param targetRepositoryName the targetRepositoryName to set
     */
    public void setTargetRepositoryName(String targetRepositoryName) {
        this.targetRepositoryName = targetRepositoryName;
    }
    
}
