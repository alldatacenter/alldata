/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.model;

import java.io.Serializable;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Represents an Qcloud COS Access Control List (ACL), including the ACL's set of grantees and the
 * permissions assigned to each grantee.
 * </p>
 * <p>
 * Each bucket and object in Qcloud COS has an ACL that defines its access control policy. When a
 * request is made, Qcloud COS authenticates the request using its standard authentication procedure
 * and then checks the ACL to verify the sender was granted access to the bucket or object. If the
 * sender is approved, the request proceeds. Otherwise, Qcloud COS returns an error.
 * </p>
 * <p>
 * An ACL contains a list of grants. Each grant consists of one grantee and one permission. ACLs
 * only grant permissions; they do not deny them.
 * </p>
 * <p>
 * For convenience, some commonly used ACLs are defined in {@link CannedAccessControlList}.
 * </p>
 * <p>
 * Note: Bucket and object ACLs are completely independent; an object does not inherit an ACL from
 * its bucket. For example, if you create a bucket and grant write access to another user, you will
 * not be able to access the user's objects unless the user explicitly grants access. This also
 * applies if you grant anonymous write access to a bucket. Only the user "anonymous" will be able
 * to access objects the user created unless permission is explicitly granted to the bucket owner.
 * </p>
 * <p>
 * Important: Do not grant the anonymous group write access to buckets, as you will have no control
 * over the objects others can store and their associated charges. For more information, see
 * {@link Grantee} and {@link Permissions}.
 * </p>
 *
 * @see CannedAccessControlList
 */
public class AccessControlList implements Serializable {
    private static final long serialVersionUID = 8095040648034788376L;

    // grant set is maintained for backwards compatibility. Both grantSet and
    // grantList cannot be non null at the same time.
    private Set<Grant> grantSet;
    private List<Grant> grantList;
    private Owner owner = null;
    private boolean existDefaultAcl;

    /**
     * Gets the owner of the {@link AccessControlList}.
     *
     * <p>
     * Every bucket and object in Qcloud COS has an owner, the user that created the bucket or
     * object. The owner of a bucket or object cannot be changed. However, if the object is
     * overwritten by another user (deleted and rewritten), the new object will have a new owner.
     * </p>
     * <p>
     * Note: Even the owner is subject to the access control list (ACL). For example, if an owner
     * does not have {@link Permission#Read} access to an object, the owner cannot read that object.
     * However, the owner of an object always has write access to the access control policy (
     * {@link Permission#WriteAcp}) and can change the ACL to read the object.
     * </p>
     *
     * @return The owner for this {@link AccessControlList}.
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * For internal use only. Sets the owner on this access control list (ACL). This method is only
     * intended for internal use by the library. The owner of a bucket or object cannot be changed.
     * However the object can be overwritten by the new desired owner (deleted and rewritten).
     *
     * @param owner The owner for this ACL.
     */
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * Adds a grantee to the access control list (ACL) with the given permission. If this access
     * control list already contains the grantee (i.e. the same grantee object) the permission for
     * the grantee will be updated.
     *
     * @param grantee The grantee to whom the permission will apply.
     * @param permission The permission to apply to the grantee.
     */
    public void grantPermission(Grantee grantee, Permission permission) {
        getGrantsAsList().add(new Grant(grantee, permission));
    }

    /**
     * Adds a set of grantee/permission pairs to the access control list (ACL), where each item in
     * the set is a {@link Grant} object.
     *
     * @param grantsVarArg A collection of {@link Grant} objects
     */
    public void grantAllPermissions(Grant... grantsVarArg) {
        for (Grant gap : grantsVarArg) {
            grantPermission(gap.getGrantee(), gap.getPermission());
        }
    }

    /**
     * Revokes the permissions of a grantee by removing the grantee from the access control list
     * (ACL).
     *
     * @param grantee The grantee to remove from this ACL.
     */
    public void revokeAllPermissions(Grantee grantee) {
        ArrayList<Grant> grantsToRemove = new ArrayList<Grant>();
        List<Grant> existingGrants = getGrantsAsList();
        for (Grant gap : existingGrants) {
            if (gap.getGrantee().equals(grantee)) {
                grantsToRemove.add(gap);
            }
        }
        grantList.removeAll(grantsToRemove);
    }

