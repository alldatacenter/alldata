import React, { useState } from 'react'
import { Dropdown, Menu, message, Tag } from 'antd'
import { useMutation, useQuery } from '@apollo/client'
import { useHistory } from 'umi'
import { QUERY_POLICIES, CREATE_MONITOR_RECORD, UPDATE_MONITOR_RECORD } from './data'
import styles from './index.less'

interface Props {
  status: 'unmonitored' | 'monitoring' | 'closed'
  targetId: string
  targetType: string
}

const MonitorButton: React.FC<Props> = ({ status: _status, targetId, targetType }) => {
  const history = useHistory()
  const [status, setStatus] = useState(_status)
  const policyRes = useQuery(QUERY_POLICIES)
  const [createMonitorRecord] = useMutation(CREATE_MONITOR_RECORD)
  const [updateMonitorRecord] = useMutation(UPDATE_MONITOR_RECORD)

  if (status === 'monitoring') {
    let link = ''
    if (targetType === 'A_1') {
      link = '/monitor/event'
    }
    if (targetType === 'A_2') {
      link = '/monitor/person'
    }
    return (
      <Tag className={styles.monitorButton} color="green" onClick={() => history.push(link)}>
        监控中
      </Tag>
    )
  }

  const onClosedMenuClick = ({ key }: any) => {
    updateMonitorRecord({
      variables: {
        targetId,
        targetType,
        policyId: key,
        status: 'A_0', // 监控中
      },
    })
      .then(() => {
        message.success('监控成功')
        setStatus('monitoring')
      })
      .catch(() => {
        message.error('监控失败')
      })
  }

  if (status === 'closed') {
    return (
      <Dropdown
        overlay={
          <Menu onClick={onClosedMenuClick}>
            {(policyRes.data?.monitorPolicies ?? []).map((item: any) => (
              <Menu.Item key={item.id}>{item.name}</Menu.Item>
            ))}
          </Menu>
        }
      >
        <Tag className={styles.monitorButton}>已关闭</Tag>
      </Dropdown>
    )
  }

  const onUnmonitoredMenuClick = ({ key }: any) => {
    createMonitorRecord({
      variables: {
        targetId,
        targetType,
        policyId: key,
      },
    })
      .then(() => {
        message.success('监控成功')
        setStatus('monitoring')
      })
      .catch(() => {
        message.error('监控失败')
      })
  }

  return (
    <Dropdown
      overlay={
        <Menu onClick={onUnmonitoredMenuClick}>
          {(policyRes.data?.monitorPolicies ?? []).map((item: any) => (
            <Menu.Item key={item.id}>{item.name}</Menu.Item>
          ))}
        </Menu>
      }
    >
      <Tag className={styles.monitorButton} color="blue">
        + 监控
      </Tag>
    </Dropdown>
  )
}

export default MonitorButton
