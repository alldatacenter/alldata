# 组件二开，定制化或者定期升级到最新社区稳定版本

## 本地启动TIS && 安装部署
> 1. 部署TIS-参考tis/README.md

## 本地启动部署Tis
> 1. mvn clean install -Dmaven.test.skip=true
>
> 2. 配置数据库
>
> source /tis-ansible/tis_console_mysql.sql
>
> 3. 配置项目web
>
> vi /tis/tis-web-config/config.properties
```markdown
project.name=TIS
runtime=daily

tis.datasource.type=mysql
tis.datasource.url=16gmaster
tis.datasource.port=3306
tis.datasource.username=root
tis.datasource.password=123456
tis.datasource.dbname=tis_console
zk.host=16gmaster:2181/tis/cloud

assemble.host=8gmaster
tis.host=8gmaster

```
> 4. 启动TIS
>
> mvn compile test -Dtest=StartTISWeb Dtis.launch.port=8080
>
> 访问 http://8gmaster:8080

> 2. 部署plugins,参考plugins/README.md

## 本地安装部署 on Linux
> 1. 安装maven3.8.1 配置settings.xml
>
> 2. Only配置
>
```
     <mirror>
      <id>alimaven</id>
      <name>aliyun maven</name>
      <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
      <mirrorOf>central</mirrorOf>
    </mirror>

```
> 3. 创建目录 /opt/data/tis/libs/plugins
>
> 4. 执行plugin软连接配置
>
```
for f in `find /mnt/poc/alldatadc/tis_poc/plugins  -name '*.tpi' -print` do echo " ln -s $f " ln -s $f /opt/data/tis/libs/plugins/${f##*/} done ;
```
> 5. 安装plugins
>
> mvn clean package -Dmaven.test.skip=true -Dappname=all
>

> 3. 部署ng-tis,参考ng-tis/README.md
## 本地打包部署 on Linux
> 1. nvm install v10.15.3
>
> 2. npm install -g @angular/cli@12.2.13
>
> 3. npm run ng:serve-jit --scripts-prepend-node-path=auto
>
> 4. curl http://localhost:4200