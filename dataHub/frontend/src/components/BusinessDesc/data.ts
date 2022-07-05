import { ReactNode } from 'react'
import { DesProps } from '@/types/interface'

export interface Props {
  className?: string
  title: ReactNode
  desc?: ReactNode
  description: DesProps[]
}
