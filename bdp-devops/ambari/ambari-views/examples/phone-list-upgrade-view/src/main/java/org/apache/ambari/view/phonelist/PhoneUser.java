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

package org.apache.ambari.view.phonelist;

/**
 *  Phone user (name/phone number) entity for the phone list view example.
 */
public class PhoneUser {

  /**
   * The user name.
   */
  private String name;

  /**
   * The user surname.
   */
  private String surname;

  /**
   * The phone number.
   */
  private String phone;

  /**
   * No-arg constructor required for JPA.
   */
  public PhoneUser() {
  }

  /**
   * Construct a phone user.
   *
   * @param name   the user name
   * @param surname   the user surname
   * @param phone  the phone number
   */
  public PhoneUser(String name, String surname, String phone) {
    this.name  = name;
    this.surname  = surname;
    this.phone = phone;
  }

  /**
   * Get the user name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the user surname.
   *
   * @param surname  the surname
   */
  public void setSurname(String surname) {
    this.surname = surname;
  }

  /**
   * Get the user surname.
   *
   * @return the surname
   */
  public String getSurname() {
    return surname;
  }

  /**
   * Set the user name.
   *
   * @param name  the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the phone number.
   *
   * @return the phone number
   */
  public String getPhone() {
    return phone;
  }

  /**
   * Set the phone number.
   *
   * @param phone  the phone number
   */
  public void setPhone(String phone) {
    this.phone = phone;
  }
}
