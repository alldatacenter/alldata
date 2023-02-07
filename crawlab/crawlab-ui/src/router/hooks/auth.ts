import {Router} from 'vue-router';
import {getToken} from '@/utils/auth';
import {ElNotification} from 'element-plus';
import {translate} from '@/utils';

const t = translate;

export const ANOMALOUS_ROUTES = [
  '/login',
];

export const initRouterAuth = (router: Router, options?: RouterAuthOptions) => {
  router.beforeEach((to, from, next) => {
    // before each
    options?.beforeEachFn?.(to, from);

    // allow anomalous routes
    if (ANOMALOUS_ROUTES.includes(to.path)) {
      return next();
    }

    // validate
    if (!getToken() ||
      (options?.validateFn && !options?.validateFn?.(to, from))) {
      ElNotification({
        title: t('common.status.unauthorized'),
        message: t('common.notification.loggedOut'),
        type: 'warning',
      });
      return next('/login');
    }

    // after each
    options?.afterEachFn?.(to, from);

    return next();
  });
};
