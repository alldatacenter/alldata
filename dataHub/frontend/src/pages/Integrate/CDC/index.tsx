import { PageHeaderWrapper } from '@ant-design/pro-layout'
import React, { useState, useEffect } from 'react'
import { Row, Col, Input } from 'antd'
import Graphin, { Utils } from '@antv/graphin'
import demoData from './spotData'

import styles from './index.less'
import '@antv/graphin/dist/index.css'

export default () => {
  // const { Option } = Select;
  const { Search } = Input
  // const [btnLoading, setBtnLoading] = useState<boolean>(false);
  const [display, setDisplay] = useState<boolean>(false)
  const data: any = Utils.mock(13).circle().graphin()

  useEffect(() => {}, [])

  const headerBtnClick = () => {
    // setBtnLoading(true);
    setTimeout(() => {
      setDisplay(!display)
      // setBtnLoading(false);
    }, 1000)
  }

  return (
    <PageHeaderWrapper>
      <section className={styles.main}>
        {display ? <Graphin data={demoData} /> : <Graphin data={data} />}
      </section>
    </PageHeaderWrapper>
  )
}
