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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.rhevm.api.model.Cluster;
import com.redhat.rhevm.api.model.Clusters;
import com.redhat.rhevm.api.resource.ClusterResource;
import com.redhat.rhevm.api.resource.ClustersResource;
import com.redhat.rhevm.api.common.util.LinkHelper;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;

public class PowerShellClustersResource
    extends AbstractPowerShellCollectionResource<Cluster, PowerShellClusterResource>
    implements ClustersResource {

    @Override
    public Clusters list(UriInfo uriInfo) {
        Clusters ret = new Clusters();
        for (Cluster cluster : PowerShellClusterResource.runAndParse(getSelectCommand("select-cluster", uriInfo, Clusters.class))) {
            ret.getClusters().add(LinkHelper.addLinks(cluster));
        }
        return ret;
    }

    @Override
    public Response add(UriInfo uriInfo, Cluster cluster) {
        StringBuilder buf = new StringBuilder();

        buf.append("$v = get-clustercompatibilityversions -datacenterid " + cluster.getDataCenter().getId() + "\n");

        buf.append("add-cluster");

        buf.append(" -clustername '" + cluster.getName() + "'");
        buf.append(" -clustercpuname '" + cluster.getCpu().getId() + "'");
        buf.append(" -datacenterid " + cluster.getDataCenter().getId());

        if (cluster.getDescription() != null) {
            buf.append(" -clusterdescription '" + cluster.getDescription() + "'");
        }

        buf.append(" -compatibilityversion $v");

        cluster = LinkHelper.addLinks(PowerShellClusterResource.runAndParseSingle(buf.toString()));

        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder().path(cluster.getId());

        return Response.created(uriBuilder.build()).entity(cluster).build();
    }

    @Override
    public void remove(String id) {
        PowerShellCmd.runCommand("remove-cluster -clusterid " + id);
        removeSubResource(id);
    }

    @Override
    public ClusterResource getClusterSubResource(UriInfo uriInfo, String id) {
        return getSubResource(id);
    }

    protected PowerShellClusterResource createSubResource(String id) {
        return new PowerShellClusterResource(id, getExecutor());
    }
}
