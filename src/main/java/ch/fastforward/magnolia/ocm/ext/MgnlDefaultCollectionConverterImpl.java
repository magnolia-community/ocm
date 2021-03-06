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

import ch.fastforward.magnolia.ocm.beans.OCMBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
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
 * Converts Collections to node structures.
 *
 * @author will
 */
public class MgnlDefaultCollectionConverterImpl extends DefaultCollectionConverterImpl {

    public static final String DEFAULT_COLLECTION_NODE_TYPE = "mgnl:contentNode";
    private static Logger log = LoggerFactory.getLogger(MgnlDefaultCollectionConverterImpl.class);

    public MgnlDefaultCollectionConverterImpl(Map atomicTypeConverters,
            ObjectConverter objectConverter,
            Mapper mapper) {
        super(atomicTypeConverters, objectConverter, mapper);
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

        ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass(ReflectionUtils.forName(collectionDescriptor.getElementClassName()));

        if (objects instanceof ManageableCollection) {
            insertManageableCollection(session, objects, collectionNode, elementClassDescriptor, collectionDescriptor);
        } else {
            insertManageableMap(session, objects, collectionNode);
        }

    }

    private void insertManageableCollection(Session session,
            ManageableObjects objects, Node collectionNode,
            ClassDescriptor elementClassDescriptor,
            CollectionDescriptor collectionDescriptor) {
        Iterator collectionIterator = objects.getIterator();
        int index = 0;
        while (collectionIterator.hasNext()) {
            Object item = collectionIterator.next();
            String elementJcrName = null;

            // If the element object has a unique id => the element jcr node name = the id value
            if (elementClassDescriptor.hasIdField()) {
                String idFieldName = elementClassDescriptor.getIdFieldDescriptor().getFieldName();
                elementJcrName = ReflectionUtils.getNestedProperty(item, idFieldName).toString();
            } else {
                elementJcrName = collectionDescriptor.getJcrElementName();
                if (elementJcrName == null) { // use PathFormat.checkFormat() here?
                    elementJcrName = nextAvailableElementName(collectionNode, index);
                }
            }

            objectConverter.insert(session, collectionNode, elementJcrName, item);
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
            NodeIterator nodeIter = collectionNode.getNodes();
            int i = 0;
            while (nodeIter.hasNext()) {
                nodeIter.next();
                i++;
            }
            while (collectionNode.getNode("" + i) != null) {
                i++;
            }
            return "" + i;
        } catch (RepositoryException ex) {
            log.error("could not check collection node for children", ex);
        }

        return COLLECTION_ELEMENT_NAME;
    }

    private void insertManageableMap(Session session, ManageableObjects objects, Node collectionNode) {

        Map map = (Map) objects.getObjects();
        for (Object key : map.keySet()) {
            Object item = map.get(key);
            objectConverter.insert(session, collectionNode, key.toString(), item);

        }
    }

