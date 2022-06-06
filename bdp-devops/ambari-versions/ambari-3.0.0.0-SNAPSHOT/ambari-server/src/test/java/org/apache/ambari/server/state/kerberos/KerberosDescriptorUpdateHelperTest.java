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

package org.apache.ambari.server.state.kerberos;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;

import java.util.Collections;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.actionmanager.StageFactoryImpl;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.AuditLoggerDefaultImpl;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.hooks.users.UserHookService;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.scheduler.ExecutionSchedulerImpl;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.persist.UnitOfWork;

import junit.framework.Assert;

@Category({category.KerberosTest.class})
public class KerberosDescriptorUpdateHelperTest extends EasyMockSupport {
  private static final KerberosDescriptorFactory KERBEROS_DESCRIPTOR_FACTORY = new KerberosDescriptorFactory();
  private static final Gson GSON = new Gson();

  @Test
  @Ignore
  @Experimental(
      feature = ExperimentalFeature.MULTI_SERVICE,
      comment = "This was a very useful test that no longer works since all of the kerberos "
          + "descriptor files for services are stored in stacks no longer shipped with Ambari. "
          + "Although we could checking a bunch of descriptors, perhaps this is better suited to "
          + "something that is run when mpacks are registered.")
  public void updateDefaultUserKerberosDescriptor() throws Exception {
    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.put("metadata.path", "src/main/resources/stacks");
        properties.put("common.services.path", "src/main/resources/common-services");
        properties.put("server.version.file", "target/version");
        properties.put("custom.action.definitions", "/tmp/nofile");
        properties.put("resources.dir", "src/main/resources");
        Configuration configuration = new Configuration(properties);

        PartialNiceMockBinder.newBuilder(KerberosDescriptorUpdateHelperTest.this).addConfigsBindings()
            .addFactoriesInstallBinding().build().configure(binder());

        install(new FactoryModuleBuilder().build(StackManagerFactory.class));

        bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(configuration);
        bind(ExtensionLinkDAO.class).toInstance(createNiceMock(ExtensionLinkDAO.class));
        bind(PersistedState.class).toInstance(createNiceMock(PersistedState.class));
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);
        bind(UnitOfWork.class).toInstance(createNiceMock(UnitOfWork.class));
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(StageFactory.class).to(StageFactoryImpl.class);
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLoggerDefaultImpl.class));
        bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
        bind(HookService.class).to(UserHookService.class);
        bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
        bind(CredentialStoreService.class).toInstance(createNiceMock(CredentialStoreService.class));
        bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementController.class));
        bind(ExecutionScheduler.class).to(ExecutionSchedulerImpl.class);
        bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelper.class));
      }
    });

    OsFamily osFamily = injector.getInstance(OsFamily.class);
    expect(osFamily.os_list()).andReturn(Collections.singleton("centos6")).anyTimes();

    ExtensionLinkDAO linkDao = injector.getInstance(ExtensionLinkDAO.class);
    expect(linkDao.findByStack(anyString(), anyString())).andReturn(Collections.emptyList()).anyTimes();

    TypedQuery<StackEntity> query = createNiceMock(TypedQuery.class);
    expect(query.setMaxResults(1)).andReturn(query).anyTimes();
    expect(query.getSingleResult()).andReturn(null).anyTimes();

    EntityManager entityManager = injector.getInstance(EntityManager.class);
    expect(entityManager.createNamedQuery("StackEntity.findByNameAndVersion", StackEntity.class)).andReturn(query).anyTimes();
    expect(entityManager.find(EasyMock.eq(MetainfoEntity.class), anyString())).andReturn(createNiceMock(MetainfoEntity.class)).anyTimes();

    AmbariMetaInfo metaInfo = new AmbariMetaInfo(injector.getInstance(Configuration.class));

    replayAll();

    injector.injectMembers(metaInfo);
    metaInfo.init();

    KerberosDescriptor hdp24 = metaInfo.getKerberosDescriptor("HDP", "2.4", false);
    KerberosDescriptor hdp25 = metaInfo.getKerberosDescriptor("HDP", "2.5", false);
    KerberosDescriptor user = new KerberosDescriptor(hdp24.toMap());

    KerberosDescriptor updated = KerberosDescriptorUpdateHelper.updateUserKerberosDescriptor(hdp24, hdp25, user);

    KerberosDescriptor composite = new KerberosDescriptor(hdp25.toMap());
    composite.update(updated);
    Assert.assertEquals(GSON.toJson(hdp25.toMap()), GSON.toJson(composite.toMap()));
  }

  @Test
  public void testUpdateProperties() throws AmbariException {
    KerberosDescriptor oldValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance("{" +
        "  \"properties\": {" +
        "    \"realm\": \"${kerberos-env/realm}\"," +
        "    \"keytab_dir\": \"/etc/security/keytabs\"," +
        "    \"additional_realms\": \"\"," +
        "    \"old_property\": \"old_value\"" +
        "  }" +
        "}");

    KerberosDescriptor newValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance("{" +
        "  \"properties\": {" +
        "    \"realm\": \"${kerberos-env/realm}\"," +
        "    \"keytab_dir\": \"/etc/security/keytabs\"," +
        "    \"additional_realms\": \"\"," +
        "    \"new_property\": \"new_value\"" +
        "  }" +
        "}");

    KerberosDescriptor userValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance("{" +
        "  \"properties\": {" +
        "    \"realm\": \"EXAMPLE.COM\"," +
        "    \"keytab_dir\": \"/etc/security/keytabs\"," +
        "    \"additional_realms\": \"\"," +
        "    \"old_property\": \"old_value\"" +
        "  }" +
        "}");

    KerberosDescriptor updatedUserValue = KerberosDescriptorUpdateHelper.updateUserKerberosDescriptor(
        oldValue,
        newValue,
        userValue);

    // "old_property" is removed
    // "new_property" is added
    // "realm" retains user set value
    Assert.assertEquals(
        KERBEROS_DESCRIPTOR_FACTORY.createInstance(
            "{\n" +
                "  \"properties\": {\n" +
                "    \"new_property\": \"new_value\",\n" +
                "    \"realm\": \"EXAMPLE.COM\",\n" +
                "    \"additional_realms\": \"\",\n" +
                "    \"keytab_dir\": \"/etc/security/keytabs\"\n" +
                "  }\n" +
                "}"),
        updatedUserValue);
  }

  @Test
  public void testUpdateIdentities() throws AmbariException {
    KerberosDescriptor oldValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{" +
            "  \"identities\": [" +
            "    {" +
            "      \"name\": \"spnego\"," +
            "      \"principal\": {" +
            "        \"value\": \"HTTP/_HOST@${realm}\"," +
            "        \"type\": \"service\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"${keytab_dir}/spnego.service.keytab\"," +
            "        \"owner\": {" +
            "          \"name\": \"root\"," +
            "          \"access\": \"r\"" +
            "        }," +
            "        \"group\": {" +
            "          \"name\": \"${cluster-env/user_group}\"," +
            "          \"access\": \"r\"" +
            "        }" +
            "      }" +
            "    }," +
            "    {" +
            "      \"name\": \"smokeuser\"," +
            "      \"principal\": {" +
            "        \"value\": \"old_value@${realm}\"," +
            "        \"type\": \"user\"," +
            "        \"configuration\": \"cluster-env/smokeuser_principal_name\"," +
            "        \"local_username\": \"${cluster-env/smokeuser}\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"${keytab_dir}/smokeuser.headless.keytab\"," +
            "        \"owner\": {" +
            "          \"name\": \"${cluster-env/smokeuser}\"," +
            "          \"access\": \"r\"" +
            "        }," +
            "        \"group\": {" +
            "          \"name\": \"${cluster-env/user_group}\"," +
            "          \"access\": \"r\"" +
            "        }," +
            "        \"configuration\": \"cluster-env/smokeuser_keytab\"" +
            "      }" +
            "    }," +
            "    {" +
            "      \"name\": \"old_identity\"," +
            "      \"principal\": {" +
            "        \"value\": \"foobar${principal_suffix}@${realm}\"," +
            "        \"type\": \"user\"," +
            "        \"configuration\": \"cluster-env/ambari_principal_name\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"${keytab_dir}/ambari.server.keytab\"" +
            "      }" +
            "    }" +
            "  ]" +
            "}");

    KerberosDescriptor newValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{" +
            "  \"identities\": [" +
            "    {" +
            "      \"name\": \"spnego\"," +
            "      \"principal\": {" +
            "        \"value\": \"HTTP/_HOST@${realm}\"," +
            "        \"type\": \"service\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"${keytab_dir}/spnego.service.keytab\"," +
            "        \"owner\": {" +
            "          \"name\": \"root\"," +
            "          \"access\": \"r\"" +
            "        }," +
            "        \"group\": {" +
            "          \"name\": \"${cluster-env/user_group}\"," +
            "          \"access\": \"r\"" +
            "        }" +
            "      }" +
            "    }," +
            "    {" +
            "      \"name\": \"smokeuser\"," +
            "      \"principal\": {" +
            "        \"value\": \"${cluster-env/smokeuser}${principal_suffix}@${realm}\"," +
            "        \"type\": \"user\"," +
            "        \"configuration\": \"cluster-env/smokeuser_principal_name\"," +
            "        \"local_username\": \"${cluster-env/smokeuser}\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"updated_dir/smokeuser.headless.keytab\"," +
            "        \"owner\": {" +
            "          \"name\": \"${cluster-env/smokeuser}\"," +
            "          \"access\": \"r\"" +
            "        }," +
            "        \"group\": {" +
            "          \"name\": \"${cluster-env/user_group}\"," +
            "          \"access\": \"r\"" +
            "        }," +
            "        \"configuration\": \"cluster-env/smokeuser_keytab\"" +
            "      }" +
            "    }," +
            "    {" +
            "      \"name\": \"ambari-server\"," +
            "      \"principal\": {" +
            "        \"value\": \"ambari-server${principal_suffix}@${realm}\"," +
            "        \"type\": \"user\"," +
            "        \"configuration\": \"cluster-env/ambari_principal_name\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"${keytab_dir}/ambari.server.keytab\"" +
            "      }" +
            "    }," +
            "    {" +
            "      \"name\": \"future_identity\"," +
            "      \"principal\": {" +
            "        \"value\": \"CHANGED_future${principal_suffix}@${realm}\"," +
            "        \"type\": \"user\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"${keytab_dir}/future.user.keytab\"" +
            "      }" +
            "    }" +
            "  ]" +
            "}");

    KerberosDescriptor userValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{" +
            "  \"identities\": [" +
            "    {" +
            "      \"name\": \"spnego\"," +
            "      \"principal\": {" +
            "        \"value\": \"CHANGED_HTTP/_HOST@${realm}\"," +
            "        \"type\": \"service\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"${keytab_dir}/spnego.service.keytab\"," +
            "        \"owner\": {" +
            "          \"name\": \"root\"," +
            "          \"access\": \"r\"" +
            "        }," +
            "        \"group\": {" +
            "          \"name\": \"${cluster-env/user_group}\"," +
            "          \"access\": \"r\"" +
            "        }" +
            "      }" +
            "    }," +
            "    {" +
            "      \"name\": \"smokeuser\"," +
            "      \"principal\": {" +
            "        \"value\": \"old_value@${realm}\"," +
            "        \"type\": \"user\"," +
            "        \"configuration\": \"cluster-env/smokeuser_principal_name\"," +
            "        \"local_username\": \"${cluster-env/smokeuser}\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"custom_dir/smokeuser.headless.keytab\"," +
            "        \"owner\": {" +
            "          \"name\": \"${cluster-env/smokeuser}\"," +
            "          \"access\": \"r\"" +
            "        }," +
            "        \"group\": {" +
            "          \"name\": \"${cluster-env/user_group}\"," +
            "          \"access\": \"r\"" +
            "        }," +
            "        \"configuration\": \"cluster-env/smokeuser_keytab\"" +
            "      }" +
            "    }," +
            "    {" +
            "      \"name\": \"old_identity\"," +
            "      \"principal\": {" +
            "        \"value\": \"foobar${principal_suffix}@${realm}\"," +
            "        \"type\": \"user\"," +
            "        \"configuration\": \"cluster-env/ambari_principal_name\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"${keytab_dir}/ambari.server.keytab\"" +
            "      }" +
            "    }," +
            "    {" +
            "      \"name\": \"custom_identity\"," +
            "      \"principal\": {" +
            "        \"value\": \"custom${principal_suffix}@${realm}\"," +
            "        \"type\": \"user\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"${keytab_dir}/custom.user.keytab\"" +
            "      }" +
            "    }," +
            "    {" +
            "      \"name\": \"future_identity\"," +
            "      \"principal\": {" +
            "        \"value\": \"future${principal_suffix}@${realm}\"," +
            "        \"type\": \"user\"" +
            "      }," +
            "      \"keytab\": {" +
            "        \"file\": \"${keytab_dir}/future.user.keytab\"" +
            "      }" +
            "    }" +
            "  ]" +
            "}");


    KerberosDescriptor updatedUserValue = KerberosDescriptorUpdateHelper.updateUserKerberosDescriptor(
        oldValue,
        newValue,
        userValue);

    Assert.assertEquals(
        GSON.toJson(KERBEROS_DESCRIPTOR_FACTORY.createInstance(
            "{\n" +
                "  \"identities\": [\n" +
                "    {\n" +
                "      \"name\": \"future_identity\",\n" +
                "      \"principal\": {\n" +
                "        \"value\": \"future${principal_suffix}@${realm}\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"keytab\": {\n" +
                "        \"file\": \"${keytab_dir}/future.user.keytab\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"custom_identity\",\n" +
                "      \"principal\": {\n" +
                "        \"value\": \"custom${principal_suffix}@${realm}\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"keytab\": {\n" +
                "        \"file\": \"${keytab_dir}/custom.user.keytab\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"spnego\",\n" +
                "      \"principal\": {\n" +
                "        \"value\": \"CHANGED_HTTP/_HOST@${realm}\",\n" +
                "        \"type\": \"service\"\n" +
                "      },\n" +
                "      \"keytab\": {\n" +
                "        \"file\": \"${keytab_dir}/spnego.service.keytab\",\n" +
                "        \"owner\": {\n" +
                "          \"name\": \"root\",\n" +
                "          \"access\": \"r\"\n" +
                "        },\n" +
                "        \"group\": {\n" +
                "          \"name\": \"${cluster-env/user_group}\",\n" +
                "          \"access\": \"r\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"smokeuser\",\n" +
                "      \"principal\": {\n" +
                "        \"value\": \"${cluster-env/smokeuser}${principal_suffix}@${realm}\",\n" +
                "        \"local_username\": \"${cluster-env/smokeuser}\",\n" +
                "        \"configuration\": \"cluster-env/smokeuser_principal_name\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"keytab\": {\n" +
                "        \"file\": \"custom_dir/smokeuser.headless.keytab\",\n" +
                "        \"owner\": {\n" +
                "          \"name\": \"${cluster-env/smokeuser}\",\n" +
                "          \"access\": \"r\"\n" +
                "        },\n" +
                "        \"group\": {\n" +
                "          \"name\": \"${cluster-env/user_group}\",\n" +
                "          \"access\": \"r\"\n" +
                "        },\n" +
                "        \"configuration\": \"cluster-env/smokeuser_keytab\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}").toMap()),
        GSON.toJson(updatedUserValue.toMap()));

    // Test that the merge of the default (stack) Kerberos descriptor and the updated user-specified
    // Kerberos descriptor yield the expected composite Kerberos descriptor.
    newValue.update(updatedUserValue);

    Assert.assertEquals(
        GSON.toJson(KERBEROS_DESCRIPTOR_FACTORY.createInstance(
            "{\n" +
                "  \"identities\": [\n" +
                "    {\n" +
                "      \"name\": \"ambari-server\",\n" +
                "      \"principal\": {\n" +
                "        \"value\": \"ambari-server${principal_suffix}@${realm}\",\n" +
                "        \"configuration\": \"cluster-env/ambari_principal_name\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"keytab\": {\n" +
                "        \"file\": \"${keytab_dir}/ambari.server.keytab\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"custom_identity\",\n" +
                "      \"principal\": {\n" +
                "        \"value\": \"custom${principal_suffix}@${realm}\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"keytab\": {\n" +
                "        \"file\": \"${keytab_dir}/custom.user.keytab\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"future_identity\",\n" +
                "      \"principal\": {\n" +
                "        \"value\": \"future${principal_suffix}@${realm}\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"keytab\": {\n" +
                "        \"file\": \"${keytab_dir}/future.user.keytab\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"spnego\",\n" +
                "      \"principal\": {\n" +
                "        \"value\": \"CHANGED_HTTP/_HOST@${realm}\",\n" +
                "        \"type\": \"service\"\n" +
                "      },\n" +
                "      \"keytab\": {\n" +
                "        \"file\": \"${keytab_dir}/spnego.service.keytab\",\n" +
                "        \"owner\": {\n" +
                "          \"name\": \"root\",\n" +
                "          \"access\": \"r\"\n" +
                "        },\n" +
                "        \"group\": {\n" +
                "          \"name\": \"${cluster-env/user_group}\",\n" +
                "          \"access\": \"r\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"smokeuser\",\n" +
                "      \"principal\": {\n" +
                "        \"value\": \"${cluster-env/smokeuser}${principal_suffix}@${realm}\",\n" +
                "        \"local_username\": \"${cluster-env/smokeuser}\",\n" +
                "        \"configuration\": \"cluster-env/smokeuser_principal_name\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"keytab\": {\n" +
                "        \"file\": \"custom_dir/smokeuser.headless.keytab\",\n" +
                "        \"owner\": {\n" +
                "          \"name\": \"${cluster-env/smokeuser}\",\n" +
                "          \"access\": \"r\"\n" +
                "        },\n" +
                "        \"group\": {\n" +
                "          \"name\": \"${cluster-env/user_group}\",\n" +
                "          \"access\": \"r\"\n" +
                "        },\n" +
                "        \"configuration\": \"cluster-env/smokeuser_keytab\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}").toMap()),
        GSON.toJson(newValue.toMap()));
  }

  @Test
  public void testUpdateConfigurations() throws AmbariException {
    KerberosDescriptor oldValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{\n" +
            "  \"configurations\": [\n" +
            "    {\n" +
            "      \"core-site\": {\n" +
            "        \"hadoop.security.authentication\": \"kerberos\",\n" +
            "        \"hadoop.security.authorization\": \"true\",\n" +
            "        \"hadoop.proxyuser.HTTP.groups\": \"${hadoop-env/proxyuser_group}\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"some-site\": {\n" +
            "        \"property.unchanged\": \"value 1\",\n" +
            "        \"property.removed\": \"removed value\",\n" +
            "        \"property.altered\": \"old value\"\n," +
            "        \"property.property.changed.in.new\": \"orig value\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"old-site\": {\n" +
            "        \"property\": \"value\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n");

    KerberosDescriptor newValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{\n" +
            "  \"configurations\": [\n" +
            "    {\n" +
            "      \"core-site\": {\n" +
            "        \"hadoop.security.authentication\": \"kerberos\",\n" +
            "        \"hadoop.security.authorization\": \"true\",\n" +
            "        \"hadoop.proxyuser.HTTP.groups\": \"${hadoop-env/proxyuser_group}\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"some-site\": {\n" +
            "        \"property.unchanged\": \"value 1\",\n" +
            "        \"property.added\": \"added value\",\n" +
            "        \"property.altered\": \"new value\",\n" +
            "        \"property.changed.in.new\": \"new value\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"new-site\": {\n" +
            "        \"property.for.new.site\": \"value\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n");

    KerberosDescriptor userValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{\n" +
            "  \"configurations\": [\n" +
            "    {\n" +
            "      \"core-site\": {\n" +
            "        \"hadoop.security.authentication\": \"kerberos\",\n" +
            "        \"hadoop.security.authorization\": \"true\",\n" +
            "        \"hadoop.proxyuser.HTTP.groups\": \"${hadoop-env/proxyuser_group}\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"some-site\": {\n" +
            "        \"property.unchanged\": \"value 1\",\n" +
            "        \"property.removed\": \"changed removed value\",\n" +
            "        \"property.altered\": \"custom value\"\n," +
            "        \"property.property.changed.in.new\": \"orig value\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"old-site\": {\n" +
            "        \"property\": \"value\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n");

    KerberosDescriptor updatedUserValue = KerberosDescriptorUpdateHelper.updateUserKerberosDescriptor(
        oldValue,
        newValue,
        userValue);

    Assert.assertEquals(
        GSON.toJson(KERBEROS_DESCRIPTOR_FACTORY.createInstance(
            "{\n" +
                "  \"configurations\": [\n" +
                "    {\n" +
                "      \"core-site\": {\n" +
                "        \"hadoop.security.authentication\": \"kerberos\",\n" +
                "        \"hadoop.security.authorization\": \"true\",\n" +
                "        \"hadoop.proxyuser.HTTP.groups\": \"${hadoop-env/proxyuser_group}\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"some-site\": {\n" +
                "        \"property.unchanged\": \"value 1\",\n" +
                "        \"property.added\": \"added value\",\n" +
                "        \"property.altered\": \"custom value\",\n" +
                "        \"property.changed.in.new\": \"new value\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n").toMap()),
        GSON.toJson(updatedUserValue.toMap()));
  }

  @Test
  public void testUpdateAuthToLocalRules() throws AmbariException {
    KerberosDescriptor oldValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{\n" +
            "  \"auth_to_local_properties\" : [\n" +
            "    \"core-site/hadoop.security.auth_to_local\",\n" +
            "    \"some-site/to.be.removed\"\n" +
            "  ]\n" +
            "}\n");

    KerberosDescriptor newValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{\n" +
            "  \"auth_to_local_properties\" : [\n" +
            "    \"core-site/hadoop.security.auth_to_local\",\n" +
            "    \"some-site/to.be.added\"\n" +
            "  ]\n" +
            "}\n");

    KerberosDescriptor userValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{\n" +
            "  \"auth_to_local_properties\" : [\n" +
            "    \"core-site/hadoop.security.auth_to_local\",\n" +
            "    \"some-site/added.by.user\"\n" +
            "  ]\n" +
            "}\n");

    KerberosDescriptor updatedUserValue = KerberosDescriptorUpdateHelper.updateUserKerberosDescriptor(
        oldValue,
        newValue,
        userValue);

    Assert.assertEquals(
        KERBEROS_DESCRIPTOR_FACTORY.createInstance(
            "{\n" +
                "  \"auth_to_local_properties\" : [\n" +
                "    \"core-site/hadoop.security.auth_to_local\",\n" +
                "    \"some-site/to.be.added\",\n" +
                "    \"some-site/added.by.user\"\n" +
                "  ]\n" +
                "}\n"),
        updatedUserValue);
  }

  @Test
  public void testUpdateServices() throws AmbariException {
    KerberosDescriptor oldValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{\n" +
            "  \"services\": [\n" +
            "    {\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.kerberos.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"nn/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"secondary_namenode_nn\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/nn.service.keytab\",\n" +
            "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.keytab.file\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.kerberos.internal.spnego.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": null\n" +
            "              },\n" +
            "              \"name\": \"/spnego\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"SECONDARY_NAMENODE\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"HDFS_CLIENT\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.datanode.kerberos.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"dn/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"datanode_dn\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/dn.service.keytab\",\n" +
            "                \"configuration\": \"hdfs-site/dfs.datanode.keytab.file\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"configurations\": [\n" +
            "            {\n" +
            "              \"hdfs-site\": {\n" +
            "                \"dfs.datanode.address\": \"0.0.0.0:1019\",\n" +
            "                \"dfs.datanode.http.address\": \"0.0.0.0:1022\"\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"DATANODE\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/nfs.kerberos.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"nfs/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"nfsgateway\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/nfs.service.keytab\",\n" +
            "                \"configuration\": \"hdfs-site/nfs.keytab.file\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"NFS_GATEWAY\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.journalnode.kerberos.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"jn/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"journalnode_jn\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/jn.service.keytab\",\n" +
            "                \"configuration\": \"hdfs-site/dfs.journalnode.keytab.file\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.journalnode.kerberos.internal.spnego.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": null\n" +
            "              },\n" +
            "              \"name\": \"/spnego\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"JOURNALNODE\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hadoop-env/hdfs_principal_name\",\n" +
            "                \"type\": \"user\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"${hadoop-env/hdfs_user}${principal_suffix}@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"hdfs\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/hdfs.headless.keytab\",\n" +
            "                \"configuration\": \"hadoop-env/hdfs_user_keytab\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.namenode.kerberos.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"nn/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"namenode_nn\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/nn.service.keytab\",\n" +
            "                \"configuration\": \"hdfs-site/dfs.namenode.keytab.file\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.namenode.kerberos.internal.spnego.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": null\n" +
            "              },\n" +
            "              \"name\": \"/spnego\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"configurations\": [\n" +
            "            {\n" +
            "              \"hdfs-site\": {\n" +
            "                \"dfs.block.access.token.enable\": \"true\"\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"NAMENODE\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"identities\": [\n" +
            "        {\n" +
            "          \"principal\": {\n" +
            "            \"configuration\": \"hdfs-site/dfs.web.authentication.kerberos.principal\",\n" +
            "            \"type\": null,\n" +
            "            \"local_username\": null,\n" +
            "            \"value\": null\n" +
            "          },\n" +
            "          \"name\": \"/spnego\",\n" +
            "          \"keytab\": {\n" +
            "            \"owner\": {\n" +
            "              \"access\": null,\n" +
            "              \"name\": null\n" +
            "            },\n" +
            "            \"file\": null,\n" +
            "            \"configuration\": \"hdfs-site/dfs.web.authentication.kerberos.keytab\",\n" +
            "            \"group\": {\n" +
            "              \"access\": null,\n" +
            "              \"name\": null\n" +
            "            }\n" +
            "          }\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"/smokeuser\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"auth_to_local_properties\": [\n" +
            "        \"core-site/hadoop.security.auth_to_local\"\n" +
            "      ],\n" +
            "      \"configurations\": [\n" +
            "        {\n" +
            "          \"core-site\": {\n" +
            "            \"hadoop.security.authorization\": \"true\",\n" +
            "            \"hadoop.security.authentication\": \"kerberos\",\n" +
            "            \"hadoop.proxyuser.HTTP.groups\": \"${hadoop-env/proxyuser_group}\"\n" +
            "          }\n" +
            "        }\n" +
            "      ],\n" +
            "      \"name\": \"HDFS\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"components\" : [\n" +
            "        {\n" +
            "          \"name\" : \"OLD_SERVICE_CLIENT\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\" : [\n" +
            "            {\n" +
            "              \"name\" : \"/HDFS/NAMENODE/hdfs\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\" : \"OLD_SERVICE_FOOBAR\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\" : [\n" +
            "            {\n" +
            "              \"name\" : \"/HDFS/NAMENODE/hdfs\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\" : \"/HIVE/HIVE_SERVER/hive_server_hive\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\" : \"OLD_SERVICE_SERVER\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"identities\" : [\n" +
            "        {\n" +
            "          \"name\" : \"/smokeuser\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"name\" : \"OLD_SERVICE\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"application-properties/atlas.authentication.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${atlas-env/metadata_user}\",\n" +
            "                \"value\": \"atlas/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"atlas\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${atlas-env/metadata_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/atlas.service.keytab\",\n" +
            "                \"configuration\": \"application-properties/atlas.authentication.keytab\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"application-properties/atlas.http.authentication.kerberos.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": \"HTTP/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"/spnego\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": null,\n" +
            "                  \"name\": null\n" +
            "                },\n" +
            "                \"file\": null,\n" +
            "                \"configuration\": \"application-properties/atlas.http.authentication.kerberos.keytab\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": null,\n" +
            "                  \"name\": null\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"ATLAS_SERVER\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"auth_to_local_properties\": [\n" +
            "        \"application-properties/atlas.http.authentication.kerberos.name.rules|new_lines_escaped\"\n" +
            "      ],\n" +
            "      \"configurations\": [\n" +
            "        {\n" +
            "          \"application-properties\": {\n" +
            "            \"atlas.authentication.method\": \"kerberos\",\n" +
            "            \"atlas.http.authentication.enabled\": \"true\",\n" +
            "            \"atlas.http.authentication.type\": \"kerberos\"\n" +
            "          }\n" +
            "        }\n" +
            "      ],\n" +
            "      \"name\": \"ATLAS\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"name\": \"EXISTING_SERVICE_CLIENT\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\": \"/HIVE/HIVE_SERVER/hive_server_hive\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"EXISTING_SERVICE_SERVER\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"EXISTING_SERVICE_ORIG_SERVER\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"identities\": [\n" +
            "        {\n" +
            "          \"name\": \"/smokeuser\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"name\": \"EXISTING_SERVICE\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n");

    KerberosDescriptor newValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(
        "{\n" +
            "  \"services\": [\n" +
            "    {\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"application-properties/atlas.jaas.KafkaClient.option.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${atlas-env/metadata_user}\",\n" +
            "                \"value\": \"atlas/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"atlas\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${atlas-env/metadata_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/atlas.service.keytab\",\n" +
            "                \"configuration\": \"application-properties/atlas.jaas.KafkaClient.option.keyTab\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"reference\": \"/ATLAS/ATLAS_SERVER/atlas\",\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"application-properties/atlas.authentication.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": null\n" +
            "              },\n" +
            "              \"name\": \"atlas_auth\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": null,\n" +
            "                  \"name\": null\n" +
            "                },\n" +
            "                \"file\": null,\n" +
            "                \"configuration\": \"application-properties/atlas.authentication.keytab\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": null,\n" +
            "                  \"name\": null\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"application-properties/atlas.authentication.method.kerberos.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": \"HTTP/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"/spnego\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": null,\n" +
            "                  \"name\": null\n" +
            "                },\n" +
            "                \"file\": null,\n" +
            "                \"configuration\": \"application-properties/atlas.authentication.method.kerberos.keytab\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": null,\n" +
            "                  \"name\": null\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"reference\": \"/ATLAS/ATLAS_SERVER/atlas\",\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"ranger-atlas-audit/xasecure.audit.jaas.Client.option.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": null\n" +
            "              },\n" +
            "              \"name\": \"ranger_atlas_audit\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": null,\n" +
            "                  \"name\": null\n" +
            "                },\n" +
            "                \"file\": null,\n" +
            "                \"configuration\": \"ranger-atlas-audit/xasecure.audit.jaas.Client.option.keyTab\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": null,\n" +
            "                  \"name\": null\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"ATLAS_SERVER\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"auth_to_local_properties\": [\n" +
            "        \"application-properties/atlas.authentication.method.kerberos.name.rules|new_lines_escaped\"\n" +
            "      ],\n" +
            "      \"configurations\": [\n" +
            "        {\n" +
            "          \"ranger-atlas-audit\": {\n" +
            "            \"xasecure.audit.jaas.Client.loginModuleControlFlag\": \"required\",\n" +
            "            \"xasecure.audit.jaas.Client.option.serviceName\": \"solr\",\n" +
            "            \"xasecure.audit.jaas.Client.loginModuleName\": \"com.sun.security.auth.module.Krb5LoginModule\",\n" +
            "            \"xasecure.audit.jaas.Client.option.useKeyTab\": \"true\",\n" +
            "            \"xasecure.audit.jaas.Client.option.storeKey\": \"false\",\n" +
            "            \"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\": \"true\"\n" +
            "          }\n" +
            "        },\n" +
            "        {\n" +
            "          \"application-properties\": {\n" +
            "            \"atlas.kafka.security.protocol\": \"PLAINTEXTSASL\",\n" +
            "            \"atlas.jaas.KafkaClient.option.storeKey\": \"true\",\n" +
            "            \"atlas.solr.kerberos.enable\": \"true\",\n" +
            "            \"atlas.jaas.KafkaClient.loginModuleControlFlag\": \"required\",\n" +
            "            \"atlas.authentication.method.kerberos\": \"true\",\n" +
            "            \"atlas.jaas.KafkaClient.option.useKeyTab\": \"true\",\n" +
            "            \"atlas.kafka.sasl.kerberos.service.name\": \"${kafka-env/kafka_user}\",\n" +
            "            \"atlas.jaas.KafkaClient.loginModuleName\": \"com.sun.security.auth.module.Krb5LoginModule\",\n" +
            "            \"atlas.jaas.KafkaClient.option.serviceName\": \"${kafka-env/kafka_user}\"\n" +
            "          }\n" +
            "        }\n" +
            "      ],\n" +
            "      \"name\": \"ATLAS\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.kerberos.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"nn/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"secondary_namenode_nn\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/nn.service.keytab\",\n" +
            "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.keytab.file\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.kerberos.internal.spnego.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": null\n" +
            "              },\n" +
            "              \"name\": \"/spnego\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"SECONDARY_NAMENODE\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"HDFS_CLIENT\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.datanode.kerberos.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"dn/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"datanode_dn\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/dn.service.keytab\",\n" +
            "                \"configuration\": \"hdfs-site/dfs.datanode.keytab.file\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"configurations\": [\n" +
            "            {\n" +
            "              \"hdfs-site\": {\n" +
            "                \"dfs.datanode.address\": \"0.0.0.0:1019\",\n" +
            "                \"dfs.datanode.http.address\": \"0.0.0.0:1022\"\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"DATANODE\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/nfs.kerberos.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"nfs/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"nfsgateway\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/nfs.service.keytab\",\n" +
            "                \"configuration\": \"hdfs-site/nfs.keytab.file\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"NFS_GATEWAY\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.journalnode.kerberos.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"jn/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"journalnode_jn\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/jn.service.keytab\",\n" +
            "                \"configuration\": \"hdfs-site/dfs.journalnode.keytab.file\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.journalnode.kerberos.internal.spnego.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": null\n" +
            "              },\n" +
            "              \"name\": \"/spnego\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"JOURNALNODE\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hadoop-env/hdfs_principal_name\",\n" +
            "                \"type\": \"user\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"${hadoop-env/hdfs_user}${principal_suffix}@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"hdfs\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/hdfs.headless.keytab\",\n" +
            "                \"configuration\": \"hadoop-env/hdfs_user_keytab\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.namenode.kerberos.principal\",\n" +
            "                \"type\": \"service\",\n" +
            "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
            "                \"value\": \"nn/_HOST@${realm}\"\n" +
            "              },\n" +
            "              \"name\": \"namenode_nn\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": \"r\",\n" +
            "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
            "                },\n" +
            "                \"file\": \"${keytab_dir}/nn.service.keytab\",\n" +
            "                \"configuration\": \"hdfs-site/dfs.namenode.keytab.file\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": \"\",\n" +
            "                  \"name\": \"${cluster-env/user_group}\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"hdfs-site/dfs.namenode.kerberos.internal.spnego.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": null\n" +
            "              },\n" +
            "              \"name\": \"/spnego\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"principal\": {\n" +
            "                \"configuration\": \"ranger-hdfs-audit/xasecure.audit.jaas.Client.option.principal\",\n" +
            "                \"type\": null,\n" +
            "                \"local_username\": null,\n" +
            "                \"value\": null\n" +
            "              },\n" +
            "              \"name\": \"/HDFS/NAMENODE/namenode_nn\",\n" +
            "              \"keytab\": {\n" +
            "                \"owner\": {\n" +
            "                  \"access\": null,\n" +
            "                  \"name\": null\n" +
            "                },\n" +
            "                \"file\": null,\n" +
            "                \"configuration\": \"ranger-hdfs-audit/xasecure.audit.jaas.Client.option.keyTab\",\n" +
            "                \"group\": {\n" +
            "                  \"access\": null,\n" +
            "                  \"name\": null\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"configurations\": [\n" +
            "            {\n" +
            "              \"hdfs-site\": {\n" +
            "                \"dfs.block.access.token.enable\": \"true\"\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"NAMENODE\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"identities\": [\n" +
            "        {\n" +
            "          \"principal\": {\n" +
            "            \"configuration\": \"hdfs-site/dfs.web.authentication.kerberos.principal\",\n" +
            "            \"type\": null,\n" +
            "            \"local_username\": null,\n" +
            "            \"value\": null\n" +
            "          },\n" +
            "          \"name\": \"/spnego\",\n" +
            "          \"keytab\": {\n" +
            "            \"owner\": {\n" +
            "              \"access\": null,\n" +
            "              \"name\": null\n" +
            "            },\n" +
            "            \"file\": null,\n" +
            "            \"configuration\": \"hdfs-site/dfs.web.authentication.kerberos.keytab\",\n" +
            "            \"group\": {\n" +
            "              \"access\": null,\n" +
            "              \"name\": null\n" +
            "            }\n" +
            "          }\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"/smokeuser\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"auth_to_local_properties\": [\n" +
            "        \"core-site/hadoop.security.auth_to_local\"\n" +
            "      ],\n" +
            "      \"configurations\": [\n" +
            "        {\n" +
            "          \"ranger-hdfs-audit\": {\n" +
            "            \"xasecure.audit.jaas.Client.loginModuleControlFlag\": \"required\",\n" +
            "            \"xasecure.audit.jaas.Client.option.serviceName\": \"solr\",\n" +
            "            \"xasecure.audit.jaas.Client.loginModuleName\": \"com.sun.security.auth.module.Krb5LoginModule\",\n" +
            "            \"xasecure.audit.jaas.Client.option.useKeyTab\": \"true\",\n" +
            "            \"xasecure.audit.jaas.Client.option.storeKey\": \"false\",\n" +
            "            \"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\": \"true\"\n" +
            "          }\n" +
            "        },\n" +
            "        {\n" +
            "          \"core-site\": {\n" +
            "            \"hadoop.security.authorization\": \"true\",\n" +
            "            \"hadoop.security.authentication\": \"kerberos\",\n" +
            "            \"hadoop.proxyuser.HTTP.groups\": \"${hadoop-env/proxyuser_group}\"\n" +
            "          }\n" +
            "        }\n" +
            "      ],\n" +
            "      \"name\": \"HDFS\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"name\": \"NEW_SERVICE_CLIENT\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\": \"/HIVE/HIVE_SERVER/hive_server_hive\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"NEW_SERVICE_FOO_BAR\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"NEW_SERVICE_SERVER\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"identities\": [\n" +
            "        {\n" +
            "          \"name\": \"/smokeuser\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"name\": \"NEW_SERVICE\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"name\": \"EXISTING_SERVICE_CLIENT\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"EXISTING_SERVICE_SERVER\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"identities\": [\n" +
            "            {\n" +
            "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"name\": \"EXISTING_SERVICE_NEW_SERVER\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"identities\": [\n" +
            "        {\n" +
            "          \"name\": \"/smokeuser\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"/HIVE/HIVE_SERVER/hive_server_hive\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"name\": \"EXISTING_SERVICE\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n");

    KerberosDescriptor userValue = KERBEROS_DESCRIPTOR_FACTORY.createInstance(oldValue.toMap());

    KerberosDescriptor updatedUserValue = KerberosDescriptorUpdateHelper.updateUserKerberosDescriptor(
        oldValue,
        newValue,
        userValue);

    Assert.assertEquals(
        GSON.toJson(KERBEROS_DESCRIPTOR_FACTORY.createInstance(
            "{\n" +
                "  \"services\": [\n" +
                "    {\n" +
                "      \"components\": [\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"application-properties/atlas.jaas.KafkaClient.option.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${atlas-env/metadata_user}\",\n" +
                "                \"value\": \"atlas/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"atlas\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${atlas-env/metadata_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/atlas.service.keytab\",\n" +
                "                \"configuration\": \"application-properties/atlas.jaas.KafkaClient.option.keyTab\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"application-properties/atlas.authentication.method.kerberos.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": \"HTTP/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"/spnego\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": null,\n" +
                "                  \"name\": null\n" +
                "                },\n" +
                "                \"file\": null,\n" +
                "                \"configuration\": \"application-properties/atlas.authentication.method.kerberos.keytab\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": null,\n" +
                "                  \"name\": null\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"ATLAS_SERVER\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"auth_to_local_properties\": [\n" +
                "        \"application-properties/atlas.authentication.method.kerberos.name.rules|new_lines_escaped\"\n" +
                "      ],\n" +
                "      \"configurations\": [\n" +
                "        {\n" +
                "          \"application-properties\": {\n" +
                "            \"atlas.kafka.security.protocol\": \"PLAINTEXTSASL\",\n" +
                "            \"atlas.jaas.KafkaClient.option.storeKey\": \"true\",\n" +
                "            \"atlas.solr.kerberos.enable\": \"true\",\n" +
                "            \"atlas.jaas.KafkaClient.loginModuleControlFlag\": \"required\",\n" +
                "            \"atlas.authentication.method.kerberos\": \"true\",\n" +
                "            \"atlas.jaas.KafkaClient.option.useKeyTab\": \"true\",\n" +
                "            \"atlas.kafka.sasl.kerberos.service.name\": \"${kafka-env/kafka_user}\",\n" +
                "            \"atlas.jaas.KafkaClient.loginModuleName\": \"com.sun.security.auth.module.Krb5LoginModule\",\n" +
                "            \"atlas.jaas.KafkaClient.option.serviceName\": \"${kafka-env/kafka_user}\"\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"ATLAS\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"components\": [\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.kerberos.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"nn/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"secondary_namenode_nn\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/nn.service.keytab\",\n" +
                "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.keytab.file\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.kerberos.internal.spnego.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": null\n" +
                "              },\n" +
                "              \"name\": \"/spnego\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"SECONDARY_NAMENODE\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"HDFS_CLIENT\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.datanode.kerberos.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"dn/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"datanode_dn\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/dn.service.keytab\",\n" +
                "                \"configuration\": \"hdfs-site/dfs.datanode.keytab.file\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"configurations\": [\n" +
                "            {\n" +
                "              \"hdfs-site\": {\n" +
                "                \"dfs.datanode.address\": \"0.0.0.0:1019\",\n" +
                "                \"dfs.datanode.http.address\": \"0.0.0.0:1022\"\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"DATANODE\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/nfs.kerberos.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"nfs/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"nfsgateway\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/nfs.service.keytab\",\n" +
                "                \"configuration\": \"hdfs-site/nfs.keytab.file\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"NFS_GATEWAY\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.journalnode.kerberos.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"jn/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"journalnode_jn\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/jn.service.keytab\",\n" +
                "                \"configuration\": \"hdfs-site/dfs.journalnode.keytab.file\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.journalnode.kerberos.internal.spnego.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": null\n" +
                "              },\n" +
                "              \"name\": \"/spnego\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"JOURNALNODE\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hadoop-env/hdfs_principal_name\",\n" +
                "                \"type\": \"user\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"${hadoop-env/hdfs_user}${principal_suffix}@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"hdfs\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/hdfs.headless.keytab\",\n" +
                "                \"configuration\": \"hadoop-env/hdfs_user_keytab\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.namenode.kerberos.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"nn/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"namenode_nn\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/nn.service.keytab\",\n" +
                "                \"configuration\": \"hdfs-site/dfs.namenode.keytab.file\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.namenode.kerberos.internal.spnego.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": null\n" +
                "              },\n" +
                "              \"name\": \"/spnego\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"configurations\": [\n" +
                "            {\n" +
                "              \"hdfs-site\": {\n" +
                "                \"dfs.block.access.token.enable\": \"true\"\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"NAMENODE\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"identities\": [\n" +
                "        {\n" +
                "          \"principal\": {\n" +
                "            \"configuration\": \"hdfs-site/dfs.web.authentication.kerberos.principal\",\n" +
                "            \"type\": null,\n" +
                "            \"local_username\": null,\n" +
                "            \"value\": null\n" +
                "          },\n" +
                "          \"name\": \"/spnego\",\n" +
                "          \"keytab\": {\n" +
                "            \"owner\": {\n" +
                "              \"access\": null,\n" +
                "              \"name\": null\n" +
                "            },\n" +
                "            \"file\": null,\n" +
                "            \"configuration\": \"hdfs-site/dfs.web.authentication.kerberos.keytab\",\n" +
                "            \"group\": {\n" +
                "              \"access\": null,\n" +
                "              \"name\": null\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"/smokeuser\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"auth_to_local_properties\": [\n" +
                "        \"core-site/hadoop.security.auth_to_local\"\n" +
                "      ],\n" +
                "      \"configurations\": [\n" +
                "        {\n" +
                "          \"core-site\": {\n" +
                "            \"hadoop.security.authorization\": \"true\",\n" +
                "            \"hadoop.security.authentication\": \"kerberos\",\n" +
                "            \"hadoop.proxyuser.HTTP.groups\": \"${hadoop-env/proxyuser_group}\"\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"HDFS\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"components\": [\n" +
                "        {\n" +
                "          \"name\": \"EXISTING_SERVICE_CLIENT\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"EXISTING_SERVICE_SERVER\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"identities\": [\n" +
                "        {\n" +
                "          \"name\": \"/smokeuser\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"EXISTING_SERVICE\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n").toMap()),
        GSON.toJson(updatedUserValue.toMap()));

    // Test that the merge of the default (stack) Kerberos descriptor and the updated user-specified
    // Kerberos descriptor yield the expected composite Kerberos descriptor.
    newValue.update(updatedUserValue);

    Assert.assertEquals(
        GSON.toJson(KERBEROS_DESCRIPTOR_FACTORY.createInstance(
            "{\n" +
                "  \"services\": [\n" +
                "    {\n" +
                "      \"components\": [\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"application-properties/atlas.jaas.KafkaClient.option.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${atlas-env/metadata_user}\",\n" +
                "                \"value\": \"atlas/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"atlas\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${atlas-env/metadata_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/atlas.service.keytab\",\n" +
                "                \"configuration\": \"application-properties/atlas.jaas.KafkaClient.option.keyTab\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"reference\": \"/ATLAS/ATLAS_SERVER/atlas\",\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"application-properties/atlas.authentication.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": null\n" +
                "              },\n" +
                "              \"name\": \"atlas_auth\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": null,\n" +
                "                  \"name\": null\n" +
                "                },\n" +
                "                \"file\": null,\n" +
                "                \"configuration\": \"application-properties/atlas.authentication.keytab\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": null,\n" +
                "                  \"name\": null\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"application-properties/atlas.authentication.method.kerberos.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": \"HTTP/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"/spnego\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": null,\n" +
                "                  \"name\": null\n" +
                "                },\n" +
                "                \"file\": null,\n" +
                "                \"configuration\": \"application-properties/atlas.authentication.method.kerberos.keytab\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": null,\n" +
                "                  \"name\": null\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"reference\": \"/ATLAS/ATLAS_SERVER/atlas\",\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"ranger-atlas-audit/xasecure.audit.jaas.Client.option.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": null\n" +
                "              },\n" +
                "              \"name\": \"ranger_atlas_audit\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": null,\n" +
                "                  \"name\": null\n" +
                "                },\n" +
                "                \"file\": null,\n" +
                "                \"configuration\": \"ranger-atlas-audit/xasecure.audit.jaas.Client.option.keyTab\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": null,\n" +
                "                  \"name\": null\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"ATLAS_SERVER\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"auth_to_local_properties\": [\n" +
                "        \"application-properties/atlas.authentication.method.kerberos.name.rules|new_lines_escaped\"\n" +
                "      ],\n" +
                "      \"configurations\": [\n" +
                "        {\n" +
                "          \"ranger-atlas-audit\": {\n" +
                "            \"xasecure.audit.jaas.Client.loginModuleControlFlag\": \"required\",\n" +
                "            \"xasecure.audit.jaas.Client.option.serviceName\": \"solr\",\n" +
                "            \"xasecure.audit.jaas.Client.loginModuleName\": \"com.sun.security.auth.module.Krb5LoginModule\",\n" +
                "            \"xasecure.audit.jaas.Client.option.useKeyTab\": \"true\",\n" +
                "            \"xasecure.audit.jaas.Client.option.storeKey\": \"false\",\n" +
                "            \"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\": \"true\"\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"application-properties\": {\n" +
                "            \"atlas.kafka.security.protocol\": \"PLAINTEXTSASL\",\n" +
                "            \"atlas.jaas.KafkaClient.option.storeKey\": \"true\",\n" +
                "            \"atlas.solr.kerberos.enable\": \"true\",\n" +
                "            \"atlas.jaas.KafkaClient.loginModuleControlFlag\": \"required\",\n" +
                "            \"atlas.authentication.method.kerberos\": \"true\",\n" +
                "            \"atlas.jaas.KafkaClient.option.useKeyTab\": \"true\",\n" +
                "            \"atlas.kafka.sasl.kerberos.service.name\": \"${kafka-env/kafka_user}\",\n" +
                "            \"atlas.jaas.KafkaClient.loginModuleName\": \"com.sun.security.auth.module.Krb5LoginModule\",\n" +
                "            \"atlas.jaas.KafkaClient.option.serviceName\": \"${kafka-env/kafka_user}\"\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"ATLAS\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"components\": [\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.kerberos.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"nn/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"secondary_namenode_nn\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/nn.service.keytab\",\n" +
                "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.keytab.file\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.secondary.namenode.kerberos.internal.spnego.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": null\n" +
                "              },\n" +
                "              \"name\": \"/spnego\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"SECONDARY_NAMENODE\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"HDFS_CLIENT\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.datanode.kerberos.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"dn/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"datanode_dn\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/dn.service.keytab\",\n" +
                "                \"configuration\": \"hdfs-site/dfs.datanode.keytab.file\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"configurations\": [\n" +
                "            {\n" +
                "              \"hdfs-site\": {\n" +
                "                \"dfs.datanode.address\": \"0.0.0.0:1019\",\n" +
                "                \"dfs.datanode.http.address\": \"0.0.0.0:1022\"\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"DATANODE\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/nfs.kerberos.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"nfs/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"nfsgateway\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/nfs.service.keytab\",\n" +
                "                \"configuration\": \"hdfs-site/nfs.keytab.file\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"NFS_GATEWAY\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.journalnode.kerberos.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"jn/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"journalnode_jn\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/jn.service.keytab\",\n" +
                "                \"configuration\": \"hdfs-site/dfs.journalnode.keytab.file\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.journalnode.kerberos.internal.spnego.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": null\n" +
                "              },\n" +
                "              \"name\": \"/spnego\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"JOURNALNODE\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hadoop-env/hdfs_principal_name\",\n" +
                "                \"type\": \"user\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"${hadoop-env/hdfs_user}${principal_suffix}@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"hdfs\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/hdfs.headless.keytab\",\n" +
                "                \"configuration\": \"hadoop-env/hdfs_user_keytab\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.namenode.kerberos.principal\",\n" +
                "                \"type\": \"service\",\n" +
                "                \"local_username\": \"${hadoop-env/hdfs_user}\",\n" +
                "                \"value\": \"nn/_HOST@${realm}\"\n" +
                "              },\n" +
                "              \"name\": \"namenode_nn\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": \"r\",\n" +
                "                  \"name\": \"${hadoop-env/hdfs_user}\"\n" +
                "                },\n" +
                "                \"file\": \"${keytab_dir}/nn.service.keytab\",\n" +
                "                \"configuration\": \"hdfs-site/dfs.namenode.keytab.file\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": \"\",\n" +
                "                  \"name\": \"${cluster-env/user_group}\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"hdfs-site/dfs.namenode.kerberos.internal.spnego.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": null\n" +
                "              },\n" +
                "              \"name\": \"/spnego\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"principal\": {\n" +
                "                \"configuration\": \"ranger-hdfs-audit/xasecure.audit.jaas.Client.option.principal\",\n" +
                "                \"type\": null,\n" +
                "                \"local_username\": null,\n" +
                "                \"value\": null\n" +
                "              },\n" +
                "              \"name\": \"/HDFS/NAMENODE/namenode_nn\",\n" +
                "              \"keytab\": {\n" +
                "                \"owner\": {\n" +
                "                  \"access\": null,\n" +
                "                  \"name\": null\n" +
                "                },\n" +
                "                \"file\": null,\n" +
                "                \"configuration\": \"ranger-hdfs-audit/xasecure.audit.jaas.Client.option.keyTab\",\n" +
                "                \"group\": {\n" +
                "                  \"access\": null,\n" +
                "                  \"name\": null\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"configurations\": [\n" +
                "            {\n" +
                "              \"hdfs-site\": {\n" +
                "                \"dfs.block.access.token.enable\": \"true\"\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"NAMENODE\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"identities\": [\n" +
                "        {\n" +
                "          \"principal\": {\n" +
                "            \"configuration\": \"hdfs-site/dfs.web.authentication.kerberos.principal\",\n" +
                "            \"type\": null,\n" +
                "            \"local_username\": null,\n" +
                "            \"value\": null\n" +
                "          },\n" +
                "          \"name\": \"/spnego\",\n" +
                "          \"keytab\": {\n" +
                "            \"owner\": {\n" +
                "              \"access\": null,\n" +
                "              \"name\": null\n" +
                "            },\n" +
                "            \"file\": null,\n" +
                "            \"configuration\": \"hdfs-site/dfs.web.authentication.kerberos.keytab\",\n" +
                "            \"group\": {\n" +
                "              \"access\": null,\n" +
                "              \"name\": null\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"/smokeuser\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"auth_to_local_properties\": [\n" +
                "        \"core-site/hadoop.security.auth_to_local\"\n" +
                "      ],\n" +
                "      \"configurations\": [\n" +
                "        {\n" +
                "          \"ranger-hdfs-audit\": {\n" +
                "            \"xasecure.audit.jaas.Client.loginModuleControlFlag\": \"required\",\n" +
                "            \"xasecure.audit.jaas.Client.option.serviceName\": \"solr\",\n" +
                "            \"xasecure.audit.jaas.Client.loginModuleName\": \"com.sun.security.auth.module.Krb5LoginModule\",\n" +
                "            \"xasecure.audit.jaas.Client.option.useKeyTab\": \"true\",\n" +
                "            \"xasecure.audit.jaas.Client.option.storeKey\": \"false\",\n" +
                "            \"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\": \"true\"\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"core-site\": {\n" +
                "            \"hadoop.security.authorization\": \"true\",\n" +
                "            \"hadoop.security.authentication\": \"kerberos\",\n" +
                "            \"hadoop.proxyuser.HTTP.groups\": \"${hadoop-env/proxyuser_group}\"\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"HDFS\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"components\": [\n" +
                "        {\n" +
                "          \"name\": \"NEW_SERVICE_CLIENT\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"/HIVE/HIVE_SERVER/hive_server_hive\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"NEW_SERVICE_FOO_BAR\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"NEW_SERVICE_SERVER\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"identities\": [\n" +
                "        {\n" +
                "          \"name\": \"/smokeuser\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"NEW_SERVICE\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"components\": [\n" +
                "        {\n" +
                "          \"name\": \"EXISTING_SERVICE_CLIENT\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"EXISTING_SERVICE_SERVER\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"identities\": [\n" +
                "            {\n" +
                "              \"name\": \"/HDFS/NAMENODE/hdfs\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"name\": \"EXISTING_SERVICE_NEW_SERVER\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"identities\": [\n" +
                "        {\n" +
                "          \"name\": \"/smokeuser\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"/HIVE/HIVE_SERVER/hive_server_hive\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"EXISTING_SERVICE\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n").toMap()),
        GSON.toJson(newValue.toMap()));
  }
}
