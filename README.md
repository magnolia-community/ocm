# Magnolia Module OCM

Jackrabbit Object Content Mapper implementation for Magnolia.

>Note: OCM is a deprecated specification. Consider using another approach for new projects.

## Upgrading to version 1.5+ for Magnolia 6+.
In Magnolia 6+ the Node2Bean mechanism works differently and can no longer construct instances of
`org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor` directly. Use `ch.fastforward.magnolia.ocm.beans.ProxyClassDescriptor` as definition class instead.

## Configuration
Objects to be managed need to be described by *class descriptors* in `/modules/ocm/config/classDescriptors`.
The configuration is a list of sub-nodes, i.e. class descriptions. A class description has the form (please see `ch.fastforward.magnolia.ocm.beans.ProxyClassDescriptor` for a full list of supported properties):

| name | type |description |
|------|------|------------|
| class | String (required) | Qualified name of the class for this definition. Used by Magnolias Node2Bean mechanism. This should be `ch.fastforward.magnolia.ocm.beans.ProxyClassDescriptor`. |
| className | String (required) | Qualified name of the Java bean class that is described by this descriptor. |
| jcrType | String (required) | Node type of the object. |
| parentJcrType | String | Type of a parent node of the object in the JCR structure. This is not necessarily the hierarchical super-type of the node. |
| nextBeanId | Long | Used by the OCM mechanism to keep track of the bean id. |
| fieldDescriptors | Map of *field descriptors* | The fields of the object, each specified by a field descriptor (see below). |

A *field descriptor* has the form (see `org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor` for a full list of all supported properties and refer to the JCR OCM documentation.):

| name | type |description |
|------|------|------------|
| class | String (required) | Qualified name of the class for this definition. Used by Magnolias Node2Bean mechanism. This should be `org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor` or a subclass. |
| fieldName | String (required) | Name of the field in the class. |
| jcrName | String | Name of the field in JCR. |
| path | boolean | true if this field represents the path of the object. |
| uuid | boolean | true if this field represents the uuid of the object. |

Example:

```yaml
CardOrder: 
  className: ch.fastforward.beans.CardOrder
  class: ch.fastforward.magnolia.ocm.beans.ProxyClassDescriptor
  jcrType: cardOrder
  nextBeanID: 1
  parentJcrType: mgnl:folder
  fieldDescriptors: 
    path: 
      class: org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor
      fieldName: path
      path: true
    uuid: 
      class: org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor
      fieldName: uuid
      uuid: true
    name: 
      class: org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor
      fieldName: name
      jcrName: name
    cardNumber: 
      class: org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor
      fieldName: cardNumber
      jcrName: cardNumber
```
