package com.alibaba.tesla.appmanager.domain.schema;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * 应用部署单 Schema 定义 (yaml 转换)
 * <p>
 * 参考文档: https://yuque.antfin.com/bdsre/xp2xoa/aglzrz
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Data
public class DeployAppSchema implements Schema, Serializable {

    private static final long serialVersionUID = -5036403088019645161L;

    /**
     * API 版本号
     */
    private String apiVersion;

    /**
     * 类型
     */
    private String kind;

    /**
     * 元信息
     */
    private MetaData metadata;

    /**
     * 定义描述文件
     */
    private Spec spec;

    /**
     * 元信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaData implements Serializable {

        private static final long serialVersionUID = 3099676869740322981L;

        private String name;
        private MetaDataAnnotations annotations;
    }

    /**
     * 元信息扩展信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaDataAnnotations implements Serializable {

        private static final long serialVersionUID = 3787386043228166939L;

        /**
         * 目标部署单元 ID，可为空
         */
        private String unitId = "";

        /**
         * 集群 ID，可为空
         */
        private String clusterId = "";

        /**
         * Namespace ID, 可为空
         */
        private String namespaceId = "";

        /**
         * Stage ID, 可为空
         */
        private String stageId = "";

        /**
         * 应用 ID
         */
        private String appId;

        /**
         * 应用实例名称
         */
        private String appInstanceName = "";

        /**
         * 应用包 ID
         */
        private Long appPackageId = 0L;

        /**
         * 元信息版本
         */
        private String appPackageVersion;

