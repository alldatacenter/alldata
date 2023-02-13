import React, {Component} from 'react';
import { Button } from 'antd';
import { connect } from 'dva';
import properties from 'appRoot/properties';
import cacheRepository from '../../utils/cacheRepository';
import localeHelper from '../../utils/localeHelper';

@connect(({ global}) => ({
    currentUser: global.currentUser,
}))
class NotFound extends Component {

    constructor(props, context) {
        super(props, context);
    }

    componentWillMount(){
        if(window.__TESLA_COMPONENT_LIB_CURRENT_APP&&window.__TESLA_COMPONENT_LIB_CURRENT_APP.name!==window.location.hash.split("/")[1]){
            const { dispatch} = this.props;
            dispatch({ type: 'global/showLoading',payload: { loading: true } });
        }
    }

    onRoleChange=(roleId)=>{
        const { dispatch} = this.props;
        dispatch({ type: 'global/switchRole',roleId:roleId,reloadCurrentPath:true });
    };


    render() {
        const { dispatch,currentUser} = this.props;
        if(window.__TESLA_COMPONENT_LIB_CURRENT_APP){
            if(window.__TESLA_COMPONENT_LIB_CURRENT_APP.name!==window.location.hash.split("/")[1]){
                dispatch({ type: 'global/showLoading',payload: { loading: true } });
                window.location.reload();
            }
        }
        let roles=currentUser.roles,msgTip=false,switchRoles=false;
        if(properties.envFlag===properties.ENV.Internal){
            let currentRole=cacheRepository.getRole(window.__TESLA_COMPONENT_LIB_CURRENT_APP.name);
            let otherRoles=roles.filter(role=>role.roleId!==currentRole);
            if(otherRoles.length){
                msgTip="抱歉，资源未找到或当前角色没有访问该资源权限,您可以切换至其他角色查看是否有权访问，如有疑问请扫描右上角答疑群咨询！";
                switchRoles=otherRoles.map(role=>{
                   return <Button type="primary" value={role.roleId} key={role.roleId} style={{marginRight:12}} onClick={(e)=>this.onRoleChange(e.target.value)}>切换至: {role.roleName}</Button>
                });
                switchRoles.push(<Button key="_back_home" type="primary" onClick={(e)=>window.location.href=""}>{localeHelper.get('BackToHome','返回首页')}</Button>);
            }
        }
        return (
            <div>
                需要增加异常页
            </div>
            /*<Exception style={{paddingTop:'100px', paddingBottom:'100px'}} type="403"
                       desc={msgTip?msgTip:`抱歉，当前访问资源没有权限，如有疑问请扫描右上角答疑群咨询！ `}
                       actions={switchRoles?switchRoles:null} />*/
        );
    }
}


export default NotFound;
