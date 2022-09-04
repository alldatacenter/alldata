# ---备用文档开发一个前端组件

<a name="Bui3v"></a>
### 1、开发约定
>    1.  组件必须在  src/modules/common/FrontEndDesigner/core/widgets/   目录下
>    1. 组件的命名建议以驼峰式命名,第一个字母大写。
>       1. 里面必须包含 ：
> 
**index.js  --**--组件入口
> **meta.js  **----组件描述
>       1. 推荐包含 ：
> 
icon.svg**    --**--组件图标
> svg文件来源推荐 [https://www.iconfont.cn/home/index](https://www.iconfont.cn/home/index) 站点，选取合适的图标后下载 64*64大小，颜色为#4185F4的文件作为图例。
>    3. 组件内能获取的重要属性、方法
>       1. widgetData      ----组件数据源返回的数据。
>       1. widgetConfig   ----组件的定义配置，其中的变量占位已经被替换。
>       1. openAction(action，actionParams，callBack)     ----打开操作的函数
>          1. 参数列表
>             1. action             ---打开的区块唯一标识
>             1. actionParams   ---传递到区块的变量集合,区块中的变量可以通过变量名引用
>             1. callBack	      ----执行后的回调，可为空
> 
其他的属性包括：应用信息、用户信息、路由信息等，可按需获取

<a name="v2Fpo"></a>
### 2、meta定义

   1. 标准格式
```json
{
    "id": "CustomRender",//组件标识
    "type": "CustomRender",//组件类型
    "name": "CustomRender",//组件名称
    "title": "JSX渲染器",//组件类型标题
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "jsx渲染块",//选择组件时候提示信息
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icon.svg'),//组件的图标
        },
        "build": {
            "time": "",
            "repo": "",
            "branch": "",
            "hash": ""
        },
        "screenshots": [],
        "updated": "",
        "version": "",
        "docs":"### 组件MarkDown文档 <br/><div><span>html区域</span><code>json</code></div>",//组件文档
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
        "defaults": { //组件默认mock配置，用于选择后出现组件预览
            "type":"CustomRender",
            "config":{
                "jsx": "填写标准的JSX"
            }
        },
        "schema": {//组件属性的schema定义，通过此定义生成组件属性面板
            "type": "object",
            "properties": {
                "jsx": {
                    "description": "填写标准的JSX",//属性表单提示
                    "title": "JSX模板",//属性标题
                    "pattern": "[a-z]",//属性合法正则校验
                    "required": false,//是否必填
                    "x-component": "Text",//用那个展示形式
                     "x-visibleExp":"config.topMenuType==='menu'",//依赖可见表达式,依赖config中的那个表单值
                    "type": "string"//数据类型
                }
            }
        },
        "dataMock": {}
    },
    "catgory": "base"//所属分类
};
```

   2. meta 枚举值
      1. catgory 取值枚举
```json
   {
       name:"base",
       title:"基础组件",
    },
    {
        name:"layout",
        title:"布局组件",

    },
    {
        name:"filter",
        title:"过滤器",

    },
    {
        name:"action",
        title:"操作",

    },
    {
        name:"block",
        title:"区块",
  
    },
    {
        name:"biz",
        title:"业务组件",

    },
    {
        name:"custom",
        title:"自定义组件",

    },
    {
        name:"other",
        title:"其他",

    }
```

      2. schema 中x-component 取值枚举
         1. 通过json schema生成属性可视化表单类型已支持类型：

Input  --普通输入<br />Text   --文本输入<br />Select -- 下拉选择<br />MultiSelect --下拉多选<br />CheckBox  -- checkbox多选<br />Radio        -- radio 单选<br />EditTable    ---可编辑表格<br />JSON        --JSON 编辑器

         2. JSON Schema   
            1. 标准定义格式参考:[https://yuque.antfin-inc.com/docs/share/f1555cbe-63dd-4250-ae77-e2745391f803?spm=ata.13261165.0.0.7ffc6aa5hR1ngc](https://yuque.antfin-inc.com/docs/share/f1555cbe-63dd-4250-ae77-e2745391f803?spm=ata.13261165.0.0.7ffc6aa5hR1ngc)
            1. 选择类型组件扩展属性定义(Select,MultiSelect,CheckBox,Radio) 示例
```json
"x-component-props":{
  // options里面定义了下来选项可选择对象
    "options":[{"value":"menu","label":"菜单类型"},{"value":"link","label":"链接类型"}]
}
```

            3.  EditTable  组件扩展属性定义 示例
```json
"x-component-props":{
  											//columns定义了编辑表格的列
                        "columns": [
                            {
                                "editProps": {
                                    "required": false,
                                    "type": 1,
                                    "inputTip":"帮助文档的标题",
                                },
                                "dataIndex": "title",
                                "title": "标题"
                            },
                            {
                                "editProps": {
                                    "required": false,
                                    "inputTip":"帮助文档的链接路径",
                                    "type": 1
                                },
                                "dataIndex": "href",
                                "title": "链接路径"
                            },
                            {
                                "editProps": {
                                    "required": false,
                                    "inputTip":"链接的描述",
                                    "type": 1
                                },
                                "dataIndex": "description",
                                "title": "描述"
                            },
                            {
                                "editProps": {
                                    "required": false,
                                    "type": 1,
                                    "inputTip":"antd3中的icon名或者图片地址或图片base64",
                                },
                                "dataIndex": "icon",
                                "title": "图标"
                            },

                        ]
                    }
```
                 其中editProps 中type可定义的类型：type 值与下面的value值等同含义
```json
{value:1,label:'普通输入'},
        {value:2,label:'文本输入'},
        {value:3,label:'下拉单选'},
        {value:4,label:'下拉多选'},
        {value:5,label:'日期选择'},
        {value:6,label:'日期范围'},
        {value:7,label:'可输入标签'},
        {value:12,label:'选择树'},
        {value:10,label:'Radio 单选'},
        {value:11,label:'CheckBox 多选'},
        {value:13,label:'时间选择'},
        {value:14,label:'Radio 按钮'},
        {value:15,label:'Slider 滑条'},
        {value:16,label:'Switch 开关'},
        {value:17,label:'级联单选'},
        {value:18,label:'密码输入'},
        {value:98,label:'人员选择'},
        {value:87,label:'分组输入'},
        {value:86,label:'JSONEditor'},
        {value:85,label:'Table'},
        {value:84,label:'关联分组'},
        {value:83,label:'AceView'},
        {value:82,label:'OamWidget'},
        {value:81,label:'文件上传'},
        {value:79,label:'SchemaForm'},
        {value:78,label:'脚本气泡卡片'},
        {value:70,label:'卡片选择'}
```

            4. meta schema属性生成单项示例，key为配置项名，值为配置项值的输入定义
               1. 输入型
```javascript
"description": {
                    "description": "布局组件Header显示的描述信息",
                    "title": "描述",
                    "required": false,
                    "x-component": "Input",
                    "x-visibleExp":"config.topMenuType==='menu'",
                    "type": "string"
                }
```

               2. 选择型

```javascript
"topMenuType": {
                    "description": "顶部菜单展示方式",
                    "title": "菜单展示方式",
                    "required": false,
                    "type": "string",
                    "x-component": "Radio",
                    "x-component-props":{
                        "options":[{"value":"menu","label":"Menu"},{"value":"link","label":"Link"}]
                    }
                }
```

               3. 编辑表格
```json
"links":{
                    "description": "若为动态集合,可前往源JSON中直接填写占位变量,格式为$(链接集合变量名)。",
                    "title": "链接集合",
                    "required": false,
                    "x-component": "EditTable",
                    "type": "string",
                    "x-component-props":{
                        "columns": [
                            {
                                "editProps": {
                                    "required": false,
                                    "type": 1,
                                    "inputTip":"帮助文档的标题",
                                },
                                "dataIndex": "title",
                                "title": "标题"
                            },
                            {
                                "editProps": {
                                    "required": false,
                                    "inputTip":"帮助文档的链接路径",
                                    "type": 1
                                },
                                "dataIndex": "href",
                                "title": "链接路径"
                            },
                            {
                                "editProps": {
                                    "required": false,
                                    "inputTip":"链接的描述",
                                    "type": 1
                                },
                                "dataIndex": "description",
                                "title": "描述"
                            },
                            {
                                "editProps": {
                                    "required": false,
                                    "type": 1,
                                    "inputTip":"antd3中的icon名或者图片地址或图片base64",
                                },
                                "dataIndex": "icon",
                                "title": "图标"
                            },

                        ]
                    }
                }
```