        /**
         * ImageTar 替换对象 (将各个 ComponentSchema 对象中的 image 替换为 actualImage)
         * <p>
         * 示例 [{"image": "reg.docker.alibaba-inc.com/abm-aone/a:b", "actualImage": "reg.env.com/abm/c:d"}]
         */
        private String imageTars = "";
    }

    @Data
    public static class DataOutput implements Serializable {

        private static final long serialVersionUID = 2150471143237594995L;

        /**
         * 产出变量名称
         */
        private String name;

        /**
         * 产出变量来源路径
         */
        private String fieldPath;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataInput implements Serializable {

        private static final long serialVersionUID = -453709853116435642L;

        /**
         * 来源变量
         */
        private DataInputValueFrom valueFrom;

        /**
         * 目标变量置入位置定位
         */
        private List<String> toFieldPaths = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dependency implements Serializable {

        private static final long serialVersionUID = -6716060765887913097L;

        /**
         * 依赖组件 ($COMPONENT_TYPE|$COMPONENT_NAME)
         */
        private String component;

        /**
         * 条件
         */
        private String condition;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterValue implements Serializable {

        private static final long serialVersionUID = -1952333694105350537L;

        /**
         * 变量值 Key
         */
        private String name;

        /**
         * 变量值 Value
         */
        private Object value = "";

        /**
         * 当前变量设置的目标地址
         */
        private List<String> toFieldPaths = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataInputValueFrom implements Serializable {

        private static final long serialVersionUID = -7599464970568861088L;

        /**
         * 数据来源输出变量名称
         */
        private String dataOutputName;
    }

    /**
     * Component - 部署目标 scope
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecComponentScopeRef implements Serializable {

        private static final long serialVersionUID = 4844472266972884506L;

        /**
         * scope API 版本
         */
        private String apiVersion;

        /**
         * scope 类型
         */
        private String kind;

        /**
         * scope 名称
         */
        private String name;

        /**
         * Spec
         */
        private JSONObject spec;
    }

    /**
     * Component - 部署目标 scope 引用
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecComponentScope implements Serializable {

        private static final long serialVersionUID = 4849226311267227547L;

        private SpecComponentScopeRef scopeRef;
    }

    /**
     * Component - 相关 trait
     */
    @Data
    public static class SpecComponentTrait implements Serializable {

        private static final long serialVersionUID = -3392792150615216023L;

        /**
         * 引用的 trait 唯一标识名称
         */
        private String name;

        /**
         * trait 在 component 的运行前还是运行后, available: pre, post
         */
        private String runtime;

        /**
         * trait 配置参数
         */
        private JSONObject spec;

        /**
         * trait 输入 input 变量列表
         */
        private List<DataInput> dataInputs = new ArrayList<>();

        /**
         * trait 产出 output 变量列表
         */
        private List<DataOutput> dataOutputs = new ArrayList<>();

        /**
         * component 部署覆盖变量列表
         */
        private List<ParameterValue> parameterValues = new ArrayList<>();

        /**
         * 获取 trait 唯一标识 ID
         *
         * @param componentRevisionContainer trait parent component revision
         * @return unique id
         */
        @JSONField(serialize = false)
        public String getUniqueId(DeployAppRevisionName componentRevisionContainer) {
            DeployAppRevisionName container = DeployAppRevisionName.builder()
                    .componentType(ComponentTypeEnum.TRAIT_ADDON)
                    .componentName(String.join("~", Arrays.asList(
                            componentRevisionContainer.getComponentType().toString(),
                            componentRevisionContainer.getComponentName(),
                            name
                    )))
                    .version("_")
                    .build();
            return container.revisionName();
        }
    }

    /**
     * Workflow Step
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowStep implements Serializable {

        private static final long serialVersionUID = 5432694768211587402L;

        /**
         * Workflow 任务类型
         */
        private String type;

        /**
         * Workflow 任务运行时机 (pre-render/post-render/post-deploy)
         */
        private String stage;

        /**
         * Workflow 任务运行参数
         */
        private JSONObject properties = new JSONObject();
    }

    /**
     * Workflow 定义
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Workflow implements Serializable {

        private static final long serialVersionUID = 196441839737994761L;

        /**
         * Workflow 任务步骤列表
         */
        private List<WorkflowStep> steps = new ArrayList<>();
    }

    /**
     * Policy 定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Policy implements Serializable {

        private static final long serialVersionUID = 3453423526119849890L;

        /**
         * Policy 类型
         */
        private String type;

        /**
         * Policy 名称
         */
        private String name;

        /**
         * Policy 配置
         */
        private JSONObject properties = new JSONObject();
    }

    /**
     * Component 定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecComponent implements Serializable {

        private static final long serialVersionUID = 5887854562121408752L;

        /**
         * revision 名称 (格式：$componentType|$componentName|$version)
         */
        private String revisionName;

        /**
         * component 部署目标 scope 列表
         */
        private List<SpecComponentScope> scopes = new ArrayList<>();

        /**
         * component 关联 trait 列表
         */
        private List<SpecComponentTrait> traits = new ArrayList<>();

        /**
         * component 依赖 input 变量列表
         */
        private List<DataInput> dataInputs = new ArrayList<>();

        /**
         * component 产生 output 变量列表
         */
        private List<DataOutput> dataOutputs = new ArrayList<>();

        /**
         * 依赖组件列表
         */
        private List<Dependency> dependencies = new ArrayList<>();

        /**
         * component 部署覆盖变量列表
         */
        private List<ParameterValue> parameterValues = new ArrayList<>();

        /**
         * 获取唯一定位 ID
         *
         * @return 唯一定位 ID
         */
        @JSONField(serialize = false)
        public String getUniqueId() {
            return revisionName;
        }

        /**
         * 获取镜像唯一定位 ID
         *
         * @return 镜像唯一定位 ID
         */
        @JSONField(serialize = false)
        public String getMirrorUniqueId() {
            return DefaultConstant.MIRROR_COMPONENT_PREFIX + revisionName;
        }

        /**
         * 从 Scope 中获取 NamespaceId
         *
         * @return namespaceId, 如果不存在则返回 null
         */
        @JSONField(serialize = false)
        public String getNamespaceId() {
            for (SpecComponentScope scope : scopes) {
                SpecComponentScopeRef ref = scope.getScopeRef();
                if (!"Namespace".equals(ref.getKind())) {
                    continue;
                }
                return ref.getName();
            }
            return "";
        }

        /**
         * 在 Scope 中设置 Namespace ID
         *
         * @param namespaceId Namespace ID
         */
        @JSONField(serialize = false)
        public void setNamespaceId(String namespaceId) {
            boolean found = false;
            for (SpecComponentScope scope : scopes) {
                SpecComponentScopeRef ref = scope.getScopeRef();
                if (!"Namespace".equals(ref.getKind())) {
                    continue;
                }
                ref.setName(namespaceId);
                found = true;
            }
            if (!found) {
                scopes.add(SpecComponentScope.builder()
                        .scopeRef(SpecComponentScopeRef.builder()
                                .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                                .kind("Namespace")
                                .name(namespaceId)
                                .build())
                        .build());
            }
        }

        /**
         * 从 Scope 中获取 ClusterId
         *
         * @return clusterId, 如果不存在则返回 null
         */
        @JSONField(serialize = false)
        public String getClusterId() {
            for (SpecComponentScope scope : scopes) {
                SpecComponentScopeRef ref = scope.getScopeRef();
                if (!"Cluster".equals(ref.getKind())) {
                    continue;
                }
                return ref.getName();
            }
            return "";
        }

        /**
         * 在 Scope 中设置 ClusterId
         *
         * @param clusterId 集群 ID
         */
        @JSONField(serialize = false)
        public void setClusterId(String clusterId) {
            boolean found = false;
            for (SpecComponentScope scope : scopes) {
                SpecComponentScopeRef ref = scope.getScopeRef();
                if (!"Cluster".equals(ref.getKind())) {
                    continue;
                }
                ref.setName(clusterId);
                found = true;
            }
            if (!found) {
                scopes.add(SpecComponentScope.builder()
                        .scopeRef(SpecComponentScopeRef.builder()
                                .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                                .kind("Cluster")
                                .name(clusterId)
                                .build())
                        .build());
            }
        }

        /**
         * 从 Scope 中获取 StageId
         *
         * @return stageId, 如果不存在则返回 null
         */
        @JSONField(serialize = false)
        public String getStageId() {
            for (SpecComponentScope scope : scopes) {
                SpecComponentScopeRef ref = scope.getScopeRef();
                if (!"Stage".equals(ref.getKind())) {
                    continue;
                }
                return ref.getName();
            }
            return "";
        }

        /**
         * 在 Scope 中设置 StageID
         *
         * @param stageId Stage ID
         */
        @JSONField(serialize = false)
        public void setStageId(String stageId) {
            boolean found = false;
            for (SpecComponentScope scope : scopes) {
                SpecComponentScopeRef ref = scope.getScopeRef();
                if (!"Stage".equals(ref.getKind())) {
                    continue;
                }
                ref.setName(stageId);
                found = true;
            }
            if (!found) {
                scopes.add(SpecComponentScope.builder()
                        .scopeRef(SpecComponentScopeRef.builder()
                                .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                                .kind("Stage")
                                .name(stageId)
                                .build())
                        .build());
            }
        }

        /**
         * 获取指定参数名称对应的参数值
         *
         * @param name 参数 Key
         * @return Value
         */
        @JSONField(serialize = false)
        public Object getParameterValue(String name) {
            assert !StringUtils.isEmpty(name);
            for (ParameterValue item : parameterValues) {
                if (name.equals(item.getName())) {
                    return item.getValue();
                }
            }
            return null;
        }
    }

    /**
     * Spec 定义
     */
    @Data
    public static class Spec implements Serializable {

        private static final long serialVersionUID = 3058552796974878875L;

        /**
         * Components 引用及配置描述列表
         */
        private List<SpecComponent> components = new ArrayList<>();

        /**
         * 全局变量
         */
        private List<ParameterValue> parameterValues = new ArrayList<>();

        /**
         * Workflow
         */
        private Workflow workflow = new Workflow();

        /**
         * 全局策略
         */
        private List<Policy> policies = new ArrayList<>();
    }
}
