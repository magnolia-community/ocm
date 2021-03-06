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
package ch.fastforward.magnolia.ocm.atomictypeconverter;

import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calendar Type Converter.
 */
public class CalendarTypeConverterImpl implements AtomicTypeConverter {

    private Logger log = LoggerFactory.getLogger(CalendarTypeConverterImpl.class);

    @Override
    public Value getValue(ValueFactory valueFactory, Object propValue) {
        if (propValue == null) {
            return null;
        }

        return valueFactory.createValue((Calendar) propValue);
    }

    @Override
    public Object getObject(Value value) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(value.getDate().getTimeInMillis());
            return calendar;
        } catch (RepositoryException e) {
            try {
                log.error("could not get date value from " + value.getString(), e);
            } catch (IllegalStateException ex) {
                log.error("could not get date and not even string value from " + value, e);
            } catch (RepositoryException ex) {
                log.error("could not get date and not even string value from " + value, e);
            }
            return null;
        }
    }

    @Override
    public String getXPathQueryValue(ValueFactory valueFactory, Object object) {
        Calendar calendar = (Calendar) object;
        //@TODO: This method should output something like "xs:dateTime('2010-10-23T00:00:00.000+02:00')"
        return new Long(calendar.getTimeInMillis()).toString();
    }
}
