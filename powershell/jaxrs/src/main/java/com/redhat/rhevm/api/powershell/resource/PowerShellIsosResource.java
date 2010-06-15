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
package com.redhat.rhevm.api.powershell.resource;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.rhevm.api.model.Iso;
import com.redhat.rhevm.api.model.Isos;
import com.redhat.rhevm.api.resource.IsoResource;
import com.redhat.rhevm.api.resource.IsosResource;
import com.redhat.rhevm.api.powershell.model.PowerShellIso;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;

public class PowerShellIsosResource implements IsosResource {

    private String dataCenterId;

    public PowerShellIsosResource(String dataCenterId) {
        this.dataCenterId = dataCenterId;
    }

    @Override
    public Isos list(UriInfo uriInfo) {
        Isos ret = new Isos();
        for (Iso iso : PowerShellIso.parse(PowerShellCmd.runCommand("get-isoimages -datacenterid " + dataCenterId))) {
            ret.getIsos().add(PowerShellIsoResource.addLinks(iso, dataCenterId));
        }
        return ret;
    }

    @Override
    public IsoResource getIsoSubResource(UriInfo uriInfo, String id) {
        return new PowerShellIsoResource(id, dataCenterId);
    }
}
