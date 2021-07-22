/**
 * This file Copyright (c) 2010-2015 Magnolia International
 * Ltd.  (http://www.magnolia-cms.com). All rights reserved.
 *
 *
 * This file is dual-licensed under both the Magnolia
 * Network Agreement and the GNU General Public License.
 * You may elect to use one or the other of these licenses.
 *
 * This file is distributed in the hope that it will be
 * useful, but AS-IS and WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, or NONINFRINGEMENT.
 * Redistribution, except as permitted by whichever of the GPL
 * or MNA you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or
 * modify this file under the terms of the GNU General
 * Public License, Version 3, as published by the Free Software
 * Foundation.  You should have received a copy of the GNU
 * General Public License, Version 3 along with this program;
 * if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * 2. For the Magnolia Network Agreement (MNA), this file
 * and the accompanying materials are made available under the
 * terms of the MNA which accompanies this distribution, and
 * is available at http://www.magnolia-cms.com/mna.html
 *
 * Any modifications to this file must keep this entire header
 * intact.
 *
 */
package ch.fastforward.magnolia.ocm.ext;

import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;

import ch.fastforward.magnolia.ocm.OcmModule;
import ch.fastforward.magnolia.ocm.beans.ProxyClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.DescriptorReader;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;

/**
 * Allows OCM class descriptors to be stored in the Magnolia config tree instead
 * of the standard mechanisms (XML and annotations).
 * @author will
 */
public class MgnlConfigDescriptorReader implements DescriptorReader {

    private final OcmModule ocmModule;

    @Inject
    public MgnlConfigDescriptorReader(OcmModule ocmModule) {
        this.ocmModule = ocmModule;
    }

    /**
     * Turns the Collection of ClassDescriptor objects stored in the CRUDModule
     * into a MappingDescriptor.
     * 
     * @return MappingDescritor object containing the ClassDescriptors stored
     * in config:/modules/ocm/config/classDescriptors
     */
    public MappingDescriptor loadClassDescriptors() {
        MappingDescriptor mappingDescriptor = new MappingDescriptor();
        Collection<ProxyClassDescriptor> descriptors = ocmModule.getClassDescriptors();
        if (descriptors == null) throw new IllegalStateException("No class descriptors configured or not bootstraped yet. Check module load order.");
        Iterator<ProxyClassDescriptor> classDescriptors = ocmModule.getClassDescriptors().iterator();
        while (classDescriptors.hasNext()) {
            mappingDescriptor.addClassDescriptor(classDescriptors.next().asClassDescriptor());
        }
        return mappingDescriptor;
    }
}
