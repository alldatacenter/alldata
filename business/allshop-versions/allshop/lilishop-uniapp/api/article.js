/**
 * 文章相关API
 */

import {http,Method} from '@/utils/request.js';
import api from '@/config/api.js';

/**
 * 获取某个分类的文章列表
 * @param category_type
 */
export function getArticleCategory(category_type) {
  return http.request({
    url: `${api.base}/pages/article-categories`,
    method: Method.GET,
    params: {category_type},
  });
}

/**
 * 获取文章详情
 * @param type
 */
export function getArticleDetail(type) {
  return http.request({
    url: `/other/article/get/${type}`,
    method: Method.GET,
  });
}

