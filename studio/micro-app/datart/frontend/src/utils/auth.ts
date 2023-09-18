import {
  DEFAULT_AUTHORIZATION_TOKEN_EXPIRATION,
  StorageKeys,
} from 'globalConstants';
import Cookies from 'js-cookie';

let tokenExpiration = DEFAULT_AUTHORIZATION_TOKEN_EXPIRATION;

export function setTokenExpiration(expires: number) {
  tokenExpiration = expires;
}

export function getToken() {
  return Cookies.get(StorageKeys.AuthorizationToken);
}

export function setToken(token: string) {
  Cookies.set(StorageKeys.AuthorizationToken, token, {
    expires: new Date(new Date().getTime() + tokenExpiration),
  });
}

export function removeToken() {
  Cookies.remove(StorageKeys.AuthorizationToken);
}
