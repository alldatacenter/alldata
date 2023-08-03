/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.examples.sampleclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.cli.*;
import org.apache.ranger.RangerClient;
import org.apache.ranger.RangerServiceException;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerServiceTags;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.apache.ranger.plugin.model.RangerTagDef.RangerTagAttributeDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SampleClient {
    private static final Logger LOG = LoggerFactory.getLogger(SampleClient.class);


    @SuppressWarnings("static-access")
    public static void main(String[] args) throws RangerServiceException {
        Gson gsonBuilder = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").setPrettyPrinting().create();
        Options options  = new Options();

        Option host = OptionBuilder.hasArgs(1).isRequired().withLongOpt("host").withDescription("hostname").create('h');
        Option auth = OptionBuilder.hasArgs(1).isRequired().withLongOpt("authType").withDescription("Authentication Type").create('k');
        Option user = OptionBuilder.hasArgs(1).isRequired().withLongOpt("user").withDescription("username").create('u');
        Option pass = OptionBuilder.hasArgs(1).isRequired().withLongOpt("pass").withDescription("password").create('p');
        Option conf = OptionBuilder.hasArgs(1).withLongOpt("config").withDescription("configuration").create('c');

        options.addOption(host);
        options.addOption(auth);
        options.addOption(user);
        options.addOption(pass);
        options.addOption(conf);

        CommandLineParser parser = new BasicParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        String hostName = cmd.getOptionValue('h');
        String userName = cmd.getOptionValue('u');
        String password = cmd.getOptionValue('p');
        String cfg      = cmd.getOptionValue('c');
        String authType = cmd.getOptionValue('k');

        RangerClient rangerClient = new RangerClient(hostName, authType, userName, password, cfg);

        String serviceDefName     = "sampleServiceDef";
        String serviceName        = "sampleService";
        String policyName         = "samplePolicy";
        String zoneName           = null;
        String roleName           = "sampleRole";
        Map<String,String> filter = Collections.emptyMap();


        /*
        Create a new Service Definition
         */

        RangerServiceDef.RangerServiceConfigDef config = new RangerServiceDef.RangerServiceConfigDef();
        config.setItemId(1L);
        config.setName("sampleConfig");
        config.setType("string");
        List<RangerServiceDef.RangerServiceConfigDef> configs = Collections.singletonList(config);

        RangerServiceDef.RangerAccessTypeDef accessType = new RangerServiceDef.RangerAccessTypeDef();
        accessType.setItemId(1L);
        accessType.setName("sampleAccess");
        List<RangerServiceDef.RangerAccessTypeDef> accessTypes = Collections.singletonList(accessType);

        RangerServiceDef.RangerResourceDef resourceDef = new RangerServiceDef.RangerResourceDef();
        resourceDef.setItemId(1L);
        resourceDef.setName("root");
        resourceDef.setType("string");
        List<RangerServiceDef.RangerResourceDef> resourceDefs = Collections.singletonList(resourceDef);

        RangerServiceDef serviceDef = new RangerServiceDef();
        serviceDef.setName(serviceDefName);
        serviceDef.setConfigs(configs);
        serviceDef.setAccessTypes(accessTypes);
        serviceDef.setResources(resourceDefs);

        RangerServiceDef createdServiceDef = rangerClient.createServiceDef(serviceDef);
        LOG.info("New Service Definition created successfully {}", gsonBuilder.toJson(createdServiceDef));

        /*
        Create a new Service
         */
        RangerService service = new RangerService();
        service.setType(serviceDefName);
        service.setName(serviceName);

        RangerService createdService = rangerClient.createService(service);
        LOG.info("New Service created successfully {}", gsonBuilder.toJson(createdService));

        /*
        All Services
         */
        List<RangerService> services = rangerClient.findServices(filter);
        String allServiceNames = "";
        for (RangerService svc: services) {
            allServiceNames = allServiceNames.concat(svc.getName() + " ");
        }
        LOG.info("List of Services : {}", allServiceNames);

        /*
        Policy Management
         */


        /*
        Create a new Policy
         */
        Map<String, RangerPolicy.RangerPolicyResource> resource = Collections.singletonMap(
                "root", new RangerPolicy.RangerPolicyResource(Collections.singletonList("/path/to/sample/resource"),false,false));
        RangerPolicy policy = new RangerPolicy();
        policy.setService(serviceName);
        policy.setZoneName(zoneName);
        policy.setName(policyName);
        policy.setResources(resource);

        RangerPolicy createdPolicy = rangerClient.createPolicy(policy);
        LOG.info("Created policy {} in zone {}: {}", policyName, zoneName, gsonBuilder.toJson(createdPolicy));

        /*
        Get a policy by name and Zone
        */
        RangerPolicy fetchedPolicy = rangerClient.getPolicyByNameAndZone(serviceName, policyName, zoneName);
        LOG.info("Fetched policy {} in zone {}: {}", policyName, zoneName ,gsonBuilder.toJson(fetchedPolicy));

        /*
        Update a policy by name and Zone
        */
        RangerPolicy updatedPolicy = rangerClient.updatePolicyByNameAndZone(serviceName, policyName, zoneName, fetchedPolicy);
        LOG.info("Updated policy {} in zone {}: {}", policyName, zoneName ,gsonBuilder.toJson(updatedPolicy));

        /*
        Delete a policy by name and zone
        */
        rangerClient.deletePolicyByNameAndZone(serviceName, policyName, zoneName);
        LOG.info("Deleted policy {} in zone {}", policyName, zoneName);


        /* import tags */
        RangerTagDef tagDefTest1 = new RangerTagDef("test1");
        RangerTagDef tagDefTest2 = new RangerTagDef("test2");

        tagDefTest1.setAttributeDefs(Arrays.asList(new RangerTagAttributeDef("attr1", "string")));

        RangerTag tagTest1Val1 = new RangerTag(tagDefTest1.getName(), Collections.singletonMap("attr1", "val1"));
        RangerTag tagTest1Val2 = new RangerTag(tagDefTest1.getName(), Collections.singletonMap("attr1", "val2"));
        RangerTag tagTest2     = new RangerTag(tagDefTest2.getName(), Collections.emptyMap());

        RangerServiceResource db1 = new RangerServiceResource(serviceName, Collections.singletonMap("database", new RangerPolicyResource("db1")));
        RangerServiceResource db2 = new RangerServiceResource(serviceName, Collections.singletonMap("database", new RangerPolicyResource("db2")));

        db1.setId(1L);
        db2.setId(2L);

        RangerServiceTags serviceTags = new RangerServiceTags();

        serviceTags.setOp(RangerServiceTags.OP_SET);
        serviceTags.getTagDefinitions().put(0L, tagDefTest1);
        serviceTags.getTagDefinitions().put(1L, tagDefTest2);
        serviceTags.getTags().put(0L, tagTest1Val1);
        serviceTags.getTags().put(1L, tagTest1Val2);
        serviceTags.getTags().put(2L, tagTest2);
        serviceTags.getServiceResources().add(db1);
        serviceTags.getServiceResources().add(db2);
        serviceTags.getResourceToTagIds().put(db1.getId(), Arrays.asList(0L, 2L));
        serviceTags.getResourceToTagIds().put(db2.getId(), Arrays.asList(1L, 2L));

        LOG.info("Importing tags: {}", serviceTags);

        rangerClient.importServiceTags(serviceName, serviceTags);

        RangerServiceTags serviceTags2 = rangerClient.getServiceTags(serviceName);

        LOG.info("Imported tags: {}", serviceTags2);

        serviceTags.setOp(RangerServiceTags.OP_DELETE);
        serviceTags.setTagDefinitions(Collections.emptyMap());
        serviceTags.setTags(Collections.emptyMap());
        serviceTags.setResourceToTagIds(Collections.emptyMap());

        LOG.info("Deleting tags: {}" + serviceTags);

        rangerClient.importServiceTags(serviceName, serviceTags);

        serviceTags2 = rangerClient.getServiceTags(serviceName);

        LOG.info("Service tags after delete: {}", serviceTags2);

        /*
        Delete a Service
         */
        rangerClient.deleteService(serviceName);
        LOG.info("Service {} successfully deleted", serviceName);


        /*
        Delete a Service Definition
         */
        rangerClient.deleteServiceDef(serviceDefName);
        LOG.info("Service Definition {} successfully deleted", serviceDefName);


        /*
        Role Management
         */

        /*
        Create a role in Ranger
         */
        RangerRole sampleRole = new RangerRole();
        sampleRole.setName(roleName);
        sampleRole.setDescription("Sample Role");
        sampleRole.setUsers(Collections.singletonList(new RangerRole.RoleMember(null,true)));
        sampleRole = rangerClient.createRole(serviceName, sampleRole);
        LOG.info("New Role successfully created {}", gsonBuilder.toJson(sampleRole));

        /*
        Update a role in Ranger
         */
        sampleRole.setDescription("Updated Sample Role");
        RangerRole updatedRole = rangerClient.updateRole(sampleRole.getId(), sampleRole);
        LOG.info("Role {} successfully updated {}", roleName, gsonBuilder.toJson(updatedRole));

        /*
        Get all roles in Ranger
         */
        List<RangerRole> allRoles = rangerClient.findRoles(filter);
        LOG.info("List of Roles {}", gsonBuilder.toJson(allRoles));
        String allRoleNames = "";
        for (RangerRole role: allRoles) {
            allRoleNames = allRoleNames.concat(role.getName() + " ");
        }
        LOG.info("List of Roles : {}", allRoleNames);

        /*
        Delete a role in Ranger
         */
        rangerClient.deleteRole(roleName, userName, serviceName);
        LOG.info("Role {} successfully deleted", roleName);
    }
}
