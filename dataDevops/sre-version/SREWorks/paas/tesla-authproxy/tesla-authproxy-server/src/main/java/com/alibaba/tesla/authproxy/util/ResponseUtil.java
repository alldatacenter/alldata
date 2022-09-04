package com.alibaba.tesla.authproxy.util;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.vo.LoginUserInfoVO;
import com.alibaba.tesla.common.utils.TeslaResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description: Response响应处理工具类，提供了response写json数据的操作 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Slf4j
public class ResponseUtil {

    /**
     * 构造未登陆的 Response 返回结构
     *
     * @param message  具体信息
     * @param loginUrl 登陆 URL
     * @return
     */
    public static TeslaResult buildNotLoginResponse(String message, String loginUrl) {
        TeslaResult ret = new TeslaResult();
        ret.setCode(TeslaResult.NOAUTH);
        ret.setMessage(message);
        Map<String, String> data = new HashMap<>(1);
        data.put("loginUrl", loginUrl);
        ret.setData(data);
        return ret;
    }

    /**
     * 写入没有登录的JSON信息
     *
     * @param response
     * @param loginUrl
     */
    public static void writeNoLoginJson(HttpServletResponse response, String loginUrl, String message) {

        LoginUserInfoVO loginUserInfoVO = new LoginUserInfoVO();
        loginUserInfoVO.setLoginUrl(loginUrl);

        Map<String, Object> noLogin = new HashMap<>();
        noLogin.put("code", TeslaResult.NOAUTH);
        noLogin.put("data", loginUserInfoVO);
        if (StringUtil.isEmpty(message)) {
            noLogin.put("message", "forbidden, login user is invalid, please login");
        } else {
            noLogin.put("message", message);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        PrintWriter out = null;
        try {
            String jsonString = objectMapper.writeValueAsString(noLogin);
            log.info("没有登录，返回JSON数据->{}", jsonString);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=utf-8");
            out = response.getWriter();
            out.append(jsonString);
        } catch (Exception e) {
            log.error("Response write login json fail. {}", ExceptionUtils.getStackTrace(e));
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Response写入异常信息JSON
     *
     * @param response
     * @param errorCode
     * @param errorMsg
     */
    public static void writeErrorJson(HttpServletResponse response, int errorCode, Object data, String errorMsg) {

        Map<String, Object> noLogin = new HashMap<String, Object>();
        noLogin.put("code", errorCode);
        noLogin.put("data", data);
        noLogin.put("message", errorMsg);

        write(noLogin, response);
    }

    public static void writeErrorJson(HttpServletResponse response, Object data) {

        Map<String, Object> noLogin = new HashMap<String, Object>();
        noLogin.put("code", TeslaResult.FAILURE);
        noLogin.put("data", data);
        noLogin.put("message", Constants.RES_MSG_SERVERERROR);

        write(noLogin, response);
    }

    private static void write(Map<String, Object> data, HttpServletResponse response) {
        ObjectMapper objectMapper = new ObjectMapper();
        PrintWriter out = null;
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=utf-8");
            out = response.getWriter();
            out.append(jsonString);
        } catch (Exception e) {
            log.error("Response write login json fail. {}", ExceptionUtils.getStackTrace(e));
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

}
