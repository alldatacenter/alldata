import { ReactNode } from 'react'

export interface Props {
  loading: boolean
  infoTitle: ReactNode
  info: ItemProps[]
  total: ItemProps[]
}

export interface ItemProps {
  label: string
  value: ReactNode
}
