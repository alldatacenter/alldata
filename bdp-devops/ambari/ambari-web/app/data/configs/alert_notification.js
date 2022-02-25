/**
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

/**
 * Alert Notification Properties
 */
module.exports = [
  {
    "name": "create_notification",
    "displayName": "Create Notification",
    "isOverridable": false,
    "isVisible": false,
    "serviceName": "MISC",
    "category": "Notifications",
    "recommendedValue": "no",
    "filename": "alert_notification"
  },
  {
    "name": "mail.smtp.host",
    "displayName": "SMTP Host",
    "displayType": "host",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "mail.smtp.port",
    "displayName": "SMTP Port",
    "displayType": "int",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "mail.smtp.from",
    "displayName": "FROM Email Address",
    "displayType": "email",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "ambari.dispatch.recipients",
    "displayName": " TO Email Address",
    "displayType": "email",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "smtp_use_auth",
    "displayName": "SMTP server requires authentication",
    "displayType": "checkbox",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "recommendedValue": true,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "ambari.dispatch.credential.username",
    "displayName": "SMTP Username",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-2",
    "filename": "alert_notification"
  },
  {
    "name": "ambari.dispatch.credential.password",
    "displayName": "SMTP Password",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-2",
    "filename": "alert_notification"
  },
  {
    "name": "mail.smtp.starttls.enable",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": false,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "mail.smtp.startssl.enable",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": false,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  }
];
