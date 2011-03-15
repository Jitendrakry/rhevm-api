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
package com.redhat.rhevm.api.mock.resource;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.rhevm.api.model.API;
import com.redhat.rhevm.api.model.ApiSummary;
import com.redhat.rhevm.api.model.Hosts;
import com.redhat.rhevm.api.model.LinkHeader;
import com.redhat.rhevm.api.model.Link;
import com.redhat.rhevm.api.model.ObjectFactory;
import com.redhat.rhevm.api.model.StorageDomains;
import com.redhat.rhevm.api.model.Users;
import com.redhat.rhevm.api.model.VMs;
import com.redhat.rhevm.api.resource.ApiResource;
import com.redhat.rhevm.api.common.util.JAXBHelper;

import static com.redhat.rhevm.api.common.util.LinkHelper.combine;

public class MockApiResource implements ApiResource {

    private static final String SEARCH_RELATION = "/search";
    private static final String SEARCH_TEMPLATE = "?search={query}";

    protected final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    protected UriInfo ui;

    public UriInfo getUriInfo() {
        return ui;
    }

    @Context
    public void setUriInfo(UriInfo uriInfo) {
        ui = uriInfo;
    }

    private API addLinks(API api) {
        addLink(api, "capabilities", false);
        addLink(api, "clusters");
        addLink(api, "datacenters");
        addLink(api, "hosts");
        addLink(api, "networks", false); // powershell has no select-network command
        addLink(api, "roles", false);
        addLink(api, "storagedomains");
        addLink(api, "templates");
        addLink(api, "users");
        addLink(api, "vms");
        return api;
    }

    private void addLink(API api, String rel, boolean searchable) {
        Link link = new Link();
        link.setRel(rel);
        link.setHref(combine(getUriInfo().getBaseUri().getPath(), rel));
        api.getLinks().add(link);

        if (searchable) {
            link = new Link();
            link.setRel(rel + SEARCH_RELATION);
            link.setHref(combine(getUriInfo().getBaseUri().getPath(), rel) + SEARCH_TEMPLATE);
            api.getLinks().add(link);
        }
    }

    private void addLink(API api, String rel) {
        addLink(api, rel, true);
    }

    private String addPath(UriBuilder uriBuilder, Link link) {
        String query = "";
        String path = relative(link);

        // otherwise UriBuilder.build() will substitute {query}
        if (path.contains("?")) {
            query = path.substring(path.indexOf("?"));
            path = path.substring(0, path.indexOf("?") - 1);
        }

        link = JAXBHelper.clone(OBJECT_FACTORY.createLink(link));
        link.setHref(uriBuilder.clone().path(path).build() + query);

        return LinkHeader.format(link);
    }

    private void addHeader(API api, Response.ResponseBuilder responseBuilder, UriBuilder uriBuilder) {
        // concantenate links in a single header with a comma-separated value,
        // which is the canonical form according to:
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
        //
        StringBuffer header = new StringBuffer();

        for (Link l : api.getLinks()) {
            header.append(addPath(uriBuilder, l)).append(",");
        }

        header.setLength(header.length() - 1);

        responseBuilder.header("Link", header);
    }

    private Response.ResponseBuilder getResponseBuilder(API api) {
        UriBuilder uriBuilder = getUriInfo().getBaseUriBuilder();

        Response.ResponseBuilder responseBuilder = Response.ok();

        addHeader(api, responseBuilder, uriBuilder);

        return responseBuilder;
    }

    private ApiSummary addSummary(API api) {
        ApiSummary summary = new ApiSummary();

        summary.setHosts(new Hosts());
        summary.getHosts().setActive(1L);
        summary.getHosts().setTotal(2L);

        summary.setStorageDomains(new StorageDomains());
        summary.getStorageDomains().setActive(2L);
        summary.getStorageDomains().setTotal(4L);

        summary.setUsers(new Users());
        summary.getUsers().setActive(4L);
        summary.getUsers().setTotal(8L);

        summary.setVMs(new VMs());
        summary.getVMs().setActive(8L);
        summary.getVMs().setTotal(16L);

        return summary;
    }

    @Override
    public Response head() {
        API api = addLinks(new API());
        return getResponseBuilder(api).build();
    }

    @Override
    public Response get() {
        API api = addLinks(new API());
        api.setSummary(addSummary(api));
        return getResponseBuilder(api).entity(api).build();
    }

    private String relative(Link link) {
        return link.getHref().substring(link.getHref().indexOf(link.getRel().split("/")[0]), link.getHref().length());
    }
}
