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
package ch.fastforward.magnolia.ocm;

import info.magnolia.module.DefaultModuleVersionHandler;
import info.magnolia.module.delta.DeltaBuilder;
import info.magnolia.module.delta.RemoveNodeWoChildren;
import info.magnolia.module.delta.RemoveNodesTask;
import info.magnolia.repository.RepositoryConstants;

import java.util.Arrays;

/**
 * This class is used to handle installation and updates of your module.
 */
public class OCMModuleVersionHandler extends DefaultModuleVersionHandler {

    public OCMModuleVersionHandler() {

        register(DeltaBuilder.checkPrecondition("1.0", ""));

        register(DeltaBuilder.update("1.2", "")
                .addTask(new RemoveNodesTask("Remove configuration for OCM samples", "Remove legacy Magnolia 4.5.x OCM samples", RepositoryConstants.CONFIG,
                        Arrays.asList(
                                "/modules/adminInterface/config/menu/data/ocmSamplePressRelease",
                                "/modules/adminInterface/config/menu/tools/ocmTest",
                                "/modules/data/config/types/ocmSamplePressRelease",
                                "/modules/data/dialogs/ocmSamplePressRelease",
                                "/modules/data/trees/ocmSamplePressRelease",
                                "/modules/ocm/config/classDescriptors/PressRelease",
                                "/modules/ocm/config/classDescriptors/Author",
                                "/modules/ocm/config/classDescriptors/URL",
                                "/modules/ocm/pages"),
                        false))
        );

        register(DeltaBuilder.update("1.2.1", "")
                .addTask(new RemoveNodeWoChildren("Remove empty data section node in legacy menu", "Remove empty data section in legacy menu", RepositoryConstants.CONFIG, "/modules/adminInterface/config/menu/data"))
                .addTask(new RemoveNodeWoChildren("Remove empty tools section node in legacy menu", "Remove empty tools section in legacy menu", RepositoryConstants.CONFIG, "/modules/adminInterface/config/menu/tools"))
                .addTask(new RemoveNodeWoChildren("Remove empty legacy menu node", "Remove empty legacy menu node", RepositoryConstants.CONFIG, "/modules/adminInterface/config/menu"))
                .addTask(new RemoveNodeWoChildren("Remove empty legacy date trees node", "Remove empty legacy date trees node", RepositoryConstants.CONFIG, "/modules/data/trees/"))
        );

    }

}
