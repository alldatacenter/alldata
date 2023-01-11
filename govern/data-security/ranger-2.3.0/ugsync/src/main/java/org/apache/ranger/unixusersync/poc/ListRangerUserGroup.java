/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 package org.apache.ranger.unixusersync.poc;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListRangerUserGroup
{
    private final int gid;
    private final String name;
    private final String passwd;
    private final List<String> userList;

    public static ListRangerUserGroup parseGroup(final String groupLine)
        throws InvalidGroupException
    {
        final String   line;
        final String[] parts;

        if(groupLine == null)
        {
            throw new IllegalArgumentException("groupLine cannot be null");
        }

        line = groupLine.trim();

        if(line.startsWith("#") || line.isEmpty())
        {
             return null;
        }

        parts = line.split(":");

        if(parts.length < 3)
        {
            throw new InvalidGroupException(groupLine + "must be in the format of name:passwd:gid[:userlist]", line);
        }

        try
        {
            final ListRangerUserGroup        group;
            final String       name;
            final String       passwd;
            final int          gid;
            final List<String> userList;

            name   = parts[0];
            passwd = parts[1];
            gid    = Integer.parseInt(parts[2]);

            if(parts.length == 4)
            {
                userList = Arrays.asList(parts[3].split(","));
            }
            else
            {
                userList = Collections.emptyList();
            }

            group = new ListRangerUserGroup(name, passwd, gid, userList);

            return group;
        }
        catch(final NumberFormatException ex)
        {
            throw new InvalidGroupException(groupLine + " gid must be a number", line);
        }
    }

    public ListRangerUserGroup(final String nm, final String pw, final int id, final List<String> users)
    {
        name     = nm;
        passwd   = pw;
        gid      = id;
        userList = Collections.unmodifiableList(new ArrayList<String>(users));
    }

    public int getGid()
    {
        return (gid);
    }

    public String getName()
    {
        return (name);
    }

    public String getPasswd()
    {
        return (passwd);
    }

    public List<String> getUserList()
    {
        return (userList);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb;

        sb = new StringBuilder();
        sb.append(name);
        sb.append(":");
        sb.append(passwd);
        sb.append(":");
        sb.append(gid);
        sb.append(":");

        for(final String user : userList)
        {
            sb.append(user);
            sb.append(",");
        }

        sb.setLength(sb.length() - 1);

        return (sb.toString());
    }
}
