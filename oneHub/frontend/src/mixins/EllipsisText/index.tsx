import React, { CSSProperties, ReactNode } from 'react'
import { Popover, Tooltip } from 'antd'
import classNames from 'classnames'

import styles from './index.less'

interface EllipsisTextProps {
  className?: string
  rows?: number
  hasPopover?: boolean
  hasTooltip?: boolean
  title?: ReactNode
  content?: ReactNode
  trigger?: string[] | string
  style?: CSSProperties
  placement?:
    | 'top'
    | 'left'
    | 'right'
    | 'bottom'
    | 'topLeft'
    | 'topRight'
    | 'bottomLeft'
    | 'bottomRight'
    | 'leftTop'
    | 'leftBottom'
    | 'rightTop'
    | 'rightBottom'
}

const EllipsisText: React.FunctionComponent<EllipsisTextProps> = ({
  className,
  rows = 1,
  hasPopover = false,
  hasTooltip = false,
  title,
  content,
  trigger = 'hover',
  placement = 'rightTop',
  style,
  children,
}) => {
  const clazz =
    rows === 1
      ? classNames([styles.ellipsisText, styles.line])
      : classNames([styles.ellipsisText, styles.lines])

  const renderText = (
    <p className={clazz} style={{ lineClamp: rows }}>
      {children}
    </p>
  )
  let contentNode = renderText

  if (hasPopover) {
    contentNode = (
      <Popover title={title} content={content} trigger={trigger} placement={placement}>
        {renderText}
      </Popover>
    )
  }

  // 如果同时给 hasPopover 和 hasTooltip，就覆盖
  if (hasTooltip) {
    contentNode = (
      <Tooltip title={content} trigger={trigger} placement={placement}>
        {renderText}
      </Tooltip>
    )
  }

  return (
    <div className={classNames([styles.main, className])} style={style}>
      {contentNode}
    </div>
  )
}

export default EllipsisText
