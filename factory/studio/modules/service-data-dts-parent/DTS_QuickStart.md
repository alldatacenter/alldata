# 数据集成单任务配置（使用datax）

**1、安装datax**

    安装参考[linux安装dataX-CSDN博客](https://blog.csdn.net/hzp666/article/details/127350768)

**2、配置datax同步mysql**
> 配置mysql隔离级别,进入mysql终端
> 
> SET GLOBAL transaction_isolation='READ-COMMITTED';

配置数据源

jdbc:mysql://16gmaster:33060/studio?useUnicode=true&characterEncoding=utf8&useLocalSessionState=true

<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/f1390cfe-078e-40a5-980a-009ac0d3a2ac">
<br/>

<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/6dac7341-230d-423b-8b15-16d418240b83">
<br/>

配置执行器

<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/925f40ee-2730-4c5e-b8e8-ccdac295d562">
<br/>

配置任务模版

<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/6ec02640-046a-49f4-a9ef-05754c878c2a">
<br/>

配置单任务

按步骤选择数据抽取库，数据合并库，映射字段，构建datax的可执行json文件，选择任务模版，点击下一步任务就创建完成了，可以在任务详情里查看

<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/3139101f-0cba-4e44-a82f-62f19cf7dfa1">
<br/>
<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/3e0b4250-1200-4f2f-9ed3-8fde2cc4b8f4">
<br/>
<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/1a9cc66e-9951-41e3-89af-77e830b77ad0">
<br/>
<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/alldata/assets/20246692/7488c7ee-311f-446f-b35c-07091e1aba8d">
<br/>

最后可以运行的json文件

```json
{
	"job": {
		"setting": {
			"speed": {
				"channel": 1,
				"bytes": 0
			},
			"errorLimit": {
				"record": 100
			},
			"restore": {
				"maxRowNumForCheckpoint": 0,
				"isRestore": false,
				"restoreColumnName": "",
				"restoreColumnIndex": 0
			},
			"log": {
				"isLogger": false,
				"level": "debug",
				"path": "",
				"pattern": ""
			}
		},
		"content": [{
			"reader": {
				"name": "mysqlreader",
				"parameter": {
					"username": "root",
					"password": "123456",
					"column": [
                      "menu_id",
                      "pid",
                      "sub_count",
                      "type",
                      "title",
                      "name",
                      "component",
                      "menu_sort",
                      "icon",
                      "path",
                      "i_frame",
                      "cache",
                      "hidden",
                      "permission",
                      "create_by",
                      "update_by",
                      "create_time",
                      "update_time"
					],
					"splitPk": "",
					"connection": [{
						"table": [
							"sys_menu_source"
						],
						"jdbcUrl": [
							"jdbc:mysql://16gmaster:33060/studio?useUnicode=true&characterEncoding=utf8&useLocalSessionState=true"
						]
					}]
				}
			},
			"writer": {
				"name": "mysqlwriter",
				"parameter": {
					"username": "root",
					"password": "123456",
					"writeMode": "insert",
					"column": [
                        "menu_id",
                        "pid",
                        "sub_count",
                        "type",
                        "title",
                        "name",
                        "component",
                        "menu_sort",
                        "icon",
                        "path",
                        "i_frame",
                        "cache",
                        "hidden",
                        "permission",
                        "create_by",
                        "update_by",
                        "create_time",
                        "update_time"
					],
					"connection": [{
						"table": [
							"sys_menu_target"
						],
						"jdbcUrl": "jdbc:mysql://16gmaster:33060/studio?useUnicode=true&characterEncoding=utf8&useLocalSessionState=true"
					}]
				}
			}
		}]
	}
}
```


