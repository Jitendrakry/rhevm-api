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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.text.MessageFormat;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.rhevm.api.model.Fault;
import com.redhat.rhevm.api.model.VmPool;

import com.redhat.rhevm.api.powershell.util.PowerShellCmd;

import org.junit.Test;

import static org.easymock.classextension.EasyMock.expect;
import static org.easymock.classextension.EasyMock.isA;

import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;


public class PowerShellVmPoolResourceTest extends AbstractPowerShellResourceTest<VmPool, PowerShellVmPoolResource> {

    private static final String POOL_ID = "1337";
    private static final String POOL_NAME = "fionnula";
    private static final String BAD_ID = "98765";
    private static final String NEW_NAME = "fidelma";

    private static final String GET_COMMAND = "get-vmpool -vmpoolid " + POOL_ID;
    private static final String GET_RETURN = "vmpoolid: " + POOL_ID + "\nname: " + POOL_NAME + PowerShellVmPoolsResourceTest.GET_RETURN_EPILOG;

    private static final String UPDATE_COMMAND = "$v = get-vmpool " + POOL_ID + "\n$v.name = '" + NEW_NAME + "'\nupdate-vmpool -vmpoolobject $v";
    private static final String UPDATE_RETURN = "vmpoolid: " + POOL_ID + "\nname: " + NEW_NAME + PowerShellVmPoolsResourceTest.GET_RETURN_EPILOG;

    protected PowerShellVmPoolResource getResource() {
        return new PowerShellVmPoolResource(POOL_ID);
    }

    @Test
    public void testGet() throws Exception {
        String [] commands = { GET_COMMAND,
                               PowerShellVmPoolsResourceTest.LOOKUP_CLUSTER_COMMAND,
                               PowerShellVmPoolsResourceTest.LOOKUP_TEMPLATE_COMMAND };
        String [] returns  = { GET_RETURN,
                               PowerShellVmPoolsResourceTest.LOOKUP_CLUSTER_RETURN,
                               PowerShellVmPoolsResourceTest.LOOKUP_TEMPLATE_RETURN };

        verifyVmPool(
            resource.get(setUpVmExpectations(commands, returns, POOL_NAME)),
            POOL_NAME);
    }

    @Test
    public void testGoodUpdate() throws Exception {
        String [] commands = { UPDATE_COMMAND,
                               PowerShellVmPoolsResourceTest.LOOKUP_CLUSTER_COMMAND,
                               PowerShellVmPoolsResourceTest.LOOKUP_TEMPLATE_COMMAND };
        String [] returns  = { UPDATE_RETURN,
                               PowerShellVmPoolsResourceTest.LOOKUP_CLUSTER_RETURN,
                               PowerShellVmPoolsResourceTest.LOOKUP_TEMPLATE_RETURN };

        verifyVmPool(
            resource.update(createMock(HttpHeaders.class),
                            setUpVmExpectations(commands, returns, NEW_NAME),
                            getVmPool(NEW_NAME)),
            NEW_NAME);
    }

    @Test
    public void testBadUpdate() throws Exception {
        try {
            UriInfo uriInfo = createMock(UriInfo.class);
            resource.update(setUpHeadersExpectation(),
                            uriInfo,
                            getVmPool(BAD_ID, NEW_NAME));
            fail("expected WebApplicationException on bad update");
        } catch (WebApplicationException wae) {
            verifyUpdateException(wae);
        }
    }

    private UriInfo setUpVmExpectations(String commands[], String[] rets, String name) throws Exception {
        if (commands != null) {
            mockStatic(PowerShellCmd.class);
            for (int i = 0 ; i < Math.min(commands.length, rets.length) ; i++) {
                if (commands[i] != null) {
                    expect(PowerShellCmd.runCommand(commands[i])).andReturn(rets[i]);
                }
            }
        }
        String href = URI_ROOT + "/vmpools/" + POOL_ID;
        UriInfo uriInfo = createMock(UriInfo.class);
        UriBuilder uriBuilder = createMock(UriBuilder.class);
        expect(uriInfo.getRequestUriBuilder()).andReturn(uriBuilder).anyTimes();
        expect(uriBuilder.build()).andReturn(new URI(href)).anyTimes();
        UriBuilder baseBuilder = createMock(UriBuilder.class);
        expect(uriInfo.getBaseUriBuilder()).andReturn(baseBuilder);
        expect(baseBuilder.clone()).andReturn(baseBuilder).anyTimes();
        expect(baseBuilder.path(isA(String.class))).andReturn(baseBuilder).anyTimes();
        expect(baseBuilder.build()).andReturn(new URI(URI_ROOT + "/foo")).anyTimes();
        replayAll();

        return uriInfo;
    }

    private HttpHeaders setUpHeadersExpectation() {
        HttpHeaders headers = createMock(HttpHeaders.class);
        List<MediaType> mediaTypes = new ArrayList<MediaType>();
        mediaTypes.add(MediaType.APPLICATION_XML_TYPE);
        expect(headers.getAcceptableMediaTypes()).andReturn(mediaTypes).anyTimes();
        replayAll();
        return headers;
    }

    private VmPool getVmPool(String name) {
        return getVmPool(POOL_ID, name);
    }

    private VmPool getVmPool(String id, String name) {
        VmPool pool = new VmPool();
        pool.setId(id);
        pool.setName(name);
        return pool;
    }

    private void verifyVmPool(VmPool pool, String name) {
        assertNotNull(pool);
        assertEquals(pool.getId(), POOL_ID);
        assertEquals(pool.getName(), name);
    }

    private void verifyUpdateException(WebApplicationException wae) {
        assertEquals(409, wae.getResponse().getStatus());
        Fault fault = (Fault)wae.getResponse().getEntity();
        assertNotNull(fault);
        assertEquals("Broken immutability constraint", fault.getReason());
        assertEquals("Attempt to set immutable field: id", fault.getDetail());
    }
}