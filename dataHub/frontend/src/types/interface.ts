import { ReactNode } from 'react'

export interface BadgeMapProps {
  key: string
  label: string
  color: string
  textColor?: string
}

export interface TagsProps {
  data: { id: number; tag: string; detail: string }[]
}

export interface DesProps {
  key: number
  label: ReactNode
  value: ReactNode
  span: number
  xs: number
  sm: number
}

export interface PageResProps {
  page: number
  pageSize: number
  total: number
}
