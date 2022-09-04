#### 组件开发
../TemplateComponent为一个标准的组件模板： 包含一份组件主文件 “index.js”, 一份组件属性描述文件 “meta.js”用于组件在设计器进行编辑，编辑表单形式参考 ./meta_items.md;
#### 主入口文件 umd.js 导出命名规则
// @extract导出命名格式 “组件名”+“meta” 
export { CarouselCompFive, CarouselCompFiveMeta }
####
打包构建umd: npm run umd                                  包名              版本          main路径
npm publish 发布获得 cdn地址 https://cdn.jsdelivr.net/npm/carousel_comp_five@0.0.3/build/dist/index.umd.js  或将打包后组件上传至自定义位置
