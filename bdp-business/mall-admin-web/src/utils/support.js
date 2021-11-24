import Cookies from "js-cookie";
const SupportKey='supportKey';
export const SupportUrl='https://coding.net/u/wlhbdp';
export function getSupport() {
  return Cookies.get(SupportKey)
}

export function setSupport(isSupport) {
  return Cookies.set(SupportKey, isSupport,{ expires: 3 })
}
