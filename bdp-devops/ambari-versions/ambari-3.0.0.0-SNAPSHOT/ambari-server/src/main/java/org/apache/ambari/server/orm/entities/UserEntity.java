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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_name"})})
@Entity
@NamedQueries({
    @NamedQuery(name = "userByName", query = "SELECT user_entity from UserEntity user_entity " +
        "where lower(user_entity.userName)=lower(:username)")
})
@TableGenerator(name = "user_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "user_id_seq"
    , initialValue = 2
    , allocationSize = 500
)
public class UserEntity {

  @Id
  @Column(name = "user_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "user_id_generator")
  private Integer userId;

  @Column(name = "user_name", nullable = false)
  private String userName;

  @Column(name = "create_time", nullable = false)
  @Basic
  private long createTime;

  @Column(name = "active", nullable = false)
  private Integer active = 1;

  @Column(name = "consecutive_failures", nullable = false)
  private Integer consecutiveFailures = 0;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "local_username")
  private String localUsername;

  @Version
  @Column(name = "version")
  private Long version;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
  private Set<MemberEntity> memberEntities = new HashSet<>();

  @OneToOne
  @JoinColumns({
      @JoinColumn(name = "principal_id", referencedColumnName = "principal_id", nullable = false)
  })
  private PrincipalEntity principal;

  @Column(name = "active_widget_layouts")
  private String activeWidgetLayouts;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<UserAuthenticationEntity> authenticationEntities = new ArrayList<>();

