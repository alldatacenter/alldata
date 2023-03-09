import React, { useCallback, useMemo, useState } from 'react'

import { Table } from 'antd'
import cs from 'classnames'

import { ResizableTitleProps, ResizeTableProps, ResizeColumnType } from './interface'
import ResizableTitle from './ResizableTitle'

import styles from './index.module.less'

const ResizeTable = (props: ResizeTableProps<any>) => {
  const { className, columns: originColumns = [], components: originComponents, ...rest } = props

  const handleResize = useCallback((index) => {
    return ((e, { size }) => {
      setColumns((prevColumns) => {
        const nextColumns = [...prevColumns!]
        nextColumns[index] = { ...nextColumns[index], width: size.width }
        return nextColumns
      })
    }) as ResizableTitleProps['onResize']
  }, [])

  const [columns, setColumns] = useState(
    originColumns?.map((column, index) => {
      if (column.width && column.resize !== false) {
        return {
          ...column,
          onHeaderCell: (col: ResizeColumnType<any>) => ({
            width: col.width,
            minWidth: col.minWidth,
            onResize: handleResize(index)
          })
        }
      }

      return column
    })
  )

  const components = useMemo(() => {
    return {
      ...originComponents,
      header: {
        ...originComponents?.header,
        cell: ResizableTitle
      }
    }
  }, [originComponents])

  return (
    <Table
      className={cs(styles.resizableTable, className)}
      columns={columns}
      components={components}
      {...rest}
    />
  )
}

export default ResizeTable
