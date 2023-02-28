import React, { FC } from 'react';
import GoBack from '../GoBack';
import './index.less';

const Title: FC<{ isBack?: boolean;children: React.ReactNode }> = ({ children, isBack }) => (
    <div className={isBack ? 'dv-title' : 'dv-title dv-title_noback'}>
        {isBack && <GoBack style={{ marginRight: 10 }} />}
        {children}
    </div>
);

export default Title;
