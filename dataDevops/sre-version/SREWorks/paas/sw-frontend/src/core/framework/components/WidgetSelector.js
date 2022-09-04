/**
 * Created by caoshuaibiao on 2021/2/1.
 */
import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Card, Tabs, Tooltip, Collapse, List, Avatar, Spin, Input, Button } from 'antd';
import service from '../../services/appMenuTreeService';
//import debounce from 'lodash.debounce';

import './index.less';
import './widgetSelector.less';
import { cloneDeep } from 'lodash';

const { Panel } = Collapse;

const { Search } = Input;
export default class WidgetSelector extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            widgetCategory: null,
            searchText: ""
        };
        //this.handleWidgetSearch = debounce(this.handleWidgetSearch, 300);
    }

    componentWillMount() {
        let { exclude, include, filterType } = this.props;
        service.getWidgetRepository().then((widgetCategory) => {
            let initCategory = [...widgetCategory];
            let customTemplate = widgetCategory.find(item => item.name === 'custom') ? widgetCategory.find(item => item.name === 'custom')['children'][0] : {};
            service.getCustomList().then(customList => {
                let jsxCompList = customList.filter(item => item.configObject.componentType === 'JSX');
                let umdCompList = customList.filter(item => item.configObject.componentType === 'UMD');
                initCategory.forEach(lit => {
                    if (lit.name === 'custom' || lit.name === 'remote') {
                        lit.children = []
                    }
                });
                jsxCompList && jsxCompList.forEach(item => {
                    let cloneTemplate = cloneDeep(customTemplate);
                    cloneTemplate['id'] = item.componentId;
                    cloneTemplate['title'] = item.name;
                    cloneTemplate['name'] = item.name;
                    cloneTemplate['info']['description'] = (item.configObject && item.configObject.description) || '组件使用说明';
                    if (item['configObject'] && item['configObject']['icon']) {
                        cloneTemplate['info']['logos']['small'] = item['configObject']['icon']
                    }
                    initCategory.forEach(lit => {
                        if (lit.name === 'custom') {
                            lit.children.push(cloneTemplate)
                        }
                    });
                })
                umdCompList && umdCompList.forEach(item => {
                    initCategory.forEach(lit => {
                        if (lit.name === 'remote' && window[item.name]) {
                            let templateMeta = cloneDeep(window[item.name][item.name+'Meta']);
                            templateMeta['info']['logos']['small'] = item['configObject']['icon'] || 'https://gw.alipayobjects.com/mdn/rms_7bc6d8/afts/img/A*pUkAQpefcx8AAAAAAAAAAABkARQnAQ'
                            window[item.name] && lit.children.push(templateMeta)
                        }
                    });
                })
                initCategory = initCategory.filter(category => {
                    if (include && include.length) {
                        return include.includes(category.name) && category.children.length > 0
                    }
                    if (exclude && exclude.length) {
                        return !exclude.includes(category.name) && category.children.length > 0
                    }
                    return category.children.length > 0
                });
                if (filterType && filterType.length) {
                    initCategory = initCategory.map(category => {
                        let filterChildren = category.children.filter(c => filterType.includes(c.type));
                        if (filterChildren.length > 0) {
                            return {
                                ...category,
                                children: filterChildren
                            }
                        }
                        return null;
                    }).filter(cate => cate !== null);
                }
                this.setState({
                    widgetCategory: initCategory,
                    filterCategory: initCategory,
                    activeKey: initCategory.length === 1 ? [initCategory[0].title] : []
                });
            })

        })

    }

    handleWidgetSearch = (searchText) => {
        let { widgetCategory } = this.state, newCategory = [], activeKey = [];
        widgetCategory.map(cate => {
            let { children } = cate, newChildren = [];
            children.forEach(widgetMeta => {
                const { title, type, info = { logos: {} } } = widgetMeta;
                const { description } = info;
                let matchString = (title + type + description).toLowerCase(), toMatch = searchText.toLowerCase();
                if (matchString.includes(toMatch)) {
                    newChildren.push(widgetMeta);
                }
            });
            if (newChildren.length > 0) {
                newCategory.push({
                    ...cate,
                    children: newChildren
                });
                if (!activeKey.includes(cate.title)) {
                    activeKey.push(cate.title)
                }
            }

        });
        this.setState({
            activeKey: activeKey,
            filterCategory: newCategory
        });
    };



    onWidgetSelected = (widgetMeta) => {
        const { onWidgetSelected } = this.props;
        onWidgetSelected && onWidgetSelected(widgetMeta);
    };

    handleCategoryClicked = (keys) => {
        this.setState({
            activeKey: keys,
        });
    };

    render() {
        const { filterCategory, activeKey } = this.state;
        if (!filterCategory) return <Spin />;
        return (
            <div className="abm_frontend_widget_component_wrapper">
                <div style={{ textAlign: "right" }}>
                    <Search
                        placeholder="输入组件名称或者功能"
                        enterButton
                        onSearch={this.handleWidgetSearch}
                        style={{ width: "100%", marginBottom: 5 }}
                    />
                </div>
                <Collapse activeKey={activeKey} bordered={false} onChange={this.handleCategoryClicked}>
                    {
                        filterCategory.map(category => {
                            return (
                                <Panel header={<span>{category.title}</span>} key={category.title} style={{ minWidth: 500 }}>
                                    <List
                                        grid={{
                                            gutter: 16,
                                            xs: 2,
                                            sm: 3,
                                            md: 4,
                                            lg: 5,
                                            xl: 5,
                                            xxl: 6,
                                        }}
                                        dataSource={category.children}
                                        renderItem={widgetMeta => {
                                            {
                                                const { info = { logos: {} } } = widgetMeta;
                                                const { small = "", large = "",fontClass="" } = info.logos;
                                                let icon = null, src = null, pic = small || large;
                                                if (pic instanceof Object) {
                                                    src = pic.default
                                                }else if(pic.includes("http") || pic.includes(".png") || pic.startsWith("data:")) {
                                                    src = pic
                                                } else {
                                                    icon = pic
                                                }
                                                return (
                                                    <List.Item>
                                                        <Tooltip title={info.description} key={widgetMeta.type}>
                                                            <Card bordered={false} hoverable size="small" onClick={() => this.onWidgetSelected(widgetMeta)}
                                                                bodyStyle={{ padding: 6 }}

                                                            >
                                                                <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                                                                    <div>
                                                                        { !fontClass && <Avatar shape="square" style={{ margin: "0px 14px" }} size={52} icon={<LegacyIcon type={icon} />} src={src} />}
                                                                        {fontClass && <span className={`iconfont sre-${fontClass}`}></span>}
                                                                    </div>
                                                                    <div style={{ transform: "scale(1)", marginTop: 0, maxWidth: 120 }} className="text-overflow">{widgetMeta.title}</div>
                                                                </div>
                                                            </Card>
                                                        </Tooltip>
                                                    </List.Item>
                                                );
                                            }
                                        }}
                                    />
                                </Panel>
                            );
                        })
                    }
                </Collapse>
            </div>
        );
    }
}