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
package com.redhat.rhevm.api.powershell.util;

import java.io.InputStream;
import java.io.IOException;
import java.util.Scanner;

public class PowerShellTestUtils {

    public static String readClassPathFile(String name) {
        InputStream is = PowerShellTestUtils.class.getClassLoader().getResourceAsStream(name);
        try {
            StringBuilder outputBuffer = new StringBuilder();
            Scanner sc = new Scanner(is);
            while (sc.hasNext()) {
                outputBuffer.append(sc.nextLine() + "\n");
            }
            return outputBuffer.toString();
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
    }
}
