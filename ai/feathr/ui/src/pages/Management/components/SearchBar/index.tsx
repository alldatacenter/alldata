import React, { forwardRef } from 'react'

import { SearchOutlined } from '@ant-design/icons'
import { Form, Select, Input, Button } from 'antd'
import { useNavigate } from 'react-router-dom'

export interface SearchBarProps {
  onSearch: (values: any) => void
}

const { Item } = Form

const RoleOptions = [
  { label: 'Admin', value: 'admin' },
  { label: 'Producer', value: 'producer' },
  { label: 'Consumer', value: 'consumer' }
]

const SearchBar = (props: SearchBarProps, ref: any) => {
  const [form] = Form.useForm()

  const navigate = useNavigate()

  const { onSearch } = props

  const onClickRoleAssign = () => {
    navigate('/management/role')
  }

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        marginBottom: 16
      }}
    >
      <Form layout="inline" form={form} onFinish={onSearch}>
        <Item label="Scope" name="scope">
          <Input
            allowClear
            placeholder="Scope (Project / Global)"
            autoComplete="off"
            style={{ width: 260 }}
          />
        </Item>
        <Item name="roleName">
          <Select allowClear placeholder="Role Name" options={RoleOptions} style={{ width: 200 }} />
        </Item>
        <Item>
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
            Search
          </Button>
        </Item>
      </Form>
      <Button type="primary" onClick={onClickRoleAssign}>
        + Create Role Assignment
      </Button>
    </div>
  )
}

const SearchBarComponent = forwardRef<unknown, SearchBarProps>(SearchBar)

SearchBarComponent.displayName = 'SearchBarComponent'

export default SearchBarComponent
