import React from 'react';

type IndexProps = {
    visible?: boolean
    onVisible?: (...args: any[]) => boolean | undefined | null,
    children?: React.ReactNode
}

const Index: React.FC<IndexProps> = ({ visible, onVisible, children }) => {
    if (onVisible && onVisible()) {
        return <>{children}</>;
    }
    if (visible) {
        return <>{children}</>;
    }
    return null;
};
export default Index;
