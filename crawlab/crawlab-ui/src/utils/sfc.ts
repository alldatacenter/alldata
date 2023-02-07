import {getRequestBaseUrl} from '@/utils/request';
import useRequest from '@/services/request';
import * as vue from 'vue';
import * as VueRouter from 'vue-router';
import * as ElementPlus from 'element-plus';
import * as ElementPlusIcons from '@element-plus/icons';
import * as CrawlabUI from '@/index';
import * as Vuex from 'vuex';

const sfcLoadModule = window['vue3-sfc-loader']?.loadModule;

const {
  getRaw,
} = useRequest();

const getLoadModuleOptions = (): any => {
  return {
    moduleCache: {
      vue,
      'vuex': Vuex,
      'vue-router': VueRouter,
      'element-plus': ElementPlus,
      '@element-plus/icons': ElementPlusIcons,
      'crawlab-ui': CrawlabUI,
    },
    pathResolve({refPath, relPath}: { refPath?: string; relPath?: string }) {
      // self
      if (relPath === '.') {
        return refPath;
      }

      // relPath is a module name ?
      if (relPath?.toString()?.[0] !== '.' && relPath?.toString()?.[0] !== '/') {
        return relPath;
      }

      return String(new URL(relPath.toString(), refPath === undefined ? window.location.toString() : refPath.toString()));
    },
    async getFile(url: string) {
      const res = await getRaw(url.toString());
      return {
        getContentData: async (_: boolean) => res.data,
      };
    },
    addStyle(textContent: string) {
      const style = Object.assign(document.createElement('style'), {textContent});
      const ref = document.head.getElementsByTagName('style')[0] || null;
      document.head.insertBefore(style, ref);
    },
  };
};

export const loadModule = (path: string) => sfcLoadModule?.(`${getRequestBaseUrl()}${path}`, getLoadModuleOptions());
