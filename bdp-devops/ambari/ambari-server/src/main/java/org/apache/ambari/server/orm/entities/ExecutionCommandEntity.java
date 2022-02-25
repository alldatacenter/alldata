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

import java.util.Arrays;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Table(name = "execution_command")
@Entity
@NamedQueries({
    @NamedQuery(name = "ExecutionCommandEntity.removeByTaskIds", query = "DELETE FROM ExecutionCommandEntity command WHERE command.taskId IN :taskIds")
})
public class ExecutionCommandEntity {

  @Id
  @Column(name = "task_id")
  private Long taskId;

  @Basic
  @Lob
  @Column(name = "command")
  private byte[] command;

  @OneToOne
  @JoinColumn(name = "task_id", referencedColumnName = "task_id", nullable = false, insertable = false, updatable = false)
  private HostRoleCommandEntity hostRoleCommand;

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public byte[] getCommand() {
    return command;
  }

  public void setCommand(byte[] command) {
    this.command = command;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExecutionCommandEntity that = (ExecutionCommandEntity) o;

    if (command != null ? !Arrays.equals(command, that.command) : that.command != null) return false;
    if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = taskId != null ? taskId.hashCode() : 0;
    result = 31 * result + (command != null ? Arrays.hashCode(command) : 0);
    return result;
  }

  public HostRoleCommandEntity getHostRoleCommand() {
    return hostRoleCommand;
  }

  public void setHostRoleCommand(HostRoleCommandEntity hostRoleCommandByTaskId) {
    this.hostRoleCommand = hostRoleCommandByTaskId;
  }
}
