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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.jdbc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Map;


/**
 * Manager of proxy classes and instances.
 * Managing includes creating proxies and tracking to re-use proxies.
 */
class ProxiesManager {
  private final InvocationReporter reporter;

  private Map<Class<?>, Class<Proxy>> interfacesToProxyClassesMap =
      new IdentityHashMap<>();

  /** Map of proxied original objects from proxied JDBC driver to
   *  proxy objects to be returned by tracing proxy driver. */
  private Map<Object, Object> proxiedsToProxiesMap = new IdentityHashMap<>();


  public ProxiesManager( final InvocationReporter reporter ) {
    this.reporter = reporter;
  }

  private Class<Proxy> getProxyClassForInterface( final Class<?> interfaceType ) {
    assert interfaceType.isInterface();

    Class<Proxy> proxyReturnClass = interfacesToProxyClassesMap.get( interfaceType );
    if ( null == proxyReturnClass ) {

      // Suppressed because we know getProxyClass returns class extending Proxy.
      @SuppressWarnings("unchecked")
      Class<Proxy> newProxyReturnClass =
          (Class<Proxy>) Proxy.getProxyClass( interfaceType.getClassLoader(),
                                              new Class[] { interfaceType });
      interfacesToProxyClassesMap.put( interfaceType, newProxyReturnClass );
      proxyReturnClass = newProxyReturnClass;
    }
    return proxyReturnClass;
  }


  /**
   * Creates or retrieves proxy instance to be returned for given original
   * instance.
   * @param  originalInstance
   *         the original object
   * @param  declaredType
   *         the declared type of source of the original object; interface type
   */
  public <INTF> INTF getProxyInstanceForOriginal( final INTF originalInstance,
                                                  final Class<?> declaredType ) {
    final INTF proxyInstance;

    assert declaredType.isAssignableFrom( originalInstance.getClass() )
        : "toBeProxied is of class (" + originalInstance.getClass().getName()
        + ") that doesn't implement specified interface " + declaredType.getName();
    // Suppressed because checked immediately above.
    @SuppressWarnings("unchecked")
    final INTF existingProxy = (INTF) proxiedsToProxiesMap.get( originalInstance );

    if ( null != existingProxy ) {
      // Repeated occurrence of original--return same proxy instance as before.
      proxyInstance = existingProxy;
    }
    else {
      // Original we haven't seen yet--create proxy instance and return that.

      Class<Proxy> proxyReturnClass = getProxyClassForInterface( declaredType );

      // Create tracing handler instance for this proxy/original pair.
      final InvocationHandler callHandler =
          new TracingInvocationHandler<INTF>( this, reporter,
                                              originalInstance, declaredType );
      try {
        // Suppressed because we know that proxy class implements INTF.
        @SuppressWarnings("unchecked")
        INTF newProxyInstance =
            (INTF)
            proxyReturnClass
            .getConstructor( new Class[] { InvocationHandler.class } )
            .newInstance( new Object[] { callHandler } );
        proxiedsToProxiesMap.put( originalInstance, newProxyInstance );
        proxyInstance = newProxyInstance;
      }
      catch ( InstantiationException | IllegalAccessException
              | IllegalArgumentException | InvocationTargetException
              | NoSuchMethodException | SecurityException e ) {
        throw new RuntimeException("Error creating proxy for " + declaredType + ": " + e, e);
      }
    }
    return proxyInstance;
  }

} // class ProxiesManager
