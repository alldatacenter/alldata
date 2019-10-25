# mall-shopping-app构建：

mall-shopping-mobile(vue项目）打包android项目

1、下载cordova

​	打开命令行，执行npm install -g cordova

2、新建mall-shopping-app项目

执行命令：cordova create mall-shopping-app com.platform mall-shopping-app

注释：cordova create mall-shopping-app（这是目录名称） com.platform mall-shopping-app（这是项目名称）

3、删除www目录下面的文件和文件夹，复制mall-shopping-mobile/dist的*.icon文件、static文件夹、index.html文件到www空文件夹

4、cd mall-shopping-app，执行>cordova platform add android@6.3.0

5、创建android.keystore(如何创建anroid.keystore参考：https://www.cnblogs.com/strinkbug/p/6858723.html)

6、cd mall-shoppingn-app，创建build.json文件，输入内容如下：

`{`
  `"android": {`
    `"release": {`
    `"keystore": "C:\\Program Files\\Java\\jdk1.8.0_191\\bin\\android.keystore",`
    `"alias": "android.keystore",`
    `"storePassword": "123456",`
    `"password": "123456"`
    `}`
  `}`
`}`

7、打包

cd mall-shoppingn-app，执行cordova build android --release

cd mall-shopping-app\platforms\android\app\build\outputs\apk\release查看app-release.apk已生成，安装使用。

8、本地运行android项目

cd mall-shopping-app，启动app-debug.bak，执行cordova run android；

启动app-release.bak，执行cordova run android --prod --release

(https://blog.csdn.net/SDF_crazy/article/details/85228452）（复制emulator目录下的文件到tools重新执行即可，全部覆盖即可)

![571911110805](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/samples/mall-shopping-03.png)

![571911110805](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/samples/shopping-app-04.png)

![571911110805](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/samples/shopping-app-05.png)

![571911110805](https://my-macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/samples/shopping-app-07.png)

