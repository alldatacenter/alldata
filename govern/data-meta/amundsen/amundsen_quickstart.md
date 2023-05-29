环境要求

docker-ce

docker-compose

python3.7（>=3.7)

git环境

1）docker安装

安装docker-ce

yum update -y yum install -y yum-utils device-mapper-persistent-data lvm2

配置软件源信息

yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo

更新安装

yum makecache fast

yum -y install docker-ce

启动docker

service docker start

配置国内源

mkdir -p /etc/docker

tee /etc/docker/daemon.json <<-'EOF' { "debug" : true, "registry-mirrors": ["https://dpayzz9i.mirror.aliyuncs.com/"], "default-address-pools" : [ { "base" : "172.31.0.0/16", "size" : 24 } ] } EOF

重启docker

systemctl restart docker

docker-compose 安装

curl -L "https://github.com/docker/compose/releases/download/1.25.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compos

添加权限

chmod +x /usr/local/bin/docker-compose

2）python3.7安装

安装依赖

yum -y install zlib-devel bzip2-devel openssl-devel ncurses-devel sqlite-devel readline-devel tk-devel gdbm-devel db4-devel libpcap-devel xz-devel
yum install libffi-devel -y

python3.7下载

wget https://www.python.org/ftp/python/3.7.0/Python-3.7.0.tgz

添加配置信息编译安装及添加软连接

./configure --prefix=/usr/python3 --enable-optimizations --with-ssl

make && make install

ln -s /usr/python3/bin/python3 /usr/bin/python3
ln -s /usr/python3/bin/pip3 /usr/bin/pip3

3）安装amundsen

clone项目

git clone --recursive https://github.com/amundsen-io/amundsen.git

导入测试数据

cd Amundsen/databuilder
python3 -m venv venv
source venv/bin/activate
pip3 install --upgrade pip
pip3 install -r requirements.txt
python3 setup.py install
python3 example/scripts/sample_data_loader.py
4）访问地址

amundsen主页访问地址

http://localhost:5000/

neo4j访问地址

http://localhost:7474/browser/

默认账号密码 neo4j/test

5）了解amundsen需要掌握知识点

本人java scala 常用，并对python不熟，要达到能看懂能使用能简单改源码对python的要求并不高（主观感受）

需要对python虚拟环境有一定了解

如需要对功能拓展则对python功底有很低的要求

需要对neo4j图数据库有一定了解

对docker需要有一定了解
源码部分bug 真实数据导入后面补充，如使用者在使用过程中发现错误，请保持对源码怀疑的态度（因我看了之后发现，同一个class多人编写，部分代码并未经过测试）
