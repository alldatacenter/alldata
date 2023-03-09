import React from 'react'

import { QueryClient, QueryClientProvider } from 'react-query'

import AzureMsal from '@/components/AzureMsal'
import { Routers } from '@/routes'

const queryClient = new QueryClient()

const enableRBAC = window.environment?.enableRBAC
const authEnable: boolean = (enableRBAC ? enableRBAC : process.env.REACT_APP_ENABLE_RBAC) === 'true'

const App = () => {
  return (
    <AzureMsal enable={authEnable}>
      <QueryClientProvider client={queryClient}>
        <Routers />
      </QueryClientProvider>
    </AzureMsal>
  )
}

export default App
