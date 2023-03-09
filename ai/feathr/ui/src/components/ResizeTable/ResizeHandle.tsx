import React, { forwardRef, LegacyRef } from 'react'

import { ResizeHandleProps } from './interface'

const ResizeHandle = (props: ResizeHandleProps, ref: LegacyRef<HTMLSpanElement>) => {
  const { handleAxis, ...restProps } = props

  return (
    <span
      ref={ref}
      className={`react-resizable-handle react-resizable-handle-${handleAxis}`}
      {...restProps}
      onClick={(e) => {
        e.stopPropagation()
      }}
    />
  )
}

const ResizeHandleComponent = forwardRef<HTMLSpanElement, ResizeHandleProps>(ResizeHandle)

ResizeHandleComponent.displayName = 'ResizeHandleComponent'

export default ResizeHandleComponent
