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
import java.text.MessageFormat;

import java.util.concurrent.Executor;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;

import com.redhat.rhevm.api.model.Snapshot;
import com.redhat.rhevm.api.model.Snapshots;
import com.redhat.rhevm.api.model.VM;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;
import com.redhat.rhevm.api.powershell.util.PowerShellParser;
import com.redhat.rhevm.api.powershell.util.PowerShellPool;
import com.redhat.rhevm.api.powershell.util.PowerShellPoolMap;
import com.redhat.rhevm.api.powershell.util.UUID;

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
public class PowerShellSnapshotsResourceTest
    extends AbstractPowerShellResourceTest<VM, PowerShellVmResource> {

    private static final String VM_NAME = "1234";
    private static final String VM_ID = Integer.toString(VM_NAME.hashCode());

    private static final String[] SNAPSHOTS = { "snap1", "snap2" };
    private static final String[] DISKS = { "disk1", "disk2", "disk3" };

    private static final long DISK_SIZE = 10;
    private static final long DISK_SIZE_BYTES = DISK_SIZE * 1024 * 1024 * 1024;

    private static final String DATE = "the time has come";

    private static final String GET_SNAPSHOT_CMD = "$vm = get-vm \"" + VM_ID + "\"; foreach ($d in $vm.getdiskimages()) { foreach ($s in get-snapshot -vmid $vm.vmid -drive $d.internaldrivemapping) { if ($s.vmsnapshotid -eq \"" + asId(SNAPSHOTS[0]) + "\") { $s; break } } }";
    private static final String GET_SNAPSHOTS_CMD = "$snaps = @(); $vm = get-vm \"" + VM_ID + "\"; foreach ($d in $vm.getdiskimages()) { $snaps += get-snapshot -vmid $vm.vmid -drive $d.internaldrivemapping } $snaps";

    protected PowerShellVmResource getResource(Executor executor, PowerShellPoolMap poolMap, PowerShellParser parser) {
        return new PowerShellVmResource(VM_ID, executor, poolMap, parser);
    }

    protected String formatDisks(String[] names, String[][]diskArgs) {
        String[] descriptions = new String[names.length];
        return formatXmlReturn("disk", names, descriptions, diskArgs);
    }

    protected String formatDisk(String name, String[]diskArgs) {
        return formatDisks(asArray(name), asArray(diskArgs));
    }

    protected String[] buildNames() {
        int nDiskSnapshots = SNAPSHOTS.length * DISKS.length;
        String[] names = new String[SNAPSHOTS.length * DISKS.length];
        for (int i = 0; i < nDiskSnapshots; i++) {
            names[i] = SNAPSHOTS[i / DISKS.length] + DISKS[i % DISKS.length];
        }
        return names;
    }

    protected String[] buildDiskArgs(int snapshotIndex, int diskIndex) {
        String[] args = new String[5];
        args[0] = Long.toString(DISK_SIZE_BYTES);
        args[1] = asId(SNAPSHOTS[snapshotIndex]);
        if (snapshotIndex == 0) {
            args[2] = UUID.EMPTY;
        } else {
            args[2] = asId(SNAPSHOTS[snapshotIndex-1]);
        }
        args[3] = DATE;
        args[4] = Integer.toString(diskIndex + 1);
        return args;
    }

    protected String[][] buildDiskArgs() {
        String[][] args = new String[SNAPSHOTS.length * DISKS.length][];
        for (int i = 0; i < SNAPSHOTS.length * DISKS.length; i++) {
            args[i] = buildDiskArgs(i / DISKS.length, i % DISKS.length);
        }
        return args;
    }

    @Test
    public void testSnapshotGet() throws Exception {
        PowerShellSnapshotsResource parent = new PowerShellSnapshotsResource(VM_ID, poolMap, parser);
        PowerShellSnapshotResource resource = new PowerShellSnapshotResource(parent, asId(SNAPSHOTS[0]));

        setUpCmdExpectations(GET_SNAPSHOT_CMD, formatDisk(SNAPSHOTS[0], buildDiskArgs(0, 0)));

        verifySnapshot(resource.get(), 0);
    }

    @Test
    public void testSnapshotsList() throws Exception {
        PowerShellSnapshotsResource resource = new PowerShellSnapshotsResource(VM_ID, poolMap, parser);

        setUpCmdExpectations(GET_SNAPSHOTS_CMD, formatDisks(buildNames(), buildDiskArgs()));

        verifySnapshots(resource.list());
    }

    private void setUpCmdExpectations(String command, String ret) throws Exception {
        mockStatic(PowerShellCmd.class);
        expect(PowerShellCmd.runCommand(setUpShellExpectations(), command)).andReturn(ret);
        replayAll();
    }

    protected PowerShellCmd setUpShellExpectations() {
        PowerShellPool pool = createMock(PowerShellPool.class);
        PowerShellCmd cmd = createMock(PowerShellCmd.class);
        expect(pool.get()).andReturn(cmd);
        expect(poolMap.get()).andReturn(pool);
        return cmd;
    }

    private void verifySnapshot(Snapshot snapshot, int i) {
        assertNotNull(snapshot);
        assertNotNull(snapshot.getVm());
        assertEquals(VM_ID, snapshot.getVm().getId());
        assertEquals(asId(SNAPSHOTS[i]), snapshot.getId());
    }

    private void verifySnapshots(Snapshots snapshots) {
        assertNotNull(snapshots);
        assertEquals(SNAPSHOTS.length, snapshots.getSnapshots().size());
        for (int i = 0; i < SNAPSHOTS.length; i++) {
            verifySnapshot(snapshots.getSnapshots().get(i), i);
        }
    }

    private static String asId(String name) {
        return Integer.toString(name.hashCode());
    }

    protected static String[] asArray(String s) {
        return new String[] { s };
    }

    protected static String[][] asArray(String[] a) {
        return new String[][] { a };
    }
}
