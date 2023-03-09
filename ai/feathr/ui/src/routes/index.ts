import { useRoutes, useNavigate } from 'react-router-dom'

import { globalStore } from '@/store'

import { routers } from './config'

export const Routers = () => {
  globalStore.navigate = useNavigate()
  return useRoutes(routers)
}
