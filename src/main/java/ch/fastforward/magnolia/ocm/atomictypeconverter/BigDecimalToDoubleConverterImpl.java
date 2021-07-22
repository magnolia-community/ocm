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

import java.math.BigDecimal;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.apache.jackrabbit.ocm.exception.IncorrectAtomicTypeException;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter;

/**
 * Converts BigDecimals to Doubles when persisting an object in JCR.
 * @see org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter
 * @author will
 */
public class BigDecimalToDoubleConverterImpl implements AtomicTypeConverter {

    @Override
    public Value getValue(ValueFactory valueFactory, Object propValue) {
        if (propValue == null) {
            return null;
        }
        return valueFactory.createValue(((BigDecimal) propValue).doubleValue());
    }

    @Override
    public Object getObject(Value value) {
        try {
            double val = value.getDouble();
            return new BigDecimal("" + val);
        } catch (RepositoryException e) {
            throw new IncorrectAtomicTypeException("Impossible to convert the value : " + value.toString(), e);
        }
    }

    @Override
    public String getXPathQueryValue(ValueFactory vf, Object object) {
        return object.toString();
    }
}
