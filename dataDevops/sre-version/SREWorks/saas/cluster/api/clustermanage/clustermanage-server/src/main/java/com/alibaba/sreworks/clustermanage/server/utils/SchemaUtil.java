package com.alibaba.sreworks.clustermanage.server.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Schema 变换工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class SchemaUtil {

    /**
     * 将 yaml 字符串转换为实际的 Schema 对象
     *
     * @param clazz   类型 Class
     * @param yamlStr Yaml 字符串
     * @return 实际对象
     */
    public static <T> T toSchema(Class<T> clazz, String yamlStr) {
        try {
            Yaml yaml = createYaml();
            JSONObject mid = yaml.loadAs(yamlStr, JSONObject.class);
            return ObjectConvertUtil.from(mid.toJSONString(), clazz);
        } catch (Exception e) {
            log.error("cannot convert yaml str to schema|yamlStr={}|exception={}",
                yamlStr, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * 将 Schema 对象还原为 yaml 字符串内容
     *
     * @param schema Schema 对象
     * @return Yaml 字符串内容
     */
    public static <T> String toYamlMapStr(T schema) {
        try {
            return createYaml().dumpAsMap(schema);
        } catch (Exception e) {
            log.error("cannot convert schema to yaml|schema={}|exception={}",
                JSONObject.toJSONString(schema), ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * 将 Schema 对象还原为 yaml 字符串内容
     *
     * @param schema Schema 对象
     * @return Yaml 字符串内容
     */
    public static <T> String toYamlStr(T schema) {
        try {
            return createYaml().dump(schema);
        } catch (Exception e) {
            log.error("cannot convert schema to yaml|schema={}|exception={}",
                JSONObject.toJSONString(schema), ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * 创建 Yaml 实例
     *
     * @return Yaml instance
     */
    public static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setSplitLines(false);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(
                Object javaBean, Property property, Object propertyValue, Tag customTag) {
                if (propertyValue == null) {
                    return null;
                } else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }
        }, options);
    }
}
