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
package ch.fastforward.magnolia.ocm.beans;

import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;

import java.util.Map;

/**
 * Proxy class to hold class descriptors stored in JCR. This class should be used for Node2Bean instead of
 * {@link ClassDescriptor} because Node2Bean cannot properly construct it directly. Not all fields are implemented.
 */
public class ProxyClassDescriptor {

    private String className;
    /**
     * Custom property. What jcr type should be used if creating a parent.
     */
    private String parentJcrType;
    private String jcrType;
    /**
     * list of super types with comma followed by space as delimiter.
     */
    private String jcrSuperTypes;
    /**
     * list of mixin types with comma followed by space as delimiter.
     */
    private String jcrMixinTypes;
    private Map<String, FieldDescriptor> fieldDescriptors;
    private FieldDescriptor idFieldDescriptor;
    private FieldDescriptor pathFieldDescriptor;
    private FieldDescriptor uuidFieldDescriptor;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getParentJcrType() {
        return parentJcrType;
    }

    public void setParentJcrType(String parentJcrType) {
        this.parentJcrType = parentJcrType;
    }

    public String getJcrType() {
        return jcrType;
    }

    public void setJcrType(String jcrType) {
        this.jcrType = jcrType;
    }

    public String getJcrSuperTypes() {
        return jcrSuperTypes;
    }

    public void setJcrSuperTypes(String jcrSuperTypes) {
        this.jcrSuperTypes = jcrSuperTypes;
    }

    public String getJcrMixinTypes() {
        return jcrMixinTypes;
    }

    public void setJcrMixinTypes(String jcrMixinTypes) {
        this.jcrMixinTypes = jcrMixinTypes;
    }

    public Map<String, FieldDescriptor> getFieldDescriptors() {
        return fieldDescriptors;
    }

    public void setFieldDescriptors(Map<String, FieldDescriptor> fieldDescriptors) {
        this.fieldDescriptors = fieldDescriptors;
    }

    public FieldDescriptor getIdFieldDescriptor() {
        return idFieldDescriptor;
    }

    public void setIdFieldDescriptor(FieldDescriptor idFieldDescriptor) {
        this.idFieldDescriptor = idFieldDescriptor;
    }

    public FieldDescriptor getPathFieldDescriptor() {
        return pathFieldDescriptor;
    }

    public void setPathFieldDescriptor(FieldDescriptor pathFieldDescriptor) {
        this.pathFieldDescriptor = pathFieldDescriptor;
    }

    public FieldDescriptor getUuidFieldDescriptor() {
        return uuidFieldDescriptor;
    }

    public void setUuidFieldDescriptor(FieldDescriptor uuidFieldDescriptor) {
        this.uuidFieldDescriptor = uuidFieldDescriptor;
    }

    public ClassDescriptor asClassDescriptor() {
        ClassDescriptor descriptor = new ClassDescriptor();
        descriptor.setClassName(className);
        descriptor.setJcrSuperTypes(jcrSuperTypes);
        descriptor.setJcrMixinTypes(jcrMixinTypes);
        descriptor.setJcrType(jcrType);
        for (FieldDescriptor fieldDescriptor : fieldDescriptors.values()) {
            descriptor.addFieldDescriptor(fieldDescriptor);
        }
        if (pathFieldDescriptor != null) descriptor.addFieldDescriptor(pathFieldDescriptor);
        if (uuidFieldDescriptor != null) descriptor.addFieldDescriptor(uuidFieldDescriptor);
        if (idFieldDescriptor != null) descriptor.addFieldDescriptor(idFieldDescriptor);
        return descriptor;
    }
}
