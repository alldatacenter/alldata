# CUBE STUDIO FOR ALL DATA

```markdown

基于cube-studio二次开发的机器学习平台

```
### 整体架构


![image](https://user-images.githubusercontent.com/20157705/167534673-322f4784-e240-451e-875e-ada57f121418.png)

cube-studio 机器学习平台，目前主要包含
 - 1、数据管理：特征平台，支持在/离线特征；数据源管理，支持结构数据和媒体标注数据管理；
 - 2、在线开发：在线的vscode/jupyter代码开发；在线镜像调试，支持免dockerfile，增量构建；
 - 3、训练编排：任务流编排，在线拖拉拽；开放的模板市场，支持tf/pytorch/mxnet/spark/ray/horovod/kaldi/volcano等分布式计算/训练任务；task的单节点debug，分布式任务的批量优先级调度，聚合日志；任务运行资源监控，报警；定时调度，支持补录，忽略，重试，依赖，并发限制，定时任务算力的智能修正；
 - 4、超参搜索：nni，katib，ray的超参搜索；
 - 5、推理服务：tf/pytorch/onnx模型的推理服务，serverless流量管控，triton gpu推理加速，依据gpu利用率/qps等指标的hpa能力，虚拟化gpu，虚拟显存等服务化能力；
 - 6、资源统筹：多集群多项目组资源统筹，联邦调度，边缘计算；

# 支持模板

提示：
- 1、可自由定制任务插件，更适用当前业务需求

| 模板  | 类型 | 组件说明 |
| :----- | :---- | :---- |
| 自定义镜像 | 基础命令 | 完全自定义单机运行环境，可自由实现所有自定义单机功能 | 
| datax | 导入导出 | 异构数据源导入导出 | 
| media-download | 数据处理 | 	分布式媒体文件下载  | 
| video-audio | 数据处理 | 	分布式视频提取音频  | 
| video-img | 数据处理 | 	分布式视频提取图片  | 
| ray | 数据处理 | python ray框架 多机分布式功能，适用于超多文件在多机上的并发处理 |
| xgb | 机器学习 | xgb模型训练 |
| ray-sklearn | 机器学习 | 基于ray框架的sklearn支持算法多机分布式并行计算  |
| volcano | 数据处理 | volcano框架的多机分布式，可紫玉控制代码，利用环境变量实现多机worker的工作与协同  | 
| pytorchjob-train | 训练 | 	pytorch的多机多卡分布式训练  | 
| horovod-train | 训练 | 	horovod的多机多卡分布式训练  | 
| tfjob-train | 训练 | tf分布式训练，内部支持plain和runner两种方式  | 
| tfjob-runner | 训练 | tf分布式-runner方式  | 
| tfjob-plain | 训练 | tf分布式-plain方式  | 
| kaldi-train | 训练 | kaldi音频分布式训练  | 
| tf-model-evaluation | 模型评估 | tensorflow2.3分布式模型评估  | 
| tf-offline-predict | 离线推理 | tf模型离线推理  | 
| model-offline-predict | 离线推理 | 	分布式模型离线推理  | 
| deploy-service | 服务部署 | 部署云原生推理服务 | 

<br>
