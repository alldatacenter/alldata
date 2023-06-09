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
package com.qlangtech.tis.manage.servlet;

import com.qlangtech.tis.manage.common.HttpConfigFileReader;
import com.qlangtech.tis.manage.common.RunContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-19
 */
public class BasicServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  // public static final XStream xstream = new XStream();
  //
  // static {
  // xstream.alias("sdomain", SnapshotDomain.class);
  // }
  protected RunContext getContext() {
    return getBeanByType(this.getServletContext(), RunContext.class);
  }

  // protected RpcCoreManage getRpcCoreManage() {
  // return WebApplicationContextUtils.getWebApplicationContext(
  // this.getServletContext()).getBean("rpcCoreManage",
  // RpcCoreManage.class);
  // }
  // static {
  //
  // try {
  // Class.forName("com.taobao.terminator.manage.common.ActionTool");
  // } catch (ClassNotFoundException e) {
  // throw new RuntimeException(e);
  // }
  //
  // }
  // protected BasicModule getBasicModule() {
  // return getBeanByType(BasicModule.class);
  // }
  public static <T> T getBeanByType(ServletContext servletContext, Class<T> clazz) {

    ApplicationContext applicationContext = (ApplicationContext) servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    Objects.requireNonNull(applicationContext
      , "applicationContext can not be null in servletContent by key:" + WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

    for (Object context : applicationContext.getBeansOfType(clazz).values()) {
    //  for (Object context : WebApplicationContextUtils.getWebApplicationContext(servletContext).getBeansOfType(clazz).values()) {
      return clazz.cast(context);
    }
    throw new IllegalStateException("can no t find:" + clazz);
  }

  // protected ActionTool getActionTool() {
  // return getBeanByType(this.getServletContext(), ActionTool.class);
  // }
  protected void wirteXml2Client(HttpServletResponse response, Object o) throws IOException {
    response.setContentType(DownloadResource.XML_CONTENT_TYPE);
    HttpConfigFileReader.xstream.toXML(o, response.getWriter());
  }

  protected void include(HttpServletRequest request, HttpServletResponse response, String path) throws ServletException, IOException {
    RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(path);
    dispatcher.include(request, response);
  }
}
