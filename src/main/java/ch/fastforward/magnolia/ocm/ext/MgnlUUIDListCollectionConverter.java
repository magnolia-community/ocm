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
package ch.fastforward.magnolia.ocm.ext;

import ch.fastforward.magnolia.ocm.beans.OCMBean;
import java.util.Iterator;
import java.util.Map;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableMap;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjects;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjectsUtil;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will convert a node with a series of nodeDatas containing UUIDs into a
 * collection of Beans created by converting the nodes behind these UUIDs.
 * @author will
 */
public class MgnlUUIDListCollectionConverter extends MgnlDefaultCollectionConverterImpl {

    private static Logger log = LoggerFactory.getLogger(MgnlUUIDListCollectionConverter.class);
    public static final String DEFAULT_COLLECTION_NODE_TYPE = "mgnl:contentNode";

    /**
     * Constructor.
     * @param atomicTypeConverters
     * @param objectConverter
     * @param mapper
     */
    public MgnlUUIDListCollectionConverter(Map atomicTypeConverters, ObjectConverter objectConverter, Mapper mapper) {
        super(atomicTypeConverters, objectConverter, mapper);
    }

    @Override
    protected ManageableObjects doGetCollection(Session session, Node parentNode, CollectionDescriptor collectionDescriptor, Class collectionFieldClass) throws RepositoryException {
        String jcrName = getCollectionJcrName(collectionDescriptor);

        if (parentNode == null || !parentNode.hasNode(jcrName)) {
            return null;
        }

        ManageableObjects objects = ManageableObjectsUtil.getManageableObjects(collectionFieldClass);
        Node collectionNode = parentNode.getNode(jcrName);
        PropertyIterator properties = collectionNode.getProperties();
        Class elementClass = ReflectionUtils.forName(collectionDescriptor.getElementClassName());

//        while (children.hasNext()) {
        String targetWorkspaceName = "data";
        Session targetWorkspaceSession = session;
        if (!session.getWorkspace().getName().equals(targetWorkspaceName)) {
            // @TODO: get the session to access the target workspace
        }
        Property currProp;
        while (properties.hasNext()) {
//            Node itemNode = children.nextNode();
            currProp = properties.nextProperty();
            if (currProp.getName().startsWith("jcr:")) {
                // skip the jcr properties like uuid, primaryType etc.
            } else {
                Node itemNode = session.getNodeByUUID(currProp.getString());
                if (itemNode != null) {
                    try {
                        Object item = objectConverter.getObject(targetWorkspaceSession, elementClass, itemNode.getPath());
                        if (item instanceof OCMBean) {
                            if (StringUtils.isBlank(((OCMBean) item).getName())) {
                                ((OCMBean) item).setName(itemNode.getName());
                            }
                            log.debug("Item " + ((OCMBean) item).getName() + " retrieved");
                        }
                        if (objects instanceof ManageableCollection) {
                            ((ManageableCollection) objects).addObject(item);
                        } else {
                            ((ManageableMap) objects).addObject(itemNode.getName(), item);
                        }
                    } catch (ObjectContentManagerException ex) {
                        log.error("Could not get object for node " + itemNode.getPath());
                    }
                }
            }

        }

        return objects;
    }

    /**
     * @see AbstractCollectionConverterImpl#doInsertCollection(Session, Node, CollectionDescriptor, ManageableCollection)
     */
    @Override
    protected void doInsertCollection(Session session,
            Node parentNode,
            CollectionDescriptor collectionDescriptor,
            ManageableObjects objects) throws RepositoryException {
        if (objects == null) {
            return;
        }

        String jcrName = collectionDescriptor.getJcrName();
        if (jcrName == null) {
            throw new JcrMappingException(
                    "The JcrName attribute is not defined for the CollectionDescriptor : "
                    + collectionDescriptor.getFieldName() + " for the classdescriptor : " + collectionDescriptor.getClassDescriptor().getClassName());
        }

        String jcrType = collectionDescriptor.getJcrType();
        if (StringUtils.isBlank(jcrType)) {
            jcrType = DEFAULT_COLLECTION_NODE_TYPE;
        }

        Node collectionNode = parentNode.addNode(jcrName, jcrType);

        if (objects instanceof ManageableCollection) {
            insertManageableCollection(session, objects, collectionNode, null, collectionDescriptor);
        } else {
//                insertManageableMap(session, objects, collectionNode);
        }
    }

