/**
 * @Date: 2020-01-07 14:01:16
 * @LastEditors  : william.xw
 * @LastEditTime : 2020-01-07 17:12:52
 * @param paramsFormat Object 字段名映射 参数为tagText subText
 * @param isRandomColor Boolean 是否开启 动态颜色 每个tag颜色都不同
 * @param autoTheme Boolean 是否开启动态主题 开启后会系统自动生成统一的颜色
 * @param color String 默认的tag颜色
 * 颜色优先级 isRandomColor>autoTheme>color>对象上的颜色
 */
import * as React from 'react';
import { Tag } from 'antd';
import _ from 'lodash';


function AntdTags(props) {
    let tags = [];
    const colors = ['#90ee90', '#2191ee', '#9a69ee', '#41ee1a', '#484aee', '#6B8E23', '#48D1CC', '#3CB371', '#388E8E', '#1874CD'];
    let { value, paramsFormat = {}, isRandomColor = false, color, autoTheme = false } = props;
    if (_.isString(value)) {
        tags = value.split(',')
    } else if (_.isArray(value)) {
        value.map((item, index) => {
            if (_.isString(item)) {
                tags.push(item)
            } else {
                let tagText = item[paramsFormat['tagText']] || item.name || '-';
                let subText = item[paramsFormat['subText']];
                tags.push({ text: tagText + (subText ? '(' + subText + ')' : ''), color: item.color })
            }
        })
    }
    if (autoTheme) {
        color = colors[Math.floor(Math.random() * colors.length)]
    }
    return <span>
        {tags.map((item, r) => <Tag
            color={isRandomColor ? colors[r % 10] : (color || item.color)}
            key={r}>{item.text || item}</Tag>)}
    </span>
}

export default AntdTags;
