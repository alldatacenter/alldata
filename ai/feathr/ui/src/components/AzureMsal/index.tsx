import React, { ReactNode } from 'react'

import { InteractionType } from '@azure/msal-browser'
import { MsalAuthenticationTemplate, MsalProvider } from '@azure/msal-react'

import { getMsalConfig } from '@/utils/utils'

const msalInstance = getMsalConfig()

export interface AzureMsalProps {
  children?: ReactNode
  enable?: boolean
}

const AzureMsal = (props: AzureMsalProps) => {
  const { children, enable } = props
  return enable ? (
    <MsalProvider instance={msalInstance}>
      <MsalAuthenticationTemplate interactionType={InteractionType.Redirect}>
        {children}
      </MsalAuthenticationTemplate>
    </MsalProvider>
  ) : (
    <>{children}</>
  )
}

export default AzureMsal
