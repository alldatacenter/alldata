| 16gmaster                      | port | ip             |
|--------------------------------| ---- | -------------- |
| system                 | 8613 | 16gmaster  |
| config                   | 8611 | 16gmaster  |
| data-market-service      | 8822 | 16gmaster  |
| service-data-integration | 8824 | 16gmaster  |
| data-metadata-service    | 8820 | 16gmaster  |

| 16gslave                      | port | ip             |
|-------------------------------| ---- | -------------- |
| eureka                  | 8610 | 16gslave    |
| gateway                 | 9538 | 16gslave    |
| service-workflow        | 8814 | 16gslave    |
| data-metadata-service-console    | 8821 | 16gslave    |
| service-data-mapping    | 8823 | 16gslave    |
| data-masterdata-service | 8828 | 16gslave    |
| data-quality-service    | 8826 | 16gslave    |

| 16gdata               | port | ip             |
|-----------------------| ---- | -------------- |
| data-standard-service | 8825 | 16gdata |
| data-visual-service   | 8827 | 16gdata |
| email-service         | 8812 | 16gdata |
| file-service          | 8811 | 16gdata |
| quartz-service        | 8813 | 16gdata |
| system-service        | 8810 | 16gdata |
| tool-monitor    | 8711 | 16gdata |


### 部署方式

> 数据库版本为 **mysql5.7** 及以上版本
### 1、`studio`数据库初始化
>
> 1.1 source install/16gmaster/studio/studio_alldatadc.sql
>
> 1.2 source install/16gmaster/studio/studio_dts.sql
>
> 1.3 source install/16gmaster/studio/studio_data_cloud.sql
>
> 1.4 source install/16gmaster/studio/studio_cloud_quartz.sql
>
> 1.5 source install/16gmaster/studio/studio_foodmart2.sql
>
> 1.6 source install/16gmaster/studio/studio_robot.sql

### 2、修改 **config** 配置中心

> **config** 文件夹下的配置文件，修改 **redis**，**mysql** 和 **rabbitmq** 的配置信息
>
### 3、项目根目录下执行 **mvn install**
>
> 获取安装包build/studio-release-0.3.2.tar.gz
>
> 上传服务器解压
>
### 4、部署`stuido`[后端]
## 单节点启动[All In One]

> 1、启动eureka on `16gslave`
>
> 2、启动config on `16gslave`
>
> 3、启动gateway on `16gslave`
>
> 4、启动masterdata on `16gslave`
>
> 5、启动metadata
>
> 6、启动其他Jar

## 三节点启动[16gmaster, 16gslave, 16gdata]
> 1. 启动`16gslave`, sh start16gslave.sh
> 2. 启动`16gdata`, sh start16gdata.sh
> 3. 启动`16gmaster`, sh start16gmaster.sh

### 5、部署`studio`[前端]:
>
> source /etc/profile
>
> cd $(dirname $0)
>
> source /root/.bashrc && nvm use v10.15.3
>
> nohup npm run dev &
>
> 5.3 访问`studio`页面
>
> curl http://localhost:8013
>
> 用户名：admin 密码：123456
