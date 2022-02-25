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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.Assert;

abstract public class KDCKerberosOperationHandlerTest extends KerberosOperationHandlerTest {

  static Method methodExecuteCommand;

  static Method methodGetExecutable;

  @BeforeClass
  public static void beforeKDCKerberosOperationHandlerTest() throws Exception {
    methodExecuteCommand = KDCKerberosOperationHandler.class.getDeclaredMethod("executeCommand", String[].class, Map.class, ShellCommandUtil.InteractiveHandler.class);
    methodGetExecutable = KerberosOperationHandler.class.getDeclaredMethod("getExecutable", String.class);
  }

  @Test
  public void testInteractivePasswordHandler() {
    KDCKerberosOperationHandler.InteractivePasswordHandler handler = new KDCKerberosOperationHandler.InteractivePasswordHandler("admin_password", "user_password");

    handler.start();
    Assert.assertEquals("admin_password", handler.getResponse("password"));
    Assert.assertFalse(handler.done());
    Assert.assertEquals("user_password", handler.getResponse("password"));
    Assert.assertFalse(handler.done());
    Assert.assertEquals("user_password", handler.getResponse("password"));
    Assert.assertTrue(handler.done());

    // Test restarting
    handler.start();
    Assert.assertEquals("admin_password", handler.getResponse("password"));
    Assert.assertFalse(handler.done());
    Assert.assertEquals("user_password", handler.getResponse("password"));
    Assert.assertFalse(handler.done());
    Assert.assertEquals("user_password", handler.getResponse("password"));
    Assert.assertTrue(handler.done());
  }

  @Override
  protected KerberosOperationHandler createMockedHandler() throws KerberosOperationException {
    KDCKerberosOperationHandler handler = createMockedHandler(methodExecuteCommand, methodGetExecutable);

    expect(handler.getExecutable(anyString()))
        .andAnswer(new IAnswer<String>() {
          @Override
          public String answer() throws Throwable {
            Object[] args = getCurrentArguments();
            return args[0].toString();
          }
        }).anyTimes();
    return handler;
  }

  @Override
  protected void setupOpenSuccess(KerberosOperationHandler handler) throws Exception {
    ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);
    expect(result.isSuccessful()).andReturn(true);

    expect(handler.executeCommand(arrayContains("kinit"), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(result)
        .anyTimes();
  }

  @Override
  protected void setupOpenFailure(KerberosOperationHandler handler) throws Exception {
    ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);
    expect(result.isSuccessful()).andReturn(false).once();
    expect(result.getExitCode()).andReturn(-1).once();
    expect(result.getStdout()).andReturn("STDOUT data").once();
    expect(result.getStderr()).andReturn("STDERR data").once();

    expect(handler.executeCommand(arrayContains("kinit"), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(result)
        .anyTimes();
  }

  protected abstract KDCKerberosOperationHandler createMockedHandler(Method... mockedMethods);

  public static class ArrayContains implements IArgumentMatcher {

    private String[] startItems;

    ArrayContains(String startItem) {
      this.startItems = new String[]{startItem};
    }

    ArrayContains(String[] startItems) {
      this.startItems = startItems;
    }

    @Override
    public boolean matches(Object o) {
      if (o instanceof String[]) {
        String[] array = (String[]) o;

        for (String item : startItems) {
          boolean valueContains = false;
          for (String value : array) {
            if (value.contains(item)) {
              valueContains = true;
              break;
            }
          }

          if (!valueContains) {
            return false;
          }
        }

        return true;
      }

      return false;
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("arrayContains(");
      stringBuffer.append(StringUtils.join(startItems, ", "));
      stringBuffer.append("\")");
    }
  }

  static String[] arrayContains(String in) {
    EasyMock.reportMatcher(new ArrayContains(in));
    return null;
  }

  static String[] arrayContains(String[] in) {
    EasyMock.reportMatcher(new ArrayContains(in));
    return null;
  }

}
