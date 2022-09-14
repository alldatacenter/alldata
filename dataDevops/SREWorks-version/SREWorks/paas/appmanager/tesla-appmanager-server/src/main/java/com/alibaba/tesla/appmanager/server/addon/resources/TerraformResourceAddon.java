package com.alibaba.tesla.appmanager.server.addon.resources;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.service.GitService;
import com.alibaba.tesla.appmanager.common.util.AddonUtil;
import com.alibaba.tesla.appmanager.common.util.CommandUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.req.git.GitCloneReq;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.server.addon.BaseAddon;
import com.alibaba.tesla.appmanager.server.addon.req.ApplyAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.req.CheckAddonInstanceExpiredReq;
import com.alibaba.tesla.appmanager.server.addon.res.ApplyAddonRes;
import com.alibaba.tesla.appmanager.server.event.loader.AddonLoadedEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Terraform - 资源 Addon
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component("TerraformResourceAddon")
public class TerraformResourceAddon extends BaseAddon {

    @Getter
    private final ComponentTypeEnum addonType = ComponentTypeEnum.RESOURCE_ADDON;

    @Getter
    private final String addonId = "terraform";

    @Getter
    private final String addonVersion = "1.0";

    @Getter
    private final String addonLabel = "Terraform";

    @Getter
    private final String addonDescription = "Terraform";

    @Getter
    private final ComponentSchema addonSchema = SchemaUtil.toSchema(ComponentSchema.class, "\n" +
            "apiVersion: core.oam.dev/v1alpha2\n" +
            "kind: Component\n" +
            "metadata:\n" +
            "  annotations: {}\n" +
            "  name: RESOURCE_ADDON.terraform\n" +
            "spec:\n" +
            "  workload:\n" +
            "    apiVersion: abm.io/v1\n" +
            "    kind: Terraform\n" +
            "    metadata:\n" +
            "      annotations: {}\n" +
            "      name: terraform\n" +
            "    spec:\n" +
            "      environments: []\n" +
            "      configuration:\n" +
            "        git:\n" +
            "          repo: ''\n" +
            "          ciAccount: ''\n" +
            "          ciToken: ''\n" +
            "          branch: ''\n" +
            "          repoPath: ''\n" +
            "      output: {}\n" +
            "      tfstate: ''\n" +
            "    dataOutputs: []\n");

    @Getter
    private final String addonConfigSchema = "";

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private GitService gitService;

    /**
     * 初始化，注册自身
     */
    @PostConstruct
    public void init() {
        publisher.publishEvent(new AddonLoadedEvent(
                this, AddonUtil.combineAddonKey(getAddonType(), getAddonId()), this.getClass().getSimpleName()));
    }

    /**
     * 检查 Addon 实例是否过期 (过期意味着需要重新 applyInstance)
     *
     * @param request 检查请求
     * @return true or false
     */
    @Override
    public boolean checkExpired(CheckAddonInstanceExpiredReq request) {
        return true;
    }

