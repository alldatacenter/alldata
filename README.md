# AllData数据中台

### [官方文档](https://alldata.readthedocs.io) ｜ [安装文档](https://github.com/alldatacenter/alldata/blob/master/install.md)

### 数据中台登录页
<img width="1416" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/189663b3-f9e2-4c83-b6fa-507dc19711a5">

### 数据中台首页
<img width="2208" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/d74d7986-73c3-4338-a0e2-3baf15d40a7b">


## 一、会员商业版
### 会员商业版线上环境地址

```
会员商业版线上环境地址，线上环境只对会员通道开放，成为会员：享受会员权益。
```
> 地址：http://122.51.43.143:5173/ui_moat
> 
> 账号：test/123456（此账号只能体验数据质量功能）
>


### 1.开源版与商业版区别
```
1.1 开源版与商业版不是同一个项目，开源版会长期存在，商业版由公司团队维护。

1.2 开源项目可以用于公司内部调研用途，如需商业使用，需要购买会员商业版，开源AllData项目使用GPL协议。

1.3 加入会员通道，购买商业版源码，可以商业使用AllData。

1.4 商业版功能更多，保证稳定性，Bug比较少，由公司架构师团队维护。

```

### 2.商业版包含创始版、新版尊贵会员、新版高级会员、新版终身会员
```
2.1 创始版属于V1.0旧版架构，V1.0旧版架构使用Vue2 + SpringCloud前后端分离架构。扩展大数据组件能力有限，目前扩展了开源组件DataSophon，此功能仅在商业版提供源码，开源版本不提供源码。

2.2 新版尊贵会员，新版高级会员，新版终身会员属于V2.0新版架构，V2.0新版架构使用Wujie微前端架构+后端可插拔架构。这套架构是全新会员商业版的核心优势，集成开源组件能力十分优秀，可以扩展支持到30-100+大数据开源组件。
```
>
> 2.3 创始版会员价格可以查看价格文档：https://docs.qq.com/doc/DVG1udXdxdWF4VHRG
>
> 2.4 新版尊贵会员价格可以查看价格文档：https://docs.qq.com/doc/DVEtIeVJ0ZlROdnJL
>
> 2.5 新版高级会员价格可以查看价格文档：https://docs.qq.com/doc/DVG9meHFCdFJCQU9I
>
> 2.6 新版终身会员价格可以查看价格文档：https://docs.qq.com/doc/DVExLZHNwcmlxTXZx
>
> 2.7 全部会员价格汇总可以查看海报：https://docs.qq.com/doc/DVFZEQWhoaGxFSXR6
>

### 3.个人版只用于个人学习使用，项目使用用途

> 3.1 个人版会员价格可以查看价格文档: https://docs.qq.com/doc/DVG9Hc0haTFhjTUNs
>

### 4.开源版功能列表[1.0架构]
```
4.1 系统管理 - system-service-parent ~ system-service ~ SystemServiceApplication

4.2 数据集成 - service-data-dts-parent ~ service-data-dts ~ DataDtsServiceApplication

4.3 元数据管理 - data-metadata-service-parent ~ data-metadata-service ~ DataxMetadataApplication

4.4 元数据管理 - data-metadata-service-parent ~ data-metadata-service-console ~ DataxConsoleApplication

4.5 数据标准 - data-standard-service-parent ~ data-standard-service ~ DataxStandardApplication

4.6 数据质量 - data-quality-service-parent ~ data-quality-service ~ DataxQualityApplication

4.7 数据资产 - data-masterdata-service-parent ~ data-masterdata-service ~ DataxMasterdataApplication

4.8 数据市场 - data-market-service-parent ~ data-market-service ~ DataxMarketApplication

4.9 数据市场 - data-market-service-parent ~ data-market-service-integration ~ DataxIntegrationApplication

4.10 数据市场 - data-market-service-parent ~ data-market-service-mapping ~ DataxMappingApplication

4.11 数据对比 - data-compare-service-parent ~ data-compare-service ~ DataCompareApplication

4.12 BI报表 - data-visual-service-parent ~ data-visual-service ~ DataxVisualApplication

4.13 流程编排 - workflow-service-parent ~ workflow-service ~ DataxWorkflowApplication

4.14 系统监控 - system-service-parent ~ system-service ~ SystemServiceApplication

4.15 邮件服务 - email-service-parent ~ email-service ~ DataxMailApplication

4.16 文件服务 - file-service-parent ~ file-service ~ DataxFileApplication

```

