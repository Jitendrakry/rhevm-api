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
import java.util.concurrent.Executor;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.rhevm.api.model.Action;
import com.redhat.rhevm.api.model.ActionsBuilder;
import com.redhat.rhevm.api.model.CdRom;
import com.redhat.rhevm.api.model.Cluster;
import com.redhat.rhevm.api.model.CpuTopology;
import com.redhat.rhevm.api.model.Disk;
import com.redhat.rhevm.api.model.Interface;
import com.redhat.rhevm.api.model.Iso;
import com.redhat.rhevm.api.model.Network;
import com.redhat.rhevm.api.model.Template;
import com.redhat.rhevm.api.model.VM;
import com.redhat.rhevm.api.model.VmPool;
import com.redhat.rhevm.api.resource.VmResource;
import com.redhat.rhevm.api.common.resource.AbstractActionableResource;
import com.redhat.rhevm.api.common.util.JAXBHelper;
import com.redhat.rhevm.api.common.util.LinkHelper;
import com.redhat.rhevm.api.common.util.ReflectionHelper;
import com.redhat.rhevm.api.powershell.model.PowerShellVM;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;


public class PowerShellVmResource extends AbstractActionableResource<VM> implements VmResource {

    private static final String CDROM_ID = Integer.toString("cdrom".hashCode());

    public PowerShellVmResource(String id, Executor executor) {
        super(id, executor);
    }

    public PowerShellVmResource(String id) {
        super(id);
    }

    public static ArrayList<PowerShellVM> runAndParse(String command) {
        return PowerShellVM.parse(PowerShellCmd.runCommand(command));
    }

    public static PowerShellVM runAndParseSingle(String command) {
        ArrayList<PowerShellVM> vms = runAndParse(command);

        return !vms.isEmpty() ? vms.get(0) : null;
    }

    public static VM addLinks(PowerShellVM vm) {
        if (vm.getDevices() != null) {
            for (CdRom cdrom : vm.getDevices().getCdRoms()) {
                LinkHelper.addLinks(cdrom.getIso());
            }
            for (Interface iface : vm.getDevices().getInterfaces()) {
                LinkHelper.addLinks(iface.getNetwork());
            }
        }

        return LinkHelper.addLinks(JAXBHelper.clone("vm", VM.class, vm));
    }

    /* Map the network names to network IDs on all the VM's network
     * interfaces. The powershell output only includes the network name.
     *
     * @param vm  the VM to modify
     * @return  the modified VM
     */
    private static PowerShellVM lookupNetworkIds(PowerShellVM vm) {
        if (vm.getDevices() == null) {
            return vm;
        }

        for (Interface iface : vm.getDevices().getInterfaces()) {
            StringBuilder buf = new StringBuilder();

            buf.append("$n = get-networks\n");
            buf.append("foreach ($i in $n) {");
            buf.append("  if ($i.name -eq '" + iface.getNetwork().getName() + "') {");
            buf.append("    $i");
            buf.append("  }");
            buf.append("}");

            Network network = new Network();
            network.setId(PowerShellNetworkResource.runAndParseSingle(buf.toString()).getId());
            iface.setNetwork(network);
        }

        return vm;
    }

    public static PowerShellVM addDevices(PowerShellVM vm) {
        StringBuilder buf = new StringBuilder();

        buf.append("$v = get-vm " + vm.getId() + "\n");
        buf.append("$v.GetDiskImages()\n");

        vm = PowerShellVM.parseDisks(vm, PowerShellCmd.runCommand(buf.toString()));

        buf = new StringBuilder();

        buf.append("$v = get-vm " + vm.getId() + "\n");
        buf.append("$v.GetNetworkAdapters()\n");

        vm = PowerShellVM.parseInterfaces(vm, PowerShellCmd.runCommand(buf.toString()));

        if (vm.getCdIsoPath() != null) {
            CdRom cdrom = new CdRom();
            cdrom.setId(CDROM_ID);
            cdrom.setIso(new Iso());
            cdrom.getIso().setId(vm.getCdIsoPath());
            vm.getDevices().getCdRoms().add(cdrom);
        }

        return lookupNetworkIds(vm);
    }

    @Override
    public VM get(UriInfo uriInfo) {
        return addLinks(addDevices(runAndParseSingle("get-vm " + getId())));
    }

