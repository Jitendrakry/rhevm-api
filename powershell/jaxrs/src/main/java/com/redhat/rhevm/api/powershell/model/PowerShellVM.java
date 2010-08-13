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
package com.redhat.rhevm.api.powershell.model;

import java.util.ArrayList;
import java.util.List;

import com.redhat.rhevm.api.model.Cluster;
import com.redhat.rhevm.api.model.CPU;
import com.redhat.rhevm.api.model.CpuTopology;

import com.redhat.rhevm.api.model.Display;
import com.redhat.rhevm.api.model.DisplayType;
import com.redhat.rhevm.api.model.Host;
import com.redhat.rhevm.api.model.OperatingSystem;
import com.redhat.rhevm.api.model.Template;
import com.redhat.rhevm.api.model.VM;
import com.redhat.rhevm.api.model.VmPool;
import com.redhat.rhevm.api.model.VmStatus;
import com.redhat.rhevm.api.powershell.enums.PowerShellBootSequence;
import com.redhat.rhevm.api.powershell.enums.PowerShellVmType;
import com.redhat.rhevm.api.powershell.util.PowerShellParser;
import com.redhat.rhevm.api.powershell.util.UUID;

public class PowerShellVM extends VM {

    private String cdIsoPath;
    public String getCdIsoPath() {
        return cdIsoPath;
    }
    public void setCdIsoPath(String cdIsoPath) {
        this.cdIsoPath = cdIsoPath;
    }

    public static String buildBootSequence(OperatingSystem os) {
        if (os == null || os.getBoot().size() <= 0) {
            return null;
        }
        String bootSequence = "";
        for (OperatingSystem.Boot boot : os.getBoot()) {
            if (boot.getDev() == null) {
                continue;
            }
            switch (boot.getDev()) {
            case HD:
                bootSequence += "C";
                break;
            case CDROM:
                bootSequence += "D";
                break;
            case NETWORK:
                bootSequence += "N";
                break;
            default:
                break;
            }
        }
        return !bootSequence.isEmpty() ? bootSequence : null;
    }

    public static String asString(DisplayType type) {
        return DisplayType.VNC.equals(type) ? "VNC" : "Spice";
    }

    private static DisplayType parseDisplayType(String s) {
        if (s == null) return null;
        else if (s.equals("VNC"))   return DisplayType.VNC;
        else if (s.equals("Spice")) return DisplayType.SPICE;
        else return null;
    }

    private static VmStatus parseStatus(String s) {
        if (s == null) return null;
        else if (s.equals("Down"))         return VmStatus.SHUTOFF;
        else if (s.equals("Paused"))       return VmStatus.PAUSED;
        else if (s.equals("PoweringDown")) return VmStatus.SHUTDOWN;
        else if (s.equals("Up") ||
                 s.equals("Powering Up"))  return VmStatus.RUNNING;
        else return null;
    }

    public static List<PowerShellVM> parse(PowerShellParser parser, String output) {
        List<PowerShellVM> ret = new ArrayList<PowerShellVM>();

        for (PowerShellParser.Entity entity : parser.parse(output)) {
            PowerShellVM vm = new PowerShellVM();

            vm.setId(entity.get("vmid"));
            vm.setName(entity.get("name"));
            vm.setDescription(entity.get("description"));
            vm.setType(entity.get("vmtype", PowerShellVmType.class).map());
            vm.setMemory(entity.get("memorysize", Integer.class) * 1024L * 1024L);
            vm.setCdIsoPath(entity.get("cdisopath"));

            VmStatus status = parseStatus(entity.get("status"));
            if (status != null) {
                vm.setStatus(status);
            }

            CpuTopology topo = new CpuTopology();
            topo.setSockets(entity.get("numofsockets", Integer.class));
            topo.setCores(entity.get("numofcpuspersocket", Integer.class));
            CPU cpu = new CPU();
            cpu.setTopology(topo);
            vm.setCpu(cpu);

            OperatingSystem os = new OperatingSystem();
            for (OperatingSystem.Boot boot : entity.get("defaultbootsequence", PowerShellBootSequence.class).map()) {
                os.getBoot().add(boot);
            }
            vm.setOs(os);

            Object hostId = entity.get("runningonhost", String.class, Integer.class).toString();
            if (!isEmptyId(hostId)) {
                Host host = new Host();
                host.setId(hostId.toString());
                vm.setHost(host);
            }

            Cluster cluster = new Cluster();
            cluster.setId(entity.get("hostclusterid", String.class, Integer.class).toString());
            vm.setCluster(cluster);

            Template template = new Template();
            template.setId(entity.get("templateid"));
            vm.setTemplate(template);

            Object poolId = entity.get("poolid", String.class, Integer.class).toString();
            if (!isEmptyId(poolId)) {
                VmPool pool = new VmPool();
                pool.setId(poolId.toString());
                vm.setVmPool(pool);
            }

            DisplayType displayType = parseDisplayType(entity.get("displaytype"));
            if (displayType != null) {
                Display display = new Display();
                display.setType(displayType);
                display.setMonitors(entity.get("numofmonitors", Integer.class));
                int port = entity.get("displayport", Integer.class);
                if (port != -1) {
                    display.setPort(port);
                }
                vm.setDisplay(display);
            }

            ret.add(vm);
        }

        return ret;
    }

    private static boolean isEmptyId(Object id) {
        return id instanceof String && id.equals(UUID.EMPTY)
               || id instanceof Integer && id.equals(-1);
    }
}
