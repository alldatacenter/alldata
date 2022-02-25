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

package org.apache.ambari.server.serveraction.kerberos;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class KerberosOperationHandlerFactoryTest {


  private static Injector injector;

  @BeforeClass
  public static void beforeClass() throws AmbariException {
    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Configuration configuration = EasyMock.createNiceMock(Configuration.class);
        expect(configuration.getServerOsFamily()).andReturn("redhat6").anyTimes();
        replay(configuration);

        bind(Configuration.class).toInstance(configuration);
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });
  }

  @Test
  public void testForAD() {
    Assert.assertEquals(MITKerberosOperationHandler.class,
      injector.getInstance(KerberosOperationHandlerFactory.class).getKerberosOperationHandler(KDCType.MIT_KDC).getClass());
  }

  @Test
  public void testForMIT() {
    Assert.assertEquals(ADKerberosOperationHandler.class,
        injector.getInstance(KerberosOperationHandlerFactory.class).getKerberosOperationHandler(KDCType.ACTIVE_DIRECTORY).getClass());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testForNone() {
    Assert.assertNull(injector.getInstance(KerberosOperationHandlerFactory.class).getKerberosOperationHandler(KDCType.NONE));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testForNull() {
    Assert.assertNull(injector.getInstance(KerberosOperationHandlerFactory.class).getKerberosOperationHandler(null));
  }
}
