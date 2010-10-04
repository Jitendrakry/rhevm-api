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

import org.junit.Test;

import java.util.List;

public class PowerShellSystemStatsTest extends PowerShellModelTest {

    private void testSystemStats(PowerShellSystemStats s, int totalVms, int activeVms, int totalHosts, int activeHosts, int totalUsers, int activeUsers, int totalStorageDomains, int activeStorageDomains) {
        assertEquals(totalVms, s.totalVms);
        assertEquals(activeVms, s.activeVms);
        assertEquals(totalHosts, s.totalHosts);
        assertEquals(activeHosts, s.activeHosts);
        assertEquals(totalUsers, s.totalUsers);
        assertEquals(activeUsers, s.activeUsers);
        assertEquals(totalStorageDomains, s.totalStorageDomains);
        assertEquals(activeStorageDomains, s.activeStorageDomains);
    }

    @Test
    public void testParse() throws Exception {
        String data = readFileContents("systemstats.xml");
        assertNotNull(data);

        PowerShellSystemStats stats = PowerShellSystemStats.parse(getParser(), data);

        assertNotNull(stats);

        testSystemStats(stats, 1, 0, 1, 0, 1, 0, 2, 0);
    }
}
