import {LocaleMessageDictionary} from '@intlify/core-base';
import {VueMessageType, I18n} from 'vue-i18n';

export declare global {
  interface L extends LocaleMessageDictionary<VueMessageType> {
    global: LGlobal;
    common: LCommon;
    layouts: LLayouts;
    router: LRouter;
    components: LComponents;
    views: LViews;
  }

  type LI18n = I18n<{en: L, zh: L}, unknown, unknown, false>;
}

export * from './components';
export * from './views';
export * from './common';
export * from './global';
export * from './layouts';
export * from './router';
