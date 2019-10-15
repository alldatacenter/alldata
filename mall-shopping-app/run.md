# 配置运行，idea或者android studio（简称as)都能运行

1、生成android.keystore

​	参考链接：https://www.jianshu.com/p/60843a9b19af

2、配置mall-shopping-app\app\build.gradle

​	2.1 如果使用idea打开项目，修改android{}里面的signingConfigs:

```groovy
`signingConfigs {
    release {
        keyAlias 'android.keystore'
        keyPassword '123456'
        storeFile file('C:\\Program Files\\Java\\jdk1.8.0_191\\bin\\android.keystore')
        storePassword '123456'
    }
}`
```
​	2.2 如果使用as打开项目，修改android{}里面的signingConfigs，打开项目结构，修改如下：

![1571111698610](C:\Users\pc\AppData\Roaming\Typora\typora-user-images\1571111698610.png)

3、打开cmd或者控制台，cd到项目根目录mall-shopping-app，执行./gradlew build

4、进入apk目录，mall-shopping-app\app\build\outputs\apk\release，安装app-release.apk，测试功能