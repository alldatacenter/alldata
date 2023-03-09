import React, { forwardRef, useCallback, useEffect, useState } from 'react'

import { Form, Select, Input, Button, message } from 'antd'

import { listUserRole, addUserRole } from '@/api'

export interface RoleFormProps {
  getRole?: (isAdmin: boolean) => void
}

const { Item } = Form
const { TextArea } = Input

const RoleOptions = [
  { label: 'Admin', value: 'admin' },
  { label: 'Producer', value: 'producer' },
  { label: 'Consumer', value: 'consumer' }
]

const ValidateRule = {
  scope: [{ required: true, message: 'Please select scope!' }],
  userName: [{ required: true, message: 'Please input user name!' }],
  roleName: [{ required: true, message: 'Please select role name!' }],
  reason: [{ required: true, message: 'Please input reason!' }]
}

const RoleForm = (props: RoleFormProps, ref: any) => {
  const [form] = Form.useForm()
  const { getRole } = props
  const [loading, setLoading] = useState<boolean>(false)

  const [scopeOptions, setScopeOptions] = useState<{ label: string; value: string }[]>([])

  const handleFinish = useCallback(
    async (values) => {
      try {
        setLoading(true)
        await addUserRole(values)
        form.resetFields()
        message.success('User role is created successfully.')
      } catch {
        message.error('Failed to create user role.')
      } finally {
        setLoading(false)
      }
    },
    [form]
  )

  const handleInit = useCallback(async () => {
    try {
      const result = await listUserRole()
      if (result.length) {
        const dataset = new Set(
          result.reduce(
            (list: string[], item) => {
              list.push(item.scope)
              return list
            },
            ['global']
          )
        )
        const options = Array.from(dataset).map((item) => {
          return {
            label: item,
            value: item
          }
        })
        setScopeOptions(options)
        return true
      } else {
        return false
      }
    } catch {
      return false
    }
  }, [])

  useEffect(() => {
    handleInit().then((isAdmin: boolean) => {
      getRole?.(isAdmin)
    })
  }, [handleInit, getRole])

  return (
    <Form
      layout="vertical"
      form={form}
      style={{ margin: '0 auto', maxWidth: 600 }}
      onFinish={handleFinish}
    >
      <Item label="Scope" name="scope" rules={ValidateRule.scope}>
        <Select
          showSearch
          placeholder={
            'Select project name in the drop-down list, or use "global" to grant access for all the projects.'
          }
          filterOption={(input: string, option?: any) => {
            return (option!.value as unknown as string).toLowerCase().includes(input.toLowerCase())
          }}
          options={scopeOptions}
        />
      </Item>
      <Item label="User Name" name="userName" rules={ValidateRule.userName}>
        <Input placeholder="Email Account or App Id" maxLength={255} />
      </Item>
      <Item label="Role Name" name="roleName" rules={ValidateRule.roleName}>
        <Select placeholder="Select a role to assign:" options={RoleOptions} />
      </Item>
      <Item label="Reason" name="reason" rules={ValidateRule.reason}>
        <TextArea placeholder="For Audit Purpose" maxLength={50} />
      </Item>
      <Item>
        <Button type="primary" htmlType="submit" loading={loading}>
          Submit
        </Button>
      </Item>
    </Form>
  )
}

const RoleFormComponent = forwardRef<unknown, RoleFormProps>(RoleForm)

RoleFormComponent.displayName = 'RoleFormComponent'

export default RoleFormComponent
