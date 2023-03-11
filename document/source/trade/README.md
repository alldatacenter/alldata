# 电商数据中台

>ALL DATA Double 微服务商城

## 启动配置教程

### 1、启动前，打包dubbo-service

>执行mvn clean package -DskipTests=TRUE打包，然后执行mvn install.

### 2、启动dubbo项目，配置tomcat端口为8091

<img width="1210" alt="image" src="https://user-images.githubusercontent.com/20246692/160220455-45898c53-0de6-4a06-80b0-ae7e758b9457.png">


### 3、启动商城项目的多个子系统

> 后台：访问http://localhost:8090

> 前端：启动mall-admin-web项目，进入项目目录，执行npm install，然后执行npm run dev；

> 后端：启动mall-admin-search项目，

> 配置tomcat端口为8092，接着启动pcManage项目，tomcat端口配置为8093；

<img width="1226" alt="image" src="https://user-images.githubusercontent.com/20246692/160220467-283a7964-27c1-4184-9ece-778e87fc38f7.png">
<img width="1217" alt="image" src="https://user-images.githubusercontent.com/20246692/160220472-68a6d9a4-e295-4b86-a9e6-75b53f821d52.png">

> 前台：小程序手机预览，移动端访问：http://localhost:6255

### 4、小程序和移动端

> 前端：商城小程序，启动mall-shopping-wc项目，

> 安装微信开发者工具，配置开发者key和secret，

> 使用微信开发者工具导入即可，然后点击编译，可以手机预览使用。

<img width="945" alt="image" src="https://user-images.githubusercontent.com/20246692/160220487-dace0ed7-c4e5-4a17-88d9-983e44c89ce5.png">


### 5、商城移动端

> mobile-h5， 进入项目目录，执行npm install和npm run dev

### 6、小程序和移动端用的是同一个后台服务，

> 启动mobileService项目，进入项目目录，配置tomcat端口8094

<img width="1221" alt="image" src="https://user-images.githubusercontent.com/20246692/160220500-7c6b9097-7a82-4f23-95be-eda9c8f9eee5.png">


### 7、商城PC端 访问http://localhost:8099

> 前端：启动computer项目，

> 进入项目目录，执行npm install和npm run dev；

### 8、启动admin-service项目，配置tomcat端口为8095；

<img width="1221" alt="image" src="https://user-images.githubusercontent.com/20246692/160220506-688f51cc-1b3d-46a9-ad3e-ec033ee69562.png">

