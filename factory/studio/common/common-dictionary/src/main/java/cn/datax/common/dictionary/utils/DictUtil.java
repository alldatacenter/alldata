package cn.datax.common.dictionary.utils;

import cn.datax.common.core.RedisConstant;
import cn.datax.common.redis.service.RedisService;
import cn.datax.common.utils.SpringContextHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DictUtil {

    private DictUtil() {}

    private static volatile DictUtil instance;

    public static DictUtil getInstance() {
        if(instance == null) {
            synchronized (DictUtil.class) {
                if(instance == null) {
                    instance = new DictUtil();
                }
            }
        }
        return instance;
    }

    private RedisService redisService = SpringContextHolder.getBean(RedisService.class);

    /**
     * 获取字典项
     * @param code
     */
    public Object getDictItemList(String code) {
        String key = RedisConstant.SYSTEM_DICT_KEY;
        Object object = redisService.get(key);
        if (null == object) {
            return null;
        }
        JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(object));
        List<Object> list = jsonArray.stream().filter(obj -> ((JSONObject) obj).get("dictCode").equals(code))
                .flatMap(obj -> ((JSONObject) obj).getJSONArray("items").stream()).collect(Collectors.toList());
        return list;
    }

    /**
     * 获取字典项值
     * @param code
     * @param text
     * @return
     */
    public Object getDictItemValue(String code, String text) {
        String key = RedisConstant.SYSTEM_DICT_KEY;
        Object object = redisService.get(key);
        if (null == object) {
            return null;
        }
        JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(object));
        Object o = jsonArray.stream().filter(obj -> ((JSONObject) obj).get("dictCode").equals(code))
                .flatMap(obj -> ((JSONObject) obj).getJSONArray("items").stream())
                .filter(obj -> ((JSONObject) obj).get("itemText").equals(text))
                .map(obj -> ((JSONObject) obj).get("itemValue")).findFirst().orElse(null);
        return o;
    }
}
