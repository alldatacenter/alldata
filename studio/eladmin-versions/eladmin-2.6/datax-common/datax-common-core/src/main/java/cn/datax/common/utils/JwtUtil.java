package cn.datax.common.utils;

import cn.datax.service.system.api.dto.JwtUserDto;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Slf4j
public class JwtUtil {







    /**
     * 获取凭证信息
     *
     * @param token jwt token串
     * @return Claims
     */
    public static Claims getClaimByToken(String token) {
        try {
            if (StringUtils.startsWithIgnoreCase(token, "Bearer ")) {
                token = token.split(" ")[1];
            }
            return Jwts.parser()
                    .setSigningKey("ZmQ0ZGI5NjQ0MDQwY2I4MjMxY2Y3ZmI3MjdhN2ZmMjNhODViOTg1ZGE0NTBjMGM4NDA5NzYxMjdjOWMwYWRmZTBlZjlhNGY3ZTg4Y2U3YTE1ODVkZDU5Y2Y3OGYwZWE1NzUzNWQ2YjFjZDc0NGMxZWU2MmQ3MjY1NzJmNTE0MzI=")
                    .parseClaimsJws(token)
                    .getBody();
        }catch (Exception e){
            HttpServletRequest request =((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String authorization = request.getHeader("Authorization");
            String url = request.getRequestURL().toString();
            String uri = request.getRequestURI();
            return null;
        }
    }


    /**
     * 获取过期时间
     *
     * @param token jwt token 串
     * @return Date
     */
    public Date getExpiration(String token) {
        return getClaimByToken(token).getExpiration();
    }


    /**
     * 验证token是否失效
     *
     * @param token token
     * @return true:过期   false:没过期
     */
    public boolean isExpired(String token) {
        try {
            final Date expiration = getExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("[JwtUtils --> isExpired]: {}", e.getMessage());
            return true;
        }
    }


//    /**
//     * 检验是否为 jwt 格式的字符串
//     *
//     * 说明: jwt 字符串由三部分组成, 分别用 . 分隔开, 所以认为有两个 . 的字符串就是jwt形式的字符串
//     * @param token jwt token串
//     * @return boolean
//     */
//    public boolean isJwtStr(String token){
//        return StringUtils.countOccurrencesOf(token, ".") == 2;
//    }


    /**
     * 获取 jwt 中的账户名
     *
     * @param token jwt token 串
     * @return String
     */
    public String getAccountName(String token){
        String subject = getClaimByToken(token).getSubject();
        JwtUserDto jwtContent = JSONObject.parseObject(subject, JwtUserDto.class);
        jwtContent.getUsername();
        return jwtContent.getUsername();
    }


    /**
     * 获取 jwt 的账户对象
     * @param token
     * @return
     */
    public static String  getTokenSubjectObject(String token){
        Claims claimByToken = getClaimByToken(token);
        String subject = claimByToken.getSubject();
        String body = JSONObject.toJSONString(subject);
        Object parse = JSON.parse(body);
        String s = parse.toString();
        return s;
    }


    /**
     * 获取 jwt 账户信息的json字符串
     * @param token
     * @return
     */
    public  String getTokenSubjectStr(String token){
        String body = JSONObject.toJSONString(getClaimByToken(token).getSubject());
        Object parse = JSON.parse(body);
        return parse.toString();
    }
}