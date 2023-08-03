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

package org.apache.atlas.web.security;

import org.jvnet.libpam.UnixUser;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

public class PamPrincipal extends Object implements Principal {
    private String userName;
    private String gecos;
    private String homeDir;
    private String shell;
    private int uid;
    private int gid;
    private Set<String> groups;

    public PamPrincipal(UnixUser user)
    {
        super();
        userName = user.getUserName();
        gecos = user.getGecos();
        homeDir = user.getDir();
        shell = user.getShell();
        uid = user.getUID();
        gid = user.getGID();
        groups = Collections.unmodifiableSet(user.getGroups());
    }

    @Override
    public String getName()
    {
        return userName;
    }

    public String getGecos()
    {
        return gecos;
    }

    public String getHomeDir()
    {
        return homeDir;
    }

    public String getShell()
    {
        return shell;
    }

    public int getUid()
    {
        return uid;
    }

    public int getGid()
    {
        return gid;
    }

    public Set<String> getGroups()
    {
        return groups;
    }
}
