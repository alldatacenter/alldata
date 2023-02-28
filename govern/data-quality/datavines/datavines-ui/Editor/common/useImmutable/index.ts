import { useRef } from 'react';

const useImmutable = <T>(value: T) => useRef(value).current;

export default useImmutable;