### 5.商业版功能列表[2.0架构，商业版提供与开源版功能，商业版功能更稳定，Bug更少]
```
5.1 系统管理 - system-service-parent ~ system-service ~ SystemServiceApplication

5.2 数据集成 - service-data-dts-parent ~ service-data-dts ~ DataDtsServiceApplication

5.3 元数据管理 - data-metadata-service-parent ~ data-metadata-service ~ DataxMetadataApplication

5.4 元数据管理 - data-metadata-service-parent ~ data-metadata-service-console ~ DataxConsoleApplication

5.5 数据标准 - data-standard-service-parent ~ data-standard-service ~ DataxStandardApplication

5.6 数据质量 - data-quality-service-parent ~ data-quality-service ~ DataxQualityApplication

5.7 数据资产 - data-masterdata-service-parent ~ data-masterdata-service ~ DataxMasterdataApplication

5.8 数据市场 - data-market-service-parent ~ data-market-service ~ DataxMarketApplication

5.9 数据市场 - data-market-service-parent ~ data-market-service-integration ~ DataxIntegrationApplication

5.10 数据市场 - data-market-service-parent ~ data-market-service-mapping ~ DataxMappingApplication

5.11 数据对比 - data-compare-service-parent ~ data-compare-service ~ DataCompareApplication

5.12 BI报表 - data-visual-service-parent ~ data-visual-service ~ DataxVisualApplication

5.13 流程编排 - workflow-service-parent ~ workflow-service ~ DataxWorkflowApplication

5.14 系统监控 - system-service-parent ~ system-service ~ SystemServiceApplication

5.15 邮件服务 - email-service-parent ~ email-service ~ DataxMailApplication

5.16 文件服务 - file-service-parent ~ file-service ~ DataxFileApplication

```

### 6.商业版新增功能列表[2.0架构，商业版提供源码，开源版不支持]
```
6.1 数据平台功能(DataSophon)

6.2 数据平台k8s功能(Cloudeon)

6.3 实时开发功能(Streampark)

6.4 实时开发IDE功能(Dinky)

6.5 离线平台功能(DolphinScheduler)

6.6 BI平台功能(Datart)

6.7 数据质量功能(Datavines)

```

## 二、 官方网站

