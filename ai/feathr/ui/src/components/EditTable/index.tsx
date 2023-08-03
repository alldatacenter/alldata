import React, { forwardRef, useImperativeHandle } from 'react'

import { Table, Form, ConfigProvider } from 'antd'
import type { ColumnType } from 'antd/es/table'

import EditTableCell from './EditTableCell'
import { EditTableProps, EditColumnType, EditTableInstance } from './interface'

const EditTable = (props: EditTableProps, ref: any) => {
  const [form] = Form.useForm()

  const { columns, isEditing, ...restProps } = props

  const mergedColumns = columns?.map((col: EditColumnType) => {
    if (!col.editable) {
      return col
    }
    return {
      ...col,
      onCell: (record: any) => ({
        record,
        inputType: col.inputType,
        dataIndex: col.dataIndex,
        title: col.title,
        editing: isEditing(record)
      })
    }
  }) as ColumnType<any>[]

  useImperativeHandle<any, EditTableInstance>(
    ref,
    () => {
      return {
        form
      }
    },
    [form]
  )

  return (
    <ConfigProvider
      renderEmpty={() => {
        return 'No tags found.'
      }}
    >
      <Form form={form} component={false}>
        <Table
          {...restProps}
          components={{
            body: {
              cell: EditTableCell
            }
          }}
          columns={mergedColumns}
        />
      </Form>
    </ConfigProvider>
  )
}

export default forwardRef<EditTableInstance, EditTableProps>(EditTable)
