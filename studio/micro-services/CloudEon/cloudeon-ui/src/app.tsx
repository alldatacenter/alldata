import Footer from '@/components/Footer';
import RightContent from '@/components/RightContent';
import { BookOutlined, LinkOutlined } from '@ant-design/icons';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import { PageLoading, SettingDrawer } from '@ant-design/pro-components';
import type { RunTimeLayoutConfig } from 'umi';
import { history, RequestConfig, useModel } from 'umi';
import { message, Image, notification } from 'antd';
import defaultSettings from '../config/defaultSettings';
import { currentUser as queryCurrentUser } from './services/ant-design-pro/api';
import logoImg from '../src/assets/images/logo2.png';
import userImg from '../src/assets/images/user.png'
import React, { useEffect } from 'react';
import * as Icon from '@ant-design/icons';
import {
  RobotOutlined,
} from '@ant-design/icons';
import { getCountActiveAPI } from './services/ant-design-pro/colony'; 
import styles from './app.less'


const isDev = process.env.NODE_ENV === 'development';
const loginPath = '/user/login';


// const menuIcons = {
//   '节点':'RobotOutlined',
//   '服务':'CloudServerOutlined',
//   '指令':'AimOutlined',
//   '告警':'AlertOutlined'
// }
// // antd4中动态创建icon
// const createIcon = (key: any) => {
//   console.log('--key:', key);
  
//   const icon = React.createElement(
//     // Icon[menuIcons[key]||'RobotOutlined'],
//     key,
//     {
//       style:{ fontSize: '24px'}
//     }
//   )
//   return icon
// }


// 接口请求全局配置
export const request: RequestConfig = {
  timeout: 1000*60,
  errorHandler:(error)  => {
    if(error && error.name==="BizError"){
      const { response } = error;
      if ('success' in response && !response.success) {
        if(('message' in response) && response?.message.includes('Token无效')){
          history.push(loginPath);
        }else{
          notification.error({
            message: '请求错误',
            description:<>{`${('message' in response) ? response.message : '接口报错' }`}</>,
            duration:3,
            style: {
                width: 500
            }
          });
          // message.error(`请求错误: ${('message' in response) ? response.message : '' }`, 3);
          return {
            success:false,
            data:[],
            message:''
          }
        }
      }
    }
  },
  errorConfig: {
  },
  // 自定义端口规范
  // errorConfig: {
  //   adaptor: res => {
  //     return {
  //       success: res.code ==config.successCode,
  //       data:res.data,
  //       errorCode:res.code,
  //       errorMessage: res.msg,
  //     };
  //   },
  middlewares: [],
  requestInterceptors: [
    (url,options) => {
        let headers = { satoken:'' }
        //判断本地session是否有数据，如果有就得到token，并付给请求头
        if(sessionStorage.getItem('token') != null){
          let c_token = sessionStorage.getItem('token');
          headers.satoken = c_token || ''
        }
        return {
          url,
          options:{...options,headers}
        }
    }
  ],
  responseInterceptors: [
    (response, options) => {
      const codeMaps = {
        502: '网关错误。',
        503: '服务不可用，服务器暂时过载或维护。',
        504: '网关超时。',
      };
      codeMaps[response.status] && message.error(codeMaps[response.status]);
      
      return response;
    }
  ],
};

/** 获取用户信息比较慢的时候会展示一个 loading */
export const initialStateConfig = {
  loading: <PageLoading />,
};

/**
 * @see  https://umijs.org/zh-CN/plugins/plugin-initial-state
 * */
export async function getInitialState(): Promise<{
  settings?: Partial<LayoutSettings>;
  currentUser?: API.CurrentUser;
  loading?: boolean;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
}> {
  const fetchUserInfo = async () => {
    // try {
    //   const msg = await queryCurrentUser();
    //   return msg.data;
    // } catch (error) {
    //   history.push(loginPath);
    // }
    return undefined;
  };
  // 如果不是登录页面，执行
  // if (history.location.pathname !== loginPath) {
  //   const currentUser = await fetchUserInfo();
  //   return {
  //     fetchUserInfo,
  //     currentUser,
  //     settings: defaultSettings,
  //   };
  // }
  return {
    fetchUserInfo,
    settings: defaultSettings,
  };
}

