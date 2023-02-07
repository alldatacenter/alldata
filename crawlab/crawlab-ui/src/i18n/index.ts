import {createI18n} from 'vue-i18n';
import en from './lang/en';
import zh from './lang/zh';

let i18n: LI18n;

export const getI18n = (): LI18n => {
  if (!i18n) {
    i18n = createI18n({
      legacy: false,
      locale: localStorage.getItem('lang') || 'en',
      messages: {
        en: en as any,
        zh: zh as any,
      },
      fallbackLocale: 'en',
      missingWarn: process.env.NODE_ENV === 'development',
      fallbackWarn: process.env.NODE_ENV === 'development',
    });
  }

  return i18n;
};

export const setI18nMessages = (messages: { [key: string]: any }) => {
  if (!i18n) {
    i18n = getI18n();
  }

  for (const lang in messages) {
    i18n.global?.mergeLocaleMessage(lang, messages[lang as 'en' | 'zh']);
  }
};
