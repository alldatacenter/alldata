import {OS_WINDOWS} from '@/constants/os';
// import osName from 'os-name';

export const getOS = (): string => {
  return '';
  // return osName().toLowerCase();
};

export const isWindows = (): boolean => {
  return getOS().includes(OS_WINDOWS);
};

export const getOSPathSeparator = () => {
  return isWindows() ? '\\' : '/';
};
