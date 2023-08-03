import React from 'react'

import { Resizable } from 'react-resizable'

import { ResizableTitleProps } from './interface'
import ResizeHandle from './ResizeHandle'

import styles from './index.module.less'

const ResizableTitle = (props: ResizableTitleProps) => {
  const { onResize, width, minWidth, ...restProps } = props

  if (!width) {
    return <th {...restProps} />
  }

  return (
    <Resizable
      draggableOpts={{
        enableUserSelectHack: false
      }}
      width={width}
      height={0}
      handle={<ResizeHandle />}
      minConstraints={minWidth ? [minWidth, minWidth] : undefined}
      onResizeStart={() => {
        document.body.classList.add(styles.colResize)
      }}
      onResizeStop={() => {
        document.body.classList.remove(styles.colResize)
      }}
      onResize={onResize}
    >
      <th {...restProps} />
    </Resizable>
  )
}

export default ResizableTitle
