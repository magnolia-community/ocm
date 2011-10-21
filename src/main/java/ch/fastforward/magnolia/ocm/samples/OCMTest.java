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
package ch.fastforward.magnolia.ocm.samples;

import info.magnolia.context.MgnlContext;
import info.magnolia.module.admininterface.TemplatedMVCHandler;
import ch.fastforward.magnolia.ocm.ext.MgnlConfigMapperImpl;
import ch.fastforward.magnolia.ocm.ext.MgnlObjectConverterImpl;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.impl.RequestObjectCacheImpl;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author will
 */
public class OCMTest extends TemplatedMVCHandler {

    private static Logger log = LoggerFactory.getLogger(OCMTest.class);
    private HashMap errors, messages;
    private Collection pressReleases;

    public OCMTest(String name, HttpServletRequest request, HttpServletResponse response) {
        super(name, request, response);
        errors = new HashMap();
        messages = new HashMap();
        pressReleases = null;
    }

    public String createPressRelease() {
        Mapper mapper = new MgnlConfigMapperImpl();
        RequestObjectCacheImpl requestObjectCache = new RequestObjectCacheImpl();
        DefaultAtomicTypeConverterProvider converterProvider = new DefaultAtomicTypeConverterProvider();
        MgnlObjectConverterImpl oc = new MgnlObjectConverterImpl(mapper, converterProvider, new ProxyManagerImpl(), requestObjectCache);
        ObjectContentManager ocm = new ObjectContentManagerImpl(MgnlContext.getHierarchyManager("data").getWorkspace().getSession(), mapper);
        ((ObjectContentManagerImpl) ocm).setObjectConverter(oc);
        ((ObjectContentManagerImpl) ocm).setRequestObjectCache(requestObjectCache);

        PressRelease pr = new PressRelease();
//      pr.setPath("/ocmtest/pressreleases/1");
        pr.setParentPath("/ocmtest/pressreleases");
        pr.setDate(new Date());
        pr.setTitle("Press Release created by OCM");
        pr.setSubTitle("Efforts to extend Content2Bean features in Magnolia");
        pr.setContent("In many cases the Content objects are simply insufficient. "
                + "Custom objets with business logic are needed. Content2Bean only "
                + "covers the basic cases. The use of Jackrabbit OCM should help "
                + "solve these problems - except that OCM has its share of "
                + "problems too...");
        pr.setRating((double) 3.1234);
        pr.setImpressions(0);
        Author author = new Author();
        author.setFirstname("Will");
        author.setLastname("Scheidegger");
        author.setEmail("willscheidegger@mac.com");
        pr.setAuthor(author);
        pr.addToUrls(new URL("http://www.fastforward.ch", "fastforward websolutions", "Custom websites for demanding customers"));
        pr.addToUrls(new URL("http://www.schneestärn.ch", "schneestärn.ch", "Sportive and relaxed moments in the snow"));
        ocm.insert(pr);
        ocm.save();

        return loadPressReleases();
    }

    public String loadPressReleases() {
        log.debug("loadPressReleases()...");
        Mapper mapper = new MgnlConfigMapperImpl();
        ObjectContentManager ocm = new ObjectContentManagerImpl(MgnlContext.getHierarchyManager("data").getWorkspace().getSession(), mapper);
        pressReleases = ocm.getObjects("select * from ocmSamplePressRelease where jcr:path like '/ocmtest/pressreleases/%'", "sql");
        log.debug("Press releases retreived: " + pressReleases);
        return VIEW_SUCCESS;
    }

    public String updatePressRelease() {
        log.debug("updatePressRelease()...");
        Mapper mapper = new MgnlConfigMapperImpl();
        ObjectContentManager ocm = new ObjectContentManagerImpl(MgnlContext.getHierarchyManager("data").getWorkspace().getSession(), mapper);
        PressRelease pr = (PressRelease) ocm.getObjectByUuid(MgnlContext.getParameter("uuid"));
        if (pr != null) {
            pr.setTitle(pr.getTitle() + " [UPDATED!]");
            ocm.update(pr);
            ocm.save();
        }
        return loadPressReleases();
    }

    public String deletePressRelease() {
        log.debug("deletePressRelease()...");
        Mapper mapper = new MgnlConfigMapperImpl();
        ObjectContentManager ocm = new ObjectContentManagerImpl(MgnlContext.getHierarchyManager("data").getWorkspace().getSession(), mapper);
        PressRelease pr = (PressRelease) ocm.getObjectByUuid(MgnlContext.getParameter("uuid"));
        if (pr != null) {
            ocm.remove(pr);
            ocm.save();
        }
        return loadPressReleases();
    }

    /**
     * @return the errors
     */
    public HashMap getErrors() {
        return errors;
    }

    /**
     * @return the messages
     */
    public HashMap getMessages() {
        return messages;
    }

    /**
     * @return the pressReleases
     */
    public Collection getPressReleases() {
        return pressReleases;
    }

    /**
     * @param pressReleases the pressReleases to set
     */
    public void setPressReleases(Collection pressReleases) {
        this.pressReleases = pressReleases;
    }
}
