import React, { ReactNode } from 'react'
import { setContext } from '@apollo/client/link/context'
import { ApolloClient, ApolloProvider, createHttpLink, InMemoryCache, gql } from '@apollo/client'
import { BasicLayoutProps, Settings as LayoutSettings } from '@ant-design/pro-layout'
import { notification } from 'antd'
import { history, RequestConfig } from 'umi'
import RightContent from '@/components/RightContent'
import Footer from '@/components/Footer'
import Provider from '@/store'
import { ResponseError } from 'umi-request'
import layoutSettings from '../config/layout'

const httpLink = createHttpLink({
  uri: API_GRAPHQL_URL,
})

const authLink = setContext((_: any, { headers }: any) => {
  const token = localStorage.getItem('token')
  return {
    headers: {
      ...headers,
      Authorization: token ? `JWT ${token}` : '',
    },
  }
})

const client = new ApolloClient({
  link: authLink.concat(httpLink),
  cache: new InMemoryCache(),
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'no-cache',
      errorPolicy: 'all',
    },
    query: {
      fetchPolicy: 'no-cache',
      errorPolicy: 'all',
    },
  },
})

export function rootContainer(container: ReactNode) {
  return (
    <ApolloProvider client={client}>
      <Provider>{container}</Provider>
    </ApolloProvider>
  )
}

const VERIFY_TOKEN = gql`
  mutation VerifyToken($token: String!) {
    verifyToken(token: $token) {
      payload
    }
  }
`

const QUERY_CURRENT_USER = gql`
  query QueryCurrentUser {
    currentUser {
      id
      username
      name
      email
      role
    }
  }
`

const whitelist = ['/user/login', '/robot/showresult']

export async function getInitialState(): Promise<{
  currentUser: Data.CurrentUser | null
  settings: LayoutSettings
}> {
  // 如果是登录页面，不执行
  if (!whitelist.includes(history.location.pathname)) {
    try {
      const token = localStorage.getItem('token')
      const { data } = await client.mutate({
        mutation: VERIFY_TOKEN,
        variables: { token },
      })

      // 如果 token 验证通过，拉取当前用户信息
      if (data.verifyToken) {
        const result = await client.query({
          query: QUERY_CURRENT_USER,
          fetchPolicy: 'no-cache',
        })

        return {
          currentUser: result.data.currentUser,
          settings: layoutSettings,
        }
      }

      throw new Error('token invalid')
    } catch (error) {
      history.push('/user/login')
    }
  }

  return {
    currentUser: null,
    settings: layoutSettings,
  }
}

export const layout = ({
  initialState,
}: {
  initialState: { settings?: LayoutSettings }
}): BasicLayoutProps => {
  return {
    rightContentRender: () => <RightContent />,
    disableContentMargin: false,
    footerRender: () => <Footer />,
    menuHeaderRender: undefined,
    ...initialState?.settings,
  }
}

const codeMessage = {
  200: '服务器成功返回请求的数据。',
  201: '新建或修改数据成功。',
  202: '一个请求已经进入后台排队（异步任务）。',
  204: '删除数据成功。',
  400: '发出的请求有错误，服务器没有进行新建或修改数据的操作。',
  401: '用户没有权限（令牌、用户名、密码错误）。',
  403: '用户得到授权，但是访问是被禁止的。',
  404: '发出的请求针对的是不存在的记录，服务器没有进行操作。',
  405: '请求方法不被允许。',
  406: '请求的格式不可得。',
  410: '请求的资源被永久删除，且不会再得到的。',
  422: '当创建一个对象时，发生一个验证错误。',
  500: '服务器发生错误，请检查服务器。',
  502: '网关错误。',
  503: '服务不可用，服务器暂时过载或维护。',
  504: '网关超时。',
}

/**
 * 异常处理程序
 */
const errorHandler = (error: ResponseError) => {
  const { response } = error
  if (response && response.status) {
    const errorText = codeMessage[response.status] || response.statusText
    const { status, url } = response

    notification.error({
      message: `请求错误 ${status}: ${url}`,
      description: errorText,
    })
  }

  if (!response) {
    notification.error({
      description: '您的网络发生异常，无法连接服务器',
      message: '网络异常',
    })
  }
  throw error
}

export const request: RequestConfig = {
  errorHandler,
}
