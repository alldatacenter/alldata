/**
 * Created by caoshuaibiao on 2019/4/9.
 * 返回结果拦截器,要求每个拦截器给出具体的提示信息及处理办法等
 */
import  localeHelper from '../utils/localeHelper';
//节点不存在业务异常
function nodeNotFoundHander(responseData) {
    if(responseData.code===9000){
        //清除本地的集群选择和主机选择缓存
        let lsCount=localStorage.length;
        for (let l=lsCount-1;l>=0;l--){
            let lkey=localStorage.key(l);
            if(lkey&&(lkey.includes("OamContentWithTreePanel")||lkey.includes("oam_tree")||lkey.includes("OamHost"))){
                localStorage.removeItem(lkey)
            }
        }
        return {
            notification:true,
            message:localeHelper.get('common.requestSourceLose','请求资源未找到或已失效,请检查请求资源')
        }
    }
    return false;
}





class OamExceptionHander{

    handler=[
        nodeNotFoundHander
    ];

    handleException(responseData){
        for(let h=0;h<this.handler.length;h++){
            let handleMsg=this.handler[h](responseData);
            if(handleMsg) return handleMsg;
        }
        return false;
    }
}


export default new OamExceptionHander();


