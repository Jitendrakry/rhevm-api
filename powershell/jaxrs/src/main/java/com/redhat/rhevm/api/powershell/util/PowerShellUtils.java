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

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PowerShellUtils {
    private static final Log log = LogFactory.getLog(PowerShellUtils.class);

    private static final String QUOTE = "'";

    public static ArrayList<HashMap<String,String>> parseProps(String str) {
        ArrayList<HashMap<String,String>> ret = new ArrayList<HashMap<String,String>>();
        HashMap<String,String> props = null;

        for (String s : str.split("\n")) {
            if (props != null && s.isEmpty()) {
                ret.add(props);
                props = null;
            }

            String[] parts = s.split(":", 2);
            if (parts.length != 2)
                continue;

            String key = parts[0].trim().toLowerCase();
            if (key.isEmpty()) {
                continue;
            }

            String value = parts[1].trim();
            if (value.isEmpty()) {
                value = null;
            }

            if (props == null) {
                props = new HashMap<String,String>();
            }

            props.put(key, value);
        }

        if (props != null) {
            ret.add(props);
        }

        return ret;
    }

    public static String escape(String arg) {
        return new StringBuffer(QUOTE).append(arg.replace("`", "``")).append(QUOTE).toString();
    }
}
