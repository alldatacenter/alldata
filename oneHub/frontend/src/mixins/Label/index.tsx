import React from 'react'
import styles from './index.less'

const Label: React.FC = ({ children }) => {
  return (
    <span className={styles.label}>
      <span className={styles.labelText}>{children}</span>
      <span className={styles.labelColon}>：</span>
    </span>
  )
}

export default Label
