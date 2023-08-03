import { InteractionRequiredAuthError, PublicClientApplication } from '@azure/msal-browser'
import Axios from 'axios'

import {
  DataSource,
  Feature,
  FeatureLineage,
  Role,
  UserRole,
  NewFeature,
  NewDatasource
} from '@/models/model'
import { globalStore } from '@/store'
import { getMsalConfig } from '@/utils/utils'

const msalInstance = getMsalConfig()
const getApiBaseUrl = () => {
  let endpoint = process.env.REACT_APP_API_ENDPOINT
  if (!endpoint || endpoint === '') {
    endpoint = window.location.protocol + '//' + window.location.host
  }
  return endpoint + '/api/v1'
}

export const fetchDataSources = async (project: string) => {
  const axios = await authAxios(msalInstance)
  return axios
    .get<DataSource[]>(`${getApiBaseUrl()}/projects/${project}/datasources`, {
      headers: {}
    })
    .then((response) => {
      return response.data
    })
}

export const fetchDataSource = async (project: string, dataSourceId: string) => {
  const axios = await authAxios(msalInstance)
  return axios
    .get<DataSource & { message: string; detail: string }>(
      `${getApiBaseUrl()}/projects/${project}/datasources/${dataSourceId}`,
      {
        params: { project: project, datasource: dataSourceId }
      }
    )
    .then((response) => {
      if (response.data.message || response.data.detail) {
        return Promise.reject(response.data.message || response.data.detail)
      } else {
        return response.data
      }
    })
}

export const fetchProjects = async () => {
  const axios = await authAxios(msalInstance)
  return axios
    .get<[]>(`${getApiBaseUrl()}/projects`, {
      headers: {}
    })
    .then((response) => {
      return response.data
    })
}

export const fetchFeatures = async (
  project: string,
  page: number,
  limit: number,
  keyword: string
) => {
  const axios = await authAxios(msalInstance)
  return axios
    .get<Feature[]>(`${getApiBaseUrl()}/projects/${project}/features`, {
      params: { keyword: keyword, page: page, limit: limit },
      headers: {}
    })
    .then((response) => {
      return response.data
    })
}

export const fetchFeature = async (project: string, featureId: string) => {
  const axios = await authAxios(msalInstance)
  return axios
    .get<Feature>(`${getApiBaseUrl()}/features/${featureId}`, {
      params: { project: project }
    })
    .then((response) => {
      return response.data
    })
}

export const fetchProjectLineages = async (project: string) => {
  const axios = await authAxios(msalInstance)
  return axios
    .get<FeatureLineage>(`${getApiBaseUrl()}/projects/${project}`, {})
    .then((response) => {
      return response.data
    })
}

export const fetchFeatureLineages = async (featureId: string) => {
  const axios = await authAxios(msalInstance)
  return axios
    .get<FeatureLineage>(`${getApiBaseUrl()}/features/${featureId}/lineage`, {})
    .then((response) => {
      return response.data
    })
}

// Following are place-holder code
export const createFeature = async (feature: Feature) => {
  const axios = await authAxios(msalInstance)
  return axios.post(`${getApiBaseUrl()}/features`, feature, {
    headers: { 'Content-Type': 'application/json;' },
    params: {}
  })
}

export const updateFeature = async (feature: Feature, id?: string) => {
  const axios = await authAxios(msalInstance)
  if (id) {
    feature.guid = id
  }
  return axios.put(`${getApiBaseUrl()}/features/${feature.guid}`, feature, {
    headers: { 'Content-Type': 'application/json;' },
    params: {}
  })
}

export const listUserRole = async () => {
  await getIdToken(msalInstance)
  const axios = await authAxios(msalInstance)
  return await axios.get<UserRole[]>(`${getApiBaseUrl()}/userroles`, {}).then((response) => {
    return response.data
  })
}

export const getUserRole = async (userName: string) => {
  const axios = await authAxios(msalInstance)
  return await axios
    .get<UserRole>(`${getApiBaseUrl()}/user/${userName}/userroles`, {})
    .then((response) => {
      return response.data
    })
}

