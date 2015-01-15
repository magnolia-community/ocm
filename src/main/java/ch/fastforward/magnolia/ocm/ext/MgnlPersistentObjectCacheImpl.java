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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.cache.impl.RequestObjectCacheImpl;

/**
 * This implementation of {@link ObjectCache} will ignore calls to clear(). This
 * should prevent objects to get get loaded multiple times. In case you need to
 * clear the cache use clearPersistantCache().
 *
 * @author will
 */
public class MgnlPersistentObjectCacheImpl extends RequestObjectCacheImpl implements ObjectCache, MgnlPersistentObjectCache, Serializable {

    private Map persistantCachedObjects = new HashMap();

    /*
     * Does NOT clear the cache as duplicate objects will be created next time
     * you're loading them. Use {@link clearPersistanceCache()} to really clear
     * the cache if you need to.
     */
    /*    @Override
     public void clear() {
     // do NOT clear the cache!
     }*/
    /**
     * Caches an object both in the request cache and the persistent cache.
     *
     * @param path
     * @param object
     */
    @Override
    public void cache(String path, Object object) {
        persistantCachedObjects.put(path, object);
        super.cache(path, object);
    }

    /**
     * This really clears the cache. It's probably better to simply add the
     * caches to the users servlet session so that they will be deleted when the
     * session expires.
     */
    public void clearPersistantCache() {
        super.clear();
        persistantCachedObjects.clear();
    }

    /**
     * Gets object from the persistent cache.
     *
     * @param path
     * @return
     */
    @Override
    public Object getObject(String path) {
        return persistantCachedObjects.get(path);
    }

    /**
     * Checks if the object exists in the persistent cache. If you want to know
     * if the object is in the request cache use isRequestCached().
     * @param path
     * @return
     */
    @Override
    public boolean isCached(String path) {
        return persistantCachedObjects.containsKey(path);
    }

    public boolean isRequestCached(String path) {
        return (super.isCached(path));
    }
}
