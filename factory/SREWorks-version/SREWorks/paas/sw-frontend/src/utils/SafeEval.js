/**
 * Created by caoshuaibiao on 2019/6/5.
 * 非绝对安全,只能保证一定的安全
 */
/*
 此方式为较安全方式，但大批量调用的时候存在性能问题,因此不适合大批量的场景,调用一次需要35ms左右,比eval、Function慢很多,因此进行了替换
const vm = require('vm');

module.exports = function safeEval (code, context, opts) {
    let sandbox = {};
    let resultKey = 'SAFE_EVAL_' + Math.floor(Math.random() * 1000000);
    sandbox[resultKey] = {};
    let clearContext = `
    (function() {
      Function = undefined;
      const keys = Object.getOwnPropertyNames(this).concat(['constructor']);
      keys.forEach((key) => {
        const item = this[key];
        if (!item || typeof item.constructor !== 'function') return;
        this[key].constructor = undefined;
      });
    })();`;
    code = clearContext + resultKey + '=' + code;
    if (context) {
        Object.keys(context).forEach(function (key) {
            sandbox[key] = context[key]
        })
    }
    vm.runInNewContext(code, sandbox, opts);
    return sandbox[resultKey];
};

*/


function compileCode(src) {
    src = `with(exposeObj) {return ${src}}`;
    return new Function('exposeObj', src);
}
function proxyObj(originObj) {
    return originObj;
    //暂时去掉代理沙箱机制
    /*let exposeObj = new Proxy(originObj, {
        has: (target, key) =>{
            if (["console", "Math", "Date"].indexOf(key) >= 0) {
                return target[key]
            }
            if (!target.hasOwnProperty(key)) {
                throw new Error(`Illegal operation for key ${key}`)
            }
            return target[key];
            //暂不控制,因为不确定业务自定义代码中的使用的范围
            return true;
        },
    });
    return exposeObj*/
}

const safeEval = (src, obj) => {
    let proxy = proxyObj(obj);
    return compileCode(src).call(proxy, proxy);
}

export default safeEval;