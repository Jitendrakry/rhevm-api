/*
 * Copyright © 2010 Red Hat, Inc.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.redhat.rhevm.api.command.vmpools;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

import com.redhat.rhevm.api.command.base.AbstractUpdateCommand;
import com.redhat.rhevm.api.model.VmPool;
import com.redhat.rhevm.api.model.VmPools;

/**
 * Update a VM Pool
 */
@Command(scope = "vmpools", name = "update", description = "Update VM Pool state")
public class VmPoolsUpdateCommand extends AbstractUpdateCommand<VmPool> {

    @Argument(index = 0, name = "name", description = "The name of the VM Pool", required = true, multiValued = false)
    protected String name;

    protected Object doExecute() throws Exception {
        doUpdate(client.getCollection("vmpools", VmPools.class).getVmPools(), VmPool.class, "cluster", name);
        return null;
    }
}

