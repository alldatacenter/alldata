import React, { useCallback } from 'react';
import { IntlProvider } from 'react-intl';
import EditLocaleEn from '@Editor/locale/en_US';
import EditLocaleZh from '@Editor/locale/zh_CN';
import { useSelector } from '@/store';
import zhCN from './zh_CN';
import enUS from './en_US';

const zh = {
    ...EditLocaleZh,
    ...zhCN,
};

const en = {
    ...EditLocaleEn,
    ...enUS,
};

export type TLocaleType = 'en' | 'zh';

declare global {
    namespace FormatjsIntl {
        interface Message {
            ids: keyof typeof en
        }
        interface IntlConfig {
          locale: TLocaleType
        }
    }
}

export const IntlWrap: React.FC<{children:React.ReactNode}> = (props) => {
    const { locale } = useSelector((r) => r.commonReducer);
    const $locale = locale.split('_')[0] as TLocaleType;
    const chooseLocale = useCallback((val: TLocaleType) => {
        switch (val) {
            case 'zh':
                return zh;
            default:
                return en;
        }
    }, []);
    return (
        <IntlProvider
            key={$locale}
            locale={$locale}
            defaultLocale={$locale}
            messages={chooseLocale($locale)}
        >
            <>
                { props.children || ''}
            </>
        </IntlProvider>
    );
};
