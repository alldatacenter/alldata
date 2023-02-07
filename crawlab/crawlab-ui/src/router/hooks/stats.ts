import {Router} from 'vue-router';
import {sendPv} from '@/utils/admin';

export const initRouterStats = (router: Router, options?: RouterStatsOptions) => {
  router.afterEach(async (to, from, next) => {
    // before each
    options?.beforeEachFn?.();

    // send pv
    if (to.path) {
      sendPv(to.path);
    }

    // after each
    options?.afterEachFn?.();
  });
};
