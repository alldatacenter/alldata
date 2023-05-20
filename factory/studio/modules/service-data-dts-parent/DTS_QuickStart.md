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
<img width="1215" alt="image" src="https://github.com/alldatacenter/all-in-data/assets/20246692/681562cc-6094-4e55-acd7-d3aee892c722">
<br/>

<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/all-in-data/assets/20246692/ae42dff9-7468-43ba-a38e-9f966b4757a1">
<br/>

配置执行器

<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/all-in-data/assets/20246692/9fa31f20-9676-4328-b8c0-fe1778358ee8">
<br/>

配置任务模版

<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/all-in-data/assets/20246692/6c9f41a3-dfbd-4b64-8b1b-52409076e936">
<br/>

配置单任务

按步骤选择数据抽取库，数据合并库，映射字段，构建datax的可执行json文件，选择任务模版，点击下一步任务就创建完成了，可以在任务详情里查看

<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/all-in-data/assets/20246692/4a6c7cff-ab29-47fa-b6bc-b0299ce075d9">
<br/>
<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/all-in-data/assets/20246692/2cbd5198-85eb-4b80-93fe-203391726e13">
<br/>
<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/all-in-data/assets/20246692/12a6f22c-7a28-49c3-8c3d-74fca9d680b4">
<br/>
<br/>
<img width="1215" alt="image" src="https://github.com/alldatacenter/all-in-data/assets/20246692/b9dbec32-82c2-48de-97c6-94875b7a39d9">
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


