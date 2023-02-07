import {ROLE_ADMIN, ROLE_NORMAL} from '@/constants/user';

declare global {
  type UserRole = ROLE_ADMIN | ROLE_NORMAL;
}
