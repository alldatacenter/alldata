import { ReactNode } from 'react'

export enum AnalysisType {
  Description = 'Description',
  Participate = 'Participate',
  PosNegative = 'PosNegative',
}

export enum RkProgressChoice {
  Left = 'Left',
  Right = 'Right',
}

export interface RkCardProps {
  className?: string
  title: string | ReactNode
  value: string | number
  value2?: string | number
  participateText?: string | number
  prefixTop?: string | ReactNode // 统计数值value的前缀
  suffixTop?: string | ReactNode // 统计数值value的后缀
  prefixBottom?: string | ReactNode // 统计数值value的后缀
  suffixBottom?: string | ReactNode // 统计数值value的后缀
  groupSeparator?: string
  type?: string
  description?: {
    label: string
    value: string | number | ReactNode
    icon?: ReactNode
  }[]
}

export interface DescriptionProps {
  className?: string
  label: string
  value: string | ReactNode
  icon?: boolean | ReactNode
}

export interface RkProgressProps {
  value: string
}
