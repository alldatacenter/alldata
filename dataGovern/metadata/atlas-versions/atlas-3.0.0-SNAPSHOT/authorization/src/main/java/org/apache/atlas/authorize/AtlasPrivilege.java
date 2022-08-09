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
package org.apache.atlas.authorize;

public enum AtlasPrivilege {
     TYPE_CREATE("type-create"),
     TYPE_UPDATE("type-update"),
     TYPE_DELETE("type-delete"),

     ENTITY_READ("entity-read"),
     ENTITY_CREATE("entity-create"),
     ENTITY_UPDATE("entity-update"),
     ENTITY_DELETE("entity-delete"),
     ENTITY_READ_CLASSIFICATION("entity-read-classification"),
     ENTITY_ADD_CLASSIFICATION("entity-add-classification"),
     ENTITY_UPDATE_CLASSIFICATION("entity-update-classification"),
     ENTITY_REMOVE_CLASSIFICATION("entity-remove-classification"),

     ADMIN_EXPORT("admin-export"),
     ADMIN_IMPORT("admin-import"),

     RELATIONSHIP_ADD("add-relationship"),
     RELATIONSHIP_UPDATE("update-relationship"),
     RELATIONSHIP_REMOVE("remove-relationship"),

     ADMIN_PURGE("admin-purge"),

     ENTITY_ADD_LABEL("entity-add-label"),
     ENTITY_REMOVE_LABEL("entity-remove-label"),
     ENTITY_UPDATE_BUSINESS_METADATA("entity-update-business-metadata"),

     TYPE_READ("type-read"),

     ADMIN_AUDITS("admin-audits");

     private final String type;

     AtlasPrivilege(String actionType){
           this.type = actionType;
     }

     public String getType() {
          return type;
     }
}
