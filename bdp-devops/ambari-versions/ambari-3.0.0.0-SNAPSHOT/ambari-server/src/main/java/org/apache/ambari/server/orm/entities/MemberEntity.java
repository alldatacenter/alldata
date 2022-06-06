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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "members", uniqueConstraints = {@UniqueConstraint(columnNames = {"group_id", "user_id"})})
@NamedQueries({
  @NamedQuery(name = "memberByUserAndGroup", query = "SELECT memberEnt FROM MemberEntity memberEnt where lower(memberEnt.user.userName)=:username AND lower(memberEnt.group.groupName)=:groupname")
})
@TableGenerator(name = "member_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "member_id_seq",
    initialValue = 1,
    allocationSize = 500
    )
public class MemberEntity {
  @Id
  @Column(name = "member_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "member_id_generator")
  private Integer memberId;

  @ManyToOne
  @JoinColumn(name = "group_id")
  private GroupEntity group;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private UserEntity user;

  public Integer getMemberId() {
    return memberId;
  }

  public void setMemberId(Integer memberId) {
    this.memberId = memberId;
  }

  public GroupEntity getGroup() {
    return group;
  }

  public void setGroup(GroupEntity group) {
    this.group = group;
  }

  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MemberEntity that = (MemberEntity) o;

    if (memberId != null ? !memberId.equals(that.memberId) : that.memberId != null) return false;
    if (group != null ? !group.equals(that.group) : that.group != null) return false;
    if (user != null ? !user.equals(that.user) : that.user != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = memberId != null ? memberId.hashCode() : 0;
    result = 31 * result + (group != null ? group.hashCode() : 0);
    result = 31 * result + (user != null ? user.hashCode() : 0);
    return result;
  }
}