    /**
     * Gets the set of {@link Grant} objects in this access control list (ACL).
     *
     * @return The set of {@link Grant} objects in this ACL.
     *
     * @deprecated This will remove the duplicate grants if received from Qcloud COS. Use
     *             {@link AccessControlList#getGrantsAsList} instead.
     */
    @Deprecated
    public Set<Grant> getGrants() {
        checkState();
        if (grantSet == null) {
            if (grantList == null) {
                grantSet = new HashSet<Grant>();
            } else {
                grantSet = new HashSet<Grant>(grantList);
                grantList = null;
            }
        }
        return grantSet;
    }

    /**
     * Both grant set and grant list cannot be null at the same time.
     */
    private void checkState() {
        if (grantList != null && grantSet != null) {
            throw new IllegalStateException("Both grant set and grant list cannot be null");
        }
    }


    /**
     * Gets the list of {@link Grant} objects in this access control list (ACL).
     *
     * @return The list of {@link Grant} objects in this ACL.
     */
    public List<Grant> getGrantsAsList() {
        checkState();
        if (grantList == null) {
            if (grantSet == null) {
                grantList = new LinkedList<Grant>();
            } else {
                grantList = new LinkedList<Grant>(grantSet);
                grantSet = null;
            }
        }

        return grantList;
    }

    public String toString() {
        return "AccessControlList [owner=" + owner + ", grants=" + getGrantsAsList() + "]";
    }

    private boolean isAllUsersGrantee(Grantee grantee) {
        String identifier = grantee.getIdentifier();
        return grantee.equals(GroupGrantee.AllUsers) ||(identifier != null &&
                identifier.equals("qcs::cam::anyone:anyone"));
    }
    /**
     * according to the returned x-cos-acl header and grantList, to judge the bucket or object acl, the cases are below:
     *
     * 1. if the header returned x-cos-acl:default, return CannedAccessControlList.Default, only for object acl
     * 2. if the body have AllUsers's Read or Write permission, combine it to decide bucket or object acl to
     *     CannedAccessControlList.PublicReadWrite or CannedAccessControlList.PublicRead
     * 3. for other cases, the object or bucket acl is CannedAccessControlList.Private
     *
     * @return CannedAccessControlList
     */
    public CannedAccessControlList getCannedAccessControl() {
        if(grantList == null) {
            return null;
        }
        // if the returned header have x-cos-acl:default, the object acl is Default access control
        // attention: get bucket acl will not return this header
        if(existDefaultAcl) {
            return CannedAccessControlList.Default;
        }

        // check the object or bucket if have AllUsers Read and Write acl
        boolean allUsersRead = false;
        boolean allUsersWrite = false;
        for(Grant grant:grantList) {
            Grantee grantee = grant.getGrantee();
            Permission permission = grant.getPermission();
            if(grantee != null && permission != null && isAllUsersGrantee(grantee)) {
                if(permission.equals(Permission.Read)) {
                    allUsersRead = true;
                }
                if(permission.equals(Permission.Write)) {
                    allUsersWrite = true;
                }
            }
        }
        // only bucket acl, will have public read and write acl
        if(allUsersRead && allUsersWrite) {
            return CannedAccessControlList.PublicReadWrite;
        }

        // bucket and object acl, may have public-read acl
        if(allUsersRead) {
            return CannedAccessControlList.PublicRead;
        }

        // in other cases, the object and bucket acl is private access
        return CannedAccessControlList.Private;
    }

    /**
     * @return true if the header have x-cos-acl:default
     */
    public boolean isExistDefaultAcl() {
        return existDefaultAcl;
    }


    /**
     * This method is only intended for internal use by the library.
     * if the response header have x-cos-acl:default, set to true
     *
     * @param existDefaultAcl, if the response header have x-cos-acl:default
     */
    public void setExistDefaultAcl(boolean existDefaultAcl) {
        this.existDefaultAcl = existDefaultAcl;
    }
}
