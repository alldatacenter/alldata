/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.common.valve;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.opensymphony.xwork2.ActionInvocation;
import com.qlangtech.tis.extension.impl.SuFormProperties;
import com.qlangtech.tis.extension.model.UpdateSite;
import com.qlangtech.tis.extension.util.GroovyShellEvaluate;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.manage.common.IAjaxResult;
import com.qlangtech.tis.manage.common.MockContext;
import com.qlangtech.tis.runtime.module.misc.DefaultMessageHandler;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
import com.qlangtech.tis.runtime.module.misc.impl.DefaultFieldErrorHandler;
import com.qlangtech.tis.trigger.util.JsonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.result.StrutsResultSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-7-13
 */
public class AjaxValve extends StrutsResultSupport implements IAjaxResult {

  private static final long serialVersionUID = -3835145419233595896L;

  public static final String BIZ_RESULT = "biz_result";

  public static final String QUERY_RESULT = "query_result";

  public static final String EXEC_NULL = "exec_null";

  /**
   * 不需要在客户端显示成功信息
   */
  // public static final String KEY_NOT_SHOW_BIZ_MSG = "not_show_biz_msg";
  @Override
  protected void doExecute(String finalLocation, ActionInvocation invocation) throws Exception {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();
    // final String resultHandler = request.getParameter("resulthandler");
    final String resultHandler = request.getParameter("resulthandler");
    if (EXEC_NULL.equals(resultHandler)) {
      return;
    }

    // Thread.sleep(5000);
    try {
      this.writeExecuteResult(request, response);
    } finally {
      SuFormProperties.subFormGetterProcessThreadLocal.remove();
      GroovyShellEvaluate.pluginThreadLocal.remove();
      UpdateSite.pluginArts.remove();
    }
  }

  @SuppressWarnings("unchecked")
  private void writeExecuteResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ActionExecResult actionExecResult = MockContext.getActionExecResult();
    List<Object> errorMsgList = actionExecResult.errorMsgList;

    List<String> msgList = //Boolean.parseBoolean(request.getParameter(KEY_NOT_SHOW_BIZ_MSG))
      //? Collections.emptyList() :
      actionExecResult.getMsgList();

