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

import ch.fastforward.magnolia.ocm.util.MgnlOCMUtil;
import info.magnolia.jcr.util.PropertyUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.ocm.exception.IncorrectPersistentClassException;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ManagerConstant;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.beanconverter.BeanConverter;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.cache.impl.RequestObjectCacheImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.CollectionConverter;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjects;
import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableObjectsUtil;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.DefaultCollectionConverterImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManageableCollectionImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManageableMapImpl;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerUtil;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.SimpleFieldsHelper;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.apache.jackrabbit.ocm.repository.NodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.VersionException;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

/**
 * Implementation of {@link ObjectConverter} which supports
 * {@link MgnlPersistentObjectCacheImpl} and creates missing parent nodes if
 * necessary.
 *
 * @author Will Scheidegger
 */
public class MgnlNewObjectConverterImpl implements ObjectConverter, Serializable {

    private final static Logger log = LoggerFactory.getLogger(MgnlNewObjectConverterImpl.class);
    private static final String DEFAULT_BEAN_CONVERTER = "org.apache.jackrabbit.ocm.manager.beanconverter.impl.DefaultBeanConverterImpl";
    private Mapper mapper;
    private AtomicTypeConverterProvider atomicTypeConverterProvider;
    private ProxyManager proxyManager;
    private SimpleFieldsHelper simpleFieldsHelp;
    private ObjectCache objectCache;

    public MgnlNewObjectConverterImpl() {
    }

    /**
     * Constructor.
     *
     * @param mapper The mapper to used
     * @param converterProvider The atomic type converter provider
     *
     */
    public MgnlNewObjectConverterImpl(Mapper mapper, AtomicTypeConverterProvider converterProvider) {
        this.mapper = mapper;
        this.atomicTypeConverterProvider = converterProvider;
        this.proxyManager = new ProxyManagerImpl();
        this.simpleFieldsHelp = new SimpleFieldsHelper(atomicTypeConverterProvider);
        // by default use the RequestObjectCacheImpl
        this.objectCache = new RequestObjectCacheImpl();
    }

    /**
     * Constructor.
     *
     * @param mapper The mapper to used
     * @param converterProvider The atomic type converter provider
     * @param proxyManager
     * @param objectCache
     *
     */
    public MgnlNewObjectConverterImpl(Mapper mapper, AtomicTypeConverterProvider converterProvider, ProxyManager proxyManager, ObjectCache objectCache) {
        this.mapper = mapper;
        this.atomicTypeConverterProvider = converterProvider;
        this.proxyManager = proxyManager;
        this.simpleFieldsHelp = new SimpleFieldsHelper(atomicTypeConverterProvider);
        this.objectCache = objectCache;
    }

    /**
     * Set the <code>Mapper</code> used to solve mappings.
     *
     * @param mapper a <code>Mapper</code>
     */
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Sets the converter provider.
     *
     * @param converterProvider an <code>AtomicTypeConverterProvider</code>
     */
    public void setAtomicTypeConverterProvider(AtomicTypeConverterProvider converterProvider) {
        this.atomicTypeConverterProvider = converterProvider;
    }

