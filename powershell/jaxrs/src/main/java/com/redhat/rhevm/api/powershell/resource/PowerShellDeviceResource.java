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

import javax.ws.rs.WebApplicationException;

import com.redhat.rhevm.api.model.BaseDevice;
import com.redhat.rhevm.api.model.BaseDevices;
import com.redhat.rhevm.api.resource.DeviceResource;


public class PowerShellDeviceResource<D extends BaseDevice, C extends BaseDevices>
    extends PowerShellReadOnlyDeviceResource<D, C>
    implements DeviceResource<D> {

    protected static final int NOT_IMPLEMENTED = 501;

    public PowerShellDeviceResource(AbstractPowerShellDevicesResource<D, C> parent, String deviceId) {
        super(parent, deviceId);
    }

    @Override
    public D update(D device) {
        throw new WebApplicationException(NOT_IMPLEMENTED);
    }
}