    /**
     * 申请 Addon 实例
     *
     * @param request 创建请求
     * @return addonInstanceId
     */
    @Override
    public ApplyAddonRes apply(ApplyAddonInstanceReq request) {
        ComponentSchema lastSchema = request.getLastSchema();
        ComponentSchema schema = request.getSchema();
        String namespaceId = request.getNamespaceId();
        String addonId = request.getAddonId();
        String addonName = request.getAddonName();
        Map<String, String> addonAttrs = request.getAddonAttrs();
        String logSuffix = String.format("namespaceId=%s|addonId=%s|addonName=%s|addonAttrs=%s", namespaceId,
                addonId, addonName, JSONObject.toJSONString(addonAttrs));

        // 克隆 Git 仓库到本地
        JSONObject spec = (JSONObject) schema.getSpec().getWorkload().getSpec();
        Path gitDir;
        try {
            gitDir = Files.createTempDirectory("terraform_");
            gitDir.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot create terraform git temp directory", e);
        }
        cloneGitRepo(gitDir, spec);
        log.info("the git repo has cloned in terraform addon|gitRequest={}|{}",
                JSONObject.toJSONString(request), logSuffix);

        // 如果之前存在 ComponentSchema，那么需要解析出之前的 terraform.tfstate 文件
        Path tfstatePath = Paths.get(gitDir.toString(), "terraform.tfstate");
        if (lastSchema != null) {
            String originTfstate = ((JSONObject) lastSchema.getSpec().getWorkload().getSpec()).getString("tfstate");
            if (StringUtils.isNotEmpty(originTfstate)) {
                try {
                    Files.writeString(tfstatePath,
                            ((JSONObject) lastSchema.getSpec().getWorkload().getSpec()).getString("tfstate"),
                            StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                            String.format("cannot write terraform.tfstate content|%s|path=%s", logSuffix, tfstatePath));
                }
            }
        }

        // 准备环境变量并执行命令
        Map<String, String> environments = getEnvironments(spec);
        String applyCommand = String.format("cd %s; /app/terraform apply -auto-approve -no-color", gitDir);
        String applyResult = CommandUtil.runLocalCommand(applyCommand, environments);
        log.info("'{}' has been executed in dir {}|{}|result={}", applyCommand, gitDir, logSuffix, applyResult);

        String outputCommand = String.format("cd %s; /app/terraform output -no-color -json", gitDir);
        String outputResult = CommandUtil.runLocalCommand(outputCommand, environments);
        log.info("'{}' has been executed in dir {}|{}|result={}", outputCommand, gitDir, logSuffix, outputResult);

        // 解析返回 JSON 输出
        spec.getJSONObject("output").putAll(JSONObject.parseObject(outputResult));

        // 获取当前的 terraform.tfstate 文件内容
        String tfstate;
        try {
            tfstate = new String(Files.readAllBytes(tfstatePath));
        } catch (Exception e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot get terraform.tfstate content|%s|path=%s", logSuffix, tfstatePath));
        }
        spec.put("tfstate", tfstate);

        // 获取当前 signature
        String hashCommand = String.format("cd %s; git rev-parse HEAD", gitDir);
        String signature = CommandUtil.runLocalCommand(hashCommand);

        // 清理目录并返回
        try {
            FileUtils.deleteDirectory(gitDir.toFile());
        } catch (Exception ignored) {
            log.warn("cannot delete directory after terraform finished|directory={}", gitDir);
        }
        return ApplyAddonRes.builder()
                .componentSchema(schema)
                .signature(signature)
                .build();
    }

    /**
     * 获取 spec 中定义的环境变量
     *
     * @param spec Spec
     * @return 环境变量
     */
    private Map<String, String> getEnvironments(JSONObject spec) {
        Map<String, String> result = new HashMap<>();
        JSONArray specEnv = spec.getJSONArray("environments");
        if (specEnv != null) {
            for (JSONObject env : specEnv.toJavaList(JSONObject.class)) {
                String name = env.getString("name");
                String value = env.getString("value");
                if (StringUtils.isAnyEmpty(name, value)) {
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid environments in terraform spec");
                }
                result.put(name, value);
            }
        }
        return result;
    }

    /**
     * 克隆 Git 仓库到本地目录
     *
     * @param spec Spec
     */
    private void cloneGitRepo(Path gitDir, JSONObject spec) {
        if (!spec.containsKey("configuration") || !spec.getJSONObject("configuration").containsKey("git")) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid terraform workflow spec");
        }
        JSONObject gitInfo = spec.getJSONObject("configuration").getJSONObject("git");
        StringBuilder logContent = new StringBuilder();
        GitCloneReq cloneReq = GitCloneReq.builder()
                .repo(gitInfo.getString("repo"))
                .branch(gitInfo.getString("branch"))
                .repoPath(gitInfo.getString("repoPath"))
                .ciAccount(gitInfo.getString("ciAccount"))
                .ciToken(gitInfo.getString("ciToken"))
                .keepGitFiles(true)
                .build();
        gitService.cloneRepo(logContent, cloneReq, gitDir);

    }
}
