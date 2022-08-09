/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view.model;

public class APIResult {

  private Status status;
  private Object data;
  private Paging paging = new Paging();
  ;

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }


  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public Paging getPaging() {
    return paging;

  }

  public void setPaging(Paging paging) {
    this.paging = paging;
  }


  public static enum Status {
    SUCCESS,
    ERROR
  }

  public static void main(String[] args) {
    System.out.println("hello");
  }

}
