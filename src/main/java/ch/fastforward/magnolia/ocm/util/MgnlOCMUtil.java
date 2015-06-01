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
package ch.fastforward.magnolia.ocm.util;

import info.magnolia.cms.security.AccessDeniedException;
import info.magnolia.context.MgnlContext;
import info.magnolia.context.SystemContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;

/**
 * A (currently very small) collection of useful methods when using OCM.
 * @author will
 */
public class MgnlOCMUtil {

    private static Logger log = LoggerFactory.getLogger(MgnlOCMUtil.class);
    private static final String CONFIG_REPO = "config";

    public static Node getClassDescriptorNode(Object object) {
        if (object != null) {
            return getClassDescriptorNode(object.getClass().getName(), MgnlContext.getSystemContext());
        } else {
            return null;
        }
    }

    public static Node getClassDescriptorNode(String className, SystemContext sysCtx) {
        try {
            String queryString = "SELECT * FROM mgnl:contentNode WHERE jcr:path LIKE '/modules/ocm/config/classDescriptors/%' AND className = '" + className + "'";
            QueryManager qm = sysCtx.getJCRSession(CONFIG_REPO).getWorkspace().getQueryManager();
            NodeIterator nodesIter = qm.createQuery(queryString, "sql").execute().getNodes();
            if (nodesIter.hasNext()) {
                return nodesIter.nextNode();
            } else {
                log.error("No class descriptor node found for query \"" + queryString + "\"");
            }
        } catch (AccessDeniedException ex) {
            log.error("Could not read or update nextBeanID in classDescripter node for class " + className, ex);
        } catch (RepositoryException ex) {
            log.error("Could not read or update nextBeanID in classDescripter node for class " + className, ex);
        }
        return null;
    }
}
