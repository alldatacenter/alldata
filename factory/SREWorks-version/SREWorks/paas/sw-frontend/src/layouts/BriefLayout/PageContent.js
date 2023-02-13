/**
 * Created by caoshuaibiao on 2021/2/25.
 */
import React from 'react';
import { Layout, Avatar, Button, PageHeader, Tabs, Statistic, Descriptions, Menu } from 'antd';
import { Link } from 'dva/router';



const { TabPane } = Tabs;
const { Content } = Layout;

const TopMenus = ({ location, currentModule }) => {
    if (!currentModule) return <div />;
    if (currentModule.children && currentModule.children.length > 0) {
        let selectKeys = [];
        const { pathname } = location;
        if (pathname) {
            const getKeys = function (routeData) {
                routeData.forEach(({ path, children, config }) => {
                    if (pathname.includes(path)) {
                        selectKeys.push(path);
                    }
                    if (config && (config.hidden === true || config.hidden === "true")) {
                        if (config.selectPathInHidden) {
                            selectKeys.push(config.selectPathInHidden);
                        }
                    }
                    if (children && children.length > 0) {
                        getKeys(children);
                    }
                })
            };
            getKeys(currentModule.children);
        }
        return (
            <div>
                <Menu
                    selectedKeys={selectKeys}
                    mode="horizontal"
                    style={{ borderBottom: "none", lineHeight: "32px" }}
                >
                    {
                        (currentModule.children || []).map(({ path, children, name, icon, component, label, config }, key) => {
                            //过滤掉带":"的路径,这些一般为子页面而不是菜单
                            if ((path && path.indexOf(":") > 0) || !label) return null;
                            if (config && (config.hidden === true || config.hidden === "true")) return null;
                            if (path) {
                                return (
                                    <Menu.Item key={path}>
                                        <Link to={path}><span>{label}</span></Link>
                                    </Menu.Item>
                                );
                            }
                        })
                    }
                </Menu>
            </div>
        )
    }
    return <div />;
};

export default class PageLayout extends React.Component {

    constructor(props) {
        super(props);

    }

    render() {
        const { menuProps, currentProduct, currentUser, content, layout = { showPageTitle: true } } = this.props;
        const { nameCn, description, descriptions = [] } = currentProduct;
        return (
            <div className="brief-layout-page-content">
                <PageHeader
                    className="common-border brief-page-content globalBackground"
                    title={
                        layout.showPageTitle !== false &&
                        <div className="brief-header-title">
                            <div>
                                <Avatar style={{ fontSize: 36, width: 48, height: 48, lineHeight: "48px", backgroundColor: 'rgb(35, 91, 157)', verticalAlign: 'middle' }} size="large">
                                    {nameCn[0]}
                                </Avatar>
                            </div>
                            <div style={{ display: "grid", marginLeft: 14 }}>
                                <h2 style={{ fontSize: 24 }}>{nameCn}</h2>
                                <p className="ant-page-header-heading-sub-title" style={{ fontWeight: "normal" }}>{description}</p>
                            </div>
                        </div>
                    }
                    footer={
                        <TopMenus {...menuProps} assign={true} />
                    }
                >
                    {
                        descriptions.length > 0 &&
                        <Descriptions size="small" column={3}>
                            {
                                descriptions.map(dItem => {
                                    return <Descriptions.Item label={dItem.label}>{dItem.value}</Descriptions.Item>
                                })
                            }
                        </Descriptions>
                    }
                </PageHeader>

                <Layout className="brief-page-content" style={{ height: `calc(100vh - 60px)`, padding: '0px 3px' }} id="__MAIN_CONTENT__">
                    <Content style={{ margin: '-8px' }}>
                        {content}
                    </Content>
                </Layout>
            </div>
        );
    }
}
