# QUICK STARK FOR ALL DATA Studio

## 远程调试，本地调试文档

### 一、打包

- 用Idea打开AllData项目，并引入POM.XML文件

![image-20230214213254462](http://yg9538.kmgy.top/image-20230214213254462.png)

- 修改**所有单独模块**的**ip-address为服务对外地址**，**defaultZone为Eureka所在服务器地址**
  - 服务器地址格式为：**http://0.0.0.0:8610/eureka** 

![image-20230214213355662](http://yg9538.kmgy.top/image-20230214213355662.png)

- 修改system  ：applicaition-dev.yml中的地址为mysql服务器地址

![image-20230214213619363](http://yg9538.kmgy.top/image-20230214213619363.png)

- 修改system  ：applicaition-dev.yml中的地址为redis服务器地址

![image-20230214213658263](http://yg9538.kmgy.top/image-20230214213658263.png)

- 修改config中的配置文件参数

![image-20230214214014661](http://yg9538.kmgy.top/image-20230214214014661.png)

- 数据库安装 install 目录下的 eladmin_alldatadc.sql，eladmin_dts.sql
- 双击maven—>clean—>package

![image-20230214214305341](http://yg9538.kmgy.top/image-20230214214305341.png)

- 修改前端服务器地址

![image-20230214215414071](http://yg9538.kmgy.top/image-20230214215414071.png)

- ![image-20230214215456634](http://yg9538.kmgy.top/image-20230214215456634.png)

- 打包代码生成dist文件夹

![image-20230214215531395](http://yg9538.kmgy.top/image-20230214215531395.png)

- dist上传到服务器，解压

![image-20230214215543857](http://yg9538.kmgy.top/image-20230214215543857.png)

- 配置nginx代理地址



### 二、部署

- **部署后端**

- 将打包生成的jar包部署在服务器上
- 服务器配置参数例表

```
| 16gmaster                      |      |                |
|--------------------------------| ---- | -------------- |
| system                     | 8613 | 16gmaster  |
| config                   | 8611 | 16gmaster  |
| service-data-market      | 8822 | 16gmaster  |
| service-data-integration | 8824 | 16gmaster  |
| service-data-metadata    | 8820 | 16gmaster  |

| 16gslave                      | port | ip             |
|-------------------------------| ---- | -------------- |
| eureka                  | 8610 | 16gslave    |
| gateway                 | 8612 | 16gslave    |
| service-workflow        | 8814 | 16gslave    |
| service-data-console    | 8821 | 16gslave    |
| service-data-mapping    | 8823 | 16gslave    |
| service-data-masterdata | 8828 | 16gslave    |
| service-data-quality    | 8826 | 16gslave    |

| 16gslave2                      | port | ip             |
|--------------------------------| ---- | -------------- |
| service-data-standard    | 8825 | 16gdata |
| service-data-visual      | 8827 | 16gdata |
| service-email            | 8812 | 16gdata |
| service-file             | 8811 | 16gdata |
| service-quartz           | 8813 | 16gdata |
| service-system           | 8810 | 16gdata |
| tool-monitor             | 8711 | 16gdata |
```

- **服务启动顺序**
  - eureka 
  - config 
  - gateway
  - system 
  - service-data-mapping 
  - service-data-market 
  - service-data-masterdata
  - ...其他服务顺序随意

- **部署前端**
- **部署nginx**

```
server {
    listen       80;
    listen       [::]:80;
    add_header Access-Control-Allow-Origin *;
    add_header Access-Control-Allow-Headers X-Requested-With;
    add_header Access-Control-Allow-Methods GET,POST,OPTIONS;
    server_name  16gmaster;	
    root /mnt/poc/alldatadc/studio_prod/studio_code/studio_prod/ui/dist/;
    index index.html;        
    include /etc/nginx/default.d/*.conf;
    location /api/{  
       proxy_pass  http://16gslave:9538/;
       proxy_set_header Host $proxy_host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}

```

### 三、访问

![image-20230214215235361](http://yg9538.kmgy.top/image-20230214215235361.png)

### 四、远程调试

- Eureka和config必须放在服务器上，其他均可放在本地调试，将Eureka路径调整为服务器Eureka ip地址。