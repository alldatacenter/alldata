# ng-tis 介绍

## 介绍
TIS UI前端部分主要依赖于 angular9 [https://angular.cn/](https://angular.cn/)，和 ng-zorro [https://ng-zorro.gitee.io/docs/introduce/zh](https://ng-zorro.gitee.io/docs/introduce/zh)构建而成。

## 构建方法

### 生产环境打包

> 为避免npm install 耗费大量时间，可以先将事先打包好的node_modules包下载到本地中，以便加快打包时间 [https://tis-release.oss-cn-beijing.aliyuncs.com/tis-console-ng-node-modules.tar](https://tis-release.oss-cn-beijing.aliyuncs.com/tis-console-ng-node-modules.tar)

``` shell
npm run ng:serve-aot
```

### 开发环境运行

``` shell
npm run ng:serve-jti
```

## 本地打包部署 on Linux
> 1. nvm install v10.15.3
> 
> 2. npm install -g @angular/cli@12.2.13
> 
> 3. npm run ng:serve-jit --scripts-prepend-node-path=auto
> 
> 4. curl http://localhost:4200