> 2.1 官方文档：https://alldata.readthedocs.io
>
> 2.2 部署教程：https://github.com/alldatacenter/alldata/blob/master/install.md
>
> 2.3 数据集成文档：https://github.com/alldatacenter/alldata/blob/master/quickstart_dts.md
>
> 2.4 BI教程文档: https://github.com/alldatacenter/alldata/blob/master/quickstart_bi.md
>
> 2.5 微信社区交流群
> 
![image](https://github.com/alldatacenter/alldata/assets/20246692/610da0a1-ee19-4173-b6ab-7f96aaf808f1)
>
> 2.6 微信搜索-视频号
> 
![image](https://github.com/alldatacenter/alldata/assets/20246692/c809b131-4b5f-4e60-ab26-81930665fc3b)
>
> 2.7 微信搜索-公众号
> 
![image](https://github.com/alldatacenter/alldata/assets/20246692/7d4312a3-1904-404d-a0ed-f36b14e7c02d)
> 
> 2.8 功能介绍（开源版只有创始版会员部分功能）
![第一：功能权益](https://github.com/alldatacenter/alldata/assets/20246692/6acb372c-bcdf-4732-9ed4-0dd7aaa8b926)

## 三、AllData GitHub网站

### 3.1 Github开源AllData

<br/>
<a href="https://github.com/alldatacenter/github-readme-stats">
  <img width="1215" align="center" src="https://github-readme-stats.vercel.app/api/pin/?username=alldatacenter&repo=alldata" />
</a>

[![Stargazers over time](https://starchart.cc/alldatacenter/alldata.svg)](https://starchart.cc/alldatacenter/alldata)


### 3.2 AllData架构设计
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/235920344-fbf3c9d2-6239-4c73-aa9c-77a72773780e.png">
<br/>

## 四、AllData会员商业版体验环境

### 4.1 首页
<img width="1421" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/40364db7-d7a1-457c-af51-94b3b6a71f8e">

### 4.2 数据集成（开源版有，商业版更稳定，bug比较少）

<img width="1423" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/862469f5-a557-4654-a348-c7d2ccc29b49">

<img width="1439" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/34fcabb5-5c93-468b-8c83-b23266a593ac">

<img width="1413" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/265f3d51-5404-4525-aa3a-1706e7c44001">

<img width="1419" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/defae4eb-3746-43d8-835c-16aa5bbaab54">

<img width="1419" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/19315163-492a-4c3b-bc4e-475e02c1a674">
<img width="1425" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/450659ad-8baa-4dfe-a3f1-6bf7bbbfaafb">
<img width="1439" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/e84dc781-ee36-4f82-ae59-fccbffd9fdd9">
<img width="1400" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/65d5e8ec-f2f2-4053-bf3c-9e46131dd54b">


### 4.3 数据质量（开源版有，商业版更稳定，bug比较少）

<img width="1412" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/def238c4-9721-4ebf-b7f8-60f2c317d27d">

![image](https://github.com/alldatacenter/alldata/assets/20246692/79a50d83-cacb-49df-9b54-5deb5cd6b2bb)

![image](https://github.com/alldatacenter/alldata/assets/20246692/47f736b1-2281-448f-b703-7fed005f1326)

![image](https://github.com/alldatacenter/alldata/assets/20246692/5e55ba7c-df80-4c0c-ab54-e83d46047cd3)

![image](https://github.com/alldatacenter/alldata/assets/20246692/5280690b-f6a1-4aba-98b9-788270d65fba)

### 4.4 数据标准（开源版有，商业版更稳定，bug比较少）

<img width="1405" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/73c735f9-ebc1-4824-8e03-713494bc9d00">

<img width="1410" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/12f4c9c4-f7d9-484d-886f-efce968e6f6e">

<img width="1424" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/81aa4cc1-8ef9-4dbf-a539-5d87ffb7b9f2">

<img width="1409" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/b091f7eb-6ba1-4c04-a065-02cc1b984862">


### 4.5 元数据管理（开源版有，商业版更稳定，bug比较少）
<img width="1411" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/6e2800e3-ff47-4bfd-8401-ed3983b588ab">

<img width="1424" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/e5497f9d-6a75-424b-98e1-4a73573f0c0b">

<img width="1405" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/c4b10f4d-c943-40a3-9091-5ee747318036">

<img width="1396" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/e12b3be1-761c-4ba0-b8d9-d688a2d56984">

<img width="1394" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/3b481394-72f0-4713-8860-220a2a6496c5">

<img width="1415" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/98475d04-b4dd-4e3a-af7f-5f01166f4650">

<img width="1415" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/8a3f6445-cf3e-4b38-a423-ab94d59c3ba4">


### 4.6 数据资产（开源版有，商业版更稳定，bug比较少）

<img width="1434" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/87351bf4-2249-421f-bde0-afa2bd115324">

<img width="1411" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/4096b37b-0534-48b1-bcb4-c592966df6f5">

### 4.7 数据服务（开源版有，商业版更稳定，bug比较少）

<img width="1400" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/53a16fc5-13c0-4c68-a6ae-549d81c6b3c7">


<img width="1420" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/7d1829e3-4443-4e4a-badb-4c803fd78765">

<img width="1393" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/b903d771-d6b3-469e-9a0e-2abf14c820c8">

<img width="1409" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/ac53ce10-e28b-423e-92e8-dd4ef3e94276">

<img width="1402" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/9058eead-2a33-4c87-b4e7-85b619ce5e78">



### 4.8 数据比对（开源版有，商业版更稳定，bug比较少）

<img width="1416" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/a7214896-7a15-443a-bd75-f024ae8e8ec3">


<img width="1428" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/424fc66b-8186-470f-ba3b-76f02035a679">

<img width="1422" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/31a9368e-fbed-43f4-911d-7551a3592049">

<img width="1407" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/b6ad5827-2896-4ef0-be66-9116f9612d01">

<img width="1410" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/2409f74f-bed4-465b-adf3-022979132247">

![image](https://github.com/alldatacenter/alldata/assets/20246692/d1d5307c-3963-49d9-b125-d999bb32a05a)

![image](https://github.com/alldatacenter/alldata/assets/20246692/c5c71ad3-e205-4920-8b03-6ac3d58a0540)

![image](https://github.com/alldatacenter/alldata/assets/20246692/1ed9226e-ecab-4e4e-b00f-4ca73228c1ec)

![image](https://github.com/alldatacenter/alldata/assets/20246692/b278918e-35a6-4d0d-8ee9-153620af0ef3)



### 4.9 BI报表（开源版有，商业版更稳定，bug比较少）

![image](https://github.com/alldatacenter/alldata/assets/20246692/6aa5e2d0-6343-4943-9963-5c8e08557e4f)

![image](https://github.com/alldatacenter/alldata/assets/20246692/e5299ec0-3428-4097-969d-caa712e672d3)

![image](https://github.com/alldatacenter/alldata/assets/20246692/0c9f3e05-ca04-47b1-a84e-e4e6643c3e6d)

![image](https://github.com/alldatacenter/alldata/assets/20246692/db822c06-3553-4e6f-aeaf-61a50e81f17a)

<img width="1427" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/9476f258-e8dd-438e-acab-4abc68410b9c">



### 4.10 流程编排（开源版有，商业版更稳定，bug比较少）

<img width="1382" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/90f23e9f-2154-474c-bc93-4b9eb1fee263">

<img width="1408" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/5d77957f-a98a-4efe-aea2-7a2f0b6b4deb">

<img width="1429" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/87c388ad-b578-4c37-8222-e6bec8dfb249">

### 4.11 系统监控（开源版有，商业版更稳定，bug比较少）
![image](https://github.com/alldatacenter/alldata/assets/20246692/725711c1-d603-4fe5-93b0-debf50b6ce74)

![image](https://github.com/alldatacenter/alldata/assets/20246692/12b47e3e-2227-42e9-96fb-a88d13b54594)

![image](https://github.com/alldatacenter/alldata/assets/20246692/6b58bffc-0133-4973-99ae-37642347ba50)

![image](https://github.com/alldatacenter/alldata/assets/20246692/0a1b1c3f-cf12-4213-8b72-deeb5c8205f8)

### 4.12 运维管理（开源版有，商业版更稳定，bug比较少）

![image](https://github.com/alldatacenter/alldata/assets/20246692/4e990fda-dbc5-41b4-a038-0b5f6c0e4539)

![image](https://github.com/alldatacenter/alldata/assets/20246692/7a94f0ec-5608-4841-9ebc-3ba5c8214544)

![image](https://github.com/alldatacenter/alldata/assets/20246692/575a7f99-dc82-48a8-bf4f-dbc2a115a0b2)

![image](https://github.com/alldatacenter/alldata/assets/20246692/0f01f27d-bd9e-4449-81e3-3f5d306a643e)

![image](https://github.com/alldatacenter/alldata/assets/20246692/cc84bd6b-c609-4d3b-82f1-33b524ab33b1)

### 4.13 数据平台（开源版没有，商业版提供源码）

<img width="1410" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/dbc8c273-1800-4395-999d-eb241c09301a">

<img width="1416" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/46a3e3d1-940a-4f23-be3c-f246bff44c3b">

<img width="1401" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/0de860ff-a277-4b33-b6c5-9f28c7397cdb">

<img width="1407" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/4e4230d1-67bb-4416-9b13-1eb8a3e4db93">


<img width="1410" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/3d8acd20-17a0-4726-94de-c46d4e008c2d">

<img width="1397" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/304adb57-61dd-4f3d-816f-f743ac628832">

<img width="1403" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/f8ac1e10-2f54-4bf2-a6a0-993949e83e08">

<img width="1416" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/058c0dde-35c5-4bab-97e9-36bf03200bd2">


![image](https://github.com/alldatacenter/alldata/assets/20246692/f1ceca6b-98f8-4064-9f6f-65f0b4e7f1c8)

![image](https://github.com/alldatacenter/alldata/assets/20246692/07051569-4183-4cf6-9128-623e712c15f0)

![image](https://github.com/alldatacenter/alldata/assets/20246692/c07399b9-f0a8-4b2d-ac9a-1b39db5784e5)

### 4.14 数据平台(K8S)（开源版没有，商业版提供源码）

<img width="1424" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/b77cfc70-796f-426b-b0e2-e9b5471c59d9">

<img width="1410" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/ae40f1fd-13e4-4844-93c4-1dabcfc53ca5">

<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/41b1fb5f-de34-4488-812f-8b217baaf54b">

<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/706345d8-a7d7-459f-b125-912bdc4cec03">

<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/ab048563-22a0-43d8-91d1-f6cf8f45781c">

<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/91065efb-c6c2-4267-8886-59761d2dc555">



### 4.15 实时开发（开源版没有，商业版提供源码）
<img width="1431" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/e397f42a-850d-49ea-bfa0-c952a9857c7f">


<img width="1389" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/eabaf802-9df7-48d3-9fa8-e886b000c1a7">

<img width="1439" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/d9e1f28b-c01b-4223-bfcb-f78e0b508945">

<img width="1436" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/bde8edef-d227-4270-9144-bd48cb6dbe72">

<img width="1439" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/f9d78174-c222-4d8b-9125-7cb773ba3137">

<img width="1438" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/35e6d823-87b1-46c8-bab9-710da80b25fb">



### 4.16 实时开发IDE（开源版没有，商业版提供源码）
<img width="1409" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/c6fac72f-0162-49f1-9897-abfcb24dc313">

<img width="1438" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/6178bb3b-3523-400a-afc3-3eeb1a7b4eb8">

<img width="1420" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/952fc1ca-9108-421b-831a-b9e171642b44">

<img width="1404" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/084e502e-025a-4347-b69f-a1b83faac96e">

<img width="1421" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/cfbe6cb8-b4a2-41f4-9fcf-96684d0c84a8">

<img width="1430" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/f164c02b-d299-4752-bc46-d2154c007eaa">

<img width="1437" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/0948259b-146a-4c20-b3e7-943f5930f10c">
<img width="1410" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/f268f2ea-6736-4d8a-ab46-6b7678906424">


### 4.17 离线平台（开源版没有，商业版提供源码）
<img width="1436" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/b26fd04d-fc0f-40b9-8446-c8c1fc3e70ce">

<img width="1436" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/af3f84f9-06ef-420c-a20e-36880b3ba2fb">

<img width="1435" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/d740db51-a4cf-4f65-8e33-8f835c1aa29b">

<img width="1411" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/80efda7c-bb2a-4b87-9739-8ebe9bf5cb5e">

<img width="1417" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/c7f1f723-ffe9-4676-80a1-ab32b6d9ddcf">

<img width="1401" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/67ba5e7b-638d-4e6c-a85a-2125fa3a256e">

<img width="1435" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/9a5bfe99-98a5-4f39-979a-5fd6236f2279">

<img width="1424" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/3d60df79-0cf2-44a6-9648-75b9e66e9532">

<img width="1404" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/2fe4dd9e-a9a3-48d4-86ce-d3293248a6c8">


<img width="1421" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/f4cb51e2-6c5d-40ec-a6ed-619d2f2efbb3">
<img width="1408" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/e6706543-63c3-4392-a174-8cf3d13d2bc2">
<img width="1424" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/043c99f5-93b7-41ee-8350-c567bea69aff">

<img width="1398" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/cdd7f495-5eb8-4dd1-b332-4f443a4a48a5">
<img width="1439" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/c2692cba-eaea-422f-8264-c3226897d705">
<img width="1412" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/de906b6c-3498-4174-9fa7-82bf79d667d2">

### 4.18 新版数据质量平台(DataVines)（开源版没有，商业版提供源码）

> 新版数据质量平台功能请查看文档

<img width="1419" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/dccc4c1d-b277-49f5-a347-22bc38599f53">

<img width="1439" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/3f187993-45ef-43ba-b04a-febb95dd4b97">

<img width="1431" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/25329eb7-bc28-48e2-843b-a129aa30e646">

<img width="1432" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/75560277-2856-43f2-bce8-06ada54832e4">

<img width="1439" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/a7a5827e-dedd-4504-ab52-307be643ef3e">

<img width="1413" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/6427fecc-e451-4cf2-9700-0142c82c6d23">

<img width="1416" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/65b59adb-9876-4e91-80aa-81f6d58305ae">

<img width="1412" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/5c4c9ff2-dd8c-4dc7-bf32-0aa2153f4ad2">

<img width="1421" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/4efa7242-5927-404e-9a18-e7274adccb3a">

<img width="800" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/433b22ba-b562-45d1-877a-2c52d8c1cc11">

<img width="1439" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/b25dba4d-28c9-4d16-bca1-e1803c5ae87a">

### 4.19 新版BI平台(Datart)（开源版没有，商业版提供源码）

<img width="1413" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/0732f03a-63b3-404d-a1d6-adfd9eff34b7">


<img width="1435" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/cf8d53d3-7292-45cb-9bd3-847b24150880">

<img width="1413" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/5a2cb21c-3b42-4626-80f3-fceb76e06a22">
<img width="1409" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/5175a50f-c6cb-4fc1-a40c-ba8a5745f26d">

<img width="1265" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/92e4ba6b-ee6e-4d54-93b9-28d3af509564">


## 五、AllData开源RoadMap

### 5.1 AllData规划
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/188898972-d78bcbb6-eb30-420d-b5e1-7168aa340555.png">
<br/>

### 5.2、全站式AllData产品路线图
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/179927878-ff9c487e-0d30-49d5-bc88-6482646d90a8.png">
<br/>

### 5.3、AllData社区开发规划
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/188899033-948583a4-841b-4233-ad61-bbc45c936ca1.png">
<br/>

### 5.4、Architecture
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/171598215-0914f665-9950-476c-97ff-e7e07aa10eaf.png">
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/171598333-d14ff53f-3af3-481c-9f60-4f891a535b5c.png">
<br/>
