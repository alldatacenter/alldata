import i18n from "i18next";
import LanguageDetector from 'i18next-browser-languagedetector';
// @ts-ignore
import enUsTrans from "./i18/locales/en-us.json";
// @ts-ignore
import zhCnTrans from "./i18/locales/zh-cn.json";
import {initReactI18next} from 'react-i18next';

i18n.use(LanguageDetector)
    .use(initReactI18next)
    .init({
        resources: {
            enUs: {
                translation: enUsTrans,
            },
            zhCn: {
                translation: zhCnTrans,
            },
        },
        fallbackLng: "enUs",
        debug: false,
        interpolation: {
            escapeValue: false,
        },
    })

export default i18n;
