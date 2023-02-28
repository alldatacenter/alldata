![](https://tokei.rs/b1/github/qlangtech/plugins)
## TIS官方插件
- Official Product Plugins For TIS
- TIS 官方插件

## Git 相关

- 删除分支: `git push origin  :heads/v2.2.0`

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
