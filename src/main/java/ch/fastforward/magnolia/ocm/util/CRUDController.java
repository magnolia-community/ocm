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
package ch.fastforward.magnolia.ocm.util;

import ch.fastforward.magnolia.ocm.beans.OCMBean;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CRUDController can be used for standard CRUD operations (Create,
 * Retrieve, Update, Delete) on OCM beans.
 * @author will
 */
public class CRUDController implements Serializable {
    private static Logger log = LoggerFactory.getLogger(CRUDController.class);
    private Collection<OCMBean> items;
    private OCMBean selectedItem;
    private int itemsPerPage, selectedPageIndex, maxNumberOfPaginationLinks = 0;
    private Map errorMessages;
    private Map searchParameters;

    /**
     * @return the beans
     */
    public Collection getItems() {
        return items;
    }

    /**
     * @param beans the beans to set
     */
    public void setItems(Collection beans) {
        this.items = beans;
    }

    /**
     * @return the itemsPerPage
     */
    public int getItemsPerPage() {
        return itemsPerPage;
    }

    /**
     * @param itemsPerPage the itemsPerPage to set
     */
    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * @return the selectedPageIndex
     */
    public int getSelectedPageIndex() {
        return selectedPageIndex;
    }

    /**
     * @param selectedPageIndex the selectedPageIndex to set
     */
    public void setSelectedPageIndex(int selectedPageIndex) {
        this.selectedPageIndex = selectedPageIndex;
    }

    /**
     * @return Returns the length of the beans list.
     */
    public int getNumberOfItems() {
        if (items != null && !items.isEmpty()) {
            return items.size();
        } else {
            return 0;
        }
    }

    /**
     * @return Returns the number of pages needed to display all records. If you
     * have 21 items and you want to display 10 items per page this method will
     * return method 3.
     */
    public int getNumberOfPages() {
        if (getNumberOfItems() > 0 && getItemsPerPage() > 0) {
            double batches = (double) getNumberOfItems() / (double) getItemsPerPage();
            return (int) Math.ceil(batches);
        } else {
            return 1;
        }
    }

    public int getPageFirstItemIndex() {
        if (this.getSelectedPageIndex() > 0) {
            return (this.getSelectedPageIndex() - 1) * getItemsPerPage();
        }
        return 0;
    }

    public int getPageLastItemIndex() {
        int index = getPageFirstItemIndex() + getItemsPerPage() - 1;
        if (index > getNumberOfItems()) {
            if (getNumberOfItems() > 0) {
                index = getNumberOfItems() - 1;
            }
        }
        return index;
    }

    /**
     * @return the maxNumberOfPaginationLinks
     */
    public int getMaxNumberOfPaginationLinks() {
        return maxNumberOfPaginationLinks;
    }

    /**
     * Sets the max number of pagination links. If there are more pages than this
     * the start and and page index will be set correspondingly.<br />
     * <br />
     * <b>Sample:</b> If you end up having 10 pages but only room to display 5
     * page links and you are currently display page # 4, page links 2 to 6
     * should be displayed. So getPaginationLinkIndexBegin() will return 2 and
     * getPaginationLinkIndexEnd() will return 6.<br />
     * <br />
     * <b>Notes:</b>
     * <ul>
     * <li>maxNumberOfPaginationLinks must be an odd number so the same
     * number of page links can be displayed to the left and right of the current
     * page. If an even number is passed in, it will be increased by 1</li>
     * <li>If the value is not set, all page links will be displayed</li>
     * </ul>
     * @param maxNumberOfPaginationLinks the maxNumberOfPaginationLinks to set
     */
    public void setMaxNumberOfPaginationLinks(int maxNumberOfPaginationLinks) {
        if (maxNumberOfPaginationLinks < 0) {
            this.maxNumberOfPaginationLinks = 0;
        } else if (maxNumberOfPaginationLinks % 2 == 0) {
            // only odd numbers allowd
            this.maxNumberOfPaginationLinks = maxNumberOfPaginationLinks + 1;
        } else {
            this.maxNumberOfPaginationLinks = maxNumberOfPaginationLinks;
        }
    }

    public int getPaginationLinkIndexBegin() {
        if (getMaxNumberOfPaginationLinks() > 0 && getMaxNumberOfPaginationLinks() < getNumberOfPages()) {
            int space = (getMaxNumberOfPaginationLinks() - 1) / 2;
            if (getSelectedPageIndex() - space > 1) {
                // do not start at the first page!
                if (getSelectedPageIndex() + space > getNumberOfPages()) {
                    return getNumberOfPages() - getMaxNumberOfPaginationLinks() + 1;
                } else {
                    return getSelectedPageIndex() - space;
                }
            }
        }
        return 1;
    }

    public int getPaginationLinkIndexEnd() {
        if (getMaxNumberOfPaginationLinks() > 0 && getMaxNumberOfPaginationLinks() < getNumberOfPages()) {
            int space = (getMaxNumberOfPaginationLinks() - 1) / 2;
            if (getSelectedPageIndex() + space < getNumberOfPages()) {
                // do not end at the last page!
                if (getSelectedPageIndex() - space < 1) {
                    return getMaxNumberOfPaginationLinks();
                } else {
                    return getSelectedPageIndex() + space;
                }
            }
        }
        return getNumberOfPages();
    }

    /**
     * @return the selectedItem
     */
    public OCMBean getSelectedItem() {
        return selectedItem;
    }

    /**
     * @param selectedItem the selectedItem to set
     */
    public void setSelectedItem(OCMBean selectedItem) {
        this.selectedItem = selectedItem;
    }

    /**
     * @return the errorMessages
     */
    public Map getErrorMessages() {
        if (errorMessages == null) {
            errorMessages = new HashMap();
        }
        return errorMessages;
    }

    /**
     * @param errorMessages the errorMessages to set
     */
    public void setErrorMessages(Map errorMessages) {
        this.errorMessages = errorMessages;
    }

    /**
     * @return the searchParameters
     */
    public Map getSearchParameters() {
        if (searchParameters == null) {
            searchParameters = new HashMap();
        }
        return searchParameters;
    }

    /**
     * @param searchParameters the searchParameters to set
     */
    public void setSearchParameters(Map searchParameters) {
        this.searchParameters = searchParameters;
    }

}
