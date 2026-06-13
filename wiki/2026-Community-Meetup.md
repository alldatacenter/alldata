# 26年4月30日-AIIData数据中台年度技术直播分享-会议纪要

本次会议纪要涵盖了AIIData数据采集 - 开发 - 治理 - AI 应用全链路闭环能力，同步讲解项目开源成长历程、商业化落地规模与底层技术架构。产品高度适配国产化信创环境，覆盖多行业业务场景，开源社区活跃度高，后续重点迭代 AI、物联网、多模态存储核心能力。

**👤 演讲人：巫林壕（杭州奥零数据科技公司创始人 & 架构师、AIIData开源数据中台作者）**

<img width="1341" height="754" alt="image" src="https://github.com/user-attachments/assets/a6ed97bf-5ae1-4923-afe6-8cb8caf059ca" />



### 一、公司及开源项目发展历程

**1、企业发展历程**

AllData开源项目始于2019年10月，至今维护超 6 年半，GitHub 拥有 3000+ Star、914 个 Fork；商业公司杭州奥零数据科技成立于2023年10月。目前已服务160+家企业用户，运营28个技术社群，社区成员3500+。

<img width="1345" height="755" alt="image" src="https://github.com/user-attachments/assets/6db5bfdb-796f-44e9-91a2-fd04388a44d8" />


**2、AllData可定义数据中台架构设计**

● 前后端框架：前端采用腾讯开源无界框架，后端基于 Spring Cloud 自研 OneFlow可插拔架构。

● 完成全栈国产化适配：支持麒麟操作系统、OceanBase、达梦等国产数据库；兼容 x86、ARM（鲲鹏）等 CPU 架构，适配国产化软硬件环境。

<img width="3094" height="2358" alt="image" src="https://github.com/user-attachments/assets/d567b74a-b713-470c-8d26-66de3601e9f8" />



### 二、商业版产品功能与开源组件集成
演示并介绍了近期新发布功能平台模块，2026 年 3-4 月先后推出物联网数据平台、实时数仓、数据资产等模块，整体已有1大支持国产信创 + 8大功能完成开源项目适配集成。

**1、支持国产信创**

<img width="2730" height="1535" alt="image" src="https://github.com/user-attachments/assets/636cdc96-6812-4d54-99ed-7ebf4783942c" />


**2、数仓建模平台——集成Kylin，支持PB级预计算与多维分析**

<img width="4480" height="2294" alt="image" src="https://github.com/user-attachments/assets/db0fec7f-985b-460b-8456-f7ba625462b6" />

<img width="4480" height="2294" alt="image" src="https://github.com/user-attachments/assets/41ac9849-eaca-421e-a47f-91ae3d4db926" />

<img width="4480" height="2294" alt="image" src="https://github.com/user-attachments/assets/cf589339-506c-4e5b-8484-aa2e667cc034" />


**3、数据资产平台——集成OpenDataWorks，统一指标/权限/服务，含血缘图谱**

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/1014d920-d33d-4fc4-a03a-17fc63fe931f" />

<img width="4480" height="2290" alt="image" src="https://github.com/user-attachments/assets/046bbdd1-593e-4404-963e-433c6736be9b" />

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/a1312554-5f98-45b7-abd5-0c6ce4f9f630" />


**4、物联网数据库平台——集成Apache IoTDB Web，管理工业时序数据**

<img width="2730" height="1976" alt="image" src="https://github.com/user-attachments/assets/3a2f8477-ab37-4c86-ae3c-d14df5092403" />

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/af712c3c-dae3-4c6e-9fcb-9746c65b444a" />

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/fbbe4529-f376-466e-a634-ad89315f19ac" />


**5、实时数仓平台——集成Apache Doris，支持MySQL协议兼容、亚秒级查询**

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/f1b8d879-8634-480d-85ab-302112b130c3" />

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/e596b7f8-3c42-47fe-9ba2-e124d134132d" />

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/38cc0c63-ca0c-4c5b-9b76-214b9a2d092c" />


**6、元数据管理平台——集成OpenMetaData，自动数据血缘追踪（表/字段级）**

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/b8d4e4a3-55ae-4413-aa72-7667b4527066" />

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/f0097755-f793-4f56-b007-fac73f2b4161" />

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/21a58865-16f4-4039-a561-507adf275de3" />


**7、离线开发IDE——集成AirFlow，可视化任务编排**

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/191caf76-365e-4c69-a639-3093293a38fe" />

<img width="4480" height="2288" alt="image" src="https://github.com/user-attachments/assets/18dadd58-81c7-46de-8910-6d525ba0a2b7" />

<img width="4480" height="2290" alt="image" src="https://github.com/user-attachments/assets/b264a6df-35e7-4412-89ee-b60abb6b648f" />


**8、多模态数据存储——集成RustFS，统一存储文件/图像/音视频等非结构化数据**

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/aa786816-bd6a-4d6d-a1b9-ae838368401f" />

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/0be3fe7a-ccbf-4def-8081-060f7ed3a353" />

<img width="4480" height="2284" alt="image" src="https://github.com/user-attachments/assets/abda00e8-4a9b-4fd7-b9b7-a33e5c9f322b" />


**9、高质量数据集平台——集成DataFlow，面向AI模型训练的数据清洗、标注、脱敏管理**

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/bc64b03e-7ca9-467a-b4df-65991426f11e" />

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/869883a9-1999-435c-96ad-775e6eca16c3" />

<img width="4480" height="2292" alt="image" src="https://github.com/user-attachments/assets/2df12482-ec02-4c9b-a4b2-5b96a6b88eae" />


### 三、AI与大模型融合方向
1、AllData定位为"数据平台底座 + 数据中台桥梁 + ML平台中层 + 大模型应用上游"。

2、已规划机器学习算法平台、大模型应用开发平台，支持NL2SQL智能问数、智能报表解读等。


### 四、项目打造与创业经验分享
**1、开源路线与社区共建**：保留Apache/BSD/MIT等原开源协议Notice与License，逐步将部分商业功能回馈开源社区。

**2、产品打造思路**：参考行业主流数据中台产品，采用精益创业、快速迭代模式；坚持 “先跑通最小流程（MVP），再逐步优化”，遵循 “可用→好用→规模化” 的产品演进路径。

**3、团队理念**：深耕开源社区，长期坚持技术打磨；划分精力配比，兼顾社区运营、开源项目迭代与商业版落地。

**4、行业选择**：紧跟数字化、数据要素、信创等行业趋势，产品落地覆盖金融、医疗、制造业、政务、水利等多个领域。
