import React from 'react'
import UserOutlined from '@ant-design/icons/UserOutlined'
import LockOutlined from '@ant-design/icons/LockOutlined'
import { Form, Input, Button, message } from 'antd'
import { useMutation, gql } from '@apollo/client'
import Footer from '@/components/Footer'
import styles from './index.less'

const TOKEN_AUTH = gql`
  mutation TokenAuth($username: String!, $password: String!) {
    tokenAuth(username: $username, password: $password) {
      payload
      token
      refreshExpiresIn
    }
  }
`

/**
 * 此方法会跳转到 redirect 参数所在的位置
 */
const replaceGoto = () => {
  // const urlParams = new URL(window.location.href)
  // const params = getPageQuery()
  // let { redirect } = params as { redirect: string }
  // if (redirect) {
  //   const redirectUrlParams = new URL(redirect)
  //   if (redirectUrlParams.origin === urlParams.origin) {
  //     redirect = redirect.substr(urlParams.origin.length)
  //     if (redirect.match(/^\/.*#/)) {
  //       redirect = redirect.substr(redirect.indexOf('#') + 1)
  //     }
  //   } else {
  //     window.location.href = '/'
  //     return
  //   }
  // }
  // history.replace(redirect || '/')
  window.location.href = '/'
}

const Login: React.FC<{}> = () => {
  const [login, { loading }] = useMutation(TOKEN_AUTH)
  // const { refresh } = useModel('@@initialState')

  const onFinish = async (values: any) => {
    try {
      const { data } = await login({ variables: values })
      if (data.tokenAuth) {
        message.success('登录成功')
        localStorage.setItem('token', data.tokenAuth.token)
        replaceGoto()
        // refresh()
      } else {
        message.warn('用户名或密码错误')
      }
    } catch (error) {
      message.error('登录失败，请重试')
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.content}>
        <div className={styles.top}>
          <div className={styles.header}>
            <span className={styles.title}>开源大数据平台</span>
          </div>
          <div className={styles.desc}>-</div>
        </div>

        <div className={styles.main}>
          <Form size="large" onFinish={onFinish}>
            <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input prefix={<UserOutlined />} placeholder="用户名" autoComplete="close" />
            </Form.Item>

            <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input prefix={<LockOutlined />} type="password" placeholder="密码" />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" loading={loading}>
                登 录
              </Button>
            </Form.Item>
          </Form>
        </div>
      </div>
      <Footer />
    </div>
  )
}

export default Login
