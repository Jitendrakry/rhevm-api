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

import com.redhat.rhevm.api.model.Action;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;
import com.redhat.rhevm.api.powershell.util.PowerShellUtils;


class CommandRunner extends AbstractPowerShellActionTask {

    private static final String COMMAND = "{0} -{1}id {2}";

    private PowerShellCmd shell;

    CommandRunner(Action action, String command, PowerShellCmd shell) {
        super(action, command);
        this.shell = shell;
    }

    CommandRunner(Action action, String command, String type, String id, PowerShellCmd shell) {
        this(action, MessageFormat.format(COMMAND, command, type, PowerShellUtils.escape(id)), shell);
    }

    public void execute() {
        handleOutput(PowerShellCmd.runCommand(shell, command));
    }

    protected void handleOutput(String output) {
        // no-op by default
    }
}
