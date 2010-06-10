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

import java.util.ArrayList;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.rhevm.api.model.Iso;
import com.redhat.rhevm.api.resource.IsoResource;


public class PowerShellIsoResource implements IsoResource {

    private static String id;

    public PowerShellIsoResource(String id) {
        this.id = id;
    }

    public static Iso addLinks(Iso iso, UriInfo uriInfo, UriBuilder uriBuilder) {
        iso.setHref(uriBuilder.build().toString());
        return iso;
    }

    @Override
    public Iso get(UriInfo uriInfo) {
        Iso iso = new Iso();
        iso.setId(id);
        iso.setName(id);

        return addLinks(iso, uriInfo, uriInfo.getRequestUriBuilder());
    }
}