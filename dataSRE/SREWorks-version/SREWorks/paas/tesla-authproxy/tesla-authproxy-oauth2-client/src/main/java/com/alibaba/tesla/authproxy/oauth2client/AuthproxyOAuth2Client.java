package com.alibaba.tesla.authproxy.oauth2client;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.constants.AuthJwtConstants;
import com.alibaba.tesla.authproxy.constants.AuthProxyConstants;
import com.alibaba.tesla.authproxy.exceptions.TeslaJwtException;
import com.alibaba.tesla.authproxy.oauth2client.conf.AuthproxyOauth2ClientProperties;
import com.alibaba.tesla.authproxy.oauth2client.dto.TeslaJwtUserInfoDTO;
import com.alibaba.tesla.authproxy.oauth2client.dto.TeslaOauth2TokenDTO;
import com.alibaba.tesla.authproxy.oauth2client.exception.Oauth2HttpException;
import com.alibaba.tesla.authproxy.util.TeslaJwtUtil;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResult;
import com.alibaba.tesla.common.utils.TeslaOKHttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * tesla authproxy oauth2 client
 *
 * @author cdx
 * @date 2019/12/31 17:22
 */
@Component
public class AuthproxyOAuth2Client {

    @Autowired
    private AuthproxyOauth2ClientProperties authproxyOauth2ClientProperties;
    @Autowired
    private ObjectMapper objectMapper;

    private ConcurrentHashMap<String, TeslaOauth2TokenDTO> tokenMap = new ConcurrentHashMap();
    private final static String OAUTH2_TOKEN_URL = "/v2/common/authProxy/oauth/token";
    private final static String OAUTH2_ADMIN_TOKEN_URL = "/authproxy/oauth2/teslaToken";

    /**
     * 从已认证请求头中获取tesla用户信息
     *
     * @param request
     * @param secretKey
     * @return
     * @throws TeslaJwtException
     */
    public TeslaJwtUserInfoDTO getTeslaJwtUserInfo(HttpServletRequest request, String secretKey)
        throws TeslaJwtException {
        String token = request.getHeader(AuthProxyConstants.HTTP_BASIC_AUTH_HEADER);
        if (StringUtils.isBlank(token)) {
            return null;
        }
        Claims claims = TeslaJwtUtil.verify(token, secretKey);
        return TeslaJwtUserInfoDTO.builder().loginName(
            String.valueOf(claims.get(AuthJwtConstants.JWT_LOGIN_NAME_CLAIM_KEY)))
            .userId(String.valueOf(claims.get(AuthJwtConstants.JWT_USER_ID_CLAIM_KEY)))
            .bucId(String.valueOf(claims.get(AuthJwtConstants.JWT_BUC_ID_CLAIM_KEY)))
            .aliyunPk(String.valueOf(claims.get(AuthJwtConstants.JWT_ALIYUN_PK_CLAIM_KEY)))
            .nickName(String.valueOf(claims.get(AuthJwtConstants.JWT_NICKNAME_CLAIM_KEY)))
            .empId(String.valueOf(claims.get(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY))).build();
    }

    /**
     * 获取tesla jwt token
     *
     * @param userName
     * @param password
     * @param clientId
     * @param clientSecret
     * @return
     */
    public String getTeslaJwtToken(String userName, String password, String clientId, String clientSecret)
        throws Exception {
        TeslaOauth2TokenDTO teslaOauth2TokenDTO;
        String tokenId = userName + password + clientId + clientSecret;
        TeslaOauth2TokenDTO dto = tokenMap.get(tokenId);
        if (dto != null) {
            Date expirationDate = new Date(dto.getTokenExpiration() * 1000);
            if (!expirationDate.before(new Date())) {
                return dto.getToken();
            }
        }

        String teslaEndpoint = authproxyOauth2ClientProperties.getTeslaGatewayEndpoint();
        String oauth2Endpoint = teslaEndpoint + OAUTH2_TOKEN_URL;
        Map<String, String> paramMap = new HashMap<>(8);
        paramMap.put("username", userName);
        paramMap.put("password", password);
        paramMap.put("client_id", clientId);
        paramMap.put("client_secret", clientSecret);
        paramMap.put("grant_type", "password");
        TeslaResult teslaResult = TeslaOKHttpClient.post(oauth2Endpoint, paramMap);
        if (teslaResult.getCode() == 200) {
            teslaOauth2TokenDTO = objectMapper.convertValue(teslaResult.getData(),
                TeslaOauth2TokenDTO.class);
            tokenMap.put(tokenId, teslaOauth2TokenDTO);
        } else {
            throw new Oauth2HttpException(String
                .format("Get tesla oauth2 token request error!oauth2Endpoint=%s,param=%s,result=%s", oauth2Endpoint,
                    paramMap, objectMapper.writeValueAsString(teslaResult)));
        }
        return teslaOauth2TokenDTO.getToken();
    }

    /**
     * admin用户根据empId或loginName获取token
     *
     * @param adminUserName
     * @param adminPassword
     * @param adminClientId
     * @param adminClientSecret
     * @param empId
     * @param loginName
     * @return
     * @throws Exception
     */
    public String getTeslaJwtTokenByAdmin(String adminUserName, String adminPassword, String adminClientId,
        String adminClientSecret, String empId, String loginName)
        throws Exception {
        String authToken =
            getTeslaJwtToken(adminUserName, adminPassword, adminClientId, adminClientSecret);
        String teslaEndpoint = authproxyOauth2ClientProperties.getTeslaGatewayEndpoint();
        String param = String.format("?loginName=%s&empId=%s", loginName == null ? "" : loginName, empId == null ?
            "" : empId);
        String oauth2Endpoint = teslaEndpoint + OAUTH2_ADMIN_TOKEN_URL + param;
        Map<String, String> paramMap = new HashMap<>(2);
        paramMap.put("Authorization", authToken);
        TeslaResult teslaResult = TeslaOKHttpClient.get(oauth2Endpoint, paramMap, null);
        if (teslaResult.getCode() == 200) {
            TeslaBaseResult teslaBaseResult = JSONObject.parseObject((String)teslaResult.getData(),
                TeslaBaseResult.class);
            if (teslaBaseResult.getCode() == 200) {
                return objectMapper.convertValue(teslaBaseResult.getData(),
                    TeslaOauth2TokenDTO.class).getToken();
            } else {
                throw new Oauth2HttpException(
                    String.format("Tesla oauth2 admin token service error!adminUserName=%s,result=%s", adminUserName,
                        objectMapper.writeValueAsString(teslaResult)));
            }
        } else {
            throw new Oauth2HttpException(
                String.format("Get tesla oauth2 admin token request error!adminUserName=%s,result=%s", adminUserName,
                    objectMapper.writeValueAsString(teslaResult)));
        }
    }

}
