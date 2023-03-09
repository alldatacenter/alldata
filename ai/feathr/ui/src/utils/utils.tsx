import { Configuration, PublicClientApplication } from '@azure/msal-browser'

import { Feature } from '@/models/model'

export const getMsalConfig = () => {
  // Use runtime environment variables if found in env-config.js, otherwise fallback to
  // env settings defined in build time.
  // Note: env-config.js is generated on the flying during contain app starts.
  const clientId = window.environment?.azureClientId ?? process.env.REACT_APP_AZURE_CLIENT_ID
  const tenantId = window.environment?.azureTenantId ?? process.env.REACT_APP_AZURE_TENANT_ID
  const authority = `https://login.microsoftonline.com/${tenantId}`
  const msalConfig: Configuration = {
    auth: {
      clientId: clientId,
      authority: authority,
      redirectUri: window.location.origin
    }
  }

  return new PublicClientApplication(msalConfig)
}

export const enum FeatureType {
  AllNodes = 'all_nodes',
  Source = 'feathr_source_v1',
  Anchor = 'feathr_anchor_v1',
  AnchorFeature = 'feathr_anchor_feature_v1',
  DerivedFeature = 'feathr_derived_feature_v1'
}

export const isFeature = (featureType: string) => {
  return (
    featureType === FeatureType.AnchorFeature ||
    featureType === FeatureType.DerivedFeature ||
    featureType === FeatureType.Source
  )
}

export const getFeatureDetailUrl = (project: string, feature: Feature) => {
  switch (feature.typeName) {
    case FeatureType.Source:
      return `/${project}/datasources/${feature.guid}`
    case FeatureType.AnchorFeature:
    case FeatureType.DerivedFeature:
      return `/${project}/features/${feature.guid}`
    default:
      return
  }
}

export const getJSONMap = (json: any = {}) => {
  return Object.keys(json).map((key) => {
    return { label: key, key }
  })
}

export const isEmpty = (obj: any = {}) => {
  return !obj || Object.getOwnPropertyNames(obj).length === 0
}
