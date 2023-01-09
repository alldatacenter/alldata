# 使用方法

## 通过编译向本地安装
```
ansible-playbook ./deploy-tis-by-compile.yml --tags initos,zk,hadoop,spark,pkg,pkg-plugin,ng-tis,tjs,assemble,indexbuilder,solr --skip-tags=deploy -i ./inventory/hosts
```
注意：不需要向仓库中部署需要添加`--skip-tags=deploy`参数
## 通过Release本地安装
```
ansible-playbook ./deploy-tis-by-release.yml --tags initos,zk,hadoop,spark,tjs,assemble,indexbuilder,solr -i ./inventory/hosts
```

## Spark安装介绍

该脚本集成了下面一些功能：

- 系统初始化：安装必备的软件，并做一些设置
- 安装系统依赖的jdk、spring-boot
- 安装 zookeeper
- 安装 hadoop 的 hdfs、 yarn
- 安装 spark，支持tidb的 tispark，方便通过jdbc连接的 thriftserver及对应yarn的spark-shuffle
- 安装 solr
- 安装 tis-console、tis-assemble
- 安装对应全量构建需要的 index-builder

脚本在 CentOS 7.6 上通过测试。

**安装前必读**：

当前版本将程序、数据主要目录都安装到 `/opt` 目录下，如果 `/opt` 不是最大分区，安装脚本会尝试将当前系统最大分区通过 symbol link 的方式链接到 `/opt`。
这种方式在目前会造成已知的BUG：**如果 hdfs、yarn 几台服务器 /opt 对应的最大分区挂载目录不一致，将导致 namenode、resourcemanager 节点最大分区挂载目录不一致的其它服务器无法启动对应的服务**。
导致该问题的原因是 `start-dfs.sh`，`start-yarn.sh` 会在一开始通过命令 `dirname "${BASH_SOURCE-$0}"` 获取脚本所在路径，并以此目录为基础查找其它可执行程序。该命令将忽略掉 symbol link，直接获取物理分区所挂载路径。
为了避免这个问题，需要在安装前将需要安装的机器最大磁盘分区都挂载到相同路径。通过命令：`lsblk | awk '{if ($7) print $4 " " $7}' | sort -h | tail -n 1 | awk '{print $2}'` 可以检查这些机器的最大分区是否为同一个目录。

## 准备阶段

### 配置 vars

需要配置 vars.yml 文件，修改下面几个参数以定制安装需要的组件，如果不安装，则需要确保这些主机已经是可用状态：

```file
need_install_zookeeper: true
need_install_hadoop: true
need_install_spark: true
need_install_tispark: true
need_install_spark_shuffle: true
```

具体依赖关系看其中的注释。

需要修改 vars.yml 中的参数 yarn_nodemanager_resource_memorymb 以配置 yarn nodemanager 可用的最大内存，根据机器的实际内存进行修改。在创建索引时，提交的物理内存为配置的 nodemanager 可用最大内存的 80%。

### 配置 hosts

inventory/hosts 文件示例如下，为了自动生成 hosts 文件，请在主机名后通过 ansible_ssh_host 指定IP，指定IP如下：

```file
[solr]
solr1.xxx ansible_ssh_host=10.33.9.192
solr2.xxx ansible_ssh_host=10.33.9.193

[hadoop-hdfs-datanode]
hadoop1.xxx ansible_ssh_host=10.1.1.1
hadoop2.xxx ansible_ssh_host=10.1.1.2
hadoop3.xxx ansible_ssh_host=10.1.1.3
```

上面例子中 `solr` 为一组主机，solr1.xxx 为单个主机，下面的操作都会指定一组主机或单个主机。

**注意**：上面例子中的主机不全，需要根据实际安装的组件，配置全部的主机和参数：

- 必须配置主机 hadoop-yarn-resource-manager、hadoop-yarn-node-manager，用于安装indexbuild；
- 如果要安装 hadoop：需要配置主机 hadoop-hdfs-namenode、hadoop-hdfs-datanode；
- 如果需要安装zookeeper：需要配置主机 zookeeper
- 如果需要安装 tispark：需要配置主机 tidb
- 必须配置参数 如果hdfs namenode没有配置ha，则配置为系统的namenode主机即可。
- 必须配置参数 tisconsole_db_url、 tisconsole_db_username、 tisconsole_db_password 为tis console 的sql语句所在的数据库连接信息。
- 如果需要安装 spark shuffle，要根据内存配置参数 spark_shuffle_max_executor、 spark_shuffle_executor_memory ，以便实现内存的有效利用。在启动 thriftserver 时，会启动一个 ApplicationMaster，消耗2G内存，另外启用一个 Container，消耗（2G + spark_shuffle_executor_memory) 的内存。后续每次启动一个 Container，就消耗 （2G + spark_shuffle_executor_memory) 的内存。内存要满足：yarn_nodemanager_resource_memorymb \* nodemanager机器数量 >= 2G + (2G + spark_shuffle_executor_memory) \* spark_shuffle_max_executor，最好流出一些余量，以免创建Container失败。

