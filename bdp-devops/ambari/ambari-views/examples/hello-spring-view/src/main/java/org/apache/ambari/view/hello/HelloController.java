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

package org.apache.ambari.view.hello;

import org.apache.ambari.view.ViewContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for the Hello Spring app view.
 */
@Controller
@RequestMapping(value = "/")
public class HelloController {

  @RequestMapping(method = RequestMethod.GET)
  public String printHello(ModelMap model, HttpServletRequest request) {

    // get the view context from the servlet context
    ViewContext viewContext = (ViewContext) request.getSession().getServletContext().getAttribute(ViewContext.CONTEXT_ATTRIBUTE);

    // get the current user name from the view context
    String userName = viewContext.getUsername();

    // add the greeting message attribute
    model.addAttribute("greeting", "Hello " + (userName == null ? "unknown user" : userName) + "!");

    return "hello";
  }
}
