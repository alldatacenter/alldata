// import Footer from '@/components/Footer';
import { login } from '@/services/ant-design-pro/api';
// import { getFakeCaptcha } from '@/services/ant-design-pro/login';
import {
  LockOutlined,
  UserOutlined,
  // AlipayCircleOutlined,
  // MobileOutlined,
  // TaobaoCircleOutlined,
  // WeiboCircleOutlined,
} from '@ant-design/icons';
// import {
//   LoginForm,
//   ProFormCaptcha,
//   ProFormCheckbox,
//   ProFormText,
// } from '@ant-design/pro-components';
import { Alert, message, Button, Image, Form, Input } from 'antd';
import React, { useState } from 'react';
import { FormattedMessage, history, SelectLang, useIntl, useModel } from 'umi';
import { loginAPI } from '@/services/ant-design-pro/colonyLogin'
import styles from './index.less';
import loginImg from '../../../assets/images/login-img.png'
import logoImg from '@/assets/images/logo2.png';


const LoginMessage: React.FC<{
  content: string;
}> = ({ content }) => (
  <Alert
    style={{
      marginBottom: 24,
    }}
    message={content}
    type="error"
    showIcon
  />
);

const Login: React.FC = () => {
  // const [userLoginState, setUserLoginState] = useState<API.stringResult>({});
  const [type, setType] = useState<string>('account');
  const { initialState, setInitialState } = useModel('@@initialState');

  const intl = useIntl();

  const fetchUserInfo = async (values: any) => {
    // const userInfo = await initialState?.fetchUserInfo?.();
    // if (userInfo) {
    //   await setInitialState((s) => ({
    //     ...s,
    //     currentUser: userInfo,
    //   }));
    // }
    if (values) {
      await setInitialState((s) => ({
        ...s,
        currentUser: values,
      }));
    }
  };

  const setToken = (value:string) =>{
    sessionStorage.setItem('token',value)
  }

  const setCurrentUser = (value:any) =>{
    sessionStorage.setItem('currentUser',value)
  }

  const handleSubmit = async (values: API.LoginParams) => {
    const params = {
      name: values.username,
      pwd: values.password
    }
    const result = await loginAPI(params)
    if(result && result.success){
      const defaultLoginSuccessMessage = intl.formatMessage({
        id: 'pages.login.success',
        defaultMessage: '登录成功！',
      });
      message.success(defaultLoginSuccessMessage);
      setToken(String(result?.data)||'')
      setCurrentUser(JSON.stringify(params))
      await fetchUserInfo(params);
      /** 此方法会跳转到 redirect 参数所在的位置 */
      if (!history) return;
      const { query } = history.location;
      const { redirect } = query as { redirect: string };
      history.push(redirect || '/');
      return;
    }
    // 如果失败显示错误信息
    result && message.error(result.message);
    // setUserLoginState(false);
  }

  // const handleSubmit1 = async (values: API.LoginParams) => {
  //   try {
  //     // 登录
  //     const msg = await login({ ...values, type });
  //     if (msg.status === 'ok') {
  //       const defaultLoginSuccessMessage = intl.formatMessage({
  //         id: 'pages.login.success',
  //         defaultMessage: '登录成功！',
  //       });
  //       message.success(defaultLoginSuccessMessage);
  //       await fetchUserInfo();
  //       /** 此方法会跳转到 redirect 参数所在的位置 */
  //       if (!history) return;
  //       const { query } = history.location;
  //       const { redirect } = query as { redirect: string };
  //       history.push(redirect || '/');
  //       return;
  //     }
  //     console.log(msg);
  //     // 如果失败去设置用户错误信息
  //     setUserLoginState(msg);
  //   } catch (error) {
  //     const defaultLoginFailureMessage = intl.formatMessage({
  //       id: 'pages.login.failure',
  //       defaultMessage: '登录失败，请重试！',
  //     });
  //     message.error(defaultLoginFailureMessage);
  //   }
  // };
  // const { status, type: loginType } = userLoginState;
  const onFinishFailed = (errorInfo: any) => {
    console.log('Failed:', errorInfo);
  };

  return (
    <div className={styles.loginLayout}>
      <div className={styles.loginContent}>
        <div className={styles.loginLeft}>
          {/* <div className={styles.title}>
            CloudEon
          </div>
          <div className={styles.subTitle}>
            基于k8s安装和运维大数据集群。
          </div> */}
          <Image
            width={400}
            preview={false}
            src={loginImg}
          />
        </div>
        <div className={styles.loginRight}>
          <div className={styles.loginText}>
            <div className={styles.title}>
              <Image style={{width:'150px',marginRight:'10px',marginBottom:'20px'}} src={logoImg}></Image>
              {/* CloudEon */}
            </div>
            <div className={styles.subTitle}>
              基于kubernetes安装和运维大数据集群
            </div>
          </div>
          <div className={styles.loginForm}>
              <Form
                name="basic"
                labelCol={{ span: 8 }}
                wrapperCol={{ span: 16 }}
                initialValues={{ remember: true }}
                onFinish={handleSubmit}
                onFinishFailed={onFinishFailed}
                autoComplete="off"
              >
                <Form.Item
                  label=""
                  name="username"
                  rules={[{ required: true, message: 'Please input your username!' }]}
                >
                  <Input prefix={<UserOutlined />} className={styles.inputItem} />
                </Form.Item>

                <Form.Item
                  label=""
                  name="password"
                  rules={[{ required: true, message: 'Please input your password!' }]}
                >
                  <Input.Password prefix={<LockOutlined />} className={styles.inputItem} />
                </Form.Item>

                <Form.Item className={styles.btnWrap}>
                  <Button type="primary" htmlType="submit" className={styles.btnItem}>
                    登录
                  </Button>
                </Form.Item>
              </Form>
          </div>

        </div>
      </div>
    </div>
  );
};

export default Login;
