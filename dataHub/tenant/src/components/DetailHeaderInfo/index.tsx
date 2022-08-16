import React from 'react'
import { Card, Col, Descriptions, Row, Skeleton } from 'antd'

import { Props } from './data'
import styles from './index.less'

export default ({ loading = true, infoTitle, info, total }: Props) => (
  <Card className={styles.main} loading={loading} bordered={false}>
    <Skeleton loading={loading} avatar active paragraph={{ rows: 6 }}>
      <Row className={styles.header} justify="space-between">
        <Col xs={18} sm={12}>
          <Card.Meta
            description={
              <Descriptions title={infoTitle} column={{ xs: 1, sm: 2 }}>
                {info.map((v) => (
                  <Descriptions.Item key={v.label} label={v.label}>
                    {v.value}
                  </Descriptions.Item>
                ))}
              </Descriptions>
            }
          />
        </Col>
        <Col xs={6} sm={6}>
          <Row className={styles.totalBox} justify="space-around">
            {total.map((i) => (
              <Col className={styles.totalInfo} sm={8} xs={24} key={i.label}>
                <p className={styles.totalLabel}>{i.label}</p>
                <p className={styles.totalValue}>{i.value}</p>
              </Col>
            ))}
          </Row>
        </Col>
      </Row>
    </Skeleton>
  </Card>
)
