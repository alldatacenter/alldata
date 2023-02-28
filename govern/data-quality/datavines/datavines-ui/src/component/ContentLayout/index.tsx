import React from 'react';

type TProps = {
    height?: number | string,
    style?: React.CSSProperties,
    children?:React.ReactNode
}

const ContentLayout: React.FC<TProps> = ({ height, style, children }) => (
    <div style={{
        height: height || '100vh',
        backgroundColor: '#fff',
        overflowY: 'auto',
        padding: '20px 20px 20px 0px',
        ...(style || {}),
    }}
    >
        {children}
    </div>
);

export default ContentLayout;
