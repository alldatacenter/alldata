package cn.datax.common.dictionary.config;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.dictionary.annotation.DictAop;
import cn.datax.common.dictionary.utils.DictUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class DictAnalysis implements ResponseBodyAdvice {

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        if (o instanceof R) {
            if (((R) o).getData() instanceof JsonPage) {
                List list = ((JsonPage) ((R) o).getData()).getData();
                List<JSONObject> items = new ArrayList<>();
                for (Object record : list) {
                    JSONObject item = JSONObject.parseObject(JSON.toJSONString(record));
                    for (Field field : record.getClass().getDeclaredFields()) {
                        // 获取自定义注解
                        DictAop dictAop = field.getAnnotation(DictAop.class);
                        if (null != dictAop) {
                            String code = dictAop.code();
                            String text = field.getName();
                            Object object = item.get(field.getName());
                            if (null != object) {
                                // 字典翻译
                                Object dictValue = DictUtil.getInstance().getDictItemValue(code, object.toString());
                                if (null != dictValue) {
                                    item.put(field.getName() + "_dictText", dictValue);
                                }
                            }
                        }
                    }
                    items.add(item);
                }
                ((JsonPage) ((R) o).getData()).setData(items);
            }
        }
        return o;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        return true;
    }
}
