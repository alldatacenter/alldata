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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm.entities;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.security.authorization.RoleAuthorization;

/**
 * Represents an admin permission.
 */
@Table(name = "adminpermission")
@Entity
@TableGenerator(name = "permission_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "permission_id_seq"
    , initialValue = 100
)
@NamedQueries({
    @NamedQuery(name = "PermissionEntity.findByName", query = "SELECT p FROM PermissionEntity p WHERE p.permissionName = :permissionName"),
    @NamedQuery(name = "PermissionEntity.findByPrincipals", query = "SELECT p FROM PermissionEntity p WHERE p.principal IN :principalList")
})
public class PermissionEntity {

  /**
   * Admin permission id constants.
   */
  public static final int AMBARI_ADMINISTRATOR_PERMISSION = 1;
  public static final int CLUSTER_USER_PERMISSION = 2;
  public static final int CLUSTER_ADMINISTRATOR_PERMISSION = 3;
  public static final int VIEW_USER_PERMISSION = 4;

  /**
   * Admin permission name constants.
   */
  public static final String AMBARI_ADMINISTRATOR_PERMISSION_NAME = "AMBARI.ADMINISTRATOR";
  public static final String CLUSTER_ADMINISTRATOR_PERMISSION_NAME = "CLUSTER.ADMINISTRATOR";
  public static final String CLUSTER_OPERATOR_PERMISSION_NAME = "CLUSTER.OPERATOR";
  public static final String SERVICE_ADMINISTRATOR_PERMISSION_NAME = "SERVICE.ADMINISTRATOR";
  public static final String SERVICE_OPERATOR_PERMISSION_NAME = "SERVICE.OPERATOR";
  public static final String CLUSTER_USER_PERMISSION_NAME = "CLUSTER.USER";
  public static final String VIEW_USER_PERMISSION_NAME = "VIEW.USER";

  /**
   * The permission id.
   */
  @Id
  @Column(name = "permission_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "permission_id_generator")
  private Integer id;


  /**
   * The permission name.
   */
  @Column(name = "permission_name")
  private String permissionName;

  /**
   * The permission's (descriptive) label
   */
  @Column(name = "permission_label")
  private String permissionLabel;

  /**
   * The permission's (admin)principal reference
   */
  @OneToOne
  @JoinColumns({
      @JoinColumn(name = "principal_id", referencedColumnName = "principal_id", nullable = false),
  })
  private PrincipalEntity principal;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "resource_type_id", referencedColumnName = "resource_type_id", nullable = false),
  })
  private ResourceTypeEntity resourceType;

  /**
   * The set of authorizations related to this permission.
   *
   * This value declares the granular details for which operations this PermissionEntity grants
   * access.
   */
  @ManyToMany
  @JoinTable(
      name = "permission_roleauthorization",
      joinColumns = {@JoinColumn(name = "permission_id")},
      inverseJoinColumns = {@JoinColumn(name = "authorization_id")}
  )
  private Set<RoleAuthorizationEntity> authorizations = new LinkedHashSet<>();

  /**
   * The permission's explicit sort order
   */
  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 1;

  // ----- PermissionEntity ---------------------------------------------------

  /**
   * Get the permission id.
   *
   * @return the permission id.
   */
  public Integer getId() {
    return id;
  }

  /**
   * Set the permission id.
   *
   * @param id  the type id.
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Get the permission name.
   *
   * @return the permission name
   */
  public String getPermissionName() {
    return permissionName;
  }

  /**
   * Set the permission name.
   *
   * @param permissionName  the permission name
   */
  public void setPermissionName(String permissionName) {
    this.permissionName = permissionName;
  }

  /**
   * Get the permission's label.
   *
   * @return the permission's label
   */
  public String getPermissionLabel() {
    return permissionLabel;
  }

  /**
   * Set the permission's label.
   *
   * @param permissionLabel  the permission's label
   */
  public void setPermissionLabel(String permissionLabel) {
    this.permissionLabel = permissionLabel;
  }

  /**
   * Get the principal entity.
   *
   * @return the principal entity
   */
  public PrincipalEntity getPrincipal() {
    return principal;
  }

  /**
   * Set the principal entity.
   *
   * @param principal  the principal entity
   */
  public void setPrincipal(PrincipalEntity principal) {
    this.principal = principal;
  }

  /**
   * Get the resource type entity.
   *
   * @return  the resource type entity
   */
  public ResourceTypeEntity getResourceType() {
    return resourceType;
  }

  /**
   * Set the resource type entity.
   *
   * @param resourceType  the resource type entity
   */
  public void setResourceType(ResourceTypeEntity resourceType) {
    this.resourceType = resourceType;
  }

  /**
   * Gets the collection of granular authorizations for this PermissionEntity
   *
   * @return a collection of granular authorizations
   */
  public Collection<RoleAuthorizationEntity> getAuthorizations() {
    return authorizations;
  }

  /**
   * Add roleAuthorization if it's not already added
   */
  public void addAuthorization(RoleAuthorizationEntity roleAuthorization) {
    authorizations.add(roleAuthorization);
  }

  /**
   * Add multiple role authorizations
   */
  public void addAuthorizations(Collection<RoleAuthorization> roleAuthorizations) {
    for (RoleAuthorization roleAuthorization : roleAuthorizations) {
      addAuthorization(createRoleAuthorizationEntity(roleAuthorization));
    }
  }

  private static RoleAuthorizationEntity createRoleAuthorizationEntity(RoleAuthorization authorization) {
    RoleAuthorizationEntity roleAuthorizationEntity = new RoleAuthorizationEntity();
    roleAuthorizationEntity.setAuthorizationId(authorization.getId());
    roleAuthorizationEntity.setAuthorizationName(authorization.name());
    return roleAuthorizationEntity;
  }

  /**
   * Gets the explicit sort order value for this PermissionEntity
   * <p/>
   * This value is used to help explicitly order permission entities. For example, order from most
   * permissive to least permissive.
   *
   * @return the explict sorting order value
   */
  public Integer getSortOrder() {
    return sortOrder;
  }

  /**
   * Sets the explicit sort order value for this PermissionEntity
   *
   * @param sortOrder a sorting order value
   */
  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PermissionEntity that = (PermissionEntity) o;

    return !(id != null ? !id.equals(that.id) : that.id != null) &&
        !(permissionName != null ? !permissionName.equals(that.permissionName) : that.permissionName != null) &&
        !(permissionLabel != null ? !permissionLabel.equals(that.permissionLabel) : that.permissionLabel != null) &&
        !(resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null) &&
        !(sortOrder != null ? !sortOrder.equals(that.sortOrder) : that.sortOrder != null) &&
        !(authorizations != null ? !authorizations.equals(that.authorizations) : that.authorizations != null);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (permissionName != null ? permissionName.hashCode() : 0);
    result = 31 * result + (permissionLabel != null ? permissionLabel.hashCode() : 0);
    result = 31 * result + (resourceType != null ? resourceType.hashCode() : 0);
    result = 31 * result + (sortOrder != null ? sortOrder.hashCode() : 0);
    result = 31 * result + (authorizations != null ? authorizations.hashCode() : 0);
    return result;
  }
}
