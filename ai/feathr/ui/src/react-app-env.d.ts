/// <reference types="react-scripts" />
declare global {
  namespace NodeJS {
    interface ProcessEnv {
      NODE_ENV: 'development' | 'production'
      PORT?: string
      PWD: string
      REACT_APP_AZURE_CLIENT_ID: string
      REACT_APP_AZURE_TENANT_ID: string
      REACT_APP_API_ENDPOINT: string
      REACR_APP_ENABLE_RBAC: boolean
    }
  }

  interface Window {
    environment: EnvironmentConfig
  }

  interface EnvironmentConfig {
    azureClientId: string
    azureTenantId: string
    enableRBAC: boolean
  }
}

export {}
