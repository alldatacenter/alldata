import { DatabaseOutlined } from "@ant-design/icons";
import  { lazy } from "react"
import { useRoutes } from "react-router-dom";
import SuspenseRoute from "./suspenseRoute";


const Login = lazy(() => import('@pages/login'));
const Home = lazy(() => import('@pages/home'));
const DataCenter = lazy(() => import('@pages/dataCenter'));
const Error404 = lazy(() => import('@pages/404'));
export const dataRouter = [
  {
    element:DataCenter,
    label:'数据中台',
    key:'/data-center',
    icon:<DatabaseOutlined />,
    children:[
      {
        element:Home,
        label:'首页',
        key:'/data-center/',
      },
    ]
  }
]



const BaseRoutes:React.FC = () => {
  const getMenuRouter:any = (menuList: any[]) => {
    return menuList.map(item => {
      let children = undefined;
      if(item.children){
        children = getMenuRouter(item.children);
      }
      return {
        path:item.key,
        children,
        element:<SuspenseRoute lazyChildren={item.element} title={item.label}/>
      }
    })
  }
  const router = [
    {
      element:Login,
      label:'登录',
      key:'/login',
    },
    ...dataRouter,
    {
      element:Error404,
      label:'404',
      key:'*',
    }
  ];
  return useRoutes(getMenuRouter(router))
}
export default BaseRoutes
