import { TableProps } from 'antd'
import type { ColumnType } from 'antd/es/table'
import { ResizeHandle, ResizableProps } from 'react-resizable'

export interface ResizeTableProps<T> extends Omit<TableProps<T>, 'columns'> {
  columns?: ResizeColumnType<T>[]
}

export interface ResizeColumnType<T> extends ColumnType<T> {
  resize?: boolean
  minWidth?: number
}

export interface ResizableTitleProps {
  onResize?: ResizableProps['onResize']
  width?: ResizableProps['width']
  minWidth?: number
}

export interface ResizeHandleProps {
  handleAxis?: ResizeHandle
}
