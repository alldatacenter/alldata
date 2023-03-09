import React, { forwardRef, useRef } from 'react'

import { Form, Input } from 'antd'

export interface SearchBarProps {
  onSearch: (values: any) => void
}

const { Item } = Form

const SearchBar = (props: SearchBarProps, ref: any) => {
  const [form] = Form.useForm()

  const { onSearch } = props

  const timeRef = useRef<any>(null)

  const onChangeKeyword = () => {
    clearTimeout(timeRef.current)
    timeRef.current = setTimeout(() => {
      form.submit()
    }, 350)
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
        <Item label="Search" name="project">
          <Input
            allowClear
            placeholder="Search Name"
            autoComplete="off"
            style={{ width: 260 }}
            onChange={onChangeKeyword}
          />
        </Item>
      </Form>
    </div>
  )
}

const SearchBarComponent = forwardRef<unknown, SearchBarProps>(SearchBar)

SearchBarComponent.displayName = 'SearchBarComponent'

export default SearchBarComponent
