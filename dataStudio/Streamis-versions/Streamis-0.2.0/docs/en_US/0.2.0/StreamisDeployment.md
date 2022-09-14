# Streamis installation and deployment documentation

## 1. Component introduction
Streamis0.2.0 provides the Streamis-JobManager component, the role of the component is <br>
1. Publish streaming applications<br>
2. Set streaming application parameters, such as the number of Flink slots, checkpoint related parameters, etc.<br>
3. Manage streaming applications (e.g. start and stop)<br>
4. Streaming application monitoring<br>


## 2. Code compilation
Streamis does not require manual compilation. You can download the installation package directly for deployment. Please [click to download the installation package](https://github.com/WeBankFinTech/Streamis/releases)

If you have already obtained the installation package, you can skip this step<br>

- The background compilation method is as follows
```
cd ${STREAMIS_CODE_HOME}
mvn -N install
mvn clean install
```
After successful compilation, the installation package will be generated in the 'assembly/target' directory of the project

- The front-end compilation method is as follows

Pre dependency: nodejs, python 2.0

```bash
cd ${STREAMIS_CODE_HOME}/web
npm i
npm run build
```
After the compilation is successful, the installation package will be generated in the `${STREAMIS_CODE_HOME}/web` directory 

## 3. Installation preparation
### 3.1 Basic environment installation
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The following software must be installed:

- MySQL (5.5+), [How to install MySQL](https://www.runoob.com/mysql/mysql-install.html)
- JDK (above 1.8.0_141), [How to install JDK](https://www.runoob.com/java/java-environment-setup.html)

### 3.2 Linkis and DSS environments
- The execution of Streamis depends on Linkis, and it needs to be version 1.1.1 and above, so you need to install Linkis above 1.1.1 and ensure that the Flink engine can be used normally.Some functions need to be supported by linkis-1.1.2.
- Datasphere studio (> =1.1.0), the development and debugging of streaming jobs depend on DSS scriptis, and the streaming production center needs to be embedded in the DSS engineering framework system, so it depends on * * dss-1.1.0 * * and above.

Before the formal installation of streamis, please install linkis-1.1.1 and dss-1.1.0 or above, and ensure that the linkis Flink engine and DSS can be used normally. For the installation of DSS and linkis, please refer to the [dss & linkis one click installation and deployment document](https://github.com/WeBankFinTech/DataSphereStudio-Doc/blob/main/zh_CN/%E5%AE%89%E8%A3%85%E9%83%A8%E7%BD%B2/DSS%E5%8D%95%E6%9C%BA%E9%83%A8%E7%BD%B2%E6%96%87%E6%A1%A3.md).

How to verify that DSS and linkis are basically available? You can create a flinksql script on DSS scriptis and execute it. If flinksql can execute correctly and return the result set, it means that the DSS and linkis environments are available.


## 4. Installation and startup

### Background installation

1.installation package preparation

Upload the installation package to the installation directory of the Linux server (currently only supports linux environment deployment), such as /appcom/install/streams, and then extract it:

```bash
cd /appcom/Install/streamis
tar -xvf wedatasphere-streamis-${streamis-version}-dist.tar.gz
```

2.Modify the database configuration
```bash
vi conf/db.sh
#Configure basic database information
```

3.Modify the basic configuration file

```bash
vi conf/config.sh
#Configure service port information
#Configure Linkis service information
```
4.Installation 
```bash
sh bin/install.sh
```

- The install.sh script will ask you if you need to initialize the database and import metadata.

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Because the user is worried that the user repeatedly executes the install.sh script to clear the user data in the database, when the install.sh is executed, the user will be asked if they need to initialize the database and import metadata.

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Yes must be selected for the first installation**.

5.start up
```bash
sh bin/start.sh
```

- Start verification
Verification method, because Streamis and Linkis use a set of Eureka, you need to check whether the Eureka page of Linkis already contains Streamis services, as shown in the figure,
![components](../../images/zh_CN/eureka_streamis.png)



### Front-end deployment

1.Install nginx
 
```bash
sudo yum install -y nginx
```
2.Deploy the front-end package
```
mkdir ${STREAMIS_FRONT_PATH}
cd ${STREAMIS_FRONT_PATH}
#Place the front-end package
unzip streamis-{streamis-version}.zip
```
3.Modify the nginx configuration file<br>

```bash
cd /etc/nginx/conf.d
vi streamis.conf
# Copy the following template and modify it according to the actual situation
```
```
server {
    listen 9088;# access port
    server_name localhost;
    location / {
        root ${STREAMIS_FRONT_PAH}; # Please modify it to the appropriate static file directory of Streamis
    index index.html index.html;
    }
    location /api {
    proxy_pass http://${Linkis_GATEWAY_IP}:${LINKIS_GATEWY_PORT}; #Back-end Linkis address, please modify it to the ip and port of the Linkis gateway
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header x_real_ipP $remote_addr;
    proxy_set_header remote_addr $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_http_version 1.1;
    proxy_connect_timeout 4s;
    proxy_read_timeout 600s;
    proxy_send_timeout 12s;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection upgrade;
    }

    #error_page 404 /404.html;
    # redirect server error pages to the static page /50x.html
    #
    error_page 500 502 503 504 /50x.html;
    location = /50x.html {
    root /usr/share/nginx/html;
    }
}
```
4.Load nginx configuration
```bash
sudo nginx -s reload
```

## 5. Access to DSS

If you want to use the streamis0.2.0 front end normally, you also need to install the DSS StreamisAppConn plug-in. Please refer to: [StreamisAppConn plug-in installation document](development/StreamisAppConnInstallationDocument.md)

## 6. Linkis Flink engine compilation and installation
If you want to run streamis0.2.0 normally, you also need to install the linkis Flink engine. Please refer to: [linkis Flink engine installation document](https://linkis.apache.org/zh-CN/docs/1.1.2/engine_usage/flink/)

## 7. Streamis component upgrade document / script
If you want to upgrade from a lower version of streamis to streamis0.2.0, please refer to: [streamis upgrade document](development/StreamisUpgradeDocumentation.md)
