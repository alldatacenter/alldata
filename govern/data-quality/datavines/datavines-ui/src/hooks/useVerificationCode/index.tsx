import React, { useState, useRef } from 'react';
import { useImmutable, useMount, usePersistFn } from '@/common';
import { $http } from '@/http';

type TVerifyCode = {
    imageByte64?: string,
    verificationCodeJwt?: string,
}

const useVerificationCode = () => {
    const [verifyCode, setVerifyCode] = useState<TVerifyCode>({ imageByte64: '', verificationCodeJwt: '' });
    const verifyCodeRef = useRef(verifyCode);
    verifyCodeRef.current = verifyCode;
    const getImage = usePersistFn(async () => {
        try {
            const res = await $http.get<TVerifyCode>('/refreshVerificationCode');
            setVerifyCode({
                imageByte64: res?.imageByte64,
                verificationCodeJwt: res?.verificationCodeJwt,
            });
        } catch (error) {
            console.log(error);
        }
    });
    useMount(() => {
        getImage();
    });
    return {
        ...verifyCode,
        RenderImage: useImmutable((props: any) => <img {...props} onClick={getImage} src={verifyCodeRef.current.imageByte64} />),
    };
};

export default useVerificationCode;
