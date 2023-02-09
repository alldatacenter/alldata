/**
* Copyright 2019 Huawei Technologies Co.,Ltd.
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use
* this file except in compliance with the License.  You may obtain a copy of the
* License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software distributed
* under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations under the License.
**/

package com.obs.services.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bucket or object ACL Include a set of permissions (
 * {@link com.obs.services.model.Permission}) authorized to specified grantee (
 * {@link com.obs.services.model.GranteeInterface}).
 */
public class AccessControlList extends HeaderResponse {
    /**
     * Pre-defined access control policy: private
     */
    public static final AccessControlList REST_CANNED_PRIVATE = new AccessControlList();

    /**
     * Pre-defined access control policy: public-read
     */
    public static final AccessControlList REST_CANNED_PUBLIC_READ = new AccessControlList();

    /**
     * Pre-defined access control policy: public-read-write
     */
    public static final AccessControlList REST_CANNED_PUBLIC_READ_WRITE = new AccessControlList();

    /**
     * Pre-defined access control policy: public-read-delivered
     */
    public static final AccessControlList REST_CANNED_PUBLIC_READ_DELIVERED = new AccessControlList();

    /**
     * Pre-defined access control policy: public-read-write-delivered
     */
    public static final AccessControlList REST_CANNED_PUBLIC_READ_WRITE_DELIVERED = new AccessControlList();

    /**
     * Pre-defined access control policy: authenticated-read
     */
    public static final AccessControlList REST_CANNED_AUTHENTICATED_READ = new AccessControlList();

    /**
     * Pre-defined access control policy: bucket-owner-read
     */
    public static final AccessControlList REST_CANNED_BUCKET_OWNER_READ = new AccessControlList();

    /**
     * Pre-defined access control policy: bucket-owner-full-control
     */
    public static final AccessControlList REST_CANNED_BUCKET_OWNER_FULL_CONTROL = new AccessControlList();

    /**
     * Pre-defined access control policy: log-delivery-write
     */
    public static final AccessControlList REST_CANNED_LOG_DELIVERY_WRITE = new AccessControlList();

    private Set<GrantAndPermission> grants;

    private Owner owner;

    private boolean delivered;

    /**
     * Check whether the object ACL is delivered.
     * 
     * @return Identifier specifying whether the ACL is delivered
     */
    public boolean isDelivered() {
        return delivered;
    }

    /**
     * Specify whether to deliver the object ACL. (This is only applicable to
     * object ACLs.)
     * 
     * @param delivered
     *            Whether to deliver the object ACL
     */
    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    /**
     * Obtain the owner.
     * 
     * @return Owner
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * Set the owner.
     * 
     * @param owner
     *            Owner
     */
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * Obtain all permissions in the ACL.
     * 
     * @return All grantee groups
     */
    public Set<GrantAndPermission> getGrants() {
        if (grants == null) {
            grants = new HashSet<GrantAndPermission>();
        }
        return grants;
    }

    /**
     * Obtain the permission specified in the ACL
     * {@link com.obs.services.model.GranteeInterface}.
     * 
     * @param grantee
     *            Authorized user
     * @return Permission list of
     *         {@link com.obs.services.model.GranteeInterface}
     */
    public List<Permission> getPermissionsForGrantee(GranteeInterface grantee) {
        List<Permission> permissions = new ArrayList<Permission>();
        for (GrantAndPermission gap : getGrants()) {
            if (gap.getGrantee().equals(grantee)) {
                permissions.add(gap.getPermission());
            }
        }
        return permissions;
    }

    /**
     * Specify permissions {@link com.obs.services.model.Permission} in the ACL
     * {@link com.obs.services.model.GranteeInterface}.
     * 
     * @param grantee
     *            Authorized user
     * @param permission
     *            Permissions defined in
     *            {@link com.obs.services.model.Permission}
     * @return Permission information
     */
    public GrantAndPermission grantPermission(GranteeInterface grantee, Permission permission) {
        return grantPermission(grantee, permission, false);
    }

    public GrantAndPermission grantPermission(GranteeInterface grantee, Permission permission, boolean delivered) {
        GrantAndPermission obj = new GrantAndPermission(grantee, permission);
        obj.setDelivered(delivered);
        getGrants().add(obj);
        return obj;
    }

    /**
     * Add grantee groups to the ACL.
     * 
     * @param grantAndPermissions
     *            Grantee group
     */
    public void grantAllPermissions(GrantAndPermission[] grantAndPermissions) {
        for (int i = 0; i < grantAndPermissions.length; i++) {
            GrantAndPermission gap = grantAndPermissions[i];
            grantPermission(gap.getGrantee(), gap.getPermission(), gap.isDelivered());
        }
    }

    /**
     * Obtain all permissions in the ACL.
     * 
     * @return All grantee groups
     */
    public GrantAndPermission[] getGrantAndPermissions() {
        return getGrants().toArray(new GrantAndPermission[getGrants().size()]);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (GrantAndPermission item : getGrantAndPermissions()) {
            sb.append(item.toString()).append(",");
        }
        sb.append("]");
        return "AccessControlList [owner=" + owner + ", grants=" + sb.toString() + "]";
    }

}
