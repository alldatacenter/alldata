<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Phone List Upgrade View Example
========
Description
-----
The Phone List Upgrade view is upgraded version of Phone List View, that demonstrates changing the data schema
and data migration from previous version. The new version adds support of storing user surname, in addition to
names and phone numbers. The user may add, modify or delete numbers from the list through the view UI.
This document also describes migration process and how to add data migration support to any view.

Data migration
-----

Any view instance can have two types of persistent data: persistence entities (separate table for each one) and
instance data (key-value storage). So the view should support migration of both types of data.

To initiate the migration process, API should be called

    PUT http://<server>:8080/api/v1/views/<targetView>/versions/<targetVersion>/instances/<targetInstance>/migrate/<originVersion>/<originInstance>

In the case of Phone List Upgrade View, to test migration of persistence entities it would be

    PUT http://<server>:8080/api/v1/views/PHONE_LIST/versions/2.0.0/instances/LIST_2/migrate/1.0.0/LIST_2

And for the instance data (key-value storage):

    PUT http://<server>:8080/api/v1/views/PHONE_LIST/versions/2.0.0/instances/LIST_1/migrate/1.0.0/LIST_1

In order to support data migration, view should implement the ViewDataMigrator interface and define the data-version in view.xml.

NOTE: Data migration for instances of same data-versions (including those which does not define data-version) IS supported
and in fact just copies all data - the class defined in the data-migrator-class in view.xml WILL NOT be instantiated.

#####view.xml

View can define the data version and ViewDataMigrator implementation in the view.xml.

      <view>
        <name>PHONE_LIST</name>
        <label>The Phone List View</label>
        <version>2.0.0</version>
        <data-version>1</data-version>
        <data-migrator-class>org.apache.ambari.view.phonelist.DataMigrator</data-migrator-class>
      </view>

If data-version is not defined, 0 is implied.


#####DataMigrator.java

To support migrations between different data versions, view should implement ViewDataMigrator interface.
Views framework calls beforeMigration() method to check if view is ready to migrate data.
View can return false and the migration will be canceled. Otherwise, methods will be called in this order:

  1. migrateEntity() for each persistence entity in the origin view. Parameters are Class objects of same entity loaded by
  corresponding ClassLoaders of origin and current view,
  2. migrateInstanceData() called once, view should copy instance data here,
  3. afterMigration() called in the end. View can do some cleanup or additional migrations if needed.

In the DataMigrator object the ViewDataMigrationContext is injected. It provides all needed methods to operate with
both origin and current DataStore/instance data and also some utility methods to simplify copying data.

    public class DataMigrator implements ViewDataMigrator {
      @Inject
      private ViewDataMigrationContext migrationContext;

      @Override
      public boolean beforeMigration() {
        return migrationContext.getOriginDataVersion() == 1;
      }

      @Override
      public void afterMigration() {
      }

      @Override
      public void migrateEntity(Class originEntityClass, Class currentEntityClass) throws ViewDataMigrationException {
        if (currentEntityClass == PhoneUser.class) {
          migrationContext.copyAllObjects(originEntityClass, currentEntityClass, new PhoneUserConverter());
        } else {
          migrationContext.copyAllObjects(originEntityClass, currentEntityClass);
        }
      }

      @Override
      public void migrateInstanceData() {
        for (Map.Entry<String, Map<String, String>> userData : migrationContext.getOriginInstanceDataByUser().entrySet()) {
          for (Map.Entry<String, String> entry : userData.getValue().entrySet()) {
            String newValue = String.format("<no surname>;%s", entry.getValue());
            migrationContext.putCurrentInstanceData(userData.getKey(), entry.getKey(), newValue);
          }
        }
      }

      private static class PhoneUserConverter implements EntityConverter {
        @Override
        public void convert(Object orig, Object dest) {
          PhoneUser destPhone = (PhoneUser) dest;

          BeanUtils.copyProperties(orig, dest);
          if (destPhone.getName() == null) {
            destPhone.setSurname("<no surname>");
          } else {
            String[] parts = destPhone.getName().split(" ");
            if (parts.length > 1) {
              destPhone.setSurname(parts[parts.length - 1]);
            } else {
              destPhone.setSurname("<no surname>");
            }
          }

        }

      }
    }

In this example, migrator supports both migration of persistence entity and instance data.
