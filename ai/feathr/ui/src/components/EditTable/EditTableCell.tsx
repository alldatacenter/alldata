import React from 'react'

import { Input, InputNumber, Form } from 'antd'

import { EditTableCellProps } from './interface'

const { Item } = Form

const EditTableCell = (props: EditTableCellProps) => {
  const { editing, dataIndex, inputType, required, rules, children, ...restProps } = props

  const inputNode = inputType === 'number' ? <InputNumber /> : <Input />

  return (
    <td {...restProps}>
      {editing ? (
        <Item required={required} rules={rules} name={dataIndex} style={{ margin: 0 }}>
          {inputNode}
        </Item>
      ) : (
        children
      )}
    </td>
  )
}

export default EditTableCell