    @Override
    public VM update(HttpHeaders headers, UriInfo uriInfo, VM vm) {
        validateUpdate(vm, headers);

        StringBuilder buf = new StringBuilder();

        buf.append("$v = get-vm " + getId() + "\n");

        if (vm.getName() != null) {
            buf.append("$v.name = '" + vm.getName() + "'\n");
        }
        if (vm.getDescription() != null) {
            buf.append("$v.description = '" + vm.getDescription() + "'\n");
        }
        if (vm.isSetMemory()) {
            buf.append(" $v.memorysize = " + Math.round((double)vm.getMemory()/(1024*1024)) + "\n");
        }
        if (vm.getCpu() != null && vm.getCpu().getTopology() != null) {
            CpuTopology topology = vm.getCpu().getTopology();
            buf.append(" $v.numofsockets = " + topology.getSockets() + "\n");
            buf.append(" $v.numofcpuspersocket = " + topology.getCores() + "\n");
        }
        String bootSequence = PowerShellVM.buildBootSequence(vm);
        if (bootSequence != null) {
            buf.append(" $v.defaultbootsequence = '" + bootSequence + "'\n");
        }

        buf.append("update-vm -vmobject $v");

        return addLinks(addDevices(runAndParseSingle(buf.toString())));
    }

    @Override
    public Response start(UriInfo uriInfo, Action action) {
        return doAction(uriInfo, new CommandRunner(action, "start-vm", "vm", getId()));
    }

    @Override
    public Response stop(UriInfo uriInfo, Action action) {
        return doAction(uriInfo, new CommandRunner(action, "stop-vm", "vm", getId()));
    }

    @Override
    public Response shutdown(UriInfo uriInfo, Action action) {
        return doAction(uriInfo, new CommandRunner(action, "shutdown-vm", "vm", getId()));
    }

    @Override
    public Response suspend(UriInfo uriInfo, Action action) {
        return doAction(uriInfo, new CommandRunner(action, "suspend-vm", "vm", getId()));
    }

    @Override
    public Response restore(UriInfo uriInfo, Action action) {
        return doAction(uriInfo, new CommandRunner(action, "restore-vm", "vm", getId()));
    }

    @Override
    public Response migrate(UriInfo uriInfo, Action action) {
        return doAction(uriInfo, new DoNothingTask(action));
    }

    @Override
    public Response move(UriInfo uriInfo, Action action) {
        return doAction(uriInfo, new DoNothingTask(action));
    }

    @Override
    public Response detach(UriInfo uriInfo, Action action) {
        return doAction(uriInfo, new CommandRunner(action, "detach-vm", "vm", getId()));
    }

    @Override
    public Response addDevice(UriInfo uriInfo, Action action) {
        AbstractActionTask task;

        if (action.getCdRom() != null) {
            task = new UpdateCdRomTask(action, action.getCdRom(), UpdateCdRomTask.ADD);
        } else if (action.getDisk() != null) {
            task = new AddDiskTask(action, action.getDisk());
        } else if (action.getInterface() != null) {
            task = new AddInterfaceTask(action, action.getInterface());
        } else {
            task = new DoNothingTask(action);
        }

        return doAction(uriInfo, task);
    }

    @Override
    public Response removeDevice(UriInfo uriInfo, Action action) {
        AbstractActionTask task;

        if (action.getCdRom() != null) {
            task = new UpdateCdRomTask(action, action.getCdRom(), UpdateCdRomTask.REMOVE);
        } else if (action.getDisk() != null) {
            task = new RemoveDiskTask(action, action.getDisk().getId());
        } else if (action.getInterface() != null) {
            task = new RemoveInterfaceTask(action, action.getInterface().getId());
        } else {
            task = new DoNothingTask(action);
        }

        return doAction(uriInfo, task);
    }

    @Override
    public Response changeCD(UriInfo uriInfo, Action action) {
        return doAction(uriInfo, new DoNothingTask(action));
    }

    @Override
    public Response ejectCD(UriInfo uriInfo, Action action) {
        return doAction(uriInfo, new DoNothingTask(action));
    }

    private class UpdateCdRomTask extends AbstractActionTask {
        public static final boolean ADD    = true;
        public static final boolean REMOVE = false;

        private CdRom cdrom;
        private boolean add;

        UpdateCdRomTask(Action action, CdRom cdrom, boolean add) {
            super(action);
            this.cdrom = cdrom;
            this.add = add;
        }

