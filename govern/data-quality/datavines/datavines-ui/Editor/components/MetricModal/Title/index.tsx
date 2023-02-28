import React from 'react';
import './index.less';

type TIndexProps = {
    title: string,
    children: React.ReactNode,
};

const Index: React.FC<TIndexProps> = ({ children, title }: TIndexProps) => (
    <>
        <div className="dv-editor__metric-title">{title}</div>
        <div style={{ padding: '10px 10px 10px' }}>
            {children}
        </div>
    </>
);

export default React.memo(Index);
