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
package com.qlangtech.tis.runtime.module.action;

import com.alibaba.citrus.turbine.Context;
import com.opensymphony.xwork2.ModelDriven;
import com.qlangtech.tis.manage.ChangeDomainForm;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.biz.dal.pojo.ApplicationCriteria.Criteria;
import com.qlangtech.tis.manage.common.IUser;
import com.qlangtech.tis.manage.common.ManageUtils;
import com.qlangtech.tis.manage.common.RunContext;
import com.qlangtech.tis.manage.common.apps.AppsFetcher.CriteriaSetter;
import com.qlangtech.tis.manage.common.apps.IAppsFetcher;
import com.qlangtech.tis.pubhook.common.Nullable;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.runtime.module.action.GroupAction.SuggestCallback;
import com.qlangtech.tis.runtime.module.control.AppDomain;
import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ChangeDomainAction extends BasicModule implements ModelDriven<ChangeDomainForm> {

    private static final long serialVersionUID = 1L;

    public static final String GROUPNAME = "changedomain";

    public static final String SELECT_APP_NAME = "selectAppDomain";

    public static final String COOKIE_SELECT_APP = SELECT_APP_NAME + (ManageUtils.isDaily() ? RunEnvironment.DAILY : RunEnvironment.ONLINE);

    @Override
    public boolean isAppNameAware() {
        return false;
    }

    /**
     * @param
     */
    public ChangeDomainAction() {
        super(GROUPNAME);
    }

    private final ChangeDomainForm form = new ChangeDomainForm();

    @Override
    public ChangeDomainForm getModel() {
        return this.form;
    }

    /**
     * 切换当前应用
     *
     * @throws Exception
     */
    public // Navigator nav,
    void doChange(Context context) throws Exception {
        Integer appid = parseSelectedAppid(form, context);
        if (appid == null) {
            return;
        }
        final Application app = this.getApplicationDAO().selectByPrimaryKey(appid);
        setAppDomainCookie(form, app);
        return2OriginUrl(form);
    }

    /**
     * 选择运行环境
     *
     * @param context
     * @throws Exception
     */
    public void doChangeRuntimeAjax(Context context) throws Exception {
        // Integer appid = parseSelectedAppid(form, context);
        // if (appid == null) {
        // return;
        // }
        setAppdomainCookie(getResponse(), this.getRequest(), RunEnvironment.getEnum(this.getString("runtime")));
    // return2OriginUrl(form);
    }

    /**
     * 利用AJAX切换选择的应用
     *
     * @param context
     * @throws Exception
     */
    public void doChangeAppAjax(Context context) throws Exception {
        Integer appid = this.getInt("appid");
        if (appid == null) {
            appid = this.getInt("selappid");
        }
        if (appid == null) {
            this.addErrorMessage(context, "请选择应用");
            return;
        }
        if (this.getAppDomain() instanceof Nullable) {
            this.addErrorMessage(context, "请先选择应用环境，日常？  预发？ 线上？");
            return;
        }
        Application app = this.getApplicationDAO().loadFromWriteDB(appid);
        setAppdomainCookie(getResponse(), this.getRequest(), this.getAppDomain().getRunEnvironment(), app);
        this.addActionMessage(context, "已经将当前应用切换成:" + AppDomain.getAppDescribe(app));
    }

    private Integer parseSelectedAppid(ChangeDomainForm form, Context context) {
        Integer appid = this.getInt("hiddenAppnamesuggest");
        if (appid != null) {
            return appid;
        }
        if (form.getBizid() == null) {
            this.addErrorMessage(context, "请选择业务线");
            return appid;
        }
        appid = form.getAppid();
        if (appid == null) {
            this.addErrorMessage(context, "请选择应用");
            return appid;
        }
        return appid;
    }

    static final List<Application> emptyAppList = Collections.emptyList();

    /**
     * 通过远程json方式，获得daily的suggest
     *
     * @param context
     * @throws Exception
     */
    public void doAppNameSuggestDaily(Context context) throws Exception {
    }

    /**
     * changedomain 页面上添加了一个
     *
     * @param context
     */
    public void doAppNameSuggest(Context context) throws Exception {
        final String appNameFuzzy = StringUtils.trimToEmpty(this.getString("query"));
        processNameSuggest(this.getRequest(), this.getUser(), this, false, appNameFuzzy);
    }

    /**
     * @param appNameFuzzy
     * @throws JSONException
     * @throws IOException   this.getRequest(), false, this.getUser(), this
     */
    public static void processNameSuggest(HttpServletRequest request, IUser user, RunContext context, boolean ismaxmatch, final String appNameFuzzy) throws JSONException, IOException {
        final List<Application> result = (!StringUtils.startsWith(appNameFuzzy, "search4") ? ChangeDomainAction.emptyAppList : getMatchApps(request, ismaxmatch, appNameFuzzy, user, context));
        GroupAction.writeSuggest2Response(appNameFuzzy, result, new SuggestCallback<Application>() {

            @Override
            public String getLiteral(Application o) {
                return o.getProjectName();
            }

            @Override
            public Object getValue(Application o) {
                return o.getAppId();
            }
        }, getResponse());
    }

    // this.getRequest(), false, this.getUser(), this
    protected static List<Application> getMatchApps(HttpServletRequest request, boolean isMaxMatch, final String appNameFuzzy, IUser user, RunContext context) {
        IAppsFetcher fetcher = null;
        fetcher = getAppsFetcher(request, isMaxMatch, user, context);
        return fetcher.getApps(new CriteriaSetter() {

            @Override
            public void set(Criteria criteria) {
                criteria.andProjectNameLike(appNameFuzzy + "%");
            }
        });
    }

    private void setAppDomainCookie(ChangeDomainForm form, final Application app) {
        setAppdomainCookie(getResponse(), this.getRequest(), RunEnvironment.getEnum(form.getRunEnviron().shortValue()), app);
    }

    private static void setAppdomainCookie(HttpServletResponse response, HttpServletRequest request, RunEnvironment runtime, final Application app) {
        final String host = request.getHeader("Host");
        addCookie(response, COOKIE_SELECT_APP, (app != null ? app.getProjectName() : ManageUtils.getAppDomain(request).getAppName()) + "_run" + (runtime == null ? StringUtils.EMPTY : runtime.getId()), StringUtils.substringBefore(host, ":"));
    }

    private static void setAppdomainCookie(HttpServletResponse response, HttpServletRequest request, RunEnvironment runtime) {
        setAppdomainCookie(response, request, runtime, null);
    }

    private void return2OriginUrl(ChangeDomainForm form) {
        Assert.assertNotNull("parameter form can not be null", form);
        // RunEnvironment change2 = RunEnvironment.getEnum(form.getRunEnviron()
        // .shortValue());
        final String referer = form.getGobackurl();
        if (StringUtils.isNotEmpty(referer)) {
        // TurbineUtil.getTurbineRunData(this.getRequest())
        // .setRedirectLocation(referer);
        }
    }

    public // Navigator nav
    void doRuntimeChange(Context context) throws Exception {
        // AppDomainInfo appdomain = this.getAppDomain();
        setAppDomainCookie(form, null);
        // addCookie(this.getResponse(), COOKIE_SELECT_APP,
        // appdomain.getAppName()
        // + "_run" + form.getRunEnviron());
        return2OriginUrl(form);
    // final String referer = form.getGobackurl();
    // if (StringUtils.isNotEmpty(referer)) {
    // TurbineUtil.getTurbineRunData(this.getRequest())
    // .setRedirectLocation(referer);
    // }
    }

    public static void addCookie(HttpServletResponse response, String name, String value, String domain) {
        addCookie(response, name, value, domain, 60 * 60 * 24 * 3);
    }

    public static void addCookie(HttpServletResponse response, String name, String value, String domain, int maxAge) {
        Assert.assertNotNull(domain);
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        cookie.setDomain(domain);
        cookie.setVersion(0);
        response.addCookie(cookie);
    }

    /**
     * 点击业务线select
     *
     * @param
     * @param context
     */
    public void doSelectChange(Context context) {
        this.setErrorMsgInvisiable(context);
        Integer bizid = this.getInt("bizid");
        if (bizid == null) {
            return;
        }
        // this.setBizObjResult(context, getAppList(bizid));
        this.setBizResult(context, getAppList(bizid));
    // try {
    // JsonUtil.copy2writer(, getResponse().getWriter());
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    // context.put("applist", );
    }
}
