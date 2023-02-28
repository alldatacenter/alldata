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
package com.qlangtech.tis.manage.common;

import com.opensymphony.xwork2.config.ConfigurationManager;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.apache.struts2.dispatcher.mapper.DefaultActionMapper;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TisActionMapper extends DefaultActionMapper {

  public static final String REQUEST_EXTENDSION_AJAX = "ajax";
  public static final String ACTION_TOKEN = "#action";

  protected void parseNameAndNamespace(String uri, ActionMapping mapping, ConfigurationManager configManager) {
    super.parseNameAndNamespace(uri, mapping, configManager);
    // StringBuffer parsedName = new StringBuffer();
    // char[] nameAry = mapping.getName().toCharArray();
    // for (int i = 0; i < nameAry.length; i++) {
    // if (Character.isUpperCase(nameAry[i])) {
    // parsedName.append('_')
    // .append(Character.toLowerCase(nameAry[i]));
    // } else {
    // parsedName.append(nameAry[i]);
    // // .append(Character.toLowerCase());
    // }
    // }
    // mapping.setMethod(BasicModule.parseMehtodName());
    mapping.setName(addUnderline(mapping.getName()).toString());
    mapping.setNamespace(mapping.getNamespace() + "#screen");
  }

  public static StringBuffer addUnderline(String value) {

    //return  UnderlineUtils.addUnderline(value);
    StringBuffer parsedName = new StringBuffer();
    char[] nameAry = value.toCharArray();
    boolean firstAppend = true;
    for (int i = 0; i < nameAry.length; i++) {
      if (Character.isUpperCase(nameAry[i])) {
        if (firstAppend) {
          parsedName.append(Character.toLowerCase(nameAry[i]));
          firstAppend = false;
        } else {
          parsedName.append('_').append(Character.toLowerCase(nameAry[i]));
        }
      } else {
        parsedName.append(nameAry[i]);
        firstAppend = false;
        // .append(Character.toLowerCase());
      }
    }
    return parsedName;


  }

  @Override
  public ActionMapping getMapping(HttpServletRequest request, ConfigurationManager configManager) {
    ActionMapping mapping = super.getMapping(request, configManager);
    String action = null;
    if (StringUtils.isNotEmpty(action = request.getParameter("action"))) {
      mapping.setName(action);
      mapping.setNamespace(StringUtils.split(mapping.getNamespace(), "#")[0] + ACTION_TOKEN);
    }
    return mapping;
  }
}
