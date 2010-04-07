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
package com.redhat.rhevm.api.dummy;

import javax.xml.bind.annotation.XmlRootElement;

import com.redhat.rhevm.api.VM;

// FIXME: this shouldn't be needed
@XmlRootElement(name = "vm")
public class DummyVM extends VM
{
	private static int counter = 0;

	public DummyVM() {
		id = Integer.toString(++counter);
		setHostId(Integer.toString(counter % 2));
	}

	public DummyVM(VM vm) {
		this();
		update(vm);
	}

	public DummyVMStatus getStatus() {
		return status;
	}
	public void setStatus(DummyVMStatus status) {
		this.status = status;
	}
	private DummyVMStatus status;

	public void update(VM vm) {
		// update writable fields only
		this.name = vm.getName();
	}
}
