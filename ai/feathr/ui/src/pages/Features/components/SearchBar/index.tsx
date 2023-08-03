import React, { useRef } from 'react'

import { Form, Input, Button } from 'antd'
import { useNavigate } from 'react-router-dom'

export interface SearchValue {
  keyword?: string
}

export interface SearchBarProps {
  project: string
  keyword?: string
  onSearch?: (values: SearchValue) => void
}

const { Item } = Form

const SearchBar = (props: SearchBarProps) => {
  const [form] = Form.useForm()

  const navigate = useNavigate()

  const { project, keyword, onSearch } = props

  const timeRef = useRef<any>(null)

  const onChangeKeyword = () => {
    clearTimeout(timeRef.current)
    timeRef.current = setTimeout(() => {
      form.submit()
    }, 350)
  }

  const onNavigateNewFeature = () => {
    navigate(`/${project}/features/new`)
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
        <Item label="Search" name="keyword" initialValue={keyword}>
          <Input placeholder="Search Name" style={{ width: 260 }} onChange={onChangeKeyword} />
        </Item>
      </Form>
      <Button type="primary" onClick={onNavigateNewFeature}>
        + Create Feature
      </Button>
    </div>
  )
}

export default SearchBar
