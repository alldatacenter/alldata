import React from 'react'
import { FormattedMessage } from 'umi'
import styles from './index.less'

const Developing: React.FC<{}> = () => (
  <div className={styles.container}>
    <h1 className={styles.title}>
      <FormattedMessage id="component.developing" />
    </h1>
  </div>
)

export default Developing
