package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.List;

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
            Yaml yaml = createYaml(JSONObject.class);
            JSONObject mid = yaml.loadAs(yamlStr, JSONObject.class);
            return ObjectConvertUtil.from(mid.toJSONString(), clazz);
        } catch (Exception e) {
            log.error("cannot convert yaml str to schema|yamlStr={}|exception={}",
                yamlStr, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * 将 yaml 字符串转换为实际的 Schema 对象 List
     *
     * @param clazz   类型 Class
     * @param yamlStr Yaml 字符串
     * @return 实际对象 List
     */
    public static <T> List<T> toSchemaList(Class<T> clazz, String yamlStr) {
        try {
            Yaml yaml = createYaml(JSONArray.class);
            JSONArray mid = yaml.loadAs(yamlStr, JSONArray.class);
            return JSONArray.parseArray(mid.toJSONString(), clazz);
        } catch (Exception e) {
            log.error("cannot convert yaml str to schema list|yamlStr={}|exception={}",
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
            return createYaml(JSONObject.class).dumpAsMap(schema);
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
            return createYaml(JSON.class).dump(schema);
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
    public static <T> String toYamlStr(T schema, Class<?> mapClass) {
        try {
            return createYaml(JSON.class, mapClass).dump(schema);
        } catch (Exception e) {
            log.error("cannot convert schema to yaml|schema={}|exception={}",
                    JSONObject.toJSONString(schema), ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * 创建 Yaml 实例
     *
     * @param cls Class 对象
     *
     * @return Yaml instance
     */
    public static Yaml createYaml(Class<?> cls) {
        return createYaml(List.of(cls), null);
    }

    /**
     * 创建 Yaml 实例
     *
     * @param classList Class 对象列表
     *
     * @return Yaml instance
     */
    public static Yaml createYaml(List<Class<?>> classList) {
        return createYaml(classList, null);
    }

    /**
     * 创建 Yaml 实例
     *
     * @param cls Class 对象
     *
     * @return Yaml instance
     */
    public static Yaml createYaml(Class<?> cls, Class<?> mapClass) {
        return createYaml(List.of(cls), List.of(mapClass));
    }

    /**
     * 创建 Yaml 实例
     *
     * @param classList Class 对象列表
     *
     * @return Yaml instance
     */
    public static Yaml createYaml(List<Class<?>> classList, List<Class<?>> mapClassList) {
        DumperOptions options = new DumperOptions();
        options.setSplitLines(false);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(
                    Object javaBean, Property property, Object propertyValue, Tag customTag) {
                if (propertyValue == null) {
                    return null;
                } else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }
        };
        if (mapClassList != null) {
            for (Class<?> cls : mapClassList) {
                representer.addClassTag(cls, Tag.MAP);
            }
        }
        return new Yaml(new ClassFilterConstructor(classList), representer, options);
    }

    /**
     * Yaml Class Filter (for Security)
     */
    public static class ClassFilterConstructor extends Constructor {

        private List<Class<?>> acceptClassList;

        public ClassFilterConstructor(List<Class<?>> classList) {
            this.acceptClassList = classList;
        }

        @Override
        protected Class<?> getClassForName(final String name) throws ClassNotFoundException {
            for (Class<?> cls : acceptClassList) {
                if (name.equals(cls.getName())) {
                    return super.getClassForName(name);
                }
            }
            throw new IllegalArgumentException(String.format("Class is not accepted: %s", name));
        }
    }
}
