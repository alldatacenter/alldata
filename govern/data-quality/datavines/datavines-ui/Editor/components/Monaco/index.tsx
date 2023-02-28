import React, { useRef, useImperativeHandle } from 'react';
import useEditor from '../../hooks/useEditor';

type InnerProps = {
    monacoRef?: any,
    style?: React.CSSProperties
}

const Editor: React.FC<InnerProps> = ({ monacoRef, style }) => {
    const divEl = useRef<any>(null);
    const { monacoInstance } = useEditor({
        elRef: divEl,
        value: '\n',
        language: 'mysql',
        tableColumnHints: [],
    });

    useImperativeHandle(monacoRef, () => ({
        getValue: () => monacoInstance?.getValue(),
    }));

    return <div id="container" className="Editor" style={style} ref={divEl} />;
};

export default Editor;
