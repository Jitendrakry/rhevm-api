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
package com.redhat.rhevm.api.command.hosts;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import com.redhat.rhevm.api.command.base.AbstractAddCommand;
import com.redhat.rhevm.api.model.Host;

/**
 * Add a new Host.
 */
@Command(scope = "hosts", name = "add", description = "Add a new Host")
public class HostsAddCommand extends AbstractAddCommand<Host> {

    @Argument(index = 0, name = "name", description = "Name of the Host to add", required = true, multiValued = false)
    protected String name;

    @Option(name = "-a", aliases = { "--address" }, description = "Host address", required = true, multiValued = false)
    private String address;

    protected Object doExecute() throws Exception {
        display(doAdd(getModel(), Host.class, "hosts", "host"));
        return null;
    }

    protected Host getModel() {
        Host model = new Host();
        model.setName(name);
        model.setAddress(address);
        return model;
    }
}

