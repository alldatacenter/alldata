import React from 'react';
import { Outlet, Link, useParams, useNavigate } from 'react-router-dom'
import { UploadOutlined, UserOutlined, VideoCameraOutlined } from '@ant-design/icons';
import { Layout, Menu, theme } from 'antd';

const { Header, Content, Footer, Sider } = Layout
const App = () => {
  const {
    token: { colorBgContainer },
  } = theme.useToken()

  const iconMap = {
    'sql': <UserOutlined />,
    'task-manage': <UserOutlined />,
  }

  const menuMap = [
    {
      key: 'sql',
      icon: UserOutlined,
      label: 'Sql',
      url: 'sql',
    },
    {
      key: 'task-manage',
      icon: UserOutlined,
      label: 'Task Manage',
      url: 'task-manage',
    }
  ]
  return (
    <Layout>
      <Sider
        breakpoint="lg"
        collapsedWidth="0"
        onBreakpoint={(broken) => {
          console.log(broken);
        }}
        onCollapse={(collapsed, type) => {
          console.log(collapsed, type);
        }}
      >
        <div className="logo" />
        <Menu
          theme="dark"
          mode="inline"
          defaultSelectedKeys={['4']}
          items={menuMap.map(
            
            (item, index) => ({
              key: String(index + 1),
              icon: React.createElement(item.icon),
              label: <Link 
              to={`/${item.url}`}
              // state={{url: t.url}}
            >{item.label}</Link>,
            }),
          )}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: 0,
            background: colorBgContainer,
          }}
        >
          <div></div>
        </Header>
        <Content
          style={{
            margin: '24px 16px 0',
          }}
        >
          <div
            style={{
              padding: 24,
              minHeight: 360,
              background: colorBgContainer,
            }}
          >
            <Outlet/>
          </div>
        </Content>
        <Footer
          style={{
            textAlign: 'center',
          }}
        >
          Flink SQL Lineage  ©2023
        </Footer>
      </Layout>
    </Layout>
  );
}

export default App