        public void run() {
            StringBuilder buf = new StringBuilder();

            String cdIsoPath;
            if (add) {
                cdIsoPath = cdrom.getIso().getId();
            } else {
                cdIsoPath = "";
                assert cdrom.getId().equals(CDROM_ID);
            }

            buf.append("$v = get-vm " + getId() + "\n");
            buf.append("$v.cdisopath = '" + cdIsoPath + "'\n");
            buf.append("update-vm -vmobject $v");

            PowerShellCmd.runCommand(buf.toString());
        }
    }

    private class AddDiskTask extends AbstractActionTask {
        private Disk disk;

        AddDiskTask(Action action, Disk disk) {
            super(action);
            this.disk = disk;
        }

        public void run() {
            StringBuilder buf = new StringBuilder();

            buf.append("$d = new-disk");
            buf.append(" -disksize " + Math.round((double)disk.getSize()/(1024*1024*1024)));
            if (disk.getFormat() != null) {
                buf.append(" -volumeformat " + disk.getFormat().toString());
            }
            if (disk.getType() != null) {
                buf.append(" -disktype " + ReflectionHelper.capitalize(disk.getType().toString()));
            }
            if (disk.getInterface() != null) {
                buf.append(" -diskinterface ");
                switch (disk.getInterface()) {
                case IDE:
                case SCSI:
                    buf.append(disk.getInterface().toString());
                    break;
                case VIRTIO:
                    buf.append("VirtIO");
                    break;
                default:
                    assert false : disk.getInterface();
                    break;
                }
            }
            if (disk.isSparse() != null) {
                buf.append(" -volumetype " + (disk.isSparse() ? "Sparse" : "Preallocated"));
            }
            if (disk.isWipeAfterDelete() != null && disk.isWipeAfterDelete()) {
                buf.append(" -wipeafterdelete");
            }
            if (disk.isPropagateErrors() != null) {
                buf.append(" -propagateerrors ");
                if (disk.isPropagateErrors()) {
                    buf.append("on");
                } else {
                    buf.append("off");
                }
            }
            buf.append("\n");

            buf.append("$v = get-vm " + getId() + "\n");

            buf.append("add-disk -diskobject $d -vmobject $v");
            if (action.getStorageDomain() != null) {
                buf.append(" -storagedomainid " + action.getStorageDomain().getId());
            }

            PowerShellCmd.runCommand(buf.toString());
        }
    }

    private class RemoveDiskTask extends AbstractActionTask {
        private String diskId;

        RemoveDiskTask(Action action, String diskId) {
            super(action);
            this.diskId = diskId;
        }

        public void run() {
            StringBuilder buf = new StringBuilder();

            buf.append("remove-disk");
            buf.append(" -vmid " + getId());
            buf.append(" -diskids " + diskId);

            PowerShellCmd.runCommand(buf.toString());
        }
    }

    private class AddInterfaceTask extends AbstractActionTask {
        private Interface iface;

        AddInterfaceTask(Action action, Interface iface) {
            super(action);
            this.iface = iface;
        }

        public void run() {
            StringBuilder buf = new StringBuilder();

            buf.append("$v = get-vm " + getId() + "\n");
            buf.append("foreach ($i in get-networks) { if ($i.networkid -eq '" + iface.getNetwork().getId() + "') { $n = $i } }\n");

            buf.append("add-networkadapter");
            buf.append(" -vmobject $v");
            buf.append(" -interfacename " + iface.getName());
            buf.append(" -networkname $n.name");
            if (iface.getType() != null) {
                buf.append(" -interfacetype " + iface.getType().toString().toLowerCase());
            }
            if (iface.getMac() != null && iface.getMac().getAddress() != null) {
                buf.append(" -macaddress " + iface.getMac().getAddress());
            }

            PowerShellCmd.runCommand(buf.toString());
        }
    }

    private class RemoveInterfaceTask extends AbstractActionTask {
        private String interfaceId;

        RemoveInterfaceTask(Action action, String interfaceId) {
            super(action);
            this.interfaceId = interfaceId;
        }

        public void run() {
            StringBuilder buf = new StringBuilder();

            buf.append("$v = get-vm " + getId() + "\n");

            buf.append("foreach ($i in $v.GetNetworkAdapters()) {");
            buf.append("  if ($i.id -eq '" + interfaceId + "') {");
            buf.append("    $n = $i");
            buf.append("  }");
            buf.append("}\n");
            buf.append("remove-networkadapter -vmobject $v -networkadapterobject $n");

            PowerShellCmd.runCommand(buf.toString());
        }
    }
}
