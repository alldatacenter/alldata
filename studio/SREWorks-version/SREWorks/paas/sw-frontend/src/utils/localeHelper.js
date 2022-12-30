/**
 * Created by caoshuaibiao on 2018/8/20.
 * 一个简单的国际化助手
 */

import IntlMessageFormat from 'intl-messageformat';
import cookie from './cookie';
// import intl from '@ali/wind-intl/lib/react'

//import * as i18next from 'i18next';


window.APPLICATION_LANGUAGE = (function () {
    let territory = cookie.read('aliyun_territory');
    if (territory === 'HK' || territory === 'TW') {
        territory = 'MO';
    }
    let bccLanguage = cookie.read('aliyun_lang') + '_' + territory;
    const allLanguages = ['zh_CN', 'zh_MO', 'en_US'];
    const bccLng = allLanguages.indexOf(bccLanguage) > -1 ? bccLanguage : '';
    const cacheLanguage = localStorage.getItem("t_lang_locale") || "zh_CN";
    return bccLng ? bccLng : cacheLanguage;
})();

let currentLocale = window.APPLICATION_LANGUAGE;
if (window.APPLICATION_LANGUAGE === 'en_US') {
    window.APPLICATION_DATE_FORMAT = "MMM D, YYYY, HH:mm:ss";
} else {
    window.APPLICATION_DATE_FORMAT = "YYYY-MM-DD HH:mm:ss";
}
let localeMapping = {};
//扫描模块语言包,并根据当前设置的语言来初始化语言配置
const localesContext = require.context('appRoot', true, /^\.\/modules\/((?!\/)[\s\S])+\/locales+\/[\s\S]*\.js$/);
localesContext.keys().forEach(key => {
    //获取每个模块的语言文件名作为localeMapping的key,同名的将进行合并
    let fileName = key.split("/").pop();
    let localName = fileName.substring(0, fileName.indexOf(".js"));
    let locale = localeMapping[localName] || {};
    localeMapping[localName] = Object.assign(locale, localesContext(key));
});


class LocaleHelper {

    /**
     * 语言变更
     * @param locale
     */
    changeLocale(locale) {
        if (currentLocale !== locale) {
            //TODO 在此进行公用语言逻辑映射,如en_GB en_xxx 统一用en_US
            currentLocale = locale;
            localStorage.setItem("t_lang_locale", locale);
            //i18next.changeLanguage(locale);
            //window.location.reload();
        }
    }

    /**
     * 获取显示内容
     * @param key  内容key
     * @param defaultMessage  默认显示内容
     * @param options intl-messageformat初始化的配置
     * @returns {*}
     */
    get(key, defaultMessage, options) {
        if (!localeMapping[currentLocale] && !options) return defaultMessage || key;
        let hasMapping = localeMapping[currentLocale];
        let msg = "";
        if (!hasMapping) {
            msg = defaultMessage || key;
        } else {
            msg = hasMapping[key] || defaultMessage || key;
        }
        if (options) {
            msg = new IntlMessageFormat(msg, currentLocale);
            return msg.format(options);
        }
        return msg;

    }

}

const localeHelper = new LocaleHelper();
export default localeHelper;