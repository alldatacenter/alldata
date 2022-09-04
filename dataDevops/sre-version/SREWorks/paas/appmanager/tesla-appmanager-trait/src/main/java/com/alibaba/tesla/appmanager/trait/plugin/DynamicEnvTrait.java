package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.CommandUtil;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态环境变量 Trait (for 专有云)
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class DynamicEnvTrait extends BaseTrait {

    public DynamicEnvTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        String cluster = getSpec().getString("cluster");
        if (StringUtils.isEmpty(cluster)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "cannot find cluster in dynamic env trait spec");
        }
        WorkloadResource workloadResource = getWorkloadRef();
        File envFile = null;
        try {
            // 导出自定义环境变量文件
            envFile = Files.createTempFile("env", ".json").toFile();
            String command = String.format("abmcli -o json deployment env list --cluster %s > %s",
                cluster, envFile.getAbsolutePath());
            String output = CommandUtil.runLocalCommand(command);
            String envFileStr = new String(Files.readAllBytes(envFile.toPath()), StandardCharsets.UTF_8);
            log.info("get dynamic env success|command={}|output={}|envFile={}", command, output, envFileStr);

            // 提取环境变量
            JSONObject envFileJson = JSONObject.parseObject(envFileStr);
            JSONObject variables = envFileJson.getJSONObject("variables");
            Map<String, String> envMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = entry.getKey();
                String value = String.valueOf(entry.getValue());
                envMap.put(key, value);
            }

            // 将变量覆盖 workload 中的同名 spec.env 对象
            JSONObject workloadSpec = (JSONObject) workloadResource.getSpec();
            workloadSpec.getJSONObject("env").putAll(envMap);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                String.format("cannot get custom env json file|exception=%s", ExceptionUtils.getStackTrace(e)));
        } finally {
            if (envFile != null) {
                if (!envFile.delete()) {
                    log.warn("cannot delete env file in dynamic env trait");
                }
            }
        }
    }
}
