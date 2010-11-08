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
package com.redhat.rhevm.api.resource;

import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;

import com.redhat.rhevm.api.model.Action;
import com.redhat.rhevm.api.model.Actionable;
import com.redhat.rhevm.api.model.Host;


@Produces(MediaType.APPLICATION_XML)
public interface HostResource extends UpdatableResource<Host> {

    @Path("{action: (approve|install|fence|activate|deactivate|commitnetconfig|iscsidiscover|iscsilogin)}/{oid}")
    public ActionResource getActionSubresource(@PathParam("action")String action, @PathParam("oid")String oid);

    @POST
    @Formatted
    @Actionable
    @Path("approve")
    public Response approve(Action action);

    @POST
    @Formatted
    @Actionable
    @Path("install")
    public Response install(Action action);

    @POST
    @Formatted
    @Actionable
    @Path("fence")
    public Response fence(Action action);

    @POST
    @Formatted
    @Actionable
    @Path("activate")
    public Response activate(Action action);

    @POST
    @Formatted
    @Actionable
    @Path("deactivate")
    public Response deactivate(Action action);

    @POST
    @Formatted
    @Actionable
    @Path("commitnetconfig")
    public Response commitNetConfig(Action action);

    @POST
    @Formatted
    @Actionable
    @Path("iscsidiscover")
    public Response iscsiDiscover(Action action);

    @POST
    @Formatted
    @Actionable
    @Path("iscsilogin")
    public Response iscsiLogin(Action action);

    @Path("nics")
    public HostNicsResource getHostNicsResource();

    @Path("storage")
    public HostStorageResource getHostStorageResource();

    @Path("tags")
    public AssignedTagsResource getTagsResource();

    @Path("permissions")
    public AssignedPermissionsResource getPermissionsResource();
}