  // ----- UserEntity --------------------------------------------------------

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    // Force the username to be lowercase
    this.userName = (userName == null) ? null : userName.toLowerCase();
  }

  /**
   * Returns the number of consecutive authentication failures since the last successful login.
   * <p>
   * This value may be used to throttle authentication attempts or lock out users. It is expected that
   * this value is reset to <code>0</code> when a successful authentication attempt was made.
   *
   * @return the number of consecutive authentication failures since the last successful login
   */
  public Integer getConsecutiveFailures() {
    return consecutiveFailures;
  }

  /**
   * Sets the number of consecutive authentication failures since the last successful login.
   * <p>
   * This value may be used to throttle authentication attempts or lock out users. It is expected that
   * this value is reset to <code>0</code> when a successful authentication attempt was made.
   * <p>
   * For each failed authentication attempt, {@link #incrementConsecutiveFailures()} should be called
   * rather than explicitly setting an incremented value.
   *
   * @param consecutiveFailures a number of consecutive authentication failures since the last successful login
   */
  public void setConsecutiveFailures(Integer consecutiveFailures) {
    this.consecutiveFailures = consecutiveFailures;
  }

  /**
   * Increments the number of consecutive authentication failures since the last successful login.
   * <p>
   * This value may be used to throttle authentication attempts or lock out users. It is expected that
   * this value is reset to <code>0</code> when a successful authentication attempt was made.
   * <p>
   * TODO: Ensure that this value is consistent when updating concurrently
   */
  public void incrementConsecutiveFailures() {
    this.consecutiveFailures++;
  }

  /**
   * Returns the display name for this user.
   * <p>
   * This value may be used in user interfaces rather than the username to show who it logged in. If
   * empty, it is expected that the user's {@link #userName} value would be used instead.
   *
   * @return the user's display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the display name for this user.
   * <p>
   * This value may be used in user interfaces rather than the username to show who it logged in. If
   * empty, it is expected that the user's {@link #userName} value would be used instead.
   *
   * @param displayName the user's display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets the local username for this user.
   * <p>
   * This value is intended to be used when accessing services via Ambari Views. If
   * empty, it is expected that the user's {@link #userName} value would be used instead.
   *
   * @return the user's local username
   */
  public String getLocalUsername() {
    return localUsername;
  }

  /**
   * Sets the local username for this user.
   * <p>
   * This value is intended to be used when accessing services via Ambari Views. If
   * empty, it is expected that the user's {@link #userName} value would be used instead.
   *
   * @param localUsername the user's local username
   */
  public void setLocalUsername(String localUsername) {
    this.localUsername = localUsername;
  }

  public long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(long createTime) {
    this.createTime = createTime;
  }

  /**
   * Returns the version number of the relevant data stored in the database.
   * <p>
   * This is used to help ensure that collisions updatin the relevant data in the database are
   * handled properly via Optimistic locking.
   *
   * @return a version number
   */
  public Long getVersion() {
    return version;
  }

  /**
   * Sets the version number of the relevant data stored in the database.
   * <p>
   * This is used to help ensure that collisions updatin the relevant data in the database are
   * handled properly via Optimistic locking.  It is recommended that this value is <b>not</b>
   * manually updated, else issues may occur when persisting the data.
   *
   * @param version a version number
   */
  public void setVersion(Long version) {
    this.version = version;
  }

  public Set<MemberEntity> getMemberEntities() {
    return memberEntities;
  }

  public void setMemberEntities(Set<MemberEntity> memberEntities) {
    this.memberEntities = memberEntities;
  }

  public Boolean getActive() {
    return active == 0 ? Boolean.FALSE : Boolean.TRUE;
  }

  public void setActive(Boolean active) {
    if (active == null) {
      this.active = null;
    } else {
      this.active = active ? 1 : 0;
    }
  }

  /**
   * Get the admin principal entity.
   *
   * @return the principal entity
   */
  public PrincipalEntity getPrincipal() {
    return principal;
  }

  /**
   * Set the admin principal entity.
   *
   * @param principal the principal entity
   */
  public void setPrincipal(PrincipalEntity principal) {
    this.principal = principal;
  }

  public String getActiveWidgetLayouts() {
    return activeWidgetLayouts;
  }

  public void setActiveWidgetLayouts(String activeWidgetLayouts) {
    this.activeWidgetLayouts = activeWidgetLayouts;
  }

  public List<UserAuthenticationEntity> getAuthenticationEntities() {
    return authenticationEntities;
  }

  public void setAuthenticationEntities(List<UserAuthenticationEntity> authenticationEntities) {
    // If the passed in value is not the same list that is stored internally, clear it and set the
    // entries to the same set that the user passed in.
    // If the passed in value is the same list, then do nothing since the internal value is already
    // set.
    if (this.authenticationEntities != authenticationEntities) {  // Tests to see if the Lists are the same object, not if they have the same content.
      this.authenticationEntities.clear();

      if (authenticationEntities != null) {
        this.authenticationEntities.addAll(authenticationEntities);
      }
    }
  }

  /**
   * Ensure the create time is set properly when the record is created.
   */
  @PrePersist
  protected void onCreate() {
    createTime = System.currentTimeMillis();
  }

  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o == null || getClass() != o.getClass()) {
      return false;
    } else {
      UserEntity that = (UserEntity) o;

      EqualsBuilder equalsBuilder = new EqualsBuilder();
      equalsBuilder.append(userId, that.userId);
      equalsBuilder.append(userName, that.userName);
      equalsBuilder.append(displayName, that.displayName);
      equalsBuilder.append(localUsername, that.localUsername);
      equalsBuilder.append(consecutiveFailures, that.consecutiveFailures);
      equalsBuilder.append(active, that.active);
      equalsBuilder.append(createTime, that.createTime);
      equalsBuilder.append(version, that.version);
      return equalsBuilder.isEquals();
    }
  }

  @Override
  public int hashCode() {
    HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
    hashCodeBuilder.append(userId);
    hashCodeBuilder.append(userName);
    hashCodeBuilder.append(displayName);
    hashCodeBuilder.append(localUsername);
    hashCodeBuilder.append(consecutiveFailures);
    hashCodeBuilder.append(active);
    hashCodeBuilder.append(createTime);
    hashCodeBuilder.append(version);
    return hashCodeBuilder.toHashCode();
  }
}