    @Override
    protected ManageableObjects doGetCollection(Session session,
            Node parentNode,
            CollectionDescriptor collectionDescriptor,
            Class collectionFieldClass) throws RepositoryException {
        String jcrName = getCollectionJcrName(collectionDescriptor);

        if (parentNode == null || !parentNode.hasNode(jcrName)) {
            return null;
        }

        ManageableObjects objects = ManageableObjectsUtil.getManageableObjects(collectionFieldClass);
        Node collectionNode = parentNode.getNode(jcrName);
        NodeIterator children = collectionNode.getNodes();
        Class elementClass = ReflectionUtils.forName(collectionDescriptor.getElementClassName());

        while (children.hasNext()) {
            Node itemNode = children.nextNode();
            if (!itemNode.isNodeType("mgnl:metaData")) {
                try {
                    Object item = objectConverter.getObject(session, elementClass, itemNode.getPath());
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
                    log.debug("Could not get object for node " + itemNode.getPath());
                }
            }

        }

        return objects;
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
            updateManagableMap(session, parentNode, collectionDescriptor, objects, jcrName);
        }

    }

    /**
     * Same as in super class except that this implementation does not delete
     * element nodes that are defined as mandatory child nodes to the collection
     * node.
     *
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
        ClassDescriptor elementClassDescriptor = mapper.getClassDescriptorByClass(ReflectionUtils.forName(collectionDescriptor.getElementClassName()));
        Node collectionNode = parentNode.getNode(jcrName);
        //  If the collection elements have not an id, it is not possible to find the matching JCR nodes
        //  => delete the complete collection
        if (!elementClassDescriptor.hasIdField() && !elementClassDescriptor.hasUUIdField()) {
            collectionNode.remove();
            collectionNode = parentNode.addNode(jcrName);
        }

        Iterator collectionIterator = objects.getIterator();

        Map updatedItems = new HashMap();
        List<String> validUuidsForTheNode = new ArrayList<String>();
        while (collectionIterator.hasNext()) {
            Object item = collectionIterator.next();
            String jcrElementName = null;

            if (elementClassDescriptor.hasUUIdField()) {
                // @TODO: look for a name attribute or getName() field in the item
                jcrElementName = collectionDescriptor.getJcrElementName();
                if (jcrElementName != null) {
                    // look for a name propery
                    Object nameProperty = ReflectionUtils.getNestedProperty(item, jcrElementName);
                    String name = (nameProperty == null) ? null : nameProperty.toString();
                    if (StringUtils.isNotBlank(name)) {
                        jcrElementName = name;
                    }
                }
                jcrElementName = (jcrElementName == null) ? COLLECTION_ELEMENT_NAME : jcrElementName;
                String uuidFieldName = elementClassDescriptor.getUuidFieldDescriptor().getFieldName();
                Object objUuid = ReflectionUtils.getNestedProperty(item, uuidFieldName);
                String currentItemUuid = (objUuid == null) ? null : objUuid.toString();
                if (currentItemUuid != null) {
                    //The Node already exists so we need to update the existing node
                    //rather than to replace it.
                    Node nodeToUpdate = collectionNode.getSession().getNodeByUUID(currentItemUuid);
                    objectConverter.update(session, currentItemUuid, item);
                    validUuidsForTheNode.add(currentItemUuid);
                } else {
                    objectConverter.insert(session, collectionNode, jcrElementName, item);
                    validUuidsForTheNode.add(ReflectionUtils.getNestedProperty(item, uuidFieldName).toString());
                }

            } else if (elementClassDescriptor.hasIdField()) {

                String idFieldName = elementClassDescriptor.getIdFieldDescriptor().getFieldName();
                jcrElementName = ReflectionUtils.getNestedProperty(item, idFieldName).toString();

                // Update existing JCR Nodes
                if (collectionNode.hasNode(jcrElementName)) {
                    objectConverter.update(session, collectionNode, jcrElementName, item);
                } else {
                    // Add new collection elements
                    objectConverter.insert(session, collectionNode, jcrElementName, item);
                }

                updatedItems.put(jcrElementName, item);
            } else {
                jcrElementName = collectionDescriptor.getJcrElementName();
                if (jcrElementName == null) { // use PathFormat.checkFormat() here?
                    jcrElementName = COLLECTION_ELEMENT_NAME;
                }
                objectConverter.insert(session, collectionNode, jcrElementName, item);
            }
        }

        // Delete JCR nodes that are not present in the collection... except the
        // ones required by the node type definition (i.e. mandatory child nodes
        // of the collection node)
        if (elementClassDescriptor.hasUUIdField()) {
            NodeType collectionNodeType = session.getWorkspace().getNodeTypeManager().getNodeType(collectionDescriptor.getJcrType());
            NodeDefinition[] childNodeTypes = collectionNodeType.getChildNodeDefinitions();
            ArrayList mandatoryChildNodeNames = new ArrayList();
            NodeDefinition currNodeDef;
            for (int i = 0; i < childNodeTypes.length; i++) {
                currNodeDef = childNodeTypes[i];
                if (currNodeDef.isMandatory()) {
                    mandatoryChildNodeNames.add(currNodeDef.getName());
                }
            }
            NodeIterator nodeIterator = collectionNode.getNodes();
            List<Node> removeNodes = new ArrayList<Node>();
            while (nodeIterator.hasNext()) {
                Node currentNode = nodeIterator.nextNode();
                if (!validUuidsForTheNode.contains(currentNode.getUUID()) && !mandatoryChildNodeNames.contains(currentNode.getName())) {
                    removeNodes.add(currentNode);
                }
            }
            for (Node aNode : removeNodes) {
                aNode.remove();
            }
            return;
        }

        // Delete JCR nodes that are not present in the collection
        if (elementClassDescriptor.hasIdField()) {
            NodeIterator nodeIterator = collectionNode.getNodes();
            List removeNodes = new ArrayList();
            while (nodeIterator.hasNext()) {
                Node child = nodeIterator.nextNode();
                if (!updatedItems.containsKey(child.getName())) {
                    removeNodes.add(child);
                }
            }
            for (int i = 0; i < removeNodes.size(); i++) {
                ((Node) removeNodes.get(i)).remove();
            }
        }
    }

    private void updateManagableMap(Session session, Node parentNode, CollectionDescriptor collectionDescriptor, ManageableObjects objects, String jcrName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
