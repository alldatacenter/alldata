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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts2.components.ActionComponent;
import org.apache.struts2.components.Component;
import org.apache.struts2.views.velocity.components.ActionDirective;
import org.apache.velocity.runtime.directive.DirectiveConstants;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-6-20
 */
public class TerminatorActionDirective extends ActionDirective {

    public String getName() {
        return "taction";
    }

    @Override
    protected Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res) {
        return new TerminatorActionComponent(stack, req, res);
    }

    @Override
    public String getBeanName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getType() {
        return DirectiveConstants.BLOCK;
    }

    private class TerminatorActionComponent extends ActionComponent {

        public TerminatorActionComponent(ValueStack stack, HttpServletRequest req, HttpServletResponse res) {
            super(stack, req, res);
        }
        // @Override
        // protected Map<String, String[]> createParametersForContext() {
        //
        // Map<String, String[]> paramContext = super
        // .createParametersForContext();
        //
        // paramContext.remove(ownservers);
        //
        // return paramContext;
        // }
        //
        // private static final String ownservers = "ownservers";
        //
        // @SuppressWarnings("all")
        // @Override
        // protected Map createExtraContext() {
        // final Map extraContext = super.createExtraContext();
        //
        // extraContext.put(ownservers, parameters.get(ownservers));
        //
        // // if (parameters != null) {
        // // // Map<String, String[]> params = new HashMap<String,
        // // // String[]>();
        // // for (Iterator i = parameters.entrySet().iterator(); i.hasNext();)
        // // {
        // // Map.Entry entry = (Map.Entry) i.next();
        // // String key = (String) entry.getKey();
        // // Object val = entry.getValue();
        // // if (val instanceof String) {
        // // continue;
        // // }
        // //
        // // if (val.getClass().isArray()
        // // && String.class == val.getClass()
        // // .getComponentType()) {
        // // // params.put(key, (String[]) val);
        // // continue;
        // // }
        // // extraContext.put(key, val);
        // // }
        // // }
        //
        // return extraContext;
        // }
    }
}
