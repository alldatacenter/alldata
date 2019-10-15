*其它语言版本: [English](README.md),[简体中文](README.zh-cn.md).*

[![Build Status](https://semaphoreapp.com/api/v1/projects/d4cca506-99be-44d2-b19e-176f36ec8cf1/128505/shields_badge.svg)](https://github.com/waynell/TinyPngPlugin) [ ![Download](https://api.bintray.com/packages/waynell/maven/TinyPngPlugin/images/download.svg?version=1.0.5) ](https://bintray.com/waynell/maven/TinyPngPlugin/1.0.5/link)

### TinyPngPlugin
`TinyPngPlugin`是一个[TinyPng](https://tinypng.com/)的Gradle插件，它能够批量地压缩你项目中的图片

### 获得Tiny的API Key
在使用该插件前， 你需要先获得一个Tiny的API Key。 首先跳转到[Tiny Developers Page](https://tinypng.com/developers)，然后输入你的姓名和邮箱来获得这个Key

*注意: 一个Key每个月可以免费压缩500张图片，超过500张后就需要付费后才能继续使用*

### 使用教程
首先在根目录中的`build.gradle`文件中添加`TinyPngPlugin`的依赖：

 	dependencies {
    	classpath 'com.waynell.tinypng:TinyPngPlugin:1.0.5'
	}

然后在app目录中的`build.gradle`文件中应用该插件，并配置`tinyinfo`：

 	apply plugin: 'com.waynell.tinypng'

 	tinyInfo {
    	resourceDir = [
			// 你的资源目录
            "app/src/main/res",
            "lib/src/main/res"
    	]
        resourcePattern = [
        	// 你的资源文件夹
        	"drawable[a-z-]*",
            "mipmap[a-z-]*"
        ]
        whiteList = [
        	// 在这里添加文件白名单，支持正则表达式
        ]
        apiKey = 'your tiny API key'
    }

使用`Android Studio`的同学，可以在`tinypng`目录中找到相关的构建任务。或者也可以直接在终端中运行`./gradlew tinyPng`命令来执行任务

`TinyPngPlugin`会将压缩结果保存到`compressed-resource.json`这个文件中，下次再运行任务时会自动跳过那些已经被压缩过的文件

### 致谢
[TinyPIC_Gradle_Plugin](https://github.com/mogujie/TinyPIC_Gradle_Plugin)

### Licence
MIT License

Copyright (c) 2016 Wayne Yang

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
