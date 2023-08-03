import React, { forwardRef, memo } from 'react'

import { RightCircleOutlined } from '@ant-design/icons'
import cs from 'classnames'
import { Handle, NodeProps, Position } from 'react-flow-renderer'
import { useNavigate } from 'react-router-dom'

import { LineageNodeProps } from './interface'

import styles from './index.module.less'

const LineageNode = (props: LineageNodeProps, ref: any) => {
  const navigate = useNavigate()

  const { label, subtitle, version, borderColor, detialUrl, active } = props.data

  const nodeTitle = version ? `${label} (v${version})` : label
  const nodeSubtitle = subtitle.replace('feathr_', '')
  const nodeColorStyle = {
    border: `2px solid ${borderColor}`
  }

  const onNodeIconClick = () => {
    if (detialUrl) {
      navigate(detialUrl)
    }
  }

  return (
    <div
      ref={ref}
      style={active ? undefined : nodeColorStyle}
      className={cs(styles.lineageNode, { [styles.lineageNodeActive]: active })}
    >
      <div className={styles.box}>
        <Handle type="target" position={Position.Left} />
        <div className={styles.title}>
          {nodeTitle}
          {active && <RightCircleOutlined className={styles.navigate} onClick={onNodeIconClick} />}
          <div className={styles.subtitle}>{nodeSubtitle}</div>
        </div>
        <Handle type="source" position={Position.Right} />
      </div>
    </div>
  )
}

const LineageNodeComponent = forwardRef<unknown, NodeProps>(LineageNode)

LineageNodeComponent.displayName = 'LineageNode'

export default memo(LineageNodeComponent)
