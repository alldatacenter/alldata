import {Router} from 'vue-router';

export const getRoutePathByDepth = (path: string, depth?: number) => {
  if (!depth) depth = 1;
  const arr = path.split('/');
  if (!arr[0]) depth += 1;
  return arr.slice(0, depth).join('/');
};

export const getTabName = (router?: Router) => {
  if (!router) return '';
  const arr = router.currentRoute.value.path.split('/');
  if (arr.length < 3) return '';
  return arr[3];
};
