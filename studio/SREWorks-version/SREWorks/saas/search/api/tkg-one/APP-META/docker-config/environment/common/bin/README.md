这套脚本是基于 java jar 应用类型的脚本升级过来，同时增加了对 Service 应用标准（Pandora boot）的支持；

## 线上环境

线上环境请使用`master`分支。由PE来配置，如果有PE权限，可以直接在psp上变更。

## 日常环境

日常环境请使用`daily`分支。

如果是新应用，可以直接通过aone来配置，参考：http://gitlab.alibaba-inc.com/middleware-container/pandora-boot/wikis/aone-guide

对于旧应用，需要应用方申请svn权限，把新脚本提交，参考：http://docs.alibaba-inc.com/pages/viewpage.action?pageId=316638780

## master分支和daily分支的不同点

只有一个不同的地方：

daily分支默认开启了 `jpda` java debug的支持，方便应用在日常开发时直接debug。

## JDK8相关

ali-jdk的官方文档： https://lark.alipay.com/aone355606/gfqllg/ulptif

推荐使用新版本的`ali-jdk`，它的安装位置是 `/opt/taobao/java`。脚本里默认的`JAVA_HOME`值就是这个。

**也就是使用新版本的`ali-jdk`不需要做任何的配置。**

## 旧版本的jdk8应用参考

http://gitlab.alibaba-inc.com/middleware-container/pandora-boot/issues/370


## 链接

[理解PSP中的关键概念 - 基线](https://www.atatech.org/articles/79890)

[基线修改参考：Sar 升级 + 基线变更（增加 permSize 大小）踩坑实录](https://www.atatech.org/articles/87023)

[Pandora Boot](http://gitlab.alibaba-inc.com/middleware-container/pandora-boot/wikis/home)

[ali-jdk的官方文档](https://lark.alipay.com/aone355606/gfqllg/ulptif)