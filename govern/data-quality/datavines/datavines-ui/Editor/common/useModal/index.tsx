import React, {
    useState, useRef, useCallback, createContext, useContext,
} from 'react';
import { Modal } from 'antd';
import { ModalProps } from 'antd/lib/modal';
import useImmutable from '../useImmutable';
import usePersistFn from '../usePersistFn';

type TModalContextProps<T = any> = {
    setModalProps: (data: ModalProps) => void;
    show: (data: T) => void;
    hide: () => void;
    data: T
}

const Context = createContext<TModalContextProps>({} as TModalContextProps);
export const useContextModal = <T=any>() => useContext<TModalContextProps<T>>(Context);

function useModal<T = any>(options: ModalProps) {
    const [visible, setVisible] = useState(false);
    const visibleRef = useRef(visible);
    visibleRef.current = visible;

    const optionsRef = useRef(options || {});
    optionsRef.current = options || {};

    const [modalProps, $setModalProps] = useState({});
    const modalPropsRef = useRef(modalProps);
    modalPropsRef.current = modalProps;

    const dataRef = useRef<T>();
    const show = usePersistFn((data: T) => {
        dataRef.current = data;
        setVisible(true);
    });
    const hide = useCallback(() => {
        setVisible(false);
    }, []);
    const setModalProps = usePersistFn((props: ModalProps) => {
        console.log('visibleRef.current', visibleRef.current, props);
        if (visibleRef.current) {
            $setModalProps(props || {});
        }
    });

    const contextRef = useRef<TModalContextProps<T>>();
    contextRef.current = {
        show,
        hide,
        setModalProps,
        data: dataRef.current,
    } as TModalContextProps<T>;

    const Render = useImmutable<React.FC<ModalProps>>(({ children, ...rest }) => {
        const $afterClose = usePersistFn(() => {
            const { afterClose } = { ...rest, ...optionsRef.current, ...modalPropsRef.current };
            if (typeof afterClose === 'function') {
                afterClose();
            }
            $setModalProps({});
            dataRef.current = undefined;
        });
        const modalProp = {
            destroyOnClose: true,
            onCancel: hide,
            onOk: hide,
            ...rest,
            ...optionsRef.current,
            ...modalPropsRef.current,
            visible: visibleRef.current,
            afterClose: $afterClose,
        };
        return (
            <Context.Provider value={contextRef.current!}>
                <Modal {...modalProp}>
                    {children}
                </Modal>
            </Context.Provider>

        );
    });
    return {
        visible,
        Render,
        setModalProps,
        hide,
        show,
        context: contextRef.current,
    };
}

export default useModal;
