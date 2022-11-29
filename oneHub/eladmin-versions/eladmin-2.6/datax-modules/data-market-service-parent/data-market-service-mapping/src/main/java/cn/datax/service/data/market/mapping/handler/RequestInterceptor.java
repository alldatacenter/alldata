package cn.datax.service.data.market.mapping.handler;

import cn.datax.common.core.DataConstant;
import cn.datax.common.exception.DataException;
import cn.datax.common.utils.IPUtil;
import cn.datax.common.utils.MD5Util;
import cn.datax.service.data.market.api.dto.ApiLogDto;
import cn.datax.service.data.market.api.dto.RateLimit;
import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.api.enums.ParamType;
import cn.datax.service.data.market.mapping.utils.ThreadUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RequestInterceptor {

    private RedisTemplate<String, Object> redisTemplate;

    public RequestInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 请求之前执行
     *
     * @return 当返回对象时，直接将此对象返回到页面，返回null时，继续执行后续操作
     * @throws Exception
     */
    public void preHandle(HttpServletRequest request, HttpServletResponse response, DataApiEntity api, Map<String, Object> params) throws Exception {
        System.out.println("************ApiInterceptor preHandle executed**********");
        String uri = request.getRequestURI();
        log.info("getRequestURI的值：" + uri);
        String ipAddr = IPUtil.getIpAddr(request);
        log.info("ipAddr的值：" + ipAddr);

        // 密钥校验
        String apiKey = request.getHeader("api_key");
        String secretKey = request.getHeader("secret_key");
        if (StrUtil.isBlank(apiKey) || StrUtil.isBlank(secretKey)) {
            throw new DataException("api_key或secret_key空");
        }
        MD5Util mt = MD5Util.getInstance();
        String apiId = mt.decode(apiKey);
        String userId = mt.decode(secretKey);

        // 黑名单校验
        String deny = api.getDeny();
        if (StrUtil.isNotBlank(deny)) {
            List<String> denyList = Arrays.asList(deny.split(","));
            if (CollUtil.isNotEmpty(denyList)) {
                for (String ip : denyList) {
                    if(ip.equals(ipAddr)){
                        throw new DataException(ip + "已被加入IP黑名单");
                    }
                }
            }
        }

        // 参数校验
        if (MapUtil.isNotEmpty(params)) {
            api.getReqParams().forEach(param -> {
                if (params.containsKey(param.getParamName())) {
                    // 参数类型是否正确
                    ParamType.parse(ParamType.getParamType(param.getParamType()), params.get(param.getParamName()));
                }
            });
        }

        // 限流校验
        RateLimit rateLimit = api.getRateLimit();
        if (DataConstant.TrueOrFalse.TRUE.getKey().equals(rateLimit.getEnable())) {
            Integer times = rateLimit.getTimes();
            Integer seconds = rateLimit.getSeconds();
            // 请求次数
            times = Optional.ofNullable(times).orElse(5);
            // 请求时间范围60秒
            seconds = Optional.ofNullable(seconds).orElse(60);
            // 根据 USER + API 限流
            String key = "user:" + userId + ":api:" + apiId;
            // 根据key获取已请求次数
            Integer maxTimes = (Integer) redisTemplate.opsForValue().get(key);
            if (maxTimes == null) {
                // set时一定要加过期时间
                redisTemplate.opsForValue().set(key, 1, seconds, TimeUnit.SECONDS);
            } else if (maxTimes < times) {
                redisTemplate.opsForValue().set(key, maxTimes + 1, seconds, TimeUnit.SECONDS);
            } else {
                throw new DataException("API调用过于频繁");
            }
        }
    }

    /**
     * 执行完毕之后执行
     *
     * @throws Exception
     */
    public void postHandle(HttpServletRequest request, HttpServletResponse response, DataApiEntity api, Map<String, Object> params, Object value) throws Exception {
    }
}
