/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * The class encapsulates the access control list (ACL) information of OSS. It
 * includes an owner and a group of &lt;{@link Grantee},{@link Permission}&gt; pair.
 */
public class AccessControlList extends GenericResult implements Serializable {

    private static final long serialVersionUID = 211267925081748283L;

    private HashSet<Grant> grants = new HashSet<Grant>();
    private CannedAccessControlList cannedACL;
    private Owner owner;

    /**
     * Gets {@link Owner}.
     * 
     * @return The {@link Owner} instance.
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * Sets the {@link Owner}.
     * 
     * @param owner
     *            {@link Owner} instance,
     */
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * Grants the {@link Grantee} with the {@link Permission}. For now the
     * Grantee must be {@link GroupGrantee#AllUsers}.
     * 
     * @param grantee
     *            The grantee, it must be {@link GroupGrantee#AllUsers} for now.
     * @param permission
     *            The permission defined in {@link Permission}.
     */
    public void grantPermission(Grantee grantee, Permission permission) {
        if (grantee == null || permission == null) {
            throw new NullPointerException();
        }

        grants.add(new Grant(grantee, permission));
    }

    /**
     * Revokes the {@link Grantee} all its permissions. For now the Grantee must
     * be {@link GroupGrantee#AllUsers}.
     * 
     * @param grantee
     *            The grantee, it must be {@link GroupGrantee#AllUsers} for now.
     */
    public void revokeAllPermissions(Grantee grantee) {
        if (grantee == null) {
            throw new NullPointerException();
        }

        ArrayList<Grant> grantsToRemove = new ArrayList<Grant>();
        for (Grant g : grants) {
            if (g.getGrantee().equals(grantee)) {
                grantsToRemove.add(g);
            }
        }
        grants.removeAll(grantsToRemove);
    }

    /**
     * Gets all {@link Grant} instances, each {@link Grant} instance specifies a
     * {@link Grantee} and its {@link Permission}.
     * 
     * @return The set of {@link Grant}.
     */
    @Deprecated
    public Set<Grant> getGrants() {
        return this.grants;
    }

    /**
     * Gets the canned ACL.
     * 
     * @return the canned ACL.
     */
    public CannedAccessControlList getCannedACL() {
        return cannedACL;
    }

    /**
     * Sets the ACL.
     * 
     * @param cannedACL the canned acl
     */
    public void setCannedACL(CannedAccessControlList cannedACL) {
        this.cannedACL = cannedACL;
    }

    /**
     * Serializes the ACL and the owner information to string()). It does not
     * include the {@link Grant} information for now.
     */
    public String toString() {
        return "AccessControlList [owner=" + owner + ", ACL=" + getCannedACL() + "]";
    }
}
