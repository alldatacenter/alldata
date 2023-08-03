import React, { useRef } from 'react'

import { Card, Typography, Alert, Space } from 'antd'

import SearchBar from './components/SearchBar'
import UserRolesTable, { SearchModel, UserRolesTableInstance } from './components/UserRolesTable'

const { Title } = Typography

const Management = () => {
  const tableRef = useRef<UserRolesTableInstance>(null)

  const handleSearch = (values: SearchModel) => {
    tableRef.current?.onSearch?.(values)
  }

  return (
    <div className="page">
      <Card>
        <Space className="display-flex" direction="vertical">
          <Alert
            message="This page is protected by Feathr Access Control. Only Project Admins
      can retrieve management details and grant or delete user roles."
            type="info"
          />
          <Title level={3}>Role Management </Title>
          <SearchBar onSearch={handleSearch} />
        </Space>
        <UserRolesTable ref={tableRef} />
      </Card>
    </div>
  )
}

export default Management
