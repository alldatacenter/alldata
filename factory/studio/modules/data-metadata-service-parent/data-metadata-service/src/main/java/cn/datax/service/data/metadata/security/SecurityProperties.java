
package cn.datax.service.data.metadata.security;

import lombok.Data;

/**
 * Jwt参数配置
 *
 * @author AllDataDC
 * @date 2023-01-27
 */
@Data
public class SecurityProperties {

    /**
     * Request Headers ： Authorization
     */
    private String header = "Authorization";

    /**
     * 令牌前缀，最后留个空格 Bearer
     */
    private String tokenStartWith = "Bearer ";

    /**
     * 必须使用最少88位的Base64对该令牌进行编码
     */
    private String base64Secret = "ZmQ0ZGI5NjQ0MDQwY2I4MjMxY2Y3ZmI3MjdhN2ZmMjNhODViOTg1ZGE0NTBjMGM4NDA5NzYxMjdjOWMwYWRmZTBlZjlhNGY3ZTg4Y2U3YTE1ODVkZDU5Y2Y3OGYwZWE1NzUzNWQ2YjFjZDc0NGMxZWU2MmQ3MjY1NzJmNTE0MzI=";

    /**
     * 令牌过期时间 此处单位/毫秒
     */
    private Long tokenValidityInSeconds = 14400000L;

    /**
     * 在线用户 key，根据 key 查询 redis 中在线用户的数据
     */
    private String onlineKey = "online-token-";

    /**
     * 验证码 key
     */
    private String codeKey = "code-key-";

    /**
     * token 续期检查
     */
    private Long detect = 1800000L;

    /**
     * 续期时间
     */
    private Long renew = 3600000L;

    public String getTokenStartWith() {
        return tokenStartWith + " ";
    }
}
