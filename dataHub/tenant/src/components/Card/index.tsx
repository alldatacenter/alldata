import React, { ReactNode } from 'react'
import { Card, Statistic, Progress } from 'antd'
import ExclamationCircleOutlined from '@ant-design/icons/ExclamationCircleOutlined'
import isEmpty from 'lodash/isEmpty'
import { AnalysisType, RkCardProps } from '@/components/Card/data'

import RkDescription from './components/Description'
import styles from './index.less'

export default ({
  className,
  title = '累计帖子数量',
  value = '0',
  value2 = '0',
  participateText = '',
  prefixTop = '',
  suffixTop = '',
  prefixBottom = '',
  suffixBottom = '',
  groupSeparator = ',',
  type = AnalysisType.Description,
  description = [],
}: RkCardProps) => {
  let num = 1
  const statisticTitle =
    typeof title === 'string' ? (
      <div className={styles.title}>
        <span>{title}</span>
        <ExclamationCircleOutlined />
      </div>
    ) : (
      title
    )

  const statisticValue = type === AnalysisType.Participate ? `${value}%` : value
  let renderDes: any[] = []
  if (!isEmpty(description) && description?.length > 2) {
    description.forEach((i, index) => {
      if ((index + 1) % 2 === 0) {
        num += 1
        renderDes.push({
          key: `key${num}`,
          data: description.slice(index - 1, index + 1),
        })
      }
    })
  } else {
    renderDes = description
  }

  return (
    <Card className={`${styles.card} ${className}`}>
      <Statistic
        className={styles.statistic}
        title={statisticTitle}
        value={statisticValue}
        groupSeparator={groupSeparator}
        prefix={prefixTop}
        suffix={suffixTop}
      />

      <footer className={styles.footer}>
        {type === AnalysisType.Description && (
          <>
            {!isEmpty(description) && description?.length > 2 ? (
              <div className={styles.footerBox}>
                {renderDes.map((i) => (
                  <div className={styles.footerDesBox} key={i.key}>
                    {i.data.map(
                      (v: {
                        label: string
                        value: string | number | ReactNode
                        icon?: ReactNode
                      }) => {
                        return (
                          <RkDescription
                            className={styles.rkDescription}
                            key={`${v.label} ${v.value}`}
                            label={v.label}
                            value={v.value}
                            icon={v.icon}
                          />
                        )
                      },
                    )}
                  </div>
                ))}
              </div>
            ) : (
              description?.map((i) => (
                <RkDescription
                  className={styles.rkDescription}
                  key={`${i.label} ${i.value}`}
                  label={i.label}
                  value={i.value}
                  icon={i.icon}
                />
              ))
            )}
          </>
        )}
        {type === AnalysisType.Participate && (
          <div className={styles.processBlock}>
            <Progress percent={Number(value)} size="small" status="active" showInfo={false} />
            {participateText && <div className={styles.participateText}>{participateText}</div>}
          </div>
        )}
        {type === AnalysisType.PosNegative && (
          <div className={styles.negativeBlock}>
            <Statistic
              className={styles.statistic}
              title="评论负面度"
              value={value2}
              groupSeparator={groupSeparator}
              prefix={prefixBottom}
              suffix={suffixBottom}
            />
          </div>
        )}
      </footer>
    </Card>
  )
}
