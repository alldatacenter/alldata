package cn.datax.service.data.market.integration.utils;

import cn.datax.common.utils.SpringContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * redis生成每日流水号
 */
public class SerialUtil {

    private static StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

    /**
     * 通过redis生成自增号
     * @param length 序号长度
     * @return
     */
    public static String getSerialNo(int length) {
        // 编号
        String key = LocalDate.now().format(DateTimeFormatter.ISO_DATE).replace("-", "");
        String no;
        // 自增获取
        Long temp = redisTemplate.opsForValue().increment(key, 1);
        if (temp == 1) {
            // 过期时间,一天
            redisTemplate.expire(key, 1, TimeUnit.DAYS);
        }
        return key + String.format("%0" + length + "d", temp);
    }
}
