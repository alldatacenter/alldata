import { lazy } from 'react';
import { TRouterItem } from './type';
import mainRouter from './mainRouter';
import detailRouter from './detailRouter';

export const routerNoLogin: TRouterItem[] = [
    {
        path: '/login',
        component: lazy(() => import(/* webpackChunkName: 'view-login' */ '@/view/Login')),
    },
    {
        path: '/register',
        component: lazy(() => import(/* webpackChunkName: 'view-register' */ '@/view/Register')),
    },
    {
        path: '/forgetPwd',
        component: lazy(() => import(/* webpackChunkName: 'view-forgetPwd' */ '@/view/ForgetPassword')),
    },
];

export {
    detailRouter,
    mainRouter,
};

export default mainRouter;
