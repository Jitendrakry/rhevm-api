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
package com.redhat.rhevm.api.command.base;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.felix.gogo.commands.Option;

import com.redhat.rhevm.api.model.Action;
import com.redhat.rhevm.api.model.BaseResource;
import com.redhat.rhevm.api.model.Link;

import static com.redhat.rhevm.api.common.util.ReflectionHelper.set;

/**
 * Performs an update on a resource
 */
public abstract class AbstractUpdateCommand extends AbstractCommand {

    @Option(name = "-f", aliases = { "--field" }, description = "Resource field to be updated", required = true, multiValued = false)
    private String field;

    @Option(name = "-v", aliases = {"--value"}, description="New value for field", required = true, multiValued = false)
    protected String value;

    protected void doUpdate(List<? extends BaseResource> collection, String name) throws Exception {
        BaseResource resource = getResource(collection, name);
        if (resource != null) {
            if (set(resource, field, value)) {
                Link link = resource.getLink();
                if (link != null) {
                    client.doUpdate(resource, field, link);
                    return;
                }
            } else {
                System.err.println(field + " is unknown or not a top-level primitive element");
            }
        }
    }
}
