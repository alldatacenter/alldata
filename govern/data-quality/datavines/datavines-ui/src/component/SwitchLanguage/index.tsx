import React, { memo } from 'react';
import { Popover } from 'antd';
import { useIntl } from 'react-intl';
import { useSelector, useCommonActions } from '@/store';
import shareData from '@/utils/shareData';
import { DV_LANGUAGE } from '@/utils/constants';

const localMap = {
    zh_CN: '简体中文',
    en_US: 'English',
};

const SwitchLanguage: React.FC<{}> = () => {
    const intl = useIntl();
    const { locale } = useSelector((r) => r.commonReducer);
    const { setLocale } = useCommonActions();
    const swichFn = () => {
        const $locale = locale === 'zh_CN' ? 'en_US' : 'zh_CN';
        setLocale($locale);
        shareData.storageSet(DV_LANGUAGE, $locale);
    };

    return (
        <Popover
            content={(
                <div style={{ width: 110, textAlign: 'center' }}>
                    {intl.formatMessage({ id: 'common_switch_language' })}
                </div>
            )}
        >
            <span onClick={swichFn} style={{ cursor: 'pointer' }}>
                {localMap[locale]}
            </span>
        </Popover>
    );
};
export default memo(SwitchLanguage);
