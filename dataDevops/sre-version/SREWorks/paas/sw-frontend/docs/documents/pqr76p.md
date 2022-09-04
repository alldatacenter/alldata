# --- 备用文档1.5 组件清单

当使用快速安装后，将会安装的组件及基本信息如下：
<a name="s6fcu"></a>
## 平台框架组件
| 组件 | 版本 | 说明 |
| --- | --- | --- |
| sw-appmanager | 1.0 | 应用管理服务，托管其他各个组件 |
| sw-frontend | 1.0 | 前端基线工程 |
| sw-frontend-service | 1.0 | 操作管理服务、配置管理服务，包含action |
| sw-authproxy | 1.0 | 权限代理服务 |
| sw-gateway | 1.0 | 网关服务 |

<a name="6HZsz"></a>
## 平台依赖中间件
包含的基础中间件如下：

| 组件 | 版本 |
| --- | --- |
| MySQL | 5.7 |
| Elasticsearch | 6.8 |
| Kafka | <br /> |
| ZooKeeper | 3.5.8 |
| Redis | 6.0 |
| MinIO | RELEASE.2021-01-05T05-22-38Z |
| Nacos | Nacos服务 |


<a name="mfovh"></a>
## 内置运维应用（服务）
场景的运维中台服务/应用：

| 应用/服务 | 版本 | 说明 |
| --- | --- | --- |
| saas-health | 1.0 | 健康服务 |
| saas-cluster | 1.0 | 集群管理 |
| saas-team | 1.0 | 团队管理 |
| saas-job | 1.0 | 作业平台 |
| saas-dataops | 1.0 | 数据运维平台 |
| saas-aiops | 1.0 | 智能运维平台 |
| saas-tkgone | 1.0 | 运维知识图谱 |


<a name="FdAVr"></a>
## 资源清单
| 名称 | 命名空间 | 计算 | 内存 |
| --- | --- | --- | --- |
| sw-appmanager | sreworks | 2C | 6G |
| sw-frontend | sreworks | 0.5C | 0.5G |
| sw-frontend-service | sreworks | 0.5C | 2G |
| sw-authproxy | sreworks | 0.5C | 1G |
| sw-gateway | sreworks | 0.5C | 1G |
| mysql | sreworks | 0.5C | 1G |
| kafka | sreworks | 0.5C | 4G |
| zookeeper | sreworks | 0.5C | 1G |
| minio | sreworks | 0.5C | 80G |
| redis | sreworks | 0.5C | 1G |
| nacos | sreworks | 0.5C | 1G |
| saas-job | sreworks | 2C | 8G |
| saas-team | sreworks | 0.5C | 2G |
| saas-app | sreworks | 0.5C | 2G |
| saas-cluster | sreworks | 0.5C | 4G |
| saas-file | sreworks | 0.5C | 2G |
| elasticsearch | sreworks-dataops | 2C | 8G |
| filebeat | sreworks-dataops | 0.1C*机器数 | 500M*机器数 |
| metricbeat | sreworks-dataops | 0.1C*机器数 | 500M*机器数 |
| mysql | sreworks-dataops | 0.5C | 1G |
| pmdb | sreworks-dataops | 0.5C | 1G |
| cmdb | sreworks-dataops | <br /> |  |
| skywalking | sreworks-dataops | 0.5C | 4G |
| vvp-flink | sreworks-dataops | 5C | 40G |
| dataset | sreworks-dataops | 0.5C | 1G |
| aiops | sreworks-aiops | 1C | 8G |

| 命名空间 | 计算 | 内存 |
| --- | --- | --- |
| sreworks | 11C | 116.5G(可以减到40G左右) |
| sreworks-dataops | 11C | 65G |
| sreworks-aiops | 6C | 8G |

