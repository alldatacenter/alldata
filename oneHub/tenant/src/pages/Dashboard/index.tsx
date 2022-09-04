import React from 'react'
import { useAccess, Access } from 'umi'
import { GridContent } from '@ant-design/pro-layout'
import Welcome from './components/Welcome'
import styles from './styles.less'

const Dashboard: React.FC<{}> = () => {
  const access = useAccess()

  return (
    <GridContent className={styles.dashboard}>
      <Welcome />
    </GridContent>
  )
}

export default Dashboard
