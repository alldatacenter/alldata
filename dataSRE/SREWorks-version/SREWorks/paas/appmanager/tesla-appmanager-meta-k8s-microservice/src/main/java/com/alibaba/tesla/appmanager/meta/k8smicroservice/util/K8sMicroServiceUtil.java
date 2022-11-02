package com.alibaba.tesla.appmanager.meta.k8smicroservice.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * K8S Microservice 工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class K8sMicroServiceUtil {

    /**
     * 获取当前的 K8S Microservice Options (build.yaml) 并替换其中的 branch / imagePush 对象
     *
     * @param branch 实际分支
     * @return 修改后的 Options 对象 (build.yaml)
     */
    public static JSONObject replaceOptionsBranch(
            ComponentTypeEnum componentType, String options, String branch,
            String dockerRegistry, String dockerNamespace) {
        Yaml yaml = SchemaUtil.createYaml(JSONObject.class);
        JSONObject root = yaml.loadAs(options, JSONObject.class);
        JSONObject optionsJson = root.getJSONObject("options");
        optionsJson.putIfAbsent("kind", DefaultConstant.DEFAULT_K8S_MICROSERVICE_KIND);

        switch (componentType) {
            case K8S_JOB:
                JSONObject job = optionsJson.getJSONObject("job");
                if (job != null) {
                    JSONObject build = job.getJSONObject("build");
                    if (StringUtils.isNotEmpty(branch)) {
                        build.put("branch", branch);
                    }

                    if (!build.containsKey("imagePush")) {
                        addImagePushProperties(build, dockerRegistry, dockerNamespace);
                    }
                }
                break;
            case K8S_MICROSERVICE:
                JSONArray containers = optionsJson.getJSONArray("containers");
                if (CollectionUtils.isNotEmpty(containers)) {
                    for (int i = 0; i < containers.size(); i++) {
                        JSONObject container = containers.getJSONObject(i);
                        JSONObject build = container.getJSONObject("build");
                        if (StringUtils.isNotEmpty(branch)) {
                            build.put("branch", branch);
                        }
                        if (!build.containsKey("imagePush")) {
                            addImagePushProperties(build, dockerRegistry, dockerNamespace);
                        }
                    }
                }

                JSONArray initContainers = optionsJson.getJSONArray("initContainers");
                if (CollectionUtils.isNotEmpty(initContainers)) {
                    for (int i = 0; i < initContainers.size(); i++) {
                        JSONObject initContainer = initContainers.getJSONObject(i);
                        JSONObject build = initContainer.getJSONObject("build");
                        if (StringUtils.isNotEmpty(branch)) {
                            build.put("branch", branch);
                        }
                        if (!build.containsKey("imagePush")) {
                            addImagePushProperties(build, dockerRegistry, dockerNamespace);
                        }
                    }
                }
                break;
            default:
                break;
        }
        return optionsJson;
    }

    /**
     * 增加 imagePush 配置
     *
     * @param build 构建对象
     * @param dockerRegistry Docker Registry
     * @param dockerNamespace Docker Namespace
     */
    private static void addImagePushProperties(JSONObject build, String dockerRegistry, String dockerNamespace) {
        build.put("imagePush", true);
        build.put("imagePushRegistry", String.format("%s/%s", dockerRegistry, dockerNamespace));
    }
}