## 确保主机可以由中控机ssh免密登陆

如果在ansible中控机没有做过 ssh-copy-id 到其它需要安装的主机，可以通过 ssh-keygen 先在中控机生成一个可以，使用下面的命令可以通过 `copy_root_sshkey.yml` 辅助拷贝到其它主机，注意如果需要一组或多组主机拷贝，则要确保一组主机有相同的root密码：

```shell
ansible solr,hadoop-hdfs-datanode -m authorized_key -a "user=root key='{{ lookup('file', '/root/.ssh/id_rsa.pub') }}'" -k
ansible solr1.xxx -m authorized_key -a "user=root key='{{ lookup('file', '/root/.ssh/id_rsa.pub') }}'" -k
```

然后可以通过下面的方式验证是否实现了免密登陆，可以一次性测试多组主机：

```shell
ansible solr,hadoop-hdfs-datanode -m ping
ansible solr1.xxx -m ping
```

## 通过已经发布的releas文件安装

cd 到ansible脚本所在目录：

```shell
ansible-playbook ./deploy-tis-by-release.yml
```

## 启动和停止系统

进入当前目录：

```shell
# 启动系统
ansible-playbook ./start.yml
# 停止系统
ansible-playbook ./stop.yml
```

如果只是因为 thriftserver 和 yarn 不稳定，需要重启 yarn 和 thriftserver，可以在当前目录用下面的命令停止和启动：

```shell
# 停止 thriftserver 和 yarn
ansible-playbook ./stop-yarn-and-thriftserver.yml
# 启动 yarn 和 thriftserver
ansible-playbook ./start-yarn-and-thriftserver.yml
```

## 初始化tis

第一次安装tis，需要向mysql数据库中，初始化数据库，其中的sql是通过下面的命令导出的：

```shell
# the option '-d' means nodata just table struct
mysqldump -d -uxx -pxxx -h127.0.0.1  tis_console > tis_console_mysql.sql
```

在启动tis后，可以通过 tis.xx:8080 通过web访问系统，需要初始化几个值：

- zkaddress：设置为几个主机，后加 `/tis/cloud` 路径，`zk1.xxx:2181,zk2.xxx:2181,zk3.xxx:2181/tis/cloud`
- tis_hdfs_root_dir: 设置为如下路径 `/xxx/data`，不需要前面类似于 `hdfs://hadoop1.xxx:9000` 这样的URL。

3.重启solr服务,注意要加上'--become'才能得到sudo权限

```file
ansible solr -i ./inventory/hosts -m service --become  -a "name=spring-boot state=restarted"
```

4.在第一次进行全量构建时，可能会因为hdfs的权限导致tag文件无法写入，需要在 hdfs 的机器上进行下面的设置：

```shell
su - hadoop
hdfs dfs -chmod -R 777 /
```

# 执行例子

## 编译

1. 编译打包`datax-docker`
   
   ``` shell script
   ansible-playbook ./deploy-tis-by-compile.yml --tags pkg,datax-docker --skip-tags=deploy -i ./inventory/hosts
   ```
2. 编译打包`uber`执行包
   ``` shell script
   ansible-playbook ./deploy-tis-by-compile.yml --tags pkg,pkg-plugin,ng-tis,uber --skip-tags=deploy -i ./inventory/hosts
   ```

## 发布

   ``` shell script
   ansible-playbook ./deploy-tis-by-compile.yml --tags pkg,pkg-plugin,ng-tis,uber,datax-docker,flink-docker,update-center,deploy  -i ./inventory/hosts
   ```



## 向ansible脚本中新添加一个role

```shell
ansible-galaxy init --init-path=roles taskcenter-worker
```

## 远程安装java

``` shell
ansible all -i "ip,"  -m include_role -a "name=jdk" -e "@vars.yml" -u root
```

## 向仓库中部署构件
```
ansible-playbook ./deploy-tis-by-compile.yml --tags pkg,pkg-plugin,ng-tis,deploy 
```


