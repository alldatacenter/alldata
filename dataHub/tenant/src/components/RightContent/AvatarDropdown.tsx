import React, { useCallback } from 'react'
// import { LogoutOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons'
import LogoutOutlined from '@ant-design/icons/LogoutOutlined'
import SettingOutlined from '@ant-design/icons/SettingOutlined'
import UserOutlined from '@ant-design/icons/UserOutlined'

import { Avatar, Menu, Spin } from 'antd'
import { history, useModel } from 'umi'
import HeaderDropdown from '../HeaderDropdown'
import styles from './index.less'

export interface GlobalHeaderRightProps {
  menu?: boolean
}

/**
 * 退出登录
 */
const loginOut = async () => {
  window.location.href = '/user/login'
}

const AvatarDropdown: React.FC<GlobalHeaderRightProps> = ({ menu }) => {
  const { initialState, setInitialState } = useModel('@@initialState')

  const onMenuClick = useCallback((event) => {
    const { key } = event

    if (key === 'logout') {
      localStorage.removeItem('token')
      // @ts-ignore
      setInitialState({ ...initialState, currentUser: null })
      loginOut()
      return
    }

    history.push(`/account/${key}`)
  }, [])

  const loading = (
    <span className={`${styles.action} ${styles.account}`}>
      <Spin
        size="small"
        style={{
          marginLeft: 8,
          marginRight: 8,
        }}
      />
    </span>
  )

  if (!initialState) {
    return loading
  }

  const { currentUser } = initialState
  if (!currentUser || !currentUser.username) {
    return loading
  }

  const menuHeaderDropdown = (
    <Menu className={styles.menu} selectedKeys={[]} onClick={onMenuClick}>
      {menu && (
        <Menu.Item key="center">
          <UserOutlined />
          个人中心
        </Menu.Item>
      )}
      {menu && (
        <Menu.Item key="settings">
          <SettingOutlined />
          个人设置
        </Menu.Item>
      )}
      {menu && <Menu.Divider />}

      <Menu.Item key="logout">
        <LogoutOutlined />
        退出登录
      </Menu.Item>
    </Menu>
  )
  return (
    <HeaderDropdown overlay={menuHeaderDropdown}>
      <span className={`${styles.action} ${styles.account}`}>
        <Avatar size="small" className={styles.avatar} icon={<UserOutlined />} alt="avatar" />
        <span className={`${styles.name} anticon`}>{currentUser.username}</span>
      </span>
    </HeaderDropdown>
  )
}

export default AvatarDropdown
