import React from 'react'

import { Space } from 'antd'
import dayjs from 'dayjs'

export interface VersionBarProps {
  className?: string
}
const VersionBar = (props: VersionBarProps) => {
  const { className } = props
  const generatedTime = dayjs(process.env.FEATHR_GENERATED_TIME).format('YYYY-MM-DD HH:mm:DD')

  return (
    <Space className={className} direction="vertical" size="small">
      <span>UI Version: {process.env.FEATHR_VERSION}</span>
      <span>UI Build Generated at {generatedTime}</span>
    </Space>
  )
}

export default VersionBar
