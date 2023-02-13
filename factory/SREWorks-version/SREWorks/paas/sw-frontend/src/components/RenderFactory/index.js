/**
 * @author caoshuaibiao
 * @date 2021/5/20 10:35
 * @Description:render 注册
 */

import * as antd from 'antd';
import '@ant-design/compatible/assets/index.css';
//antd3 icon 注册兼容
import { Icon as LegacyIcon } from '@ant-design/compatible';
//antd4中icon注册
import * as icons from '@ant-design/icons';
antd.Icon=LegacyIcon;
let commonRenders={};
const commonRenderContext=require.context('./', true, /^\.\/common\/((?!\/)[\s\S])+\/index\.js$/);
commonRenderContext.keys().forEach(key => {
    //获取每个挂件包,以包名为key注册到内置组件映射挂件对象上
    let name=key.split("/")[2];
    commonRenders[name]=commonRenderContext(key)
});

let renders={
    "antd":antd,
    "common":commonRenders,
    "icon":icons
};
export default function getRenders() {
    return renders;
}