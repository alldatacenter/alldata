import React from 'react'

import { DescriptionProps } from '@/components/Card/data'
import styles from './index.less'

const Description: React.FunctionComponent<DescriptionProps> = ({
  className,
  label,
  value,
  icon = false,
}) => {
  return (
    <div className={`${styles.description} ${className}`}>
      <div className={styles.desTitle}>{label}</div>
      <div className={styles.desValue}>
        {icon && icon}
        <div className={styles.text}>{value}</div>
      </div>
    </div>
  )
}

export default Description
