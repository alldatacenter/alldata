import React from 'react'
import { Row, Col } from 'antd'
import { validateDate } from '@/utils/utils'
import { RkProgressChoice, RkProgressProps } from '@/components/Card/data'
import isEmpty from 'lodash/isEmpty'

import styles from './index.less'

export default ({ value }: RkProgressProps) => {
  const choice =
    validateDate(Number(value), 'negative') && !isEmpty(value)
      ? RkProgressChoice.Left
      : RkProgressChoice.Right

  const progressValue = `${Math.abs(Number(value)) * 100 || 0}%`

  return (
    <div className={styles.main}>
      <Row>
        <Col span={10}>
          <div className={styles.left}>
            {choice === RkProgressChoice.Left && (
              <div style={{ width: progressValue }} className={styles.leftColor} />
            )}
          </div>
        </Col>
        <Col span={0.1}>
          <div className={styles.middle} />
        </Col>
        <Col span={10}>
          <div className={styles.right}>
            {choice === RkProgressChoice.Right && (
              <div style={{ width: progressValue }} className={styles.rightColor} />
            )}
          </div>
        </Col>
      </Row>
    </div>
  )
}
