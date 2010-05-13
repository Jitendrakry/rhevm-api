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
package com.redhat.rhevm.api.dummy.resource;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.rhevm.api.common.util.JAXBHelper;
import com.redhat.rhevm.api.model.Action;
import com.redhat.rhevm.api.model.ActionsBuilder;
import com.redhat.rhevm.api.model.ActionValidator;
import com.redhat.rhevm.api.model.Link;
import com.redhat.rhevm.api.model.StorageDomain;
import com.redhat.rhevm.api.resource.AttachmentsResource;
import com.redhat.rhevm.api.resource.StorageDomainResource;

import static com.redhat.rhevm.api.model.StorageDomainStatus.*;

public class DummyStorageDomainResource extends AbstractDummyResource<StorageDomain> implements StorageDomainResource {

    private DummyAttachmentsResource attachments;

    /**
     * Package-protected ctor, never needs to be instantiated by JAX-RS framework.
     *
     * @param storageDomain  encapsulated StorageDomain
     */
    DummyStorageDomainResource(String id) {
        super(id);
        getModel().setStatus(UNINITIALIZED);
        this.attachments = new DummyAttachmentsResource(id);
    }

    protected StorageDomain newModel() {
        return new StorageDomain();
    }

    // FIXME: this needs to be atomic
    public void updateModel(StorageDomain storageDomain) {
        // update writable fields only
        getModel().setName(storageDomain.getName());
    }

    public StorageDomain addLinks(UriBuilder uriBuilder) {
        StorageDomain storageDomain = JAXBHelper.clone(OBJECT_FACTORY.createStorageDomain(getModel()));
        storageDomain.setHref(uriBuilder.build().toString());

        ActionValidator actionValidator = new StorageDomainActionValidator(storageDomain);
        ActionsBuilder actionsBuilder = new ActionsBuilder(uriBuilder, StorageDomainResource.class, actionValidator);
        storageDomain.setActions(actionsBuilder.build());

        Link link = new Link();
        link.setRel("attachments");
        link.setHref(uriBuilder.clone().path("attachments").build().toString());
        storageDomain.getLinks().clear();
        storageDomain.getLinks().add(link);
        return storageDomain;
    }

    @Override
    public StorageDomain get(UriInfo uriInfo) {
        return addLinks(uriInfo.getRequestUriBuilder());
    }

    @Override
    public StorageDomain update(HttpHeaders headers, UriInfo uriInfo, StorageDomain storageDomain) {
        validateUpdate(storageDomain, headers);
        updateModel(storageDomain);
        return addLinks(uriInfo.getRequestUriBuilder());
    }

    @Override
    public void initialize(UriInfo uriInfo, Action action) {
        // FIXME: error if not uninitialized
        getModel().setStatus(UNATTACHED);
    }

    @Override
    public void teardown(UriInfo uriInfo, Action action) {
        // FIXME: error if not unattached
        getModel().setStatus(UNINITIALIZED);
    }

    public AttachmentsResource getAttachmentsResource() {
        return attachments;
    }

    /**
     * Build a URI for any existing attachment to the given data center
     *
     * @param uriInfo  URI context of the current request
     * @param dataCenterId  the ID of the data center
     * @return  a URI representing the attachment
     */
    public String getAttachmentHref(UriInfo uriInfo, String dataCenterId) {
        return attachments.getAttachmentHref(uriInfo, dataCenterId);
    }

    private class StorageDomainActionValidator implements ActionValidator {
        private StorageDomain storageDomain;

        public StorageDomainActionValidator(StorageDomain storageDomain) {
            this.storageDomain = storageDomain;
        }

        @Override
        public boolean validateAction(String action) {
            switch (storageDomain.getStatus()) {
            case UNINITIALIZED:
                return action.equals("initialize");
            case UNATTACHED:
            case ACTIVE:
            case INACTIVE:
                return false;
            case LOCKED:
            case MIXED:
            default:
                assert false : storageDomain.getStatus();
                return false;
            }
        }
    }
}
