import React, { useState, useEffect } from 'react';
import { render } from 'react-dom';
import { Spin } from 'antd';
import useWatch from '../useWatch';

let div: any = null;

const useLoading = () => {
    const [loading, setLoading] = useState<boolean>(false);
    useWatch(loading, () => {
        if (div) {
            div.style.display = loading ? 'block' : 'none';
        }
    });

    useEffect(() => {
        if (!div) {
            div = document.createElement('div');
            div.style.display = 'none';
            render(
                (
                    <div
                        style={{
                            width: '100%',
                            height: '100%',
                            position: 'fixed',
                            zIndex: '9999',
                            left: 0,
                            top: 0,
                            background: 'rgba(222,222,222, 0.6)',
                            textAlign: 'center',
                        }}
                    >
                        <Spin size="large" style={{ marginTop: '220px' }} />
                    </div>
                ), div,
            );
            document.body.appendChild(div);
        }
        return () => {
            div.style.display = 'none';
        };
    }, []);
    return setLoading;
};

export default useLoading;
