# --- 备用文档4.2.3 运维后端开发

<a name="Kwm77"></a>
## 1 概述
<a name="eOwsz"></a>
## 2 组件开发
<a name="dNMZI"></a>
### 2.1 微服务开发
<a name="xITzT"></a>
### 2.2 helm组件包引入
<a name="tTrck"></a>
## 3 应用构建
<a name="Pciwl"></a>
## 4 应用部署
<a name="gWI8G"></a>
## 5 应用测试
<a name="AXnPL"></a>
## 6 运维应用市场
<a name="TxRmw"></a>
## 7 服务管理


---


纯前端的运维应用配置
```yaml
apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
spec:
  parameterValues:
    - name: CLUSTER_ID
      value: master
  components: 
  - dataInputs: []
    dataOutputs: []
    dependencies: []
    revisionName: INTERNAL_ADDON|productopsv2|_
    scopes:
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Namespace
        name: sreworks
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Stage
        name: 'prod'
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Cluster
        name: 'master'
    parameterValues:
    - name: TARGET_ENDPOINT
      value: "prod-flycore-paas-action"
      toFieldPaths:
        - spec.targetEndpoint
    - name: STAGE_ID
      value: 'prod'
      toFieldPaths:
        - spec.stageId

```
