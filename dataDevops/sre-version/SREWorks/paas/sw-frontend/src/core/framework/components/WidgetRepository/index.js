/**
 * Created by caoshuaibiao on 2021/2/1.
 * 组件的存储库,扫描内置组件定义生成组件及组件分类
 */
import { getBuiltInWidgetMetaMapping } from '../../core/BuiltInWidgets';

const widgetMetasContext=require.context('./', true, /^\.\/meta\/[\s\S]*\.js$/);
let builtInWidgetList=[...Object.values(getBuiltInWidgetMetaMapping())];
// builtInWidgetList = builtInWidgetList.push(window.CarouselCompBackup.CarouselCompMeta)
const builtInWidgetCatgory=[
    {
        name:"base",
        title:"基础组件",
        children:[]
    },
    {
        name:"staticComp",
        title:"landing组件",
        children:[]
    },
    {
        name:"layout",
        title:"布局组件",
        children:[]
    },
    {
        name:"filter",
        title:"过滤器",
        children:[]
    },
    {
        name:"action",
        title:"操作",
        children:[]
    },
    {
        name:"block",
        title:"区块",
        children:[]
    },
    {
        name:"charts",
        title:"图表",
        children:[]
    },
    {
        name:"biz",
        title:"业务组件",
        children:[]
    },
    {
        name:"custom",
        title:"自定义组件",
        children:[]
    },
    {
        name:"remote",
        title:"远程组件",
        children:[]
    }
    // {
    //     name:"other",
    //     title:"其他",
    //     children:[]
    // }
];
const builtInWidgetMetaMapping={};
widgetMetasContext.keys().forEach(key => {
    let widgetName=key.split("/")[2];
    const meta=widgetMetasContext(key);
    if(!meta.type){
        meta.type=widgetName;
    }
    builtInWidgetList.push(meta);
    builtInWidgetMetaMapping[meta.type]=meta;
});
//合并新的组件渲染引擎机制,此处需全部迁移至新的里面

builtInWidgetList.forEach(widgetMeta=>{
    builtInWidgetCatgory.forEach(cat=>{
       if(cat.name===(widgetMeta.catgory||"other")){
           cat.children.push(widgetMeta);
       }
    });
});
export function getBuiltInWidgetList(){
    return builtInWidgetList;
}

export function getBuiltInWidgetCatgory() {
    return builtInWidgetCatgory;
}

export function getLegacyWidgetMeta(model) {
    return builtInWidgetMetaMapping[model.type];
}