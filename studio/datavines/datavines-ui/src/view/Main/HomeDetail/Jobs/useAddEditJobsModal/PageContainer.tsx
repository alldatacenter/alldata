import React, { FC } from 'react';
import './pageContainer.less';

type TPageContainerProps = {
    footer?: React.ReactNode,
    children: React.ReactNode,
    style?:any
}

const PageContainer: FC<TPageContainerProps> = ({ footer, children, style }) => (
    <>
        <div className="dv-page-container__children" style={style}>
            {children}
        </div>
        <div className="dv-page-container__footer">
            {footer}
        </div>

    </>
);

export default PageContainer;
