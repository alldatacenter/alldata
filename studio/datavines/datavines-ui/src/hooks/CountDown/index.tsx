import {
    useEffect, useState, useRef, useMemo,
} from 'react';
import { useWatch } from '@/common';

const useCountDown = (time = 60) => {
    const [remaining, setRemaining] = useState(0);
    const hasRemaining = useMemo(() => remaining >= 1 && remaining <= time, [remaining]);
    const timer = useRef<any>();
    const start = () => {
        if (hasRemaining) {
            return;
        }
        setRemaining(time - 1);
    };
    useWatch(remaining, () => {
        if (timer.current) {
            clearTimeout(timer.current);
        }
        if (remaining <= 0) {
            return;
        }
        timer.current = setTimeout(() => {
            if (remaining >= 1) {
                setRemaining(remaining - 1);
            }
        }, 1000);
    });
    useEffect(() => () => {
        if (timer.current) {
            clearTimeout(timer.current);
        }
    });

    return [remaining, start, hasRemaining] as const;
};
export default useCountDown;
