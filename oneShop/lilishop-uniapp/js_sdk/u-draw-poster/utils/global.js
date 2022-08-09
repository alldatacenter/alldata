var _a;
export const PLATFORM = typeof process !== 'undefined' ? (_a = process === null || process === void 0 ? void 0 : process.env) === null || _a === void 0 ? void 0 : _a.VUE_APP_PLATFORM : undefined;
/** 全局对象 */
const _uni = (function () {
    if (typeof uni != "undefined")
        return uni;
    if (typeof wx != "undefined")
        return wx;
    return uni;
})();
export default _uni;
