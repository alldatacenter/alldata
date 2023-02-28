import React, { useState } from 'react';
import { Layout } from 'antd';
import { MenuUnfoldOutlined, MenuFoldOutlined } from '@ant-design/icons';
import AsideMenu, { MenuItem } from '../MenuAside';
import HeaderTop from '../../Header';
import './index.less';

const { Header, Content, Sider } = Layout;

type TMainProps = {
    menus: MenuItem[],
    visible?: boolean,
    children?:React.ReactNode
};

const MenuLayout: React.FC<TMainProps> = ({ children, menus, visible = true }) => {
    const [collapsed, setCollapsed] = useState(true);
    const onCollapse = (bool: boolean) => {
        setCollapsed(bool);
    };
    if (!visible) {
        return <>{children}</>;
    }
    return (
        <Layout>
            {/* <Header className="dv-header-layout">
                <HeaderTop />
            </Header> */}
            <Layout>
                <Sider
                    style={{
                        height: '100vh', overflow: 'auto', backgroundColor: 'transparent',
                    }}
                    trigger={(
                        <div style={{ position: 'absolute', right: 15, fontSize: 16 }}>
                            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                        </div>
                    )}
                    collapsed={collapsed}
                    onCollapse={onCollapse}
                >
                    <AsideMenu
                        menus={menus}
                    />
                </Sider>
                <Layout>
                    <Content>
                        {children}
                    </Content>
                </Layout>
            </Layout>
        </Layout>
    );
};

export default MenuLayout;
