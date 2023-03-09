import React, { forwardRef, useEffect, useImperativeHandle, useRef, useState } from 'react'

import { DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { Popconfirm, Button, Space, Table, Form, Input, Typography } from 'antd'

import EditTable from '@/components/EditTable'
import { EditTableInstance, EditColumnType } from '@/components/EditTable/interface'

export interface AddTagsProps {
  onChange?: (tabs: Tag[]) => void
}

export interface AddTagsInstance {
  getTags: () => Tag[]
}

export interface Tag {
  name: string
  value: string
}

const { Link } = Typography

const AddTags = (props: AddTagsProps, ref: any) => {
  const [form] = Form.useForm()

  const { onChange } = props

  const [tags, setTags] = useState<Tag[]>([])

  const editTableRef = useRef<EditTableInstance>(null)

  const [editingKey, setEditingKey] = useState('')

  const isEditing = (record: Tag) => record.name === editingKey

  const updateTabs = (oldTag: Tag, newTag: Tag) => {
    const newData = [...tags]
    const index = newData.findIndex((item) => oldTag.name === item.name)
    if (index > -1) {
      const item = newData[index]
      newData.splice(index, 1, {
        ...item,
        ...newTag
      })
      setTags(newData)
      setEditingKey('')
    } else {
      newData.push(newTag)
      setTags(newData)
      setEditingKey('')
    }
  }
  const onSave = (record: Tag) => {
    const { form } = editTableRef.current!
    form.validateFields().then((tag: Tag) => {
      updateTabs(record, tag)
    })
  }

  const onCancel = () => {
    setEditingKey('')
  }

  const onEdit = (record: Tag) => {
    const { form } = editTableRef.current!
    form?.setFieldsValue(record)
    setEditingKey(record.name)
  }

  const onDelete = (record: Tag) => {
    setTags((tags) => {
      const index = tags.findIndex((tag) => tag.name === record.name)
      tags.splice(index, 1)
      return [...tags]
    })
  }

  const onAdd = () => {
    form.validateFields().then((value: Tag) => {
      updateTabs(value, value)
      form.resetFields()
    })
  }

  const columns: EditColumnType<Tag>[] = [
    {
      title: 'Name',
      dataIndex: 'name',
      editable: true
    },
    {
      title: 'Value',
      dataIndex: 'value',
      editable: true
    },
    {
      title: 'Actions',
      width: 120,
      render: (record: Tag) => {
        const editable = isEditing(record)
        return (
          <Space>
            {editable ? (
              <>
                <Link
                  onClick={() => {
                    onSave(record)
                  }}
                >
                  Save
                </Link>
                <Link onClick={onCancel}>Cancel</Link>
              </>
            ) : (
              <>
                <Link
                  title="edit"
                  onClick={() => {
                    onEdit(record)
                  }}
                >
                  <EditOutlined />
                </Link>
                <Popconfirm
                  title="Sure to delete?"
                  onConfirm={() => {
                    onDelete(record)
                  }}
                >
                  <Link title="delete">
                    <DeleteOutlined />
                  </Link>
                </Popconfirm>
              </>
            )}
          </Space>
        )
      }
    }
  ]

  useImperativeHandle<any, AddTagsInstance>(
    ref,
    () => {
      return {
        getTags: () => {
          return tags
        }
      }
    },
    [form]
  )

  useEffect(() => {
    onChange?.(tags)
  }, [tags])

  return (
    <EditTable
      ref={editTableRef}
      summary={() => (
        <Form form={form} component={false}>
          <Table.Summary>
            <Table.Summary.Row style={{ verticalAlign: 'baseline' }}>
              <Table.Summary.Cell index={1}>
                <Form.Item
                  rules={[
                    {
                      required: true
                    }
                  ]}
                  style={{ marginBottom: 0 }}
                  name="name"
                >
                  <Input />
                </Form.Item>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={2}>
                <Form.Item
                  rules={[
                    {
                      required: true
                    }
                  ]}
                  style={{ marginBottom: 0 }}
                  name="value"
                >
                  <Input />
                </Form.Item>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={3}>
                <Button onClick={onAdd}>Add</Button>
              </Table.Summary.Cell>
            </Table.Summary.Row>
          </Table.Summary>
        </Form>
      )}
      rowKey="name"
      isEditing={isEditing}
      columns={columns}
      pagination={false}
      dataSource={tags}
    />
  )
}

export default forwardRef<AddTagsInstance, AddTagsProps>(AddTags)
