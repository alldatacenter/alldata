import {
  OS_WINDOWS,
  OS_MAC,
  OS_LINUX,
} from '@/constants/os';

export declare global {
  type OS = OS_WINDOWS | OS_MAC | OS_LINUX;
}
