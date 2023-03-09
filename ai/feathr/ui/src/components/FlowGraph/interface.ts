import { CSSProperties } from 'react'

import { NodeProps, ReactFlowProps } from 'react-flow-renderer'

import { FeatureLineage } from '@/models/model'
import { FeatureType } from '@/utils/utils'

export interface NodeData {
  id: string
  label: string
  subtitle: string
  featureId: string
  version: string
  borderColor?: string
  active?: boolean
  detialUrl?: string
}

export interface FlowGraphProps {
  className?: string
  style?: CSSProperties
  minHeight?: string | number
  height?: string | number
  loading?: boolean
  data?: FeatureLineage
  nodeId?: string
  project?: string
  snapGrid?: ReactFlowProps['snapGrid']
  featureType?: FeatureType
}

export interface LineageNodeProps extends NodeProps<NodeData> {}
