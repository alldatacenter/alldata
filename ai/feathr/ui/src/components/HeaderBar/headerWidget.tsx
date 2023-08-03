import React from 'react'

import { LogoutOutlined, UserOutlined } from '@ant-design/icons'
import { useMsal } from '@azure/msal-react'
import { Dropdown, Avatar } from 'antd'

interface HeaderWidgetProps {
  username?: string
}

const menuItems = [
  {
    key: 'logout',
    icon: <LogoutOutlined />,
    label: 'Logout'
  }
]
const HeaderWidget = (props: HeaderWidgetProps) => {
  const { username } = props

  const { instance } = useMsal()

  const handleMenuClick = () => {
    instance.logoutRedirect().catch((e) => {
      console.error(e)
    })
  }

  if (!username) {
    return null
  }

  return (
    <Dropdown menu={{ items: menuItems, onClick: handleMenuClick }}>
      <div className="dropdown-trigger">
        <Avatar size="small" style={{ marginRight: 4 }} icon={<UserOutlined />} />
        <span>{username}</span>
      </div>
    </Dropdown>
  )
}

export default HeaderWidget
