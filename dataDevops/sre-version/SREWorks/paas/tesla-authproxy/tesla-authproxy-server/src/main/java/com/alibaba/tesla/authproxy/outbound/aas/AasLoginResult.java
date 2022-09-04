package com.alibaba.tesla.authproxy.outbound.aas;

import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.util.StringUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.List;

/**
 * AAS 登录结果返回
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AasLoginResult {

    private List<Cookie> cookies;
    private AasLoginStatusEnum status;
    private String message;
    private String loginAliyunId;
    private String loginAliyunIdTicket;
    private UserDO userInfo;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    AasLoginResult(String response, List<Cookie> cookies) throws AuthProxyThirdPartyError {
        this.cookies = cookies;
        parseStatus(response);
        parseCookies(cookies);
    }

    /**
     * 从 AAS 登录返回界面解析的 message 中获取当前状态
     */
    private void parseStatus(String aasResponse) {
        // 根据 AAS Response，获取其中的 login-tip span 的内容，存入 message
        Document document = Jsoup.parse(aasResponse);
        Elements loginTipElement = document.select("span.login-tip");
        message = loginTipElement.text();
        // 当成功时 message 为空，修改为 SUCCESS 字符串
        if (StringUtil.isEmpty(message)) {
            message = "SUCCESS";
        }
        // 根据获取的 login-tip message 来确认当前状态
        status = AasLoginStatusEnum.build(message);
    }

    /**
     * 从 AAS 登录返回的 Cookie 中解析
     */
    private void parseCookies(List<Cookie> aasCookies) throws AuthProxyThirdPartyError {
        for (Cookie cookie : aasCookies) {
            switch (cookie.name()) {
                case "login_aliyunid":
                    loginAliyunId = StringUtil.trim(cookie.value(), '"');
                    break;
                case "login_aliyunid_ticket":
                    loginAliyunIdTicket = cookie.value();
                    break;
                default:
                    break;
            }
        }
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void setCookies(List<Cookie> cookies) {
        this.cookies = cookies;
    }

    public AasLoginStatusEnum getStatus() {
        return status;
    }

    public void setStatus(AasLoginStatusEnum status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLoginAliyunId() {
        return loginAliyunId;
    }

    public void setLoginAliyunId(String loginAliyunId) {
        this.loginAliyunId = loginAliyunId;
    }

    public String getLoginAliyunIdTicket() {
        return loginAliyunIdTicket;
    }

    public void setLoginAliyunIdTicket(String loginAliyunIdTicket) {
        this.loginAliyunIdTicket = loginAliyunIdTicket;
    }

    public UserDO getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserDO userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public String toString() {
        return gson.toJson(this);
    }

}
