import Vue from 'vue';
import VueI18n from 'vue-i18n';
import zhLocale from './lang/zh-CN';
import enLocale from './lang/en-US';
import zhCnLocale from 'view-design/src/locale/lang/zh-CN';
import enUsLocale from 'view-design/src/locale/lang/en-US';

Vue.use(VueI18n);

// 根据浏览器信息自动设置语言
const navLang = navigator.language;
const localLang = (navLang == 'zh-CN' || navLang == 'en-US') ? navLang : false;
const lang = window.localStorage.lang || localLang || 'zh-CN';

Vue.config.lang = lang;

// 多语言配置 vue-i18n 6.x+
Vue.locale = () => { };
const messages = {
    'zh-CN': Object.assign(zhCnLocale, zhLocale),
    'en-US': Object.assign(enUsLocale, enLocale)
};
const i18n = new VueI18n({
    locale: lang,
    messages
});

export default i18n;
