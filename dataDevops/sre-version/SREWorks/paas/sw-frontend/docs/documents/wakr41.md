# --- 备用文档3.4 智能运维服务

      智能运维平台依托数据平台能力,结合大数据沉淀的智能运维场景,覆盖了包括异常检测(监控管理)、时序预测(资源管理)以及聚类服务等多种智能运维场景,为企业级用户提供智能化的IT运维服务，助力企业运维进入智能化时代，为业务发展保驾护航。
<a name="a2oyf"></a>
## 1，功能简介
<a name="FBsaU"></a>
### 1.1 算法库

- 统计预测 SES、 ESD、 STL、 T-Test
- 自然语言处理 Word2Vec、 AFT Tree、 TFIDF
- 分类决策 XGBoost、 MILP、 DB SCAN
<a name="HJOHf"></a>
### 1.2 模型管理

- 模型管理: 包括模型训练、模型上线\下线等
- 模型测试:
<a name="BaRSE"></a>
### 1.3 智能化服务

- 时序分析: 时序预测、异常检测、线性规划
- 文本处理: 聚类服务、知识推荐
<a name="YhfYS"></a>
## 2，核心优势
<a name="fhHqI"></a>
### 2.1 kubeflow与arena
      开源工具Arena诞生于阿里云的深度学习解决方案。支持多种学习框架（如TensorFlow，Caffe，Hovorod和Pytorch等）。深度整合云资源和服务。有效利用CPU和GPU等异构资源，屏敝 Kubernetes、Tensorflow、Caffe, 用户只需要关注算法本身。<br />      Arena 负责提供业界最佳实践。Arena 是一个高效的工具，支持 AI 作业的运维，支持监控集群中GPU的分配情况以及任务使用情况、任务和训练日志的查看等。
<a name="jPlBP"></a>
### 2.2 数据平台基座
      打通数据平台的数据集服务, 为智能平台提供数据支持, 以场景数据驱动智能平台的建设。
<a name="tWsRa"></a>
### 2.3 内置服务
      基于Arena方案, 结合大数据沉淀的智能运维场景, SREWorks内置了部分智能化服务, 包括但不限于异常检测<br />、时序预测以及模式聚类等, 随着版本迭代, 我们会集成更多的内置智能服务。<br />      内置智能服务,我们会提供openapi, 方便用户接入和使用
<a name="QXSfU"></a>
## 3，平台架构
<a name="EZYow"></a>
## ![image.png](https://intranetproxy.alipay.com/skylark/lark/0/2021/png/227744/1626334020847-629e846b-be4f-481a-af60-f7bdbb541fdb.png#clientId=ucc0df092-be7d-4&from=paste&height=439&id=u4dc6815b&margin=%5Bobject%20Object%5D&name=image.png&originHeight=439&originWidth=785&originalType=binary&ratio=1&size=62543&status=done&style=none&taskId=ubb1f72ec-7afe-474f-ae5f-1ec915c4c8c&width=785)
<a name="hZGa4"></a>
## 4，快速入门
后续补充指标异常检测或指标时序预测的接入说明,结合页面描述
<a name="rHKiF"></a>
## 5，场景案例
<a name="GONtb"></a>
### 5.1 预测服务
<a name="IPFZl"></a>
### 5.2 异常检测
<a name="AhLIX"></a>
## 6，二次开发
用户需要本地安装arena客户端, 参考arena的cli工具文档, 按需提交AI训练任务或者AI智能服务。<br />需要掌握arena-cli的使用
<a name="x20Bw"></a>
## 7，FAQ
