package datart.server.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import datart.core.base.exception.Exceptions;
import datart.core.common.FileUtils;
import datart.core.common.UrlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.util.ReflectionUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

public class CustomPropertiesValidate implements EnvironmentPostProcessor {

    private static final String CONFIG_HOME = "config/datart.conf";

    private static final String DATABASE_URL = "spring.datasource.url";

    private static final String CONFIG_DATABASE_URL = "datasource.ip";

    private static final String DEFAULT_APPLICATION_CONFIG = "config/profiles/application-config.yml";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources propertySources = environment.getPropertySources();
        Properties properties = loadCustomProperties();
        //this.validateConfig(properties);
        propertySources.addFirst(new PropertiesPropertySource("datartConfig", properties));
        switchProfile(environment);
        String jdbcUrl = processDBUrl(environment);
        if (StringUtils.isNotBlank(jdbcUrl)) {
            properties.setProperty(DATABASE_URL, jdbcUrl);
            propertySources.addFirst(new PropertiesPropertySource("datartConfig", properties));
        }
    }

    private Properties loadCustomProperties() {
        Properties properties = new Properties();
        File file = new File(FileUtils.concatPath(System.getProperty("user.dir"), CONFIG_HOME));
        try (InputStream inputStream = new FileInputStream(file)) {
            properties.load(inputStream);
        } catch (Exception e) {
            System.err.println("Failed to load the datart configuration (config/datart.conf), use application-config.yml");
            return new Properties();
        }
        List<Object> removeKeys = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String val = String.valueOf(entry.getValue()).trim();
            if (StringUtils.isBlank(val)) {
                removeKeys.add(entry.getKey());
            }
            entry.setValue(val);
        }
        for (Object key : removeKeys) {
            properties.remove(key);
        }
        return properties;
    }

    private void validateConfig(Properties properties) {
        CustomConfigValidateBean customConfigValidateBean = JSON.parseObject(JSON.toJSONString(properties), CustomConfigValidateBean.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<CustomConfigValidateBean>> validate = validator.validate(customConfigValidateBean);
        LinkedList<String> errorMessages = new LinkedList<>();
        for (ConstraintViolation<CustomConfigValidateBean> violation : validate) {
            String configName = getConfigName(violation.getRootBeanClass(), violation.getPropertyPath().toString());
            errorMessages.add(configName + violation.getMessage());
        }
        if (errorMessages.size() > 0) {
            String msg = "Failed to get the necessary parameters, please check the configuration in the file(config/datart.conf)\nThe reasons: ";
            msg = msg + errorMessages.getFirst();
            errorMessages.removeFirst();
            errorMessages.addFirst(msg);
            Exceptions.msg(StringUtils.join(errorMessages, ", "));
        }
    }

    private String getConfigName(Class clazz, String fieldName) {
        Field field = ReflectionUtils.findRequiredField(clazz, fieldName);
        JSONField annotation = field.getAnnotation(JSONField.class);
        if (annotation != null) {
            return annotation.name();
        }
        return fieldName;
    }

    private void switchProfile(ConfigurableEnvironment environment) {
        try {
            String url = getDefaultDBUrl(environment);
            if (url == null || (url.contains("null") && environment.getProperty(CONFIG_DATABASE_URL) == null)) {
                environment.setActiveProfiles("demo");
                // remove default config propertySource
                String defaultConfig = null;
                for (PropertySource<?> propertySource : environment.getPropertySources()) {
                    if (propertySource.getName().contains("application-config")) {
                        defaultConfig = propertySource.getName();
                    }
                }
                if (defaultConfig != null) {
                    environment.getPropertySources().remove(defaultConfig);
                }
                // add demo propertySource
                List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load("demo", new ClassPathResource("application-demo.yml"));
                if (propertySources != null && propertySources.size() > 0) {
                    for (PropertySource<?> propertySource : propertySources) {
                        environment.getPropertySources().addFirst(propertySource);
                    }
                }
                System.err.println("【********* Invalid database configuration. Datart is running in demo mode *********】");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String processDBUrl(ConfigurableEnvironment environment) {
        String jdbcUrl = environment.getProperty(DATABASE_URL);
        if (!StringUtils.startsWith(jdbcUrl, "jdbc:mysql")) {
            return "";
        }
        Boolean isModify = false;
        String[] split = StringUtils.split(jdbcUrl, "?");
        if (split.length > 1) {
            Map<String, Object> urlParams = UrlUtils.getParamsMap(split[1]);
            if (!"true".equals(urlParams.getOrDefault("allowMultiQueries", "false"))) {
                isModify = true;
                urlParams.put("allowMultiQueries", "true");
            }
            if (!urlParams.containsKey("characterEncoding")) {
                isModify = true;
                urlParams.put("characterEncoding", "utf-8");
            }
            jdbcUrl = split[0] + "?" + UrlUtils.covertMapToUrlParams(urlParams);
        } else {
            isModify = true;
            jdbcUrl = jdbcUrl + "?allowMultiQueries=true&characterEncoding=utf-8";
        }
        if (isModify) {
            return jdbcUrl;
        }
        return "";
    }

    private String getDefaultDBUrl(ConfigurableEnvironment environment) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.size() > 0 && !Arrays.asList("demo", "config").containsAll(activeProfiles)) {
            // running other profiles
            return "";
        }
        try {
            List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load(DEFAULT_APPLICATION_CONFIG, new FileSystemResource(DEFAULT_APPLICATION_CONFIG));
            if (CollectionUtils.isEmpty(propertySources)) {
                System.err.println("Default config application-config not found ");
                return null;
            }
            return environment.getProperty(DATABASE_URL);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Default config application-config not found ");
        }
        return null;
    }

}
