import React from 'react';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import enUS from 'antd/locale/en_US';
import { useSelector } from '@/store';

const ConfigProviderWrap:React.FC<{children:React.ReactNode}> = ({ children }) => {
    const { prefixCls, locale } = useSelector((r) => r.commonReducer);
    const getLocale = () => {
        if (locale === 'zh_CN') {
            return zhCN;
        }
        return enUS;
    };
    return (
        <ConfigProvider theme={{ token: { colorPrimary: '#FFD56A', borderRadius: 10 } }} locale={getLocale()} prefixCls={prefixCls}>
            {children}
        </ConfigProvider>
    );
};

export {
    ConfigProviderWrap,
};
