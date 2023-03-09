import { TableProps, FormRule, FormInstance } from 'antd'
import type { ColumnType } from 'antd/es/table'

export interface EditTableProps<T = any> extends TableProps<T> {
  // columns?: EditColumnType<T>[]
  isEditing: (record: any) => boolean
}

export interface EditColumnType<T = any> extends ColumnType<T> {
  editable?: boolean
  inputType?: 'number' | 'text'
  rules?: FormRule[]
  required?: boolean
}

export interface EditTableCellProps<T = any> extends React.HTMLAttributes<HTMLElement> {
  editing?: boolean
  dataIndex: string
  inputType?: 'number' | 'text'
  rules?: FormRule[]
  required?: boolean
  children?: React.ReactNode
}

export interface EditTableInstance {
  form: FormInstance
}
