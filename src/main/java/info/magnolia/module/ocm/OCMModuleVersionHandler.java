/**
 * This file Copyright (c) 2009-2011 Magnolia International
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
package info.magnolia.module.ocm;

import info.magnolia.module.DefaultModuleVersionHandler;
import info.magnolia.module.InstallContext;
import info.magnolia.module.data.setup.RegisterNodeTypeTask;
import info.magnolia.module.delta.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to handle installation and updates of your module.
 */
public class OCMModuleVersionHandler extends DefaultModuleVersionHandler {

    public OCMModuleVersionHandler() {
//        final Delta updateDelta = DeltaBuilder.update("1.0-SNAPSHOT", "This delta will be applied if your module is asked to be updated to version 1.0-SNAPSHOT")
//            .addTask(new SetPropertyTask(ContentRepository.CONFIG, "/modules/mymodule/config", "someProperty", "someValue"));
//
//        register(updateDelta);
        super();
    }

    /**
     *
     * @param installContext
     * @return a list with all the RegisterNodeTypeTask objects needed for the
     * shop node types
     */
    @Override
    protected List getBasicInstallTasks(InstallContext installContext) {
        final List<Task> installTasks = new ArrayList<Task>();
        // make sure we register the type before doing anything else
        installTasks.add(new RegisterNodeTypeTask("ocmSamplePressRelease"));
        installTasks.addAll(super.getBasicInstallTasks(installContext));
        return installTasks;
    }

    @Override
    protected List getExtraInstallTasks(InstallContext installContext) {
        final List installTasks = new ArrayList();
//        installTasks.add(new CreateNodeTask("Install module configuration", "Adds the config node to your module", ContentRepository.CONFIG, "/modules/mymodule/config", "someNode", ItemType.CONTENTNODE.getSystemName()));

        return installTasks;
    }
}