export const addUserRole = async (role: Role) => {
  const axios = await authAxios(msalInstance)
  return await axios
    .post(`${getApiBaseUrl()}/users/${role.userName}/userroles/add`, role, {
      headers: { 'Content-Type': 'application/json;' },
      params: {
        project: role.scope,
        role: role.roleName,
        reason: role.reason
      }
    })
    .then((response) => {
      return response
    })
}

export const deleteUserRole = async (userrole: UserRole) => {
  const axios = await authAxios(msalInstance)
  const reason = 'Delete from management UI.'
  return await axios
    .delete(`${getApiBaseUrl()}/users/${userrole.userName}/userroles/delete`, {
      headers: { 'Content-Type': 'application/json;' },
      params: {
        project: userrole.scope,
        role: userrole.roleName,
        reason: reason
      }
    })
    .then((response) => {
      return response
    })
}

export const getIdToken = async (msalInstance: PublicClientApplication): Promise<string> => {
  const activeAccount = msalInstance.getActiveAccount() // This will only return a non-null value if you have logic somewhere else that calls the setActiveAccount API
  const accounts = msalInstance.getAllAccounts()
  const request = {
    scopes: ['User.Read'],
    account: activeAccount || accounts[0]
  }

  let idToken = ''

  // Silently acquire an token for a given set of scopes. Will use cached token if available, otherwise will attempt to acquire a new token from the network via refresh token.
  // A known issue may cause token expire: https://github.com/AzureAD/microsoft-authentication-library-for-js/issues/4206
  await msalInstance
    .acquireTokenSilent(request)
    .then((response) => {
      idToken = response.idToken
    })
    .catch((error) => {
      // acquireTokenSilent can fail for a number of reasons, fallback to interaction
      if (error instanceof InteractionRequiredAuthError) {
        msalInstance.acquireTokenPopup(request).then((response) => {
          idToken = response.idToken
        })
      }
    })

  return idToken
}

export const authAxios = async (msalInstance: PublicClientApplication) => {
  const token = await getIdToken(msalInstance)
  const axios = Axios.create({
    headers: {
      Authorization: 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    baseURL: getApiBaseUrl()
  })

  axios.interceptors.response.use(
    (response) => {
      return response
    },
    (error) => {
      if (error.response?.status === 403) {
        const detail = error.response.data.detail
        globalStore.navigate?.('/403', { replace: true, state: detail })
      } else if (error.response?.status === 404) {
        globalStore.navigate?.('/404', {
          replace: true,
          state: `the '${globalStore.project}' project is not found.`
        })
      }
      return Promise.reject(error.response.data)
      //TODO: handle other response errors
    }
  )
  return axios
}

export const deleteEntity = async (enity: string) => {
  const axios = await authAxios(msalInstance)
  return axios.delete(`${getApiBaseUrl()}/entity/${enity}`)
}

export const getDependent = async (entity: string) => {
  const axios = await authAxios(msalInstance)
  return await axios.get(`${getApiBaseUrl()}/dependent/${entity}`).then((response) => {
    return response
  })
}

export const createAnchorFeature = async (
  project: string,
  anchor: string,
  anchorFeature: NewFeature
) => {
  const axios = await authAxios(msalInstance)
  return axios
    .post(`${getApiBaseUrl()}/projects/${project}/anchors/${anchor}/features`, anchorFeature)
    .then((response) => {
      return response
    })
}

export const createDerivedFeature = async (project: string, derivedFeature: NewFeature) => {
  const axios = await authAxios(msalInstance)
  return axios
    .post(`${getApiBaseUrl()}/projects/${project}/derivedfeatures`, derivedFeature)
    .then((response) => {
      return response
    })
}

export const createSource = async (project: string, datasource: NewDatasource) => {
  const axios = await authAxios(msalInstance)
  return axios
    .post(`${getApiBaseUrl()}/projects/${project}/datasources`, datasource)
    .then((response) => {
      return response
    })
}
