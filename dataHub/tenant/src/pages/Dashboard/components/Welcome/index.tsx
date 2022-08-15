import React from 'react'
import { useModel } from 'umi'
import { Row, Col, Card, Avatar } from 'antd'
import UserOutlined from '@ant-design/icons/UserOutlined'
import { getGrettings } from './utils'

const Welcome: React.FC<{}> = () => {
  const { initialState } = useModel('@@initialState')

  if (!initialState?.currentUser) {
    return null
  }

  const { currentUser } = initialState

  return (
    <Card style={{ marginBottom: 10 }}>
      <Row align="middle" gutter={24}>
        <Col>
          <Avatar size={48} icon={<UserOutlined />} />
        </Col>
        <Col>
          <h3 style={{ margin: 0 }}>
            {getGrettings()}，{currentUser.name || currentUser.username}
          </h3>
        </Col>
      </Row>
    </Card>
  )
}

export default Welcome
