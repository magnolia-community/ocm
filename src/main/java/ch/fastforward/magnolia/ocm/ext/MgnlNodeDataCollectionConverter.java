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
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableMap;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjects;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjectsUtil;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.DefaultCollectionConverterImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts collections to node structures.
 * @TODO: clean up code
 * @TODO: document
 * @author will
 */
public class MgnlNodeDataCollectionConverter extends DefaultCollectionConverterImpl {

    private static Logger log = LoggerFactory.getLogger(MgnlNodeDataCollectionConverter.class);
    public static final String DEFAULT_COLLECTION_NODE_TYPE = "mgnl:contentNode";

    /**
     * Constructor.
     * @param atomicTypeConverters
     * @param objectConverter
     * @param mapper
     */
    public MgnlNodeDataCollectionConverter(Map atomicTypeConverters, ObjectConverter objectConverter, Mapper mapper) {
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
        Property currProp;
        AtomicTypeConverter atomicTypeConverter = (AtomicTypeConverter) atomicTypeConverters.get(elementClass);
        if (atomicTypeConverter != null) {
            while (properties.hasNext()) {
                currProp = properties.nextProperty();
                if (currProp.getName().startsWith("jcr:")) {
                    // skip the jcr properties like uuid, primaryType etc.
                } else {
                    Object valueObject = atomicTypeConverter.getObject(currProp.getValue());
                    if (valueObject != null) {
                        if (objects instanceof ManageableCollection) {
                            ((ManageableCollection) objects).addObject(valueObject);
                        } else {
                            ((ManageableMap) objects).addObject(currProp.getName(), valueObject);
                        }
                    } else {
                        log.error("Could not convert " + currProp.getValue().getString() + " to " + elementClass);
                    }
                }
            }
        } else {
            log.error("No converter found for conversions to " + elementClass);
        }

        return objects;
    }

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
        /*        ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass(ReflectionUtils.forName(collectionDescriptor.getElementClassName()));

        if (objects instanceof ManageableCollection) {
        insertManageableCollection(session, objects, collectionNode, elementClassDescriptor, collectionDescriptor);
        } else {
        insertManageableMap(session, objects, collectionNode);
        }*/

    }

    private void insertManageableCollection(Session session,
            ManageableObjects objects, Node collectionNode,
            ClassDescriptor elementClassDescriptor,
            CollectionDescriptor collectionDescriptor) {
        Iterator collectionIterator = objects.getIterator();
        int index = 0;
        Class elementClass = ReflectionUtils.forName(collectionDescriptor.getElementClassName());
//        ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass(ReflectionUtils.forName(collectionDescriptor.getElementClassName()));
        Property currProp;
        AtomicTypeConverter atomicTypeConverter = (AtomicTypeConverter) atomicTypeConverters.get(elementClass);
        if (atomicTypeConverter != null) {
            while (collectionIterator.hasNext()) {
                Object item = collectionIterator.next();
                try {
                    Value objectValue = atomicTypeConverter.getValue(session.getValueFactory(), item);
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
    }

    private String nextAvailableElementName(Node collectionNode, int index) {
        // first check if the collection node already exists in the repository
        if (collectionNode.isNew()) {
            // We do not need to check the names of the element nodes since the
            // whole collection is new. Simply return the index.
            return "" + index;
        }
        try {
//            NodeIterator nodeIter = collectionNode.getNodes();
            // get all properties
/*            PropertyIterator propertyIter = collectionNode.getProperties();
            // filter out all jcr properties like jcr:uuid
            ArrayList properties = new ArrayList();
            Property currProperty;
            while (propertyIter.hasNext()) {
                currProperty = (Property) propertyIter.next();
                if (currProperty.getName().startsWith("jcr:")) {
                    // skip
                } else {
                    properties.add(currProperty);
                }
            }*/
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
