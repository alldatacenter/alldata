/*
 * @Author: Mr.Mao
 * @LastEditors: Mr.Mao
 * @Date: 2020-10-12 08:49:27
 * @LastEditTime: 2020-12-09 13:54:10
 * @Description:
 * @任何一个傻子都能写出让电脑能懂的代码，而只有好的程序员可以写出让人能看懂的代码
 */
import uni from "./global";
import { isBaseUrl, isNetworkUrl, isTmpUrl } from './utils';
// 下载指定地址图片, 如果不符合下载图片, 则直接返回
export const downloadImgUrl = (url) => {
    const isLocalFile = isBaseUrl(url) || isTmpUrl(url) || !isNetworkUrl(url);
    return new Promise((resolve, reject) => {
        if (isLocalFile) {
            return resolve(url);
        }
        uni.downloadFile({
            url,
            success: (res) => resolve(res.tempFilePath),
            fail: reject
        });
    });
};
// 获取当前指定 node 节点
export const getCanvas2dContext = (selector, componentThis) => {
    return new Promise(resolve => {
        const query = (componentThis ?
            uni.createSelectorQuery().in(componentThis) :
            uni.createSelectorQuery());
        query.select(selector)
            .fields({ node: true }, res => {
            const node = res === null || res === void 0 ? void 0 : res.node;
            resolve(node || {});
        }).exec();
    });
};
