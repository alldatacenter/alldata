### 说明

Vue + iview

### 项目结构

```
├─dist              # 构建后静态资源
├─node_modules
├─public            # 公共index.html
└─src
    ├─apps               # 独立模块
    │  ├─scriptis        # scriptis相关功能
    │  │  ├─assets       # 应用所需的图片、css等资源
    │  │  ├─config       #
    │  │  ├─i18n         # 国际化的中英文json
    │  │  ├─module       # 当前应用所需的模块，每个模块相对独立，模块私有资源内置
    │  │  ├─service
    │  │  └─view         # 当前应用的页面，路由在同级目录下router.js下配置
    │  └─workflows
    ├─common                    # 全局使用的公共服务和方法
    ├─components                # 全局使用的公共组件部分
    ├─dss                       # 主应用，apps里的应用都是其子路由
    ├─main.js                   # 应用启动入口
    └─router.js                 # 合并后的应用路由
```

### 建议/约束

新增功能模块先确定涉及应用，按照上面目录结构维护代码同时建议遵守以下约束：

- apps应用模块间不要相互依赖，可复用部分抽离
- 可复用组件，资源需要合理放置
- 新增功能模块需要按照现有目录约束建立文件，已有功能修改应在有限模块内进行，控制影响范围
- 全局共用组件、公共基础样式、工具方法修改需评估后才能修改，并且重点review


### 前端开发、构建打包

```
# 开发启动DSS
npm run serve

# 打包应用
npm run build
```