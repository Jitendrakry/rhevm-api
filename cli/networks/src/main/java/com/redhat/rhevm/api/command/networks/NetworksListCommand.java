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
package com.redhat.rhevm.api.command.networks;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import com.redhat.rhevm.api.command.base.AbstractListCommand;
import com.redhat.rhevm.api.model.Network;
import com.redhat.rhevm.api.model.Networks;

/**
 * Displays the Networks
 */
@Command(scope = "networks", name = "list", description = "Lists Networks.")
public class NetworksListCommand extends AbstractListCommand<Network> {

    @Option(name = "-b", aliases = {"--bound"}, description="Upper bound on number of Networks to display", required = false, multiValued = false)
    protected int limit = Integer.MAX_VALUE;

    @Option(name = "-s", aliases = {"--search"}, description="Query constraint on ISOs", required = false, multiValued = false)
    protected String constraint;

    protected Object doExecute() throws Exception {
        doList(client.getCollection("networks", Networks.class, constraint).getNetworks(), limit);
        return null;
    }
}