    private void insertManageableCollection(Session session,
            ManageableObjects objects, Node collectionNode,
            ClassDescriptor elementClassDescriptor,
            CollectionDescriptor collectionDescriptor) {
        Iterator collectionIterator = objects.getIterator();
        int index = 0;
        Class elementClass = ReflectionUtils.forName(collectionDescriptor.getElementClassName());
        while (collectionIterator.hasNext()) {
            Object item = collectionIterator.next();
            try {
                Value objectValue = session.getValueFactory().createValue(((OCMBean) item).getUuid());
                String elementJcrName = nextAvailableElementName(collectionNode, index);
                collectionNode.setProperty(elementJcrName, objectValue);
            } catch (UnsupportedRepositoryOperationException ex) {
                log.error("Could not convert " + item + "(" + item.getClass().getCanonicalName() + ") to Value", ex);
            } catch (RepositoryException ex) {
                log.error("Could not convert " + item + "(" + item.getClass().getCanonicalName() + ") to Value", ex);
            }
            index++;
        }
    }

    private String nextAvailableElementName(Node collectionNode, int index) {
        // first check if the collection node already exists in the repository
        if (collectionNode.isNew()) {
            // We do not need to check the names of the element nodes since the
            // whole collection is new. Simply return the index.
            return "" + index;
        }
        try {
            try {
                while (collectionNode.getProperty("" + index) != null) {
                    index++;
                }
            } catch (PathNotFoundException ex) {
                log.debug("No property named \"" + index + "\" found - will use this as next property name.");
            }
            return "" + index;
        } catch (RepositoryException ex) {
            log.error("could not check collection node for children", ex);
        }

        return COLLECTION_ELEMENT_NAME;
    }

    /**
     * Exact copy of DefaultCollectionConverterImpl.doUpdateCollection(), only
     * needed because it calls private methods that needed to be adapted...
     * @see AbstractCollectionConverterImpl#doUpdateCollection(Session, Node, CollectionDescriptor, ManageableCollection)
     */
    @Override
    protected void doUpdateCollection(Session session,
            Node parentNode,
            CollectionDescriptor collectionDescriptor,
            ManageableObjects objects) throws RepositoryException {

        String jcrName = getCollectionJcrName(collectionDescriptor);
        boolean hasNode = parentNode.hasNode(jcrName);
        // If the new value for the collection is null, drop the node matching to the collection
        if (objects == null) {
            if (hasNode) {
                parentNode.getNode(jcrName).remove();
            }
            return;
        }

        // If there is not yet a node matching to the collection, insert the collection
        if (!hasNode) {
            this.doInsertCollection(session, parentNode, collectionDescriptor, objects);
            return;
        }
        // update process
        if (objects instanceof ManageableCollection) {
            updateManagableCollection(session, parentNode, collectionDescriptor, objects, jcrName);
        } else {
//            updateManagableMap(session, parentNode, collectionDescriptor, objects, jcrName);
        }

    }

    /**
     * Same as in super class except that this implementation does not delete
     * element nodes that are defined as mandatory child nodes to the collection
     * node.
     * @param session
     * @param parentNode
     * @param collectionDescriptor
     * @param objects
     * @param jcrName
     * @throws PathNotFoundException
     * @throws RepositoryException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws ItemExistsException
     */
    private void updateManagableCollection(Session session, Node parentNode,
            CollectionDescriptor collectionDescriptor,
            ManageableObjects objects, String jcrName)
            throws PathNotFoundException, RepositoryException,
            VersionException, LockException, ConstraintViolationException,
            ItemExistsException {

//        ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass(ReflectionUtils.forName(collectionDescriptor.getElementClassName()));
        Node collectionNode = parentNode.getNode(jcrName);
        // The collectionNode must exists. Otherwise the doUpdateCollections()
        // method would have called insertManagable... instead of updateManagable...

        // You cannot update "collection" properties because the names will
        // most likely not match with the index -> delete all properties and
        // add them afterwards again
        PropertyIterator propertyIterator = collectionNode.getProperties();
        Property currProperty;
        while (propertyIterator.hasNext()) {
            currProperty = propertyIterator.nextProperty();
            if (currProperty.getName().startsWith("jcr:")) {
                // skip
            } else {
                // remove property
                currProperty.remove();
            }
        }
        insertManageableCollection(session, objects, collectionNode, null, collectionDescriptor);

    }
}
