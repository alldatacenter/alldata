import React from 'react';
import moment from 'moment';
import { Popover } from 'antd';

export const defaultTime = (text: number[], format?: string) => {
    if (!text || text.length <= 0) {
        return '--';
    }
    const $format = format || 'YYYY-MM-DD HH:mm:ss';
    const t1 = text.slice(0, 3).join('-');
    const t2 = text.slice(3).join(':');
    return moment(`${t1} ${t2}`).format($format);
};

const DefaultRender = ({ text, width }: {text: any, width: any}) => {
    if ([undefined, null, ''].includes(text)) return <>--</>;
    return (
        <Popover
            content={(
                <div style={{ maxWidth: width, wordWrap: 'break-word', wordBreak: 'break-word' }}>
                    {text || '--'}
                </div>
            )}
            trigger="hover"
        >
            <div
                style={{
                    maxWidth: width,
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                }}
            >
                {text}
            </div>
        </Popover>
    );
};

export const defaultRender = (text: any, width = 200) => <DefaultRender text={text} width={typeof width !== 'number' ? 200 : width} />;
