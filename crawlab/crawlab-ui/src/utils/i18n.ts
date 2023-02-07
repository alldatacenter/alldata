import {getI18n} from '@/i18n';
import {updateTitle} from '@/utils/dom';
import {LOCAL_STORAGE_KEY_LANG} from '@/constants/localStorage';

export const translate = (path: string, args?: any): string => {
  const i18n = getI18n();
  const global = i18n.global;
  const {t} = global;
  const res = args !== undefined ? t(path, args) : t(path);
  if (typeof res === 'string') return res;
  return path;
};
window._t = translate;

export const translateC = (c: number, path: string): string => {
  const i18n = getI18n();
  const global = i18n.global;
  const {n} = global;
  const res = n(c, path);
  if (typeof res === 'string') return res;
  return path;
};
window._tc = translateC;

export const translatePlugin = (pluginName: string, path: string): string => {
  return translate(`plugins.${pluginName}.${path}`);
};
window._tp = translatePlugin;

export const setGlobalLang = (lang: Lang) => {
  // set local storage
  localStorage.setItem(LOCAL_STORAGE_KEY_LANG, lang);

  // set i18n instance
  getI18n().global.locale.value = lang;

  // update title
  const title = translate('global.title');
  const subTitle = translate('global.subTitle');
  updateTitle(`${title} | ${subTitle}`);
};

export const getLanguage = (): string => {
  return getI18n().global.locale.value === 'zh' ? 'zh_CN' : 'en';
};