let timer:any = null

history.block((location, action) => {
	//每次路由变动都会走这里
  if(location.pathname.includes('user/login') && timer){
    console.log("location.pathname.includes('user/login'):", location.pathname.includes('user/login'));
    // 清除定时任务
    clearInterval(timer)
    timer = null
  }
})

// ProLayout 支持的api https://procomponents.ant.design/components/layout
export const layout: RunTimeLayoutConfig = ({ initialState, setInitialState }) => {
  
  return {
    rightContentRender: () => <RightContent />,
    disableContentMargin: false,
    waterMarkProps: {
      // 水印
      // content: initialState?.currentUser?.name,
    },
    footerRender: () => <Footer />,
    onPageChange: () => {
      const { location } = history;
      // 如果没有登录，重定向到 login
      let c_token = sessionStorage.getItem('token');

      if (!c_token && location.pathname !== loginPath) {
        history.push(loginPath);
      }
    },
    links: isDev
      ? [
          // <Link key="openapi" to="/umi/plugin/openapi" target="_blank">
          //   <LinkOutlined />
          //   <span>OpenAPI 文档</span>
          // </Link>,
          // <Link to="/~docs" key="docs">
          //   <BookOutlined />
          //   <span>业务组件文档</span>
          // </Link>,
        ]
      : [],
    menuHeaderRender: undefined,
    // 自定义 403 页面
    // unAccessible: <div>unAccessible</div>,
    // 增加一个 loading 的状态
    childrenRender: (children: any, props: { location: { pathname: string | string[]; }; }) => {
      if (initialState?.loading) return <PageLoading />;
      return (
        <>
          {children}
          {!props.location?.pathname?.includes('/login') && (
            <SettingDrawer
              disableUrlParams
              enableDarkTheme
              settings={initialState?.settings}
              onSettingChange={(settings) => {
                setInitialState((preInitialState) => ({
                  ...preInitialState,
                  settings,
                }));
              }}
            />
          )}
        </>
      );
    },
    ...initialState?.settings,
    logo:<><Image src={logoImg}/></>,
    // layout: 'side',
    breakpoint: true,
    fixedHeader:true,
    collapsed:false,
    collapsedButtonRender: false,
    menuFooterRender:()=><></>,
    menuItemRender: (itemProps: any, defaultDom: any, props: any) => {
      // console.log('--menuItemRender: ');
      const getData = JSON.parse(sessionStorage.getItem('colonyData') || '{}')
      const { actionCount, setActionCount } = useModel('colonyModel', model => ({ actionCount: model.actionCount, setActionCount: model.setActionCountModel }));
      
      useEffect(()=>{
        const getCount = async()=>{
          const result = await getCountActiveAPI({clusterId:getData.clusterId})
            setActionCount(result?.data || 0)
        }
        if(getData && getData.clusterId && !timer && !location.href.includes('user/login') && !location.href.includes('colony/colonyMg')){
          getCount()
          timer = setInterval(getCount,3000)
        } 
        return ()=>{
          clearInterval(timer)
          timer = null
        }        
      },[])  

     return <div style={{height:'100%',display:'flex',justifyContent: 'center',flexDirection:'column', alignItems:'center',width:'100%'}} 
        onClick={() => {
          history.push(itemProps.path);
        }}>
            <div style={{display:'inline-flex',lineHeight:'30px', fontSize: '24px',position:'relative'}}>
              {/* {itemProps.icon} */}
              
              {itemProps.name == '指令' ? (<>
              <div className={`${styles.countWrap} ${actionCount ? styles.countBoxshow:''}`}>
                <div className={styles.countBox}>
                  {actionCount}
                </div>
              </div>
              </>):''}
              {/* {createIcon(itemProps.icon)} */}
              <div style={{fontSize: '24px'}}>{itemProps.icon}</div>
            </div>
            <div style={{lineHeight:'20px',fontSize:'12px'}}>{itemProps.name}</div>
      </div>
  }
  };
};
