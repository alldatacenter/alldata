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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@Table(name = "user_authentication")
@Entity
@NamedQueries({
    @NamedQuery(name = "UserAuthenticationEntity.findAll",
        query = "SELECT entity FROM UserAuthenticationEntity entity"),
    @NamedQuery(name = "UserAuthenticationEntity.findByType",
        query = "SELECT entity FROM UserAuthenticationEntity entity where lower(entity.authenticationType)=lower(:authenticationType)"),
    @NamedQuery(name = "UserAuthenticationEntity.findByTypeAndKey",
        query = "SELECT entity FROM UserAuthenticationEntity entity where lower(entity.authenticationType)=lower(:authenticationType) and entity.authenticationKey=:authenticationKey"),
    @NamedQuery(name = "UserAuthenticationEntity.findByUser",
        query = "SELECT entity FROM UserAuthenticationEntity entity where entity.user.userId=:userId")
})
@TableGenerator(name = "user_authentication_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "user_authentication_id_seq"
    , initialValue = 2
)
public class UserAuthenticationEntity {

  @Id
  @Column(name = "user_authentication_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "user_authentication_id_generator")
  private Long userAuthenticationId;

  @Column(name = "authentication_type", nullable = false)
  @Enumerated(EnumType.STRING)
  @Basic
  private UserAuthenticationType authenticationType = UserAuthenticationType.LOCAL;

  @Column(name = "authentication_key")
  @Basic
  private String authenticationKey;

  @Column(name = "create_time", nullable = false)
  @Basic
  private long createTime;

  @Column(name = "update_time", nullable = false)
  @Basic
  private long updateTime ;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
  private UserEntity user;

  public Long getUserAuthenticationId() {
    return userAuthenticationId;
  }

  public void setUserAuthenticationId(Long userAuthenticationId) {
    this.userAuthenticationId = userAuthenticationId;
  }

  public UserAuthenticationType getAuthenticationType() {
    return authenticationType;
  }

  public void setAuthenticationType(UserAuthenticationType authenticationType) {
    this.authenticationType = authenticationType;
  }

  public String getAuthenticationKey() {
    return authenticationKey;
  }

  public void setAuthenticationKey(String authenticationKey) {
    this.authenticationKey = authenticationKey;
  }

  public long getCreateTime() {
    return createTime;
  }

  public long getUpdateTime() {
    return updateTime;
  }

  /**
   * Get the relevant {@link UserEntity} associated with this {@link UserAuthenticationEntity}.
   *
   * @return a {@link UserEntity}
   */
  public UserEntity getUser() {
    return user;
  }

  /**
   * Set the relevant {@link UserEntity} associated with this {@link UserAuthenticationEntity}.
   *
   * @param user a {@link UserEntity}
   */
  public void setUser(UserEntity user) {
    this.user = user;
  }

  /**
   * Ensure the create time and update time are set properly when the record is created.
   */
  @PrePersist
  protected void onCreate() {
    final long now = System.currentTimeMillis();
    createTime = now;
    updateTime = now;
  }

  /**
   * Ensure the update time is set properly when the record is updated.
   */
  @PreUpdate
  protected void onUpdate() {
    updateTime = System.currentTimeMillis();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o == null || getClass() != o.getClass()) {
      return false;
    } else {
      UserAuthenticationEntity that = (UserAuthenticationEntity) o;

      EqualsBuilder equalsBuilder = new EqualsBuilder();
      equalsBuilder.append(userAuthenticationId, that.userAuthenticationId);
      equalsBuilder.append(authenticationType, that.authenticationType);
      equalsBuilder.append(authenticationKey, that.authenticationKey);
      equalsBuilder.append(createTime, that.createTime);
      equalsBuilder.append(updateTime, that.updateTime);
      return equalsBuilder.isEquals();
    }
  }

  @Override
  public int hashCode() {
    HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
    hashCodeBuilder.append(userAuthenticationId);
    hashCodeBuilder.append(authenticationType);
    hashCodeBuilder.append(authenticationKey);
    hashCodeBuilder.append(createTime);
    hashCodeBuilder.append(updateTime);
    return hashCodeBuilder.toHashCode();
  }
}
