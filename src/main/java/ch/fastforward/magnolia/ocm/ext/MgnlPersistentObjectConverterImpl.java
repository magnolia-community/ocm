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

import java.io.Serializable;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ManagerConstant;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.SimpleFieldsHelper;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * The object converter will update nodes in its persistant cache before
 * returning them. If it has not been initialized with a
 * MgnlPersistantObjectConverterImpl as ObjectCache it will simply behave like
 * the normal MgnlObjectConverterImpl.
 *
 * @author will
 */
public class MgnlPersistentObjectConverterImpl extends MgnlObjectConverterImpl implements Serializable {

    private final MgnlPersistentObjectCacheImpl objectCache;
    private Mapper mapper;
    private SimpleFieldsHelper simpleFieldsHelp;

    public MgnlPersistentObjectConverterImpl(Mapper mapper, DefaultAtomicTypeConverterProvider converterProvider, ProxyManagerImpl proxyManagerImpl, ObjectCache requestObjectCache) {
        super(mapper, converterProvider, proxyManagerImpl, requestObjectCache);
        if (requestObjectCache instanceof MgnlPersistentObjectCacheImpl) {
            objectCache = (MgnlPersistentObjectCacheImpl) requestObjectCache;
        } else {
            objectCache = null;
        }
        this.mapper = mapper;
        this.simpleFieldsHelp = new SimpleFieldsHelper(atomicTypeConverterProvider);
    }
    
    /**
     * Set the <code>Mapper</code> used to solve mappings.
     *
     * @param mapper
     *            a <code>Mapper</code>
     */
    @Override
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
        super.setMapper(mapper);
    }

    @Override
    public Object getObject(Session session, String path) {
        try {
            if (!session.nodeExists(path)) {
                return null;
            }
            if (objectCache == null) {
                return super.getObject(session, path);
            } else {
                if (objectCache.isRequestCached(path)) {
                    // the object has already been loaded in this request
                    // -> no need to update
                } else if (objectCache.isCached(path)) {
                    // The object only exists in the persistant cache. It might
                    // contain outdated data. Let's refresh it!
                    Object object = objectCache.getObject(path);
                    update(session, object);
                    objectCache.cache(path, object);
                }
            }
            return super.getObject(session, path);

        } catch (PathNotFoundException pnfe) {
            // HINT should never get here
            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
        }
    }

    /**
     * Updates an existing bean object with the latest data from its
     * corresponding node.
     *
     * @param session
     * @param object
     */
    public void updateBean(Session session, Object object) {
        String path = this.getPath(session, object);
        try {
            if (!session.nodeExists(path)) {
                return;
            }
            ClassDescriptor classDescriptor;
            Node node = session.getNode(path);
            if (node.hasProperty(ManagerConstant.DISCRIMINATOR_CLASS_NAME_PROPERTY)) {
                String className = node.getProperty(ManagerConstant.DISCRIMINATOR_CLASS_NAME_PROPERTY).getValue().getString();
                classDescriptor = mapper.getClassDescriptorByClass(ReflectionUtils.forName(className));
            } else {
                String nodeType = node.getPrimaryNodeType().getName();
                if (nodeType.equals(ManagerConstant.FROZEN_NODE_TYPE)) {
                    nodeType = node.getProperty(ManagerConstant.FROZEN_PRIMARY_TYPE_PROPERTY).getString();
                }
                classDescriptor = mapper.getClassDescriptorByNodeType(nodeType);
            }

            if (null == classDescriptor) {
                throw new JcrMappingException("Impossible to find the classdescriptor for " + path
                        + ". There is no discriminator and associated  JCR node type");
            }

            simpleFieldsHelp.retrieveSimpleFields(session, classDescriptor, node, object);
//            retrieveBeanFields(session, classDescriptor, node, object, false);
//            retrieveCollectionFields(session, classDescriptor, node, object, false);

        } catch (PathNotFoundException pnfe) {
            // HINT should never get here
            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
        }

    }
    

    
}
