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
package com.qlangtech.tis.runtime.module.misc;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.runtime.module.misc.impl.DefaultFieldErrorHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-12-24
 */
public class DefaultMessageHandler extends DefaultFieldErrorHandler implements IMessageHandler {

  // private static final long serialVersionUID = 1L;
  @SuppressWarnings("unchecked")
  public void addActionMessage(final Context context, String msg) {
    List<String> msgList = (List<String>) context.get(ACTION_MSG);
    if (msgList == null) {
      msgList = new ArrayList<String>();
    }
    msgList.add(msg);
    context.put(ACTION_MSG, msgList);
  }

  @Override
  public void setBizResult(Context context, Object value, boolean overwriteable) {
    value = overwriteable ? value : new CanNotOverwriteableWrapper(value);
    Object previous = null;

    if ((previous = context.get(ACTION_BIZ_RESULT)) != null) {
      if ((previous instanceof CanNotOverwriteableWrapper)) {
        //context.put(ACTION_BIZ_RESULT, value);
        return;
      }
    }

    context.put(ACTION_BIZ_RESULT, value);
//    if ((previous = context.put(ACTION_BIZ_RESULT, value)) != null) {
//      if (previous instanceof CanNotOverwriteableWrapper) {
//        throw new IllegalStateException("setBizResult relevant val can not be overwrite in apply context");
//      }
//    }
  }

  public static Object getBizResult(Context context) {
    Object o = context.get(ACTION_BIZ_RESULT);

    if (o == null) {
      return null;
    }

    if (o instanceof CanNotOverwriteableWrapper) {
      return ((CanNotOverwriteableWrapper) o).target;
    } else {
      return o;
    }
  }


  /**
   * setBizResult(Context context, Object value, boolean overwriteable) :overwriteable 为false之时，context中设置该实例
   */
  private static class CanNotOverwriteableWrapper {
    final Object target;

    public CanNotOverwriteableWrapper(Object target) {
      this.target = target;
    }
  }


//    @SuppressWarnings("unchecked")
//    public boolean hasErrors(Context context) {
//
//        return context.hasErrors();
//    }

  @Override
  public void errorsPageShow(Context context) {
    context.put(ACTION_ERROR_PAGE_SHOW, true);
  }

  /**
   * 添加错误信息
   *
   * @param context
   * @param msg
   */
  @SuppressWarnings("all")
  public void addErrorMessage(final Context context, String msg) {
    List<String> msgList = (List<String>) context.get(ACTION_ERROR_MSG);
    if (msgList == null) {
      msgList = new ArrayList<String>();
      context.put(ACTION_ERROR_MSG, msgList);
    }
    msgList.add(msg);
    context.put(ACTION_MSG, new ArrayList<String>());
  }
}
