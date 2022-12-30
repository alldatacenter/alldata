import i18n from 'i18next';
import Backend from 'i18next-http-backend';
import { initReactI18next } from 'react-i18next';

// don't want to use this?
// have a look at the Quick start guide
// for passing in lng and translations on init

i18n
  // load translation using http -> see /public/locales (i.e. https://github.com/i18next/react-i18next/tree/master/example/react/public/locales)
  // learn more: https://github.com/i18next/i18next-http-backend
  .use(Backend)
  // pass the i18n instance to react-i18next.
  .use(initReactI18next)
  // init i18next
  // for all options read: https://www.i18next.com/overview/configuration-options
  .init({
    // Commenting it as we have fallbackLng for case of language detected as en-GB or en-US
    // supportedLngs: ['en','it'],
    backend: {
      loadPath: `${__webpack_public_path__}locales/{{lng}}/{{ns}}.json`,
    },
    fallbackLng: 'en',
    load: 'languageOnly',
    debug: true,
    // add any namespaces you're using here for loading purposes
    ns: ['public'],
    defaultNS: 'public',
    nsSeparator: ':',
    keySeparator: '.',
    react: {
      useSuspense: true,
      wait: true,
    },

    interpolation: {
      defaultVariables: { connectorName: '' },
      escapeValue: false, // not needed for react as it escapes by default
    },
  });

export default i18n;
