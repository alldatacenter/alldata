import { useState } from 'react';
import loader from '@monaco-editor/loader';
import { useMount } from '../../common';

function useMonaco(monacoConfig: any) {
    // eslint-disable-next-line no-underscore-dangle
    const [monaco, setMonaco] = useState<any>(loader.__getMonacoInstance());

    useMount(() => {
        let cancelable: any;
        if (monacoConfig) {
            loader.config(monacoConfig);
        }
        if (!monaco) {
            cancelable = loader.init();

            cancelable.then(($monaco: any) => {
                setMonaco($monaco);
            });
        }

        return () => cancelable?.cancel();
    });

    return monaco;
}

export default useMonaco;