    List<List<List<DefaultFieldErrorHandler.FieldError>>> pluginErrorList = actionExecResult.getPluginErrorList();
    Object bizResult = actionExecResult.getBizResult();
    Boolean errorPageShow = actionExecResult.getErrorPageShow();
    writeInfo2Client(actionExecResult, response, errorPageShow, errorMsgList, msgList, pluginErrorList, bizResult);
  }

  private static List<Object> getErrorMsgList(Context context) {
    return (List<Object>) context.get(IMessageHandler.ACTION_ERROR_MSG);
  }

  /**
   * @param response
   * @param errorMsgList
   * @param msgList
   * @param extendVal    业务系统出了 errors 和msgs之外还要传其他的值
   * @throws IOException
   */
  public static void writeInfo2Client(IExecResult actionExecResult, HttpServletResponse response, Boolean errorPageShow
    , List<Object> errorMsgList, List<String> msgList
    , List<List<List<DefaultFieldErrorHandler.FieldError>>> pluginErrorList, Object extendVal) throws IOException {
    //try {
    StringBuffer result = buildResultStruct(actionExecResult, errorPageShow, errorMsgList, msgList, pluginErrorList, extendVal);
    writeJson(response, result);
//    } catch (JSONException e) {
//      throw new IOException(e);
//    }
  }

  public static StringBuffer buildResultStruct(Context context) {
    ActionExecResult r = new ActionExecResult(context).invoke();
    return buildResultStruct(r, r.errorPageShow, r.errorMsgList, r.msgList, r.pluginErrorList, r.getBizResult());
  }

  private static StringBuffer buildResultStruct(IExecResult actionExecResult, Boolean errorPageShow, List<Object> errorMsgList
    , List<String> msgList, List<List<List<DefaultFieldErrorHandler.FieldError>>> pluginErrorList, Object extendVal) {
    StringBuffer result = new StringBuffer();
    result.append("{\n");
    result.append(" \"").append(KEY_SUCCESS).append("\":").append(actionExecResult.isSuccess());
    JSONArray errors = new JSONArray();
    if (errorMsgList != null) {
      for (Object msg : errorMsgList) {
        if (msg instanceof String) {
          errors.add(msg);
        } else if (msg instanceof TisException.ErrMsg) {
          // errors.put();
          errors.add(msg);
        } else {
          throw new IllegalStateException("illegal error type:" + msg.getClass().getName());
        }

      }
    }
    result.append(",\n \"").append(KEY_ERROR_MSG).append("\":").append(JsonUtil.toString(errors));
    if (errorPageShow != null) {
      result.append(",\n \"").append(IMessageHandler.ACTION_ERROR_PAGE_SHOW).append("\":").append(errorPageShow);
    }
    JSONArray msgs = new JSONArray();
    if (msgList != null) {
      for (String msg : msgList) {
        msgs.add(msg);
      }
    }
    result.append(",\n \"").append(KEY_MSG).append("\":").append(JsonUtil.toString(msgs));
    if (extendVal != null) {
      result.append(",\n \"").append(KEY_BIZRESULT).append("\":");
      if (extendVal instanceof org.json.JSONObject) {
        result.append(((org.json.JSONObject) extendVal).toString(1));
      } else if (extendVal instanceof JSONArray) {
        result.append(JsonUtil.toString((JSONArray) extendVal));
      } else {
        //com.alibaba.fastjson.JSON.toJSONString(extendVal, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat)
        result.append(JsonUtil.toString(extendVal));
      }
    }
    if (pluginErrorList != null) {
      JSONArray pluginErrs = new JSONArray();
      for (List<List<DefaultFieldErrorHandler.FieldError>> /**
       * item
       */
        onePluginOfItems : pluginErrorList) {
        JSONArray itemErrs = convertItemsErrorList(onePluginOfItems);
        pluginErrs.add(itemErrs);
      }
      result.append(",\n \"").append(IAjaxResult.KEY_ERROR_FIELDS).append("\":");
      result.append(JsonUtil.toString(pluginErrs));
    }
    result.append("\n}");
    return result;
  }

  private static JSONArray convertItemsErrorList(List<List<DefaultFieldErrorHandler.FieldError>> itemsErrorList) {
    JSONArray itemErrs = new JSONArray();
    for (List<DefaultMessageHandler.FieldError> fieldErrors : itemsErrorList) {
      JSONArray ferrs = new JSONArray();
      JSONObject o = null;
      for (DefaultMessageHandler.FieldError ferr : fieldErrors) {
        o = new JSONObject();
        o.put("name", ferr.getFieldName());
        if (StringUtils.isNotEmpty(ferr.getMsg())) {
          o.put("content", ferr.getMsg());
        }
        if (ferr.itemsErrorList != null) {
          o.put(IAjaxResult.KEY_ERROR_FIELDS, convertItemsErrorList(ferr.itemsErrorList));
        }
        ferrs.add(o);
      }
      itemErrs.add(ferrs);
    }
    return itemErrs;
  }

  private static void writeJson(HttpServletResponse response, StringBuffer execResult) throws IOException {
    // try {
    // Thread.sleep(1000);
    // } catch (InterruptedException e) {
    //
    // }
    response.setContentType("text/json;charset=UTF-8");
    response.getWriter().write(execResult.toString());
  }

  public interface IExecResult {

    boolean isSuccess();
  }

  public static class ActionExecResult implements IExecResult {

    private Context context;

    private List<Object> errorMsgList;

    public void addErrorMsg(List<String> msgs) {
      this.errorMsgList.addAll(msgs);
    }

    private List<String> msgList;

    private List<List<List<DefaultFieldErrorHandler.FieldError>>> pluginErrorList;

    private Object bizResult;

    private Boolean errorPageShow;

    public boolean isSuccess() {
      return (errorMsgList == null || errorMsgList.isEmpty()) && (pluginErrorList == null || pluginErrorList.isEmpty());
    }

    public ActionExecResult(Context context) {
      this.context = context;
    }

    // public List<String> getErrorMsgList() {
    // return errorMsgList;
    // }
    public List<String> getMsgList() {
      return msgList;
    }

    public List<List<List<DefaultFieldErrorHandler.FieldError>>> getPluginErrorList() {
      return pluginErrorList;
    }

    public Object getBizResult() {
      return bizResult;
    }

    public Boolean getErrorPageShow() {
      return errorPageShow;
    }

    public List<Object> getErrorMsgs() {
      return this.errorMsgList;
    }

    public ActionExecResult invoke() {
      errorMsgList = getErrorMsgList(context);
      msgList = (List<String>) context.get(IMessageHandler.ACTION_MSG);
      pluginErrorList = (List<List<List<DefaultFieldErrorHandler.FieldError>>>) context.get(IFieldErrorHandler.ACTION_ERROR_FIELDS);
      bizResult = context.get(IMessageHandler.ACTION_BIZ_RESULT);
      errorPageShow = (Boolean) context.get(IMessageHandler.ACTION_ERROR_PAGE_SHOW);
      return this;
    }
  }
}
