package ch.fastforward.magnolia.ocm.ext;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;

import javax.jcr.Session;

/**
 * Created by will on 31.05.15.
 */
public class MgnlObjectContentManagerImpl extends ObjectContentManagerImpl {

    public MgnlObjectContentManagerImpl(Session session, Mapper mapper) {
        super(session, mapper);
    }

    public void move(Object object, String destPath) {
        // first, make sure that the parent node exists
        MgnlObjectConverterImpl.getParentNode(session, StringUtils.substringBeforeLast(destPath, "/"), object);
        // then move
        super.move(objectConverter.getPath(session, object), destPath);
    }
}
