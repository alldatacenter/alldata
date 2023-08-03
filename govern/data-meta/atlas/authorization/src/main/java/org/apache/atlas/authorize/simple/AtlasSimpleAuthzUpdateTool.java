/** Licensed to the Apache Software Foundation (ASF) under one
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

package org.apache.atlas.authorize.simple;

import java.io.IOException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.SerializationFeature;

public class AtlasSimpleAuthzUpdateTool {


    public static void main(String[] args) {

        if (args != null & args.length > 0) {
            updateSimpleAuthzJsonWithRelationshipPermissions(args[0]);
        } else {
            System.out.println("Provide Atlas conf path");
        }

    }


    public static void updateSimpleAuthzJsonWithRelationshipPermissions(String jsonConfPath) {

        List<String> wildCard = new ArrayList<String>();
        wildCard.add(".*");

        try {

            ObjectMapper mapper = new ObjectMapper();
            AtlasSimpleAuthzPolicy authzPolicy = mapper.readValue(new File(jsonConfPath + "/atlas-simple-authz-policy.json"), AtlasSimpleAuthzPolicy.class);


            AtlasSimpleAuthzPolicy.AtlasAuthzRole dataAdmin = authzPolicy.getRoles().get("ROLE_ADMIN");
            boolean permissionUpdated = false;


            if (dataAdmin != null && dataAdmin.getRelationshipPermissions() == null) {
                AtlasSimpleAuthzPolicy.AtlasRelationshipPermission relationshipPermissions = new AtlasSimpleAuthzPolicy.AtlasRelationshipPermission();
                relationshipPermissions.setPrivileges(wildCard);

                relationshipPermissions.setRelationshipTypes(wildCard);

                relationshipPermissions.setEnd1EntityClassification(wildCard);
                relationshipPermissions.setEnd1EntityId(wildCard);
                relationshipPermissions.setEnd1EntityType(wildCard);

                relationshipPermissions.setEnd2EntityClassification(wildCard);
                relationshipPermissions.setEnd2EntityId(wildCard);
                relationshipPermissions.setEnd2EntityType(wildCard);

                List<AtlasSimpleAuthzPolicy.AtlasRelationshipPermission> relationshipPermissionsList = new ArrayList<AtlasSimpleAuthzPolicy.AtlasRelationshipPermission>();


                relationshipPermissionsList.add(relationshipPermissions);

                dataAdmin.setRelationshipPermissions(relationshipPermissionsList);
                permissionUpdated = true;
            }


            AtlasSimpleAuthzPolicy.AtlasAuthzRole dataSteward = authzPolicy.getRoles().get("DATA_STEWARD");
            List<String> permissiondataSteward = new ArrayList<String>();

            permissiondataSteward.add("add-relationship");
            permissiondataSteward.add("update-relationship");
            permissiondataSteward.add("remove-relationship");

            if (dataSteward != null && dataSteward.getRelationshipPermissions() == null) {
                AtlasSimpleAuthzPolicy.AtlasRelationshipPermission relationshipPermissions = new AtlasSimpleAuthzPolicy.AtlasRelationshipPermission();
                relationshipPermissions.setPrivileges(permissiondataSteward);
                relationshipPermissions.setRelationshipTypes(wildCard);

                relationshipPermissions.setEnd1EntityClassification(wildCard);
                relationshipPermissions.setEnd1EntityId(wildCard);
                relationshipPermissions.setEnd1EntityType(wildCard);

                relationshipPermissions.setEnd2EntityClassification(wildCard);
                relationshipPermissions.setEnd2EntityId(wildCard);
                relationshipPermissions.setEnd2EntityType(wildCard);


                List<AtlasSimpleAuthzPolicy.AtlasRelationshipPermission> relationshipPermissionsList = new ArrayList<AtlasSimpleAuthzPolicy.AtlasRelationshipPermission>();
                relationshipPermissionsList.add(relationshipPermissions);
                dataSteward.setRelationshipPermissions(relationshipPermissionsList);
                permissionUpdated = true;
            }

            if(permissionUpdated) {
                writeUsingFiles(jsonConfPath + "/atlas-simple-authz-policy.json", toJson(authzPolicy, mapper));
            }


        } catch (Exception e) {
            System.err.println(" Error while updating JSON " + e.getMessage());
        }

    }


    public static String toJson(Object obj, ObjectMapper mapper) {
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // to beautify json

        String ret;
        try {
            if (obj instanceof JsonNode && ((JsonNode) obj).isTextual()) {
                ret = ((JsonNode) obj).textValue();
            } else {
                ret = mapper.writeValueAsString(obj);
            }
        } catch (IOException e) {

            ret = null;
        }
        return ret;
    }


    private static void writeUsingFiles(String file, String data) {
        try {
            Files.write(Paths.get( file ), data.getBytes());
        } catch (IOException e) {
            System.err.println(" Error while writeUsingFiles JSON " + e.getMessage());
        }
    }
}