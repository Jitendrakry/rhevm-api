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

import java.text.MessageFormat;

import java.util.concurrent.Executor;

import com.redhat.rhevm.api.model.Action;
import com.redhat.rhevm.api.model.Host;
import com.redhat.rhevm.api.model.HostNIC;
import com.redhat.rhevm.api.model.HostNics;
import com.redhat.rhevm.api.model.Network;
import com.redhat.rhevm.api.model.Slaves;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;
import com.redhat.rhevm.api.powershell.util.PowerShellException;
import com.redhat.rhevm.api.powershell.util.PowerShellParser;
import com.redhat.rhevm.api.powershell.util.PowerShellPool;
import com.redhat.rhevm.api.powershell.util.PowerShellPoolMap;

import org.junit.Test;

import org.junit.runner.RunWith;

import static org.easymock.classextension.EasyMock.expect;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { PowerShellCmd.class })
public class PowerShellHostNicsResourceTest
    extends AbstractPowerShellResourceTest<Host, PowerShellHostResource> {

    private static final String HOST_NAME = "1234";

    private static final String[] NICS = { "eth0", "eth1" };
    private static final String[] NETS = { "net0", "net1" };
    private static final String[] MACS = { "00:31:20:5F:F9:DD", "00:31:20:5F:F9:EE" };
    private static final String[] IPS = { "192.168.1.7", "192.168.2.10" };
    private static final String[] SUBNETS = { "255.255.255.0", "255.255.255.0" };

    private static final String GET_NIC_CMD = "$h = get-host \"" + asId(HOST_NAME) + "\"; foreach ($n in $h.getnetworkadapters()) { if ($n.id -eq \"" + asId(NICS[0]) + "\") { $n; break } }";
    private static final String GET_NICS_CMD = "$h = get-host \"" + asId(HOST_NAME) + "\"; $h.getnetworkadapters()";

    public static final String LOOKUP_NETWORK_ID_COMMAND = "$n = get-networks; foreach ($i in $n) '{' if ($i.name -eq \"{0}\") '{' $i; break '} }'";

    public static final String[][] extraArgs = new String[][] {
        { NETS[0], MACS[0], IPS[0], SUBNETS[0] },
        { NETS[1], MACS[1], IPS[1], SUBNETS[1] },
    };

    private static final String DATA_CENTER_ID = "999";

    private static final String[] networkArgs = new String[] { DATA_CENTER_ID };

    private static final String ATTACH_NETWORK_CMD = "$host = get-host \"" + asId(HOST_NAME) + "\"; foreach ($n in get-networks) { if ($n.networkid -eq \"" + asId(NETS[0]) + "\") { $net = $n; break } } foreach ($n in $host.getnetworkadapters()) { if ($n.id -eq \"" + asId(NICS[0]) + "\") { $nic = $n; break } } attach-logicalnetworktonetworkadapter -hostobject $host -network $net -networkadapter $nic";
    private static final String ATTACH_NETWORK_BY_NAME_CMD = "$host = get-host \"" + asId(HOST_NAME) + "\"; foreach ($n in get-networks) { if ($n.name -eq \"" + NETS[0] + "\") { $net = $n; break } } foreach ($n in $host.getnetworkadapters()) { if ($n.id -eq \"" + asId(NICS[0]) + "\") { $nic = $n; break } } attach-logicalnetworktonetworkadapter -hostobject $host -network $net -networkadapter $nic";
    private static final String DETACH_NETWORK_CMD = "$host = get-host \"" + asId(HOST_NAME) + "\"; foreach ($n in get-networks) { if ($n.networkid -eq \"" + asId(NETS[0]) + "\") { $net = $n; break } } foreach ($n in $host.getnetworkadapters()) { if ($n.id -eq \"" + asId(NICS[0]) + "\") { $nic = $n; break } } detach-logicalnetworkfromnetworkadapter -hostobject $host -network $net -networkadapter $nic";

    private static final String NIC_URI = "hosts/" + asId(HOST_NAME) + "/nics/" + asId(NICS[0]);
    private static final String ACTION_RETURN = "replace with realistic powershell return";
    private static final String FAILURE = "replace with realistic powershell failure";
    private static final String REASON = "Powershell command \"" + ATTACH_NETWORK_CMD + "\" failed with " + FAILURE;
    private static final String DETAIL = "at com.redhat.rhevm.api.powershell.util.PowerShellCmd.runCommand(";

    protected PowerShellHostResource getResource(Executor executor, PowerShellPoolMap poolMap, PowerShellParser parser) {
        return new PowerShellHostResource(asId(HOST_NAME), executor, poolMap, parser);
    }

    protected String formatNics(String[] names, String[][] args) {
        String[] descriptions = new String[names.length];
        return formatXmlReturn("hostnic", names, descriptions, args);
    }

    protected String formatNic(String name, String[] args) {
        return formatNics(asArray(name), asArray(args));
    }

    protected String formatNetwork(String name) {
        return formatXmlReturn("network",
                               new String[] { name },
                               new String[] { "" },
                               networkArgs);
    }

    protected String formatLookupNetworkCmd(int i) {
        return MessageFormat.format(LOOKUP_NETWORK_ID_COMMAND, NETS[i]);
    }

    @Test
    public void testGet() {
        PowerShellHostNicsResource parent = new PowerShellHostNicsResource(asId(HOST_NAME), executor, poolMap, parser);
        PowerShellHostNicResource resource = new PowerShellHostNicResource(asId(NICS[0]), executor, poolMap, parser, parent);

        String[] commands = { GET_NIC_CMD,
                              formatLookupNetworkCmd(0) };
        String[] returns = { formatNic(NICS[0], extraArgs[0]),
                             formatNetwork(NETS[0]) };

        setUpCmdExpectations(commands, returns);

        verifyHostNic(resource.get(), 0);
    }

    @Test
    public void testHostNicsList() {
        PowerShellHostNicsResource resource = new PowerShellHostNicsResource(asId(HOST_NAME), executor, poolMap, parser);

        String[] commands = { GET_NICS_CMD,
                              formatLookupNetworkCmd(0),
                              formatLookupNetworkCmd(1) };
        String[] returns = { formatNics(NICS, extraArgs),
                             formatNetwork(NETS[0]),
                             formatNetwork(NETS[1]) };

        setUpCmdExpectations(commands, returns);

        verifyHostNics(resource.list());
    }

    @Test
    public void testAttach() throws Exception {
        PowerShellHostNicsResource parent = new PowerShellHostNicsResource(asId(HOST_NAME), executor, poolMap, parser);
        PowerShellHostNicResource resource = new PowerShellHostNicResource(asId(NICS[0]), executor, poolMap, parser, parent);

        Action action = getAction();
        action.setNetwork(new Network());
        action.getNetwork().setId(asId(NETS[0]));

        verifyActionResponse(
            resource.attach(setUpActionExpectation(NIC_URI, "attach", ATTACH_NETWORK_CMD, ACTION_RETURN),
                            action),
            NIC_URI,
            false);
    }

    @Test
    public void testAttachByName() throws Exception {
        PowerShellHostNicsResource parent = new PowerShellHostNicsResource(asId(HOST_NAME), executor, poolMap, parser);
        PowerShellHostNicResource resource = new PowerShellHostNicResource(asId(NICS[0]), executor, poolMap, parser, parent);

        Action action = getAction();
        action.setNetwork(new Network());
        action.getNetwork().setName(NETS[0]);

        verifyActionResponse(
            resource.attach(setUpActionExpectation(NIC_URI, "attach", ATTACH_NETWORK_BY_NAME_CMD, ACTION_RETURN),
                            action),
            NIC_URI,
            false);
    }

    @Test
    public void testDetach() throws Exception {
        PowerShellHostNicsResource parent = new PowerShellHostNicsResource(asId(HOST_NAME), executor, poolMap, parser);
        PowerShellHostNicResource resource = new PowerShellHostNicResource(asId(NICS[0]), executor, poolMap, parser, parent);

        Action action = getAction();
        action.setNetwork(new Network());
        action.getNetwork().setId(asId(NETS[0]));

        verifyActionResponse(
            resource.detach(setUpActionExpectation(NIC_URI, "detach", DETACH_NETWORK_CMD, ACTION_RETURN),
                            action),
            NIC_URI,
            false);
    }

    @Test
    public void testAttachAsync() throws Exception {
        PowerShellHostNicsResource parent = new PowerShellHostNicsResource(asId(HOST_NAME), executor, poolMap, parser);
        PowerShellHostNicResource resource = new PowerShellHostNicResource(asId(NICS[0]), executor, poolMap, parser, parent);

        Action action = getAction(true);
        action.setNetwork(new Network());
        action.getNetwork().setId(asId(NETS[0]));

        verifyActionResponse(
            resource.attach(setUpActionExpectation(NIC_URI, "attach", ATTACH_NETWORK_CMD, ACTION_RETURN),
                            action),
            NIC_URI,
            true);
    }

    @Test
    public void testAttachAsyncFailed() throws Exception {
        PowerShellHostNicsResource parent = new PowerShellHostNicsResource(asId(HOST_NAME), executor, poolMap, parser);
        PowerShellHostNicResource resource = new PowerShellHostNicResource(asId(NICS[0]), executor, poolMap, parser, parent);

        Action action = getAction(true);
        action.setNetwork(new Network());
        action.getNetwork().setId(asId(NETS[0]));

        verifyActionResponse(
            resource.attach(setUpActionExpectation(NIC_URI, "attach", ATTACH_NETWORK_CMD, new PowerShellException(FAILURE)),
                            action),
            NIC_URI,
            true,
            REASON,
            DETAIL);
    }

    private void setUpCmdExpectations(String command, String ret) {
        setUpCmdExpectations(asArray(command), asArray(ret));
    }

    private void setUpCmdExpectations(String commands[], String returns[]) {
        mockStatic(PowerShellCmd.class);
        for (int i = 0 ; i < Math.min(commands.length, returns.length) ; i++) {
            if (commands[i] != null) {
                expect(PowerShellCmd.runCommand(setUpPoolExpectations(), commands[i])).andReturn(returns[i]);
            }
        }
        replayAll();
    }

    protected PowerShellPool setUpPoolExpectations() {
        PowerShellPool pool = createMock(PowerShellPool.class);
        expect(poolMap.get()).andReturn(pool);
        return pool;
    }

    private void verifyHostNic(HostNIC nic, int i) {
        assertNotNull(nic);
        assertNotNull(nic.getHost());
        assertEquals(asId(HOST_NAME), nic.getHost().getId());
        assertEquals(asId(NICS[i]), nic.getId());
        assertNotNull(nic.getNetwork());
        assertEquals(asId(NETS[i]), nic.getNetwork().getId());
        assertNotNull(nic.getMac());
        assertEquals(MACS[i], nic.getMac().getAddress());
        assertNotNull(nic.getIp());
        assertEquals(IPS[i], nic.getIp().getAddress());
        assertEquals(SUBNETS[i], nic.getIp().getNetmask());
    }

    private void verifyHostNics(HostNics nics) {
        assertNotNull(nics);
        assertEquals(NICS.length, nics.getHostNics().size());
        for (int i = 0; i < NICS.length; i++) {
            verifyHostNic(nics.getHostNics().get(i), i);
        }
    }
}
