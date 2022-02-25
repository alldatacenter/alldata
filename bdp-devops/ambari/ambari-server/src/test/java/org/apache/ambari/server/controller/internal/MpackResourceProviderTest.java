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
package org.apache.ambari.server.controller.internal;


import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.MpackRequest;
import org.apache.ambari.server.controller.MpackResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.state.Module;
import org.apache.ambari.server.state.Mpack;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class MpackResourceProviderTest {

  private MpackDAO m_dao;
  private Injector m_injector;
  private AmbariManagementController m_amc;

  @Before
  public void before() throws Exception {
    m_dao = EasyMock.createNiceMock(MpackDAO.class);
    m_amc = EasyMock.createNiceMock(AmbariManagementController.class);

    m_injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule()).with(
            new MockModule()));
  }

  @Test
  public void testGetResourcesMpacks() throws Exception {
    Resource.Type type = Resource.Type.Mpack;

    Resource resourceExpected1 = new ResourceImpl(Resource.Type.Mpack);
    resourceExpected1.setProperty(MpackResourceProvider.MPACK_RESOURCE_ID, (long)1);
    resourceExpected1.setProperty(MpackResourceProvider.MPACK_NAME, "TestMpack1");
    resourceExpected1.setProperty(MpackResourceProvider.MPACK_VERSION, "3.0");
    resourceExpected1.setProperty(MpackResourceProvider.MPACK_URI, "abcd.tar.gz");
    resourceExpected1.setProperty(MpackResourceProvider.REGISTRY_ID, null);

    Resource resourceExpected2 = new ResourceImpl(Resource.Type.Mpack);
    resourceExpected2.setProperty(MpackResourceProvider.MPACK_RESOURCE_ID, (long)2);
    resourceExpected2.setProperty(MpackResourceProvider.MPACK_NAME, "TestMpack2");
    resourceExpected2.setProperty(MpackResourceProvider.MPACK_VERSION, "3.0");
    resourceExpected2.setProperty(MpackResourceProvider.MPACK_URI, "abc.tar.gz");
    resourceExpected2.setProperty(MpackResourceProvider.REGISTRY_ID, (long)1);

    Set<MpackResponse> entities = new HashSet<>();
    Mpack entity = new Mpack();
    entity.setResourceId(new Long(1));
    entity.setMpackId("1");
    entity.setMpackUri("abcd.tar.gz");
    entity.setName("TestMpack1");
    entity.setVersion("3.0");
    MpackResponse mr1 = new MpackResponse(entity);
    entities.add(mr1);

    entity = new Mpack();
    entity.setResourceId(new Long(2));
    entity.setMpackId("2");
    entity.setMpackUri("abc.tar.gz");
    entity.setName("TestMpack2");
    entity.setVersion("3.0");
    entity.setRegistryId(new Long(1));
    MpackResponse mr2 = new MpackResponse(entity);
    entities.add(mr2);



    // set expectations
    EasyMock.expect(m_amc.getMpacks()).andReturn(entities).anyTimes();

    // replay
    replay(m_amc);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
            type, m_amc
    );

    // create the request
    Request request = PropertyHelper.getReadRequest();

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(2, resources.size());

    for (Resource resource : resources) {
      Long mpackId = (Long) resource.getPropertyValue(MpackResourceProvider.MPACK_RESOURCE_ID);
      if (mpackId == (long) 1) {
        Assert.assertEquals(resourceExpected1.getPropertyValue(MpackResourceProvider.MPACK_NAME), (String) resource.getPropertyValue(MpackResourceProvider.MPACK_NAME));
        Assert.assertEquals(resourceExpected1.getPropertyValue(MpackResourceProvider.MPACK_VERSION), (String) resource.getPropertyValue(MpackResourceProvider.MPACK_VERSION));
        Assert.assertEquals(resourceExpected1.getPropertyValue(MpackResourceProvider.MPACK_URI), (String) resource.getPropertyValue(MpackResourceProvider.MPACK_URI));
        Assert.assertEquals(resourceExpected1.getPropertyValue(MpackResourceProvider.REGISTRY_ID), (Long) resource.getPropertyValue(MpackResourceProvider.REGISTRY_ID));
      } else if (mpackId == (long) 2) {
        Assert.assertEquals(resourceExpected2.getPropertyValue(MpackResourceProvider.MPACK_NAME), (String) resource.getPropertyValue(MpackResourceProvider.MPACK_NAME));
        Assert.assertEquals(resourceExpected2.getPropertyValue(MpackResourceProvider.MPACK_VERSION), (String) resource.getPropertyValue(MpackResourceProvider.MPACK_VERSION));
        Assert.assertEquals(resourceExpected2.getPropertyValue(MpackResourceProvider.MPACK_URI), (String) resource.getPropertyValue(MpackResourceProvider.MPACK_URI));
        Assert.assertEquals(resourceExpected2.getPropertyValue(MpackResourceProvider.REGISTRY_ID), (Long) resource.getPropertyValue(MpackResourceProvider.REGISTRY_ID));
      } else {
        Assert.assertTrue(false);
      }
    }

    // verify
    verify(m_amc);
  }

  @Test
  public void testGetResourcesMpackId() throws Exception {
    Resource.Type type = Resource.Type.Mpack;

    Predicate predicate = new PredicateBuilder().property(
            MpackResourceProvider.MPACK_RESOURCE_ID).equals(
            Long.valueOf(1).toString()).toPredicate();

    MpackEntity entity = new MpackEntity();
    entity.setId((long) 1);
    entity.setMpackUri("abcd.tar.gz");
    entity.setMpackName("TestMpack1");
    entity.setMpackVersion("3.0");

    Mpack mpack = new Mpack();
    mpack.setResourceId((long) 1);
    mpack.setMpackId("1");
    mpack.setMpackUri("abcd.tar.gz");
    mpack.setName("TestMpack1");
    mpack.setVersion("3.0");

    MpackResponse mpackResponse = new MpackResponse(mpack);

    ArrayList<Module> packletArrayList = new ArrayList<>();
    org.apache.ambari.server.state.Module module = new Module();
    module.setName("testService");
    //module.setType(Module.PackletType.SERVICE_PACKLET);
    module.setDefinition("testDir");
    module.setVersion("3.0");
    packletArrayList.add(module);

    Resource resourceExpected1 = new ResourceImpl(Resource.Type.Mpack);
    resourceExpected1.setProperty(MpackResourceProvider.MPACK_RESOURCE_ID, (long)1);
    resourceExpected1.setProperty(MpackResourceProvider.MPACK_NAME, "TestMpack1");
    resourceExpected1.setProperty(MpackResourceProvider.MPACK_VERSION, "3.0");
    resourceExpected1.setProperty(MpackResourceProvider.MPACK_URI, "abcd.tar.gz");
    resourceExpected1.setProperty(MpackResourceProvider.REGISTRY_ID, null);
    resourceExpected1.setProperty(MpackResourceProvider.MODULES,packletArrayList);

    // set expectations
    EasyMock.expect(m_dao.findById((long)1)).andReturn(entity).anyTimes();
    EasyMock.expect(m_amc.getModules((long)1)).andReturn(packletArrayList).anyTimes();
    // set expectations
    EasyMock.expect(m_amc.getMpack((long)1)).andReturn(mpackResponse).anyTimes();

    // replay
    replay(m_dao,m_amc);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Mpack, m_amc);

    // create the request
    Request request = PropertyHelper.getReadRequest();

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request,predicate);

    Assert.assertEquals(1, resources.size());
    for(Resource resource: resources){
      Assert.assertEquals(resourceExpected1.getPropertyValue(MpackResourceProvider.MPACK_NAME), (String) resource.getPropertyValue(MpackResourceProvider.MPACK_NAME));
      Assert.assertEquals(resourceExpected1.getPropertyValue(MpackResourceProvider.MPACK_VERSION), (String) resource.getPropertyValue(MpackResourceProvider.MPACK_VERSION));
      Assert.assertEquals(resourceExpected1.getPropertyValue(MpackResourceProvider.MPACK_URI), (String) resource.getPropertyValue(MpackResourceProvider.MPACK_URI));
      Assert.assertEquals(resourceExpected1.getPropertyValue(MpackResourceProvider.REGISTRY_ID), (Long) resource.getPropertyValue(MpackResourceProvider.REGISTRY_ID));
      Assert.assertEquals(resourceExpected1.getPropertyValue(MpackResourceProvider.MODULES),(ArrayList)resource.getPropertyValue(MpackResourceProvider.MODULES));
  }
    // verify
    verify(m_dao,m_amc);

  }

  @Test
  public void testCreateResources() throws Exception {
    MpackRequest mpackRequest = new MpackRequest();
    String mpackUri = Paths.get("src/test/resources/mpacks-v2/abc.tar.gz").toUri().toURL().toString();
    mpackRequest.setMpackUri(mpackUri);
    Request request = createMock(Request.class);
    MpackResponse response = new MpackResponse(setupMpack());
    Set<Map<String, Object>> properties = new HashSet<>();
    Map propertyMap = new HashMap();
    propertyMap.put(MpackResourceProvider.MPACK_URI,mpackUri);
    properties.add(propertyMap);

    // set expectations
    EasyMock.expect(m_amc.registerMpack(mpackRequest)).andReturn(response).anyTimes();
    EasyMock.expect(request.getProperties()).andReturn(properties).anyTimes();
    replay(m_amc,request);
    // end expectations

    MpackResourceProvider provider = (MpackResourceProvider) AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Mpack, m_amc);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    RequestStatusImpl requestStatus = (RequestStatusImpl) provider.createResources(request);
    Set<Resource> associatedResources = requestStatus.getAssociatedResources();

    Assert.assertEquals(1,associatedResources.size());
    for(Resource r : associatedResources){
      Assert.assertEquals((long)100,r.getPropertyValue(MpackResourceProvider.MPACK_RESOURCE_ID));
      Assert.assertEquals("testMpack",r.getPropertyValue(MpackResourceProvider.MPACK_NAME));
      Assert.assertEquals("3.0",r.getPropertyValue(MpackResourceProvider.MPACK_VERSION));
      Assert.assertEquals("../../../../../../../resources/mpacks-v2/abc.tar.gz",r.getPropertyValue(MpackResourceProvider.MPACK_URI));
    }
    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Mpack, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    Assert.assertEquals(request, lastEvent.getRequest());
    Assert.assertNull(lastEvent.getPredicate());

    verify(m_amc,request);
  }


  public Mpack setupMpack() {
    Mpack mpack = new Mpack();
    mpack.setResourceId((long)100);
    mpack.setModules(new ArrayList<Module>());
    mpack.setPrerequisites(new HashMap<String, String>());
    mpack.setRegistryId(new Long(100));
    mpack.setVersion("3.0");
    mpack.setMpackUri("../../../../../../../resources/mpacks-v2/abc.tar.gz");
    mpack.setDescription("Test mpack");
    mpack.setName("testMpack");

    return mpack;
  }

  /**
   *
   */
  private class MockModule implements com.google.inject.Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(EntityManager.class).toInstance(EasyMock.createMock(EntityManager.class));
      binder.bind(MpackDAO.class).toInstance(m_dao);
      binder.bind(AmbariManagementController.class).toInstance(m_amc);
    }
  }
}