    /**
     * Insert the object into the JCR repository and tries to create missing
     * parent nodes if a parentJcrType is provided in the class descriptor.
     *
     * @param session the JCR session
     * @param object the object to insert
     * @throws ObjectContentManagerException when it is not possible to insert
     * the object
     *
     */
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
            Node classDescNode = MgnlOCMUtil.getClassDescriptorNode(object);
            if (classDescNode == null) {
                throw new ObjectContentManagerException("Impossible to insert the object at '" + path + "'", pnfe);
            } else try {
                if (!classDescNode.hasProperty("parentJcrType")) {
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
                        String parentJcrType = PropertyUtil.getString(classDescNode, "parentJcrType");
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
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to insert the object at '" + path
                    + "'", re);
        }
        this.insert(session, parentNode, nodeName, object);
    }

    /**
     * Creates a node from the provided object and inserts it underneath the
     * parentNode.
     *
     * @param session
     * @param parentNode
     * @param nodeName
     * @param object
     */
    public void insert(Session session, Node parentNode, String nodeName, Object object) {
        ClassDescriptor classDescriptor = getMapper().getClassDescriptorByClass(object.getClass());

        String jcrType = classDescriptor.getJcrType();
        if ((jcrType == null) || jcrType.equals("")) {
            jcrType = ManagerConstant.NT_UNSTRUCTURED;
        }

        Node objectNode;
        try {
            objectNode = parentNode.addNode(nodeName, jcrType);
        } catch (NoSuchNodeTypeException nsnte) {
            throw new JcrMappingException("Unknown node type " + jcrType + " for mapped class " + object.getClass(), nsnte);
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Cannot create new node of type " + jcrType + " from mapped class "
                    + object.getClass(), re);
        }

        String[] mixinTypes = classDescriptor.getJcrMixinTypes();
        String mixinTypeName = null;
        try {

            // Add mixin types
            if (null != classDescriptor.getJcrMixinTypes()) {
                for (int i = 0; i < mixinTypes.length; i++) {
                    mixinTypeName = mixinTypes[i].trim();
                    objectNode.addMixin(mixinTypeName);
                }
            }

            // Add mixin types defined in the associated interfaces
            if (!classDescriptor.hasDiscriminator() && classDescriptor.hasInterfaces()) {
                Iterator interfacesIterator = classDescriptor.getImplements().iterator();
                while (interfacesIterator.hasNext()) {
                    String interfaceName = (String) interfacesIterator.next();
                    ClassDescriptor interfaceDescriptor = getMapper()
                            .getClassDescriptorByClass(ReflectionUtils.forName(interfaceName));
                    objectNode.addMixin(interfaceDescriptor.getJcrType().trim());
                }
            }

            // If required, add the discriminator node type
            if (classDescriptor.hasDiscriminator()) {
                addDiscriminatorProperty(object, objectNode);
            }

        } catch (NoSuchNodeTypeException nsnte) {
            throw new JcrMappingException("Unknown mixin type " + mixinTypeName + " for mapped class " + object.getClass(), nsnte);
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Cannot create new node of type " + jcrType + " from mapped class "
                    + object.getClass(), re);
        }

        getSimpleFieldsHelp().storeSimpleFields(session, object, classDescriptor, objectNode);
        insertBeanFields(session, object, classDescriptor, objectNode);
        insertCollectionFields(session, object, classDescriptor, objectNode);
        getSimpleFieldsHelp().refreshUuidPath(session, classDescriptor, objectNode, object);

        // cache if we have a persistant cache
        if (objectCache instanceof MgnlPersistentObjectCache) {
            try {
                objectCache.cache(objectNode.getPath(), object);
            } catch (RepositoryException ex) {
                log.error("Could not cache object " + object, ex);
            }
        }
    }

    private void addDiscriminatorProperty(Object object, Node objectNode)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException,
            ValueFormatException {

        try {
            objectNode.setProperty(ManagerConstant.DISCRIMINATOR_CLASS_NAME_PROPERTY,
                    ReflectionUtils.getBeanClass(object).getName());

        } catch (RepositoryException e) {
            // if it is not possible to add the CLASS_NAME_PROPERTY due to strong constraints in the
            // node type definition, try to add the Discriminator node type.
            String mixinTypeName;
            mixinTypeName = ManagerConstant.DISCRIMINATOR_NODE_TYPE;
            objectNode.addMixin(mixinTypeName);
            objectNode.setProperty(ManagerConstant.DISCRIMINATOR_CLASS_NAME_PROPERTY,
                    ReflectionUtils.getBeanClass(object).getName());
        }

    }

    /**
     * Updates the node in the JCR repository with the date from this object.
     *
     * @param session
     * @param object
     */
    public void update(Session session, Object object) {
        String path = this.getPath(session, object);
        try {
            String parentPath = NodeUtil.getParentPath(path);
            String nodeName = NodeUtil.getNodeName(path);
            Node parentNode = session.getNode(parentPath);
            this.update(session, parentNode, nodeName, object);
        } catch (PathNotFoundException pnfe) {
            throw new ObjectContentManagerException("Impossible to update the object at '" + path + "'", pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to update the object at '" + path
                    + "'", re);
        }
    }

    /**
     * Updates the node in the JCR repository with the date from this object.
     *
     * @param session
     * @param uuId
     * @param object
     */
    public void update(Session session, String uuId, Object object) {
        try {
            ClassDescriptor classDescriptor = getMapper().getClassDescriptorByClass(ReflectionUtils.getBeanClass(object));
            Node objectNode = session.getNodeByIdentifier(uuId);
            update(session, objectNode, object);
        } catch (PathNotFoundException pnfe) {
            throw new ObjectContentManagerException("Impossible to update the object with UUID: " + uuId, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to update the object with UUID: " + uuId, re);
        }
    }

    /**
     * Updates the node in the JCR repository with the date from this object.
     *
     * @param session
     * @param parentNode
     * @param nodeName
     * @param object
     */
    public void update(Session session, Node parentNode, String nodeName, Object object) {
        try {
            ClassDescriptor classDescriptor = getMapper().getClassDescriptorByClass(ReflectionUtils.getBeanClass(object));
            Node objectNode = getNode(parentNode, classDescriptor, nodeName, object);
            update(session, objectNode, object);
        } catch (PathNotFoundException pnfe) {
            throw new ObjectContentManagerException("Impossible to update the object: " + nodeName + " at node : " + parentNode, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to update the object: "
                    + nodeName + " at node : " + parentNode, re);
        }
    }

    /**
     * Updates the node in the JCR repository with the date from this object.
     *
     * @param session
     * @param objectNode
     * @param object
     */
    public void update(Session session, Node objectNode, Object object) {
        ClassDescriptor classDescriptor = getMapper().getClassDescriptorByClass(ReflectionUtils.getBeanClass(object));

        checkNodeType(session, classDescriptor);

        checkCompatiblePrimaryNodeTypes(session, objectNode, classDescriptor, false);

        getSimpleFieldsHelp().storeSimpleFields(session, object, classDescriptor, objectNode);
        updateBeanFields(session, object, classDescriptor, objectNode);
        updateCollectionFields(session, object, classDescriptor, objectNode);
        getSimpleFieldsHelp().refreshUuidPath(session, classDescriptor, objectNode, object);
    }

    /**
     * Gets the JCR node for the object. If multiple nodes with the name exist
     * the node with the correct uuid will be returned.
     *
     * @param parentNode the parent node at which to look for the node element.
     * @param nodeName the node name to look for
     * @param object the data.
     * @param classDescriptor
     * @return The child node we are interested in.
     */
    private Node getNode(Node parentNode, ClassDescriptor classDescriptor, String nodeName, Object object) throws RepositoryException {
        if (parentNode == null) {
            return null;
        }
        NodeIterator nodes = parentNode.getNodes(nodeName);
        if (nodes.getSize() == 1) {
            return nodes.nextNode();
        }
        if (classDescriptor.hasUUIdField()) {
            String uuidFieldName = classDescriptor.getUuidFieldDescriptor().getFieldName();
            Object objUuid = ReflectionUtils.getNestedProperty(object, uuidFieldName);
            String currentItemUuid = (objUuid == null) ? null : objUuid.toString();
            if (currentItemUuid != null) {
                //The Node already exists so we need to update the existing node 
                //rather than to replace it.
                return parentNode.getSession().getNodeByIdentifier(currentItemUuid);
            } else {
                throw new NullPointerException("Cannot locate the node to update since there is no UUID provided even though, " + classDescriptor.getClassName() + " has been mapped with a UUID field , " + uuidFieldName);
            }

        }
        return parentNode.getNode(nodeName);

    }

    /**
     * Gets the object from a node path. If a {@link MgnlPersistentObjectCache}
     * is being used, it will update the cached object with the latest data from
     * the repository and then return the cached object.
     *
     * @param session
     * @param path
     * @return
     */
    public Object getObject(Session session, String path) {
        try {
            if (!session.nodeExists(path)) {
                return null;
            }

            if (getObjectCache().isCached(path)) {
                if (getObjectCache() instanceof MgnlPersistentObjectCache) {
                    updateObject(session, getObjectCache().getObject(path));
                }
                return getObjectCache().getObject(path);
            }

            ClassDescriptor classDescriptor;
            Node node = session.getNode(path);
            if (node.hasProperty(ManagerConstant.DISCRIMINATOR_CLASS_NAME_PROPERTY)) {
                String className = node.getProperty(ManagerConstant.DISCRIMINATOR_CLASS_NAME_PROPERTY).getValue().getString();
                classDescriptor = getMapper().getClassDescriptorByClass(ReflectionUtils.forName(className));
            } else {
                String nodeType = node.getPrimaryNodeType().getName();
                if (nodeType.equals(ManagerConstant.FROZEN_NODE_TYPE)) {
                    nodeType = node.getProperty(ManagerConstant.FROZEN_PRIMARY_TYPE_PROPERTY).getString();
                }
                classDescriptor = getMapper().getClassDescriptorByNodeType(nodeType);
            }

            if (null == classDescriptor) {
                throw new JcrMappingException("Impossible to find the classdescriptor for " + path
                        + ". There is no discriminator and associated  JCR node type");
            }

            Object object = ReflectionUtils.newInstance(classDescriptor.getClassName());

            if (!objectCache.isCached(path)) {
                getObjectCache().cache(path, object);
            }

            getSimpleFieldsHelp().retrieveSimpleFields(session, classDescriptor, node, object);
            retrieveBeanFields(session, classDescriptor, node, object, false);
            retrieveCollectionFields(session, classDescriptor, node, object, false);

            return object;

        } catch (PathNotFoundException pnfe) {
            // HINT should never get here
            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
        }
    }

    /**
     * Gets the object from a node path.
     *
     * @param session
     * @param clazz
     * @param path
     * @return
     */
    public Object getObject(Session session, Class clazz, String path) {
        try {
            if (!session.nodeExists(path)) {
                return null;
            }

            if (getObjectCache().isCached(path)) {
                if (getObjectCache() instanceof MgnlPersistentObjectCache) {
                    updateObject(session, getObjectCache().getObject(path));
                }
                return getObjectCache().getObject(path);
            }

            ClassDescriptor classDescriptor = getClassDescriptor(clazz);

            checkNodeType(session, classDescriptor);

            Node node = session.getNode(path);
            if (!classDescriptor.isInterface()) {
                node = getActualNode(session, node);
                checkCompatiblePrimaryNodeTypes(session, node, classDescriptor, true);
            }

            ClassDescriptor alternativeDescriptor = null;
            if (classDescriptor.usesNodeTypePerHierarchyStrategy()) {
                if (node.hasProperty(ManagerConstant.DISCRIMINATOR_CLASS_NAME_PROPERTY)) {
                    String className = node.getProperty(ManagerConstant.DISCRIMINATOR_CLASS_NAME_PROPERTY).getValue().getString();
                    alternativeDescriptor = getClassDescriptor(ReflectionUtils.forName(className));
                }
            } else {
                if (classDescriptor.usesNodeTypePerConcreteClassStrategy()) {
                    String nodeType = node.getPrimaryNodeType().getName();
                    if (!nodeType.equals(classDescriptor.getJcrType())) {
                        alternativeDescriptor = classDescriptor.getDescendantClassDescriptor(nodeType);

                        // in case we an alternative could not be found by walking
                        // the class descriptor hierarchy, check whether we would
                        // have a descriptor for the node type directly (which
                        // may the case if the class descriptor hierarchy is
                        // incomplete due to missing configuration. See JCR-1145
                        // for details.
                        if (alternativeDescriptor == null) {
                            alternativeDescriptor = getMapper().getClassDescriptorByNodeType(nodeType);
                        }
                    }
                }
            }

            // if we have an alternative class descriptor, check whether its
            // extends (or is the same) as the requested class.
            if (alternativeDescriptor != null) {
                Class alternativeClazz = ReflectionUtils.forName(alternativeDescriptor.getClassName());
                if (clazz.isAssignableFrom(alternativeClazz)) {
                    clazz = alternativeClazz;
                    classDescriptor = alternativeDescriptor;
                }
            }

            // ensure class is concrete (neither interface nor abstract)
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                throw new JcrMappingException("Cannot instantiate non-concrete class " + clazz.getName()
                        + " for node " + path + " of type " + node.getPrimaryNodeType().getName());
            }

            Object object = ReflectionUtils.newInstance(classDescriptor.getClassName());

            if (!objectCache.isCached(path)) {
                getObjectCache().cache(path, object);
            }

            getSimpleFieldsHelp().retrieveSimpleFields(session, classDescriptor, node, object);
            retrieveBeanFields(session, classDescriptor, node, object, false);
            retrieveCollectionFields(session, classDescriptor, node, object, false);

            return object;
        } catch (PathNotFoundException pnfe) {
            // HINT should never get here
            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
        }
    }

    private void updateObject(Session session, Object object) {
        // make sure the node for this object actually exists
        // TODO: what if it does not exist anymore?
        //  - currently > do not update
        //  - alternatively > throw exception?
        //  - remove from persistent cache?
        String path = getPath(session, object);
        if (StringUtils.isBlank(path)) {
            // seems like this object has not been stored in the JCR repo yet!!!
        } else {
            try {
                if (!session.nodeExists(path)) {
                    return;
                }
            } catch (RepositoryException ex) {
                log.error("Could not check the existance of path " + path, ex);
                return;
            }
            ClassDescriptor classDescriptor = getMapper().getClassDescriptorByClass(object.getClass());
            Node node;
            try {
                node = session.getNode(path);
                getSimpleFieldsHelp().retrieveSimpleFields(session, classDescriptor, node, object);
                retrieveBeanFields(session, classDescriptor, node, object, false);
                retrieveCollectionFields(session, classDescriptor, node, object, false);
            } catch (RepositoryException ex) {
                log.error("Could not get node at path " + path, ex);
            }
        }
    }

    public void retrieveAllMappedAttributes(Session session, Object object) {
        String path = null;
        try {
            ClassDescriptor classDescriptor = getClassDescriptor(object.getClass());
            String pathFieldName = classDescriptor.getPathFieldDescriptor().getFieldName();
            path = (String) ReflectionUtils.getNestedProperty(object, pathFieldName);
            Node node = session.getNode(path);
            retrieveBeanFields(session, classDescriptor, node, object, true);
            retrieveCollectionFields(session, classDescriptor, node, object, true);

        } catch (PathNotFoundException pnfe) {

            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
        }
    }

    public void retrieveMappedAttribute(Session session, Object object, String attributeName) {
        String path = null;
        ClassDescriptor classDescriptor = null;
        try {
            classDescriptor = getClassDescriptor(object.getClass());
            String pathFieldName = classDescriptor.getPathFieldDescriptor().getFieldName();
            path = (String) ReflectionUtils.getNestedProperty(object, pathFieldName);
            Node node = session.getNode(path);
            BeanDescriptor beanDescriptor = classDescriptor.getBeanDescriptor(attributeName);
            if (beanDescriptor != null) {
                this.retrieveBeanField(session, beanDescriptor, node, object, true);
            } // Check if the attribute is a collection
            else {
                CollectionDescriptor collectionDescriptor = classDescriptor.getCollectionDescriptor(attributeName);
                if (collectionDescriptor != null) {
                    this.retrieveCollectionField(session, collectionDescriptor, node, object, true);
                } else {
                    throw new ObjectContentManagerException("Impossible to retrieve the mapped attribute. The attribute '"
                            + attributeName + "'  is not a bean or a collection for the class : " + classDescriptor.getClassName());
                }
            }

        } catch (PathNotFoundException pnfe) {

            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
        }
    }

    /**
     * Validates the node type used by the class descriptor.
     *
     * @param session the current session
     * @param classDescriptor descriptor
     * @throws JcrMappingException thrown if the node type is unknown
     * @throws org.apache.jackrabbit.ocm.exception.RepositoryException thrown if
     * an error occured in the underlying repository
     */
    private void checkNodeType(Session session, ClassDescriptor classDescriptor) {
        String jcrTypeName = null;
        try {

            //Don't check the primary node type for interfaces. They are only associated to mixin node type
            if (classDescriptor.isInterface()) {
                String[] mixinTypes = classDescriptor.getJcrMixinTypes();
                for (int i = 0; i < mixinTypes.length; i++) {
                    jcrTypeName = mixinTypes[i];
                    session.getWorkspace().getNodeTypeManager().getNodeType(jcrTypeName);
                }
            } else {
                jcrTypeName = classDescriptor.getJcrType();
                if (jcrTypeName != null && !jcrTypeName.equals("")) {
                    session.getWorkspace().getNodeTypeManager().getNodeType(jcrTypeName);
                }
            }
        } catch (NoSuchNodeTypeException nsnte) {
            throw new JcrMappingException("Mapping for class '" + classDescriptor.getClassName()
                    + "' use unknown primary or mixin node type '" + jcrTypeName + "'");
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(re);
        }
    }

    /**
     * Checks if the node type in the class descriptor is compatible with the
     * specified node node type.
     *
     * @param session the current session
     * @param node node against whose node type the compatibility is checked
     * @param classDescriptor class descriptor
     * @param checkVersionNode
     * <tt>true</tt> if the check should continue in case the
     * <tt>node</tt> is a version node, <tt>false</tt> if no check against
     * version node should be performed
     *
     * @throws ObjectContentManagerException thrown if node types are
     * incompatible
     * @throws org.apache.jackrabbit.ocm.exception.RepositoryException thrown if
     * an error occured in the underlying repository
     */
    private void checkCompatiblePrimaryNodeTypes(Session session, Node node, ClassDescriptor classDescriptor,
            boolean checkVersionNode) {
        try {
            NodeType nodeType = node.getPrimaryNodeType();

            boolean compatible = checkCompatibleNodeTypes(nodeType, classDescriptor);

            if (!compatible && checkVersionNode && ManagerConstant.FROZEN_NODE_TYPE.equals(nodeType.getName())) {
                NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                nodeType = ntMgr.getNodeType(node.getProperty(ManagerConstant.FROZEN_PRIMARY_TYPE_PROPERTY).getString());

                compatible = checkCompatibleNodeTypes(nodeType, classDescriptor);
            }

            if (!compatible) {
                throw new ObjectContentManagerException("Cannot map object of type '" + classDescriptor.getClassName() + "'. Node type '"
                        + node.getPrimaryNodeType().getName() + "' does not match descriptor node type '"
                        + classDescriptor.getJcrType() + "'");
            }
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(re);
        }
    }

    /**
     * Node types compatibility check.
     *
     * @param nodeType target node type
     * @param descriptor descriptor containing source node type
     * @return <tt>true</tt> if nodes are considered compatible,
     * <tt>false</tt> otherwise
     */
    private boolean checkCompatibleNodeTypes(NodeType nodeType, ClassDescriptor descriptor) {

        //return true if node type is not used
        if (descriptor.getJcrType() == null || descriptor.getJcrType().equals("")) {
            return true;
        }

        if (nodeType.getName().equals(descriptor.getJcrType())) {
            return true;
        }

        NodeType[] superTypes = nodeType.getSupertypes();
        for (int i = 0; i < superTypes.length; i++) {
            if (superTypes[i].getName().equals(descriptor.getJcrType())) {
                return true;
            }
        }

        return false;
    }

    /**
     * @throws JcrMappingException
     */
    public String getPath(Session session, Object object) {
        ClassDescriptor classDescriptor = getMapper().getClassDescriptorByClass(object.getClass());

        final FieldDescriptor pathFieldDescriptor = classDescriptor.getPathFieldDescriptor();
        if (pathFieldDescriptor == null) {
            throw new JcrMappingException(
                    "Class of type: "
                    + object.getClass().getName()
                    + " has no path mapping. Maybe attribute path=\"true\" for a field element of this class in mapping descriptor is missing "
                    + " or maybe it is defined in an ancestor class which has no mapping descriptor.");
        }
        String pathField = pathFieldDescriptor.getFieldName();

        return (String) ReflectionUtils.getNestedProperty(object, pathField);
    }

    private void retrieveBeanFields(Session session, ClassDescriptor classDescriptor, Node node, Object object,
            boolean forceToRetrieve) {
        Iterator beanDescriptorIterator = classDescriptor.getBeanDescriptors().iterator();
        while (beanDescriptorIterator.hasNext()) {
            BeanDescriptor beanDescriptor = (BeanDescriptor) beanDescriptorIterator.next();
            this.retrieveBeanField(session, beanDescriptor, node, object, forceToRetrieve);
        }
    }

    private void retrieveBeanField(Session session, BeanDescriptor beanDescriptor, Node node, Object object, boolean forceToRetrieve) {
        if (!beanDescriptor.isAutoRetrieve() && !forceToRetrieve) {
            return;
        }

        String beanName = beanDescriptor.getFieldName();
        String beanPath = ObjectContentManagerUtil.getPath(session, beanDescriptor, node);

        Object bean = null;
        if (getObjectCache().isCached(beanPath)) {
            bean = getObjectCache().getObject(beanPath);
            ReflectionUtils.setNestedProperty(object, beanName, bean);
        } else {
            Class beanClass = ReflectionUtils.getPropertyType(object, beanName);

            String converterClassName = null;
            if (null == beanDescriptor.getConverter() || "".equals(beanDescriptor.getConverter())) {
                converterClassName = DEFAULT_BEAN_CONVERTER;
            } else {
                converterClassName = beanDescriptor.getConverter();
            }

            Object[] param = {this.getMapper(), this, this.getAtomicTypeConverterProvider()};
            BeanConverter beanConverter = (BeanConverter) ReflectionUtils.invokeConstructor(converterClassName, param);
            if (beanDescriptor.isProxy()) {
                if (beanDescriptor.getJcrType() != null && !"".equals(beanDescriptor.getJcrType())) {
                    // If a mapped jcrType has been set, use it as proxy parent class instead of the bean property type.
                    // This way, we can handle proxies when bean property type is an interface.
                    try {
                        String className = getMapper().getClassDescriptorByNodeType(beanDescriptor.getJcrType()).getClassName();
                        if (log.isDebugEnabled()) {
                            log.debug("a mapped jcrType has been specified, switching from <" + beanClass + "> to <" + ReflectionUtils.forName(className));
                        }
                        beanClass = ReflectionUtils.forName(className);

                    } catch (IncorrectPersistentClassException e) {
                        if (log.isDebugEnabled()) {
                            log.debug(beanDescriptor.getClassDescriptor().getJcrType() + " is not mapped");
                        }
                    }
                }

                bean = getProxyManager().createBeanProxy(beanConverter, beanConverter.getPath(session, beanDescriptor, node), session, node, beanDescriptor, getMapper().getClassDescriptorByClass(beanClass), beanClass, bean);
            } else {
                bean = beanConverter.getObject(session, node, beanDescriptor, getMapper().getClassDescriptorByClass(beanClass), beanClass, bean);
            }
            getObjectCache().cache(beanPath, bean);
            ReflectionUtils.setNestedProperty(object, beanName, bean);
        }
    }

    private void retrieveCollectionFields(Session session, ClassDescriptor classDescriptor, Node parentNode, Object object,
            boolean forceToRetrieve) {
        Iterator collectionDescriptorIterator = classDescriptor.getCollectionDescriptors().iterator();
        while (collectionDescriptorIterator.hasNext()) {
            CollectionDescriptor collectionDescriptor = (CollectionDescriptor) collectionDescriptorIterator.next();
            this.retrieveCollectionField(session, collectionDescriptor, parentNode, object, forceToRetrieve);
        }
    }

    private void retrieveCollectionField(Session session, CollectionDescriptor collectionDescriptor, Node parentNode, Object object, boolean forceToRetrieve) {
        if (!collectionDescriptor.isAutoRetrieve() && !forceToRetrieve) {
            return;
        }

        CollectionConverter collectionConverter = this.getCollectionConverter(session, collectionDescriptor);
        Class collectionFieldClass = ReflectionUtils.getPropertyType(object, collectionDescriptor.getFieldName());
        if (collectionDescriptor.isProxy()) {
            Object proxy = getProxyManager().createCollectionProxy(session, collectionConverter, parentNode,
                    collectionDescriptor, collectionFieldClass);
            ReflectionUtils.setNestedProperty(object, collectionDescriptor.getFieldName(), proxy);
        } else {
            ManageableObjects objects = collectionConverter.getCollection(session, parentNode, collectionDescriptor, collectionFieldClass);
            if (objects == null) {
                ReflectionUtils.setNestedProperty(object, collectionDescriptor.getFieldName(), null);
            } else {
                // TODO: find another for managing custom ManageableObjects classes
                if (!objects.getClass().equals(ManageableCollectionImpl.class)
                        && !objects.getClass().equals(ManageableMapImpl.class)) {
                    ReflectionUtils.setNestedProperty(object, collectionDescriptor.getFieldName(), objects);
                } else {
                    ReflectionUtils.setNestedProperty(object, collectionDescriptor.getFieldName(), objects.getObjects());
                }
            }

        }

    }

    private void insertBeanFields(Session session, Object object, ClassDescriptor classDescriptor, Node objectNode) {
        Iterator beanDescriptorIterator = classDescriptor.getBeanDescriptors().iterator();
        while (beanDescriptorIterator.hasNext()) {
            BeanDescriptor beanDescriptor = (BeanDescriptor) beanDescriptorIterator.next();

            if (!beanDescriptor.isAutoInsert()) {
                continue;
            }

            Object bean = ReflectionUtils.getNestedProperty(object, beanDescriptor.getFieldName());
            if (bean != null) {
                String converterClassName = null;

                if (null == beanDescriptor.getConverter() || "".equals(beanDescriptor.getConverter())) {
                    converterClassName = DEFAULT_BEAN_CONVERTER;
                } else {
                    converterClassName = beanDescriptor.getConverter();
                }

                Object[] param = {this.getMapper(), this, this.getAtomicTypeConverterProvider()};
                BeanConverter beanConverter = (BeanConverter) ReflectionUtils.invokeConstructor(converterClassName, param);
                beanConverter.insert(session, objectNode, beanDescriptor, getMapper().getClassDescriptorByClass(bean.getClass()), bean, classDescriptor, object);
            }
        }
    }

    private void updateBeanFields(Session session, Object object, ClassDescriptor classDescriptor, Node objectNode) {
        Iterator beanDescriptorIterator = classDescriptor.getBeanDescriptors().iterator();
        while (beanDescriptorIterator.hasNext()) {
            BeanDescriptor beanDescriptor = (BeanDescriptor) beanDescriptorIterator.next();
            if (!beanDescriptor.isAutoUpdate()) {
                continue;
            }

            Object bean = ReflectionUtils.getNestedProperty(object, beanDescriptor.getFieldName());

            String converterClassName = null;
            if (null == beanDescriptor.getConverter() || "".equals(beanDescriptor.getConverter())) {
                converterClassName = DEFAULT_BEAN_CONVERTER;
            } else {
                converterClassName = beanDescriptor.getConverter();
            }

            Object[] param = {this.getMapper(), this, this.getAtomicTypeConverterProvider()};
            BeanConverter beanConverter = (BeanConverter) ReflectionUtils.invokeConstructor(converterClassName, param);
            Class beanClass = ReflectionUtils.getPropertyType(object, beanDescriptor.getFieldName());
            // if the bean is null, remove existing node
            if ((bean == null)) {

                beanConverter.remove(session, objectNode, beanDescriptor, getMapper().getClassDescriptorByClass(beanClass), bean, classDescriptor, object);

            } else {
                beanConverter.update(session, objectNode, beanDescriptor, getMapper().getClassDescriptorByClass(beanClass), bean, classDescriptor, object);
            }

        }
    }

    private void insertCollectionFields(Session session, Object object, ClassDescriptor classDescriptor, Node objectNode) {
        Iterator collectionDescriptorIterator = classDescriptor.getCollectionDescriptors().iterator();

        while (collectionDescriptorIterator.hasNext()) {
            CollectionDescriptor collectionDescriptor = (CollectionDescriptor) collectionDescriptorIterator.next();

            if (!collectionDescriptor.isAutoInsert()) {
                continue;
            }

            CollectionConverter collectionConverter = this.getCollectionConverter(session, collectionDescriptor);
            Object collection = ReflectionUtils.getNestedProperty(object, collectionDescriptor.getFieldName());
            ManageableObjects manageableCollection = ManageableObjectsUtil.getManageableObjects(collection);

            collectionConverter.insertCollection(session, objectNode, collectionDescriptor, manageableCollection);
        }
    }

    private void updateCollectionFields(Session session, Object object, ClassDescriptor classDescriptor, Node objectNode) {
        Iterator collectionDescriptorIterator = classDescriptor.getCollectionDescriptors().iterator();

        while (collectionDescriptorIterator.hasNext()) {
            CollectionDescriptor collectionDescriptor = (CollectionDescriptor) collectionDescriptorIterator.next();
            if (!collectionDescriptor.isAutoUpdate()) {
                continue;
            }

            CollectionConverter collectionConverter = this.getCollectionConverter(session, collectionDescriptor);
            Object collection = ReflectionUtils.getNestedProperty(object, collectionDescriptor.getFieldName());
            ManageableObjects manageableCollection = ManageableObjectsUtil.getManageableObjects(collection);

            collectionConverter.updateCollection(session, objectNode, collectionDescriptor, manageableCollection);
        }
    }

    public CollectionConverter getCollectionConverter(Session session, CollectionDescriptor collectionDescriptor) {
        String className = collectionDescriptor.getCollectionConverter();
        Map atomicTypeConverters = this.getAtomicTypeConverterProvider().getAtomicTypeConverters();
        if (className == null) {
            return new DefaultCollectionConverterImpl(atomicTypeConverters, this, this.getMapper());
        } else {
            return (CollectionConverter) ReflectionUtils.invokeConstructor(className, new Object[]{atomicTypeConverters, this, this.getMapper()});
        }

    }

    private ClassDescriptor getClassDescriptor(Class beanClass) {
        ClassDescriptor classDescriptor = getMapper().getClassDescriptorByClass(beanClass);
        if (null == classDescriptor) {
            throw new JcrMappingException("Class of type: " + beanClass.getName()
                    + " is not JCR persistable. Maybe element 'class-descriptor' for this type in mapping file is missing");
        }

        return classDescriptor;
    }

    private Node getActualNode(Session session, Node node) throws RepositoryException {
        NodeType type = node.getPrimaryNodeType();
        if (type.getName().equals("nt:versionedChild")) {

            String uuid = node.getProperty("jcr:childVersionHistory").getValue().getString();
            Node actualNode = session.getNodeByIdentifier(uuid);
            String name = actualNode.getName();
            actualNode = session.getNodeByIdentifier(name);

            return actualNode;
        }

        return node;
    }

    /**
     * @return the mapper
     */
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * @return the atomicTypeConverterProvider
     */
    public AtomicTypeConverterProvider getAtomicTypeConverterProvider() {
        return atomicTypeConverterProvider;
    }

    /**
     * @return the proxyManager
     */
    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    /**
     * @return the simpleFieldsHelp
     */
    public SimpleFieldsHelper getSimpleFieldsHelp() {
        return simpleFieldsHelp;
    }

    /**
     * @return the requestObjectCache
     */
    public ObjectCache getObjectCache() {
        return objectCache;
    }

    /**
     * @param requestObjectCache the requestObjectCache to set
     */
    public void setObjectCache(ObjectCache requestObjectCache) {
        this.objectCache = requestObjectCache;
    }

}
