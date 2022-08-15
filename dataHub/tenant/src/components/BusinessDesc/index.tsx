import React from 'react'
import { Descriptions, Divider } from 'antd'
import { Props } from '@/components/BusinessDesc/data'
import isEmpty from 'lodash/isEmpty'
import styles from './index.less'

const BusinessDesc: React.FunctionComponent<Props> = ({
  className,
  title,
  desc,
  description,
  children,
}) => {
  return (
    <main className={`${styles.businessDesc} ${className}`}>
      <div className={styles.title}>
        <h3>{title}</h3>
        <>{desc}</>
      </div>
      <Divider className={styles.divider} />

      {!isEmpty(children) ? (
        <>{children}</>
      ) : (
        <Descriptions className={styles.description} column={{ sm: 4, xs: 1 }}>
          {description.map((i) => (
            <Descriptions.Item key={`${i.key} ${i.label}`} label={i.label} span={i.span}>
              {i.value}
            </Descriptions.Item>
          ))}
        </Descriptions>
      )}
    </main>
  )
}

export default BusinessDesc
