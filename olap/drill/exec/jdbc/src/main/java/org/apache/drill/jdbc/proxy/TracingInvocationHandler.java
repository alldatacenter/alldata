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
import java.lang.reflect.Method;

/**
 * Tracing proxy-invocation handler.
 * Reports invocations of methods of given proxied object to given invocation
 * reporter.
 */
class TracingInvocationHandler<INTF> implements InvocationHandler
{
  private final ProxiesManager proxiesManager;
  private final InvocationReporter callReporter;
  private final INTF proxiedObject;
  private final Class<?> proxiedInterface;


  /**
   * Constructs invocation handler for given object, treats ~as given (single)
   * interface.
   *
   * @param  proxiesManager
   *         the proxies manager to use for creating new proxy objects
   * @param  reporter
   *         the invocation report to use to report invocation events
   * @param  proxiedObject
   *         ...
   * @param  proxiedInterface
   *         ...
   */
  TracingInvocationHandler( final ProxiesManager proxiesManager,
                            final InvocationReporter reporter,
                            final INTF proxiedObject,
                            final Class<?> proxiedInterface ) {
    this.proxiesManager = proxiesManager;
    this.callReporter = reporter;
    this.proxiedObject = proxiedObject;
    this.proxiedInterface = proxiedInterface;
  }


  @Override
  public Object invoke( Object proxy, Method method, Object[] args )
      throws Throwable {

    // Report that method was called:
    callReporter.methodCalled( proxiedObject, proxiedInterface, method, args );

    final Object rawReturnedResult;
    final Object netReturnedResult;
    try {
      // Invoke proxied original object's method:
      rawReturnedResult = method.invoke( proxiedObject, args );

      if ( null == rawReturnedResult ) {
        netReturnedResult = null;
      }
      else {
        Class<?> methodReturnType = method.getReturnType();

        if ( ! methodReturnType.isInterface() ) {
          // Declared type is not an interface type, so don't proxy the returned
          // instance.  (We could proxy and intercept some methods, but we can't
          // intercept all, so intercepting only some would be misleading.)
          netReturnedResult = rawReturnedResult;
        }
        else {
          // Get the new or existing proxying instance for the returned instance.
          netReturnedResult =
              proxiesManager.getProxyInstanceForOriginal( rawReturnedResult,
                                                          methodReturnType );
        }
      }
    }
    catch ( IllegalAccessException | IllegalArgumentException e ) {
      throw new RuntimeException(
          "Unexpected/unhandled error calling proxied method: " + e, e );
    }
    catch ( InvocationTargetException e ) {
      Throwable thrownResult = e.getCause();
      // Report that method threw exception:
      callReporter.methodThrew( proxiedObject, proxiedInterface, method, args,
                                thrownResult );
      throw thrownResult;
    }

    // Report that method returned:
    callReporter.methodReturned( proxiedObject, proxiedInterface, method, args,
                                 rawReturnedResult );
    return netReturnedResult;
  } // invoke(...)

} // class ProxyInvocationHandler<INTF>
