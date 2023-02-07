import {RouteLocationNormalized} from 'vue-router';

export declare global {
  interface RouterAuthOptions {
    beforeEachFn?: (to?: RouteLocationNormalized, from?: RouteLocationNormalized) => void;
    afterEachFn?: (to?: RouteLocationNormalized, from?: RouteLocationNormalized) => void;
    validateFn?: (to: RouteLocationNormalized, from?: RouteLocationNormalized) => boolean;
  }
}
