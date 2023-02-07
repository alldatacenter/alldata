import {Directive} from 'vue';
import {getRouter} from '@/router';
import {getStore} from '@/store';

const auth: Directive<HTMLElement, Auth> = {
  mounted(el, binding) {
    // router
    const router = getRouter();

    // current route path
    const currentRoutePath = router?.currentRoute?.value?.path;

    // skip if current route path is empty
    if (!currentRoutePath) return;

    // action
    let action: string;
    if (typeof binding.value === 'string') {
      action = binding.value;
    } else if (typeof binding.value === 'object') {
      action = binding.value.action;
    } else if (typeof binding.value === 'undefined') {
      action = el.getAttribute('class') || '';
    } else {
      return;
    }

    // skip if action is empty
    if (!action) return;

    // store
    const store = getStore();
    const {
      layout: layoutState,
    } = store.state as RootStoreState;

    // action visible function
    const {actionVisibleFn} = layoutState;

    // skip if action visible function is empty
    if (!actionVisibleFn) return;

    // hide element if not allowed
    if (!actionVisibleFn(currentRoutePath, action)) {
      el.hidden = true;
    }
  }
};

export default auth;
