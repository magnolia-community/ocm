/**
 * This file Copyright (c) 2010-2011 Magnolia International
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
package ch.fastforward.magnolia.ocm.beans;

import ch.fastforward.magnolia.ocm.util.MgnlOCMUtil;
import info.magnolia.cms.core.Content;
import info.magnolia.cms.security.AccessDeniedException;
import info.magnolia.cms.util.NodeDataUtil;
import javax.jcr.RepositoryException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author will
 */
public class OCMNumberedBean extends OCMBean {

    private Long number = null;
    private static Logger log = LoggerFactory.getLogger(OCMNumberedBean.class);

    @Override
    public String getName() {
        if (number == null) {
            // Get the next number from the configuration
            // At this point simply get it form the class descriptor config node.
            // Later we should have the possibility to use a custom class
            // descriptor object which contains things like this.
            Content classDescNode = MgnlOCMUtil.getClassDescriptorNode(this);
            if (classDescNode != null) {
                Long nextBeanID;
                if (classDescNode.getNodeData("nextBeanID").isExist()) {
                    nextBeanID = classDescNode.getNodeData("nextBeanID").getLong();
                    try {
                        NodeDataUtil.getOrCreateAndSet(classDescNode, "nextBeanID", nextBeanID + 1);
                        classDescNode.updateMetaData();
                        classDescNode.getParent().save();
                        number = nextBeanID;
                        return nextBeanID.toString();
                    } catch (AccessDeniedException ex) {
                        log.error("Could not access class descriptor node at " + classDescNode.getHandle(), ex);
                    } catch (RepositoryException ex) {
                        log.error("Could not access class descriptor node at " + classDescNode.getHandle(), ex);
                    }
                } else {
                    log.error("The class descriptor node at " + classDescNode.getHandle() + " does not have a nextBeanID property.");
                }
            }
        } else {
            return number.toString();
        }
        return super.getName();
    }

    /**
     * @param name the name to set
     */
    @Override
    public void setName(String name) {
        if (StringUtils.isNotBlank(name)) {
            try {
                this.number = new Long(name);
            } catch (NumberFormatException nfe) {
                log.debug("Illegal name for " + this.getClass().getName() + " object");
            }
        }
    }
}
