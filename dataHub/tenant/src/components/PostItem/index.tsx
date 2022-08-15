import React from 'react'
import moment from 'moment'
import { Row, Col, Tag } from 'antd'
import AuditOutlined from '@ant-design/icons/AuditOutlined'
import BarChartOutlined from '@ant-design/icons/BarChartOutlined'
import { NEGLinksProps } from '@/pages/Database/Person/data'
import LinkMark from '@/mixins/LinkMark'
import isNumber from 'lodash/isNumber'
import { LinkTagColor, SourcesColor } from '@/types/const'
import styles from './styles.less'

interface Props {
  data: NEGLinksProps
}

const PersonItem: React.FC<Props> = ({ data }) => {
  const isRecently = moment() <= moment(data.publishedAt).add(7, 'days')
  // const evidenceMap = { 0: '未固证', 1: '已固证' }
  // const analyseMap = { 0: '未分析', 1: '分析中', 2: '已分析' }
  const color = SourcesColor.find((s) => s.source === data.sourceName)?.color || '#A0A0A0'

  return (
    <div className={styles.postItem}>
      <Row gutter={16}>
        <Col xs={24} sm={18}>
          <div className={styles.tagList}>
            <Tag
              color={color}
              style={{ color: '#fff', backgroundColor: color, borderColor: color }}
            >
              {data.sourceName || '其他'}
            </Tag>
            {data.account && <Tag color="#108ee9">{data.account}</Tag>}
            {data.linkSortedTags
              .sort((a, b) => a.categoryId - b.categoryId)
              .map((i) => (
                <Tag color={LinkTagColor[i.categoryId]} key={i.categoryId + i.name}>
                  {i.name}
                </Tag>
              ))}
          </div>
          <a className={styles.title} href={data.url} target="_blank" rel="noreferrer">
            <LinkMark data={data} rows={5} />
          </a>
          <ul className={styles.processList}>
            <li>
              <AuditOutlined />
              未固证
            </li>
            <li>
              <BarChartOutlined />
              未分析
            </li>
          </ul>
          {/* {(data.briefing || (data.comments && data.comments.length > 0)) && ( */}
          {/*  <ul className={styles.feedbackList}> */}
          {/*    {data.briefing && ( */}
          {/*      <li> */}
          {/*        <span>简报：</span> */}
          {/*        <p>{data.briefing}</p> */}
          {/*      </li> */}
          {/*    )} */}
          {/*    {data.comments && */}
          {/*      data.comments.map((item: any) => ( */}
          {/*        <li key={item.name + item.text}> */}
          {/*          <span>{item.name}</span> */}
          {/*          <p>{item.text}</p> */}
          {/*        </li> */}
          {/*      ))} */}
          {/*  </ul> */}
          {/* )} */}
        </Col>
        <Col xs={24} sm={6}>
          <ul className={styles.metaList}>
            <li>
              {isRecently && <span className={styles.flag}>新</span>}
              <span>{moment(data.publishedAt).format('YYYY-MM-DD HH:mm:ss')}</span>
            </li>
            <li>
              {isNumber(data.diffUse) && <>声量({data.diffUse})</>}
              转({data.repostsCount}) 评({data.commentsCount}) 赞({data.attitudesCount})
            </li>
          </ul>
        </Col>
      </Row>
    </div>
  )
}

export default PersonItem
