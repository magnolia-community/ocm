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

import info.magnolia.cms.core.Content;
import ch.fastforward.magnolia.ocm.util.MgnlOCMUtil;
import java.io.Serializable;
import java.util.Stack;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ObjectConverterImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.repository.NodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of the standard ObjectConverter with the additional feature that it
 * will try to create the parent node structure on inserts.
 *
 * @author will
 */
public class MgnlObjectConverterImpl extends ObjectConverterImpl implements Serializable {

    private static Logger log = LoggerFactory.getLogger(MgnlObjectConverterImpl.class);

    public MgnlObjectConverterImpl(Mapper mapper, DefaultAtomicTypeConverterProvider converterProvider, ProxyManagerImpl proxyManagerImpl, ObjectCache requestObjectCache) {
        super(mapper, converterProvider, proxyManagerImpl, requestObjectCache);
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Works exactly as the method in the superclass except that it will try to
     * create missing parent nodes if a parentJcrType is provided in the class
     * descriptor.
     * @param session
     * @param object
     * @see org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter#insert(javax.jcr.Session,
     *      java.lang.Object)
     */
    @Override
    public void insert(Session session, Object object) {
        String path = this.getPath(session, object);
        String parentPath = NodeUtil.getParentPath(path);
        String nodeName = NodeUtil.getNodeName(path);
        Node parentNode = null;
        try {
            parentNode = (Node) session.getItem(parentPath);
        } catch (PathNotFoundException pnfe) {
            log.debug("Parent node at " + parentPath + " not found. Trying to create it...");
            // Look for parentJcrType in the class descriptor
            Content classDescNode = MgnlOCMUtil.getClassDescriptorNode(object);
            if (classDescNode == null) {
                throw new ObjectContentManagerException("Impossible to insert the object at '" + path + "'", pnfe);
            } else if (!classDescNode.getNodeData("parentJcrType").isExist()) {
                throw new ObjectContentManagerException("Impossible to insert the object at '" + path + "'."
                        + " Parent node does not exist and no parentJcrType is specified in classDescriptor");
            } else {
                // Get the closest existing parent node. Start at the branches to
                // minimize the danger of access problems.
                String currParentPath = parentPath;
                String currName = StringUtils.substringAfterLast(currParentPath, "/");
                Stack<String> pathElements = new Stack();
                while (parentNode == null && parentPath.contains("/") && parentPath.length() > 1) {
                    pathElements.push(currName);
                    currParentPath = StringUtils.substringBeforeLast(currParentPath, "/");
                    if (currParentPath.length() == 0) {
                        currParentPath = "/";
                    }
                    currName = StringUtils.substringAfterLast(currParentPath, "/");
                    try {
                        parentNode = (Node) session.getItem(currParentPath);
                    } catch (PathNotFoundException pnfe2) {
                        // do nothing, just try again with the parent node
                    } catch (AccessDeniedException ade) {
                        throw new ObjectContentManagerException("Impossible to insert the object at '" + path + "'."
                                + " Access denied on parent node at " + currParentPath, ade);
                    } catch (RepositoryException re) {
                        throw new ObjectContentManagerException("Impossible to insert the object at '" + path + "'."
                                + " RepositoryException at " + currParentPath, re);
                    }
                }
                if (parentNode != null) {
                    // now create all the missing parent nodes
                    String parentJcrType = classDescNode.getNodeData("parentJcrType").getString();
                    while (!pathElements.isEmpty()) {
                        try {
                            currName = pathElements.pop();
                            parentNode = parentNode.addNode(currName, parentJcrType);
                        } catch (NoSuchNodeTypeException ex) {
                            log.error("Could not create parent node at " + parentNode + " with name " + currName, ex);
                        } catch (ItemExistsException ex) {
                            log.error("Could not create parent node at " + parentNode + " with name " + currName, ex);
                        } catch (PathNotFoundException ex) {
                            log.error("Could not create parent node at " + parentNode + " with name " + currName, ex);
                        } catch (LockException ex) {
                            log.error("Could not create parent node at " + parentNode + " with name " + currName, ex);
                        } catch (VersionException ex) {
                            log.error("Could not create parent node at " + parentNode + " with name " + currName, ex);
                        } catch (ConstraintViolationException ex) {
                            log.error("Could not create parent node at " + parentNode + " with name " + currName, ex);
                        } catch (RepositoryException ex) {
                            log.error("Could not create parent node at " + parentNode + " with name " + currName, ex);
                        }
                    }
                } else {
                    // could not even find the root node?!
                    throw new ObjectContentManagerException("Impossible to insert the object at '" + path + "'."
                            + " Parent nodes do not exist.");
                }
            }
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to insert the object at '" + path
                    + "'", re);
        }
        this.insert(session, parentNode, nodeName, object);
    }
}
