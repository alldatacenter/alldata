# APP版本更新、强制更新、漂亮的更新界面、IOS更新（跳转IOS store）、wgt更新

### QQ交流群(学习干货多多) 607391225
![QQ交流群](http://qn.kemean.cn//upload/202004/14/15868301778472k7oubi6.png)

# [点击跳转-插件示例](https://ext.dcloud.net.cn/plugin?id=2009)
# [点击跳转-5年的web前端开源的uni-app快速开发模板-下载看文档](https://ext.dcloud.net.cn/plugin?id=2009)
 
### 常见问题
1.安卓apk下载完成后没有更新APP?

答：问题是因为没有添加APP安装应用的权限，解决方法在`manifest.json`文件里面`APP模块权限配置`的`Android打包权限配置`勾选以下权限
```
<uses-permission android:name=\"android.permission.INSTALL_PACKAGES\"/>  
<uses-permission android:name=\"android.permission.REQUEST_INSTALL_PACKAGES\"/>
```
若还有问题请看[安装apk无法执行的解决方案](https://ask.dcloud.net.cn/article/35703 "安装apk无法执行的解决方案")

2.APP更新后版本号没变，还是之前的版本号？

答：可能是更新的安装包没有升级版本号，`manifest.json`文件里面基本设置`应用版本号`和`应用版本名称`需要升高

3.APP更新后没有覆盖之前的APP？

答：可能是更新的安装包`包名`和APP的`包名`不一样

### 第一步配置APP更新接口
在`APPUpdate.js`里面`getServerNo`函数方法配置更新接口
```
let httpData = {
	version:version
};
if (platform == "android") {
	httpData.type = 1101;
} else {
	httpData.type = 1102;
}
/* 接口入参说明
 * version: 应用当前版本号（已自动获取）
 * type：平台（1101是安卓，1102是IOS）
 */ 
// 可以用自己项目的请求方法
$http.get("api/kemean/aid/app_version", httpData).then(res => {
	/*接口出参说明 （res数据说明）
	* | 参数名称	     | 一定返回 	| 类型	    | 描述
	* | -------------|--------- | --------- | ------------- |
	* | versionCode	 | y	    | int	    | 版本号        |
	* | versionName	 | y	    | String	| 版本名称      |
	* | versionInfo	 | y	    | String	| 版本信息 \n 换行（例如：1.修改了bug1 \n 2.修改了bug2 \n 3.修改了bug3）      |
	* | forceUpdate	 | y	    | boolean	| 是否强制更新  |
	* | downloadUrl	 | y	    | String	| 版本下载链接 `IOS安装包更新请放跳转store应用商店链接,安卓apk和wgt文件放文件下载链接` |
	*/
   // 只要callback上面的数据就ok(返回数据就表示接口允许更新)
   // 示例返回数据
   callback && callback({
	   versionCode: 101,
	   versionName: "1.0.1",
	   versionInfo: "1.修改了bug1 \n 2.修改了bug2 \n 3.修改了bug3",
	   forceUpdate: false,
	   downloadUrl: "http://www.xxx.com/download/123",
   });
});
```

### 第二步 使用方法
``` 
// App.vue页面

// #ifdef APP-PLUS
import APPUpdate from "@/plugins/APPUpdate";
// #endif

onLaunch: function(e) {
	// #ifdef APP-PLUS
	APPUpdate();
	// #endif
}
```

### 第三步 添加APP安装应用的权限
在`manifest.json`文件里面`APP模块权限配置`的`Android打包权限配置`勾选以下权限
```
<uses-permission android:name=\"android.permission.INSTALL_PACKAGES\"/>  
<uses-permission android:name=\"android.permission.REQUEST_INSTALL_PACKAGES\"/>
```

### 修改弹窗的主题色或弹窗图标
在`APPUpdate.js`里面上面`$mainColor`常量中定义主题颜色，`$iconUrl`常量中定义图标地址

### 检查APP是否有新版本（一般在设置页面使用）
```
// #ifdef APP-PLUS
import APPUpdate, { getCurrentNo } from "@/plugins/APPUpdate";
// #endif
export default {
	data() {
		return {
			version: "" // 版本号
		};
	},
	//第一次加载
	onLoad(e) {
		// #ifdef APP-PLUS
		getCurrentNo(res => {
			// 进页面获取当前APP版本号（用于页面显示）
			this.version = res.version;
		});
		// #endif
	},
	//方法
	methods: {
		// 检查APP是否有新版本
		onAPPUpdate() {
			// true 没有新版本的时候有提示，默认：false
			APPUpdate(true);
		}
	}
}
```