Documentation
-------------

The documentation is hosted at https://yuque.antfin-inc.com/bdsre/userguid/hl710y .

Installation
------------

Tesla FaaS requires **Python **Python 2.x >= 2.7**.

Install from aliyun PyPI::

    # 前置依赖
    # 需要本机 pip 版本 >= 8.1.1，推荐 9.0.1：
    $ pip install --trusted-host -i http://mirrors.aliyun.com/pypi/simple -U pip
    # Tesla SDK 依赖，日常环境：
    $ pip install --trusted-host mirrors.aliyun.com -i http://mirrors.aliyun.com/pypi/simple -U git+http://gitlab.alibaba-inc.com/pe3/tesla-sdk-python.git@release/5.8.0
    # Tesla SDK 依赖，生产环境：
    $ pip install --trusted-host mirrors.aliyun.com -i http://mirrors.aliyun.com/pypi/simple -U git+http://gitlab-sc.alibaba-inc.com/pe3/tesla-sdk-python.git@release/5.8.0

    # 安装 v1.0.0 版本，如果需要安装master分支上的最新版本，去掉命令中的 @v1.0.0
    # 生产环境
    $ sudo -H pip install --trusted-host mirrors.aliyun.com -i http://mirrors.aliyun.com/pypi/simple -U git+http://gitlab-sc.alibaba-inc.com/alisre/tesla-faas.git@v1.0.0
    # 测试环境
    $ sudo -H pip install --trusted-host mirrors.aliyun.com -i http://mirrors.aliyun.com/pypi/simple -U git+http://gitlab.alibaba-inc.com/alisre/tesla-faas.git@v1.0.0

    # 在Mac上安装
    $ pip install --user --trusted-host mirrors.aliyun.com -i http://mirrors.aliyun.com/pypi/simple -U  git+http://gitlab.alibaba-inc.com/alisre/tesla-faas.git@v1.0.0
    # 安装完之后 tesla-faas 可执行文件位于 `~/Library/Python/2.7/bin/tesla-faas`, 请自行将`~/Library/Python/2.7/bin/`添加在 PATH 中，方便执行tesla-faas命令


Usage
-----

Basic usage::

    $ tesla-faas [OPTIONS] APP_SRC

Where ``APP_SRC`` is the dir of user handlers code ``~/workspace/tesla-hello/``.
``OPTIONS`` is as follows:  ``--bind localhost:8888 --workers 2``.

Example with test app::

    $ cd examples
    # 本地开发机, 启动两个hello world示例服务
    $ tesla-faas ~/tesla-faas/examples/hello ~/tesla-faas/examples/hello1
    # AliOS 机器
    $ /home/tops/bin/tesla-faas ~/tesla-faas/examples/hello ~/tesla-faas/examples/hello1
    $ curl localhost:8888


