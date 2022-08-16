import { gql } from '@apollo/client'

export const monitorStatusMap = {
  unmonitored: '+ 监控',
  monitoring: '监控中',
  closed: '关闭',
}

export const QUERY_POLICIES = gql`
  query MonitorPolicies {
    monitorPolicies {
      id
      name
      desc
    }
  }
`

export const CREATE_MONITOR_RECORD = gql`
  mutation CreateMonitorRecord(
    $targetId: ID!
    $targetType: CommonMonitorRecordTargetType!
    $policyId: ID!
  ) {
    createMonitorRecord(targetId: $targetId, targetType: $targetType, policyId: $policyId) {
      monitorRecord {
        id
      }
    }
  }
`

export const UPDATE_MONITOR_RECORD = gql`
  mutation UpdateMonitorRecord(
    $targetId: ID!
    $targetType: CommonMonitorRecordTargetType!
    $policyId: ID
    $status: CommonMonitorRecordStatus
  ) {
    updateMonitorRecord(
      targetId: $targetId
      targetType: $targetType
      policyId: $policyId
      status: $status
    ) {
      monitorRecord {
        id
      }
    }
  }
`
