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

package org.apache.ambari.server.view;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.net.URL;

import org.apache.ambari.server.view.configuration.ViewConfig;
import org.junit.Test;

import junit.framework.Assert;

/**
 * ViewClassLoader test.
 */
public class ViewClassLoaderTest {

  @Test
  public void testGetResource() throws Exception {
    ClassLoader parentClassLoader = createMock(ClassLoader.class);
    URL parentResource = new File("parent-resource").toURI().toURL();
    ViewConfig viewConfig = createNiceMock(ViewConfig.class);

    expect(parentClassLoader.getResource("parent-resource")).andReturn(parentResource).once();

    replay(parentClassLoader, viewConfig);

    File file = new File("./src/test/resources");
    URL testURL = file.toURI().toURL();

    URL[] urls = new URL[]{testURL};

    ViewClassLoader classLoader = new ViewClassLoader(viewConfig, parentClassLoader, urls);

    URL url = classLoader.getResource("ambari.properties");

    Assert.assertNotNull(url);

    url = classLoader.getResource("parent-resource");

    Assert.assertNotNull(url);
    Assert.assertSame(parentResource, url);

    verify(parentClassLoader, viewConfig);
  }

  @Test
  public void testLoadClass() throws Exception {
    TestClassLoader parentClassLoader = createMock(TestClassLoader.class);
    Class parentClass = Object.class;
    ViewConfig viewConfig = createNiceMock(ViewConfig.class);

    expect(parentClassLoader.getPackage("org.apache.ambari.server.view")).andReturn(null).anyTimes();
    expect(parentClassLoader.loadClass("java.lang.Object")).andReturn(parentClass).anyTimes();
    expect(parentClassLoader.loadClass("ParentClass")).andReturn(parentClass).once();
    expect(parentClassLoader.loadClass("org.apache.ambari.server.controller.spi.ResourceProvider")).andReturn(parentClass).once();
    expect(parentClassLoader.loadClass("org.apache.ambari.view.ViewContext")).andReturn(parentClass).once();
    expect(parentClassLoader.loadClass("javax.xml.parsers.SAXParserFactory")).andReturn(parentClass).once();
    expect(parentClassLoader.loadClass("com.google.inject.AbstractModule")).andReturn(parentClass).once();
    expect(parentClassLoader.loadClass("org.slf4j.LoggerFactory")).andReturn(parentClass).once();
    expect(parentClassLoader.loadClass("com.sun.jersey.api.ConflictException")).andReturn(parentClass).once();
    expect(parentClassLoader.loadClass("org.apache.velocity.VelocityContext")).andReturn(parentClass).once();

    replay(parentClassLoader, viewConfig);

    File file = new File("./target/test-classes");
    URL testURL = file.toURI().toURL();

    URL[] urls = new URL[]{testURL};

    ViewClassLoader classLoader = new ViewClassLoader(viewConfig, parentClassLoader, urls);

    // should be loaded by parent loader
    Class clazz = classLoader.loadClass("ParentClass");

    Assert.assertNotNull(clazz);
    Assert.assertSame(parentClass, clazz);

    clazz = classLoader.loadClass("org.apache.ambari.server.controller.spi.ResourceProvider");

    Assert.assertNotNull(clazz);
    Assert.assertSame(parentClass, clazz);

    clazz = classLoader.loadClass("org.apache.ambari.view.ViewContext");

    Assert.assertNotNull(clazz);
    Assert.assertSame(parentClass, clazz);

    clazz = classLoader.loadClass("javax.xml.parsers.SAXParserFactory");

    Assert.assertNotNull(clazz);
    Assert.assertSame(parentClass, clazz);

    clazz = classLoader.loadClass("com.google.inject.AbstractModule");

    Assert.assertNotNull(clazz);
    Assert.assertSame(parentClass, clazz);

    clazz = classLoader.loadClass("org.slf4j.LoggerFactory");

    Assert.assertNotNull(clazz);
    Assert.assertSame(parentClass, clazz);

    clazz = classLoader.loadClass("com.sun.jersey.api.ConflictException");

    Assert.assertNotNull(clazz);
    Assert.assertSame(parentClass, clazz);

    clazz = classLoader.loadClass("org.apache.velocity.VelocityContext");

    Assert.assertNotNull(clazz);
    Assert.assertSame(parentClass, clazz);

    verify(parentClassLoader, viewConfig);
  }

  public class TestClassLoader extends ClassLoader {
    @Override
    public Package getPackage(String s) {
      return super.getPackage(s);
    }
  }
}
