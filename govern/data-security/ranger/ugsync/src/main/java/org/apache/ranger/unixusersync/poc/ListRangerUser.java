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


public class ListRangerUser
{
    private final String uid;
    private final String uname;
    private final String gid;

    public static ListRangerUser parseUser(final String userLine)
        throws InvalidUserException
    {
        final String   line;
        final String[] parts;

        if(userLine == null)
        {
            throw new IllegalArgumentException("userLine cannot be null");
        }

        line = userLine.trim();

        if(line.startsWith("#") || line.isEmpty())
        {
             return null;
        }

        parts = line.split(":");

        if(parts.length < 3)
        {
            throw new InvalidUserException(userLine + "must be in the format of name:passwd:gid[:userlist]", line);
        }

        try
        {
            final ListRangerUser       xaUser;
            final String       uname;
            final String	   uid;
            final String       gid;

            uname  = parts[0];
            uid    = parts[2];
            gid    = parts[3];

            xaUser = new ListRangerUser(uname, uid, gid);

            return xaUser;
        }
        catch(final NumberFormatException ex)
        {
            throw new InvalidUserException(userLine + " uid must be a number", line);
        }
    }

    public ListRangerUser(final String nm, final String userid, final String grpid )
    {
        uname    = nm;
        uid      = userid;
        gid 	 = grpid;

    }

    public String getGid()
    {
        return (gid);
    }

    public String getName()
    {
        return (uname);
    }

    public String getUid()
    {
        return (uid);
    }


    @Override
    public String toString()
    {
        final StringBuilder sb;

        sb = new StringBuilder();
        sb.append(uname);
        sb.append(":");
        sb.append(uid);
        sb.append(":");
        sb.append(gid);

        sb.setLength(sb.length() - 1);

        return (sb.toString());
    }
}
