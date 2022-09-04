
import * as util from '../../../../../utils/utils';
import cacheRepository from '../../../../../utils/cacheRepository';
import httpClient from '../../../../../utils/httpClient';
import properties from '../../../../../properties';

function getHeader() {
  return {
    headers: {
      "X-Biz-App": util.getNewBizApp(),
    },
  };
}
class flyAdminService {
  //获取详情页信息
  getApplicationVersion(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${params.appId}/app-packages/latest-version`, { params: params });
  }

  getApplicationServiceList(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${params.appId}/components`, { params: params });
  }

  //依赖组件的输出参数
  getListDataOutputs(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${params.appId}/addon/${params.addonName}/data-outputs`, { params: params });
  }

  //获取组件可用版本
  getListComponentVersions(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${params.appId}/component-packages/${params.componentType}/${params.componentName}/latest-version`, { params: params });
  }

  getMicroservice(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${params.appId}/k8s-microservices/${params.id}`, { params: params });
  }

  getListVar(appId) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${appId}/variables?type=env`);
  }

  getStageList(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/namespaces/${util.getUrlParams().namespaceId || properties.defaultNamespace}/stages`, {
      ...getHeader(),
      params: params,
    });
  }

  getK8SMicroservice(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${params.appId}/k8s-microservices/${params.id}`, { params: params });
  }

  postPackage(params) {
    return httpClient.post(`gateway/v2/foundation/appmanager/apps/${params.appId}/app-package-tasks`, params);
  }

  getComponentList(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/addon`, { params: params });
  }

  postMicroservice(params) {
    return httpClient.post(`gateway/v2/foundation/appmanager/apps/${params.appId}/k8s-microservices`, params);
  }

  putMicroservice(params) {
    return httpClient.put(`gateway/v2/foundation/appmanager/apps/${params.appId}/k8s-microservices/${params.id}`, params);
  }

  getMicroserviceDetail(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${params.appId}/k8s-microservices/${params.id}`, { params: params });
  }

  postAddon(params) {
    return httpClient.post(`gateway/v2/foundation/appmanager/apps/${params.appId}/addon`, params);
  }

  putAddon(params) {
    return httpClient.put(`gateway/v2/foundation/appmanager/apps/${params.appId}/addon/${params.addonName}`, params);
  }

  getAddonDetail(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${params.appId}/addon/${params.addonName}`, { params: params });
  }

  getGlobalConfiguration(appId) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${appId}/variables`);
  }

  postGlobalConfiguration(params, appId) {
    return httpClient.put(`gateway/v2/foundation/appmanager/apps/${appId}/variables`, params);
  }

  postVarEnv(params, appId) {
    return httpClient.put(`gateway/v2/foundation/appmanager/apps/${appId}/variables?type=env`, params);
  }

  getPackageTaskDetail(params) {
    return httpClient.get(`gateway/v2/foundation/appmanager/apps/${params.appId}/app-package-tasks/${params.taskId}`, { params: params });
  }

  postDeploy(yaml, autoEnvironment) {
    return httpClient.post("gateway/v2/foundation/appmanager/deployments/launch?autoEnvironment=" + autoEnvironment, yaml,
      { headers: { "Content-Type": "text/xml" } },
    );
  }

  getRoleList(params) {
    return httpClient.get("gateway/v2/common/authProxy/roles", {
      params: params, headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  setRoleList(params) {
    return httpClient.post("gateway/v2/common/authProxy/roles", params, {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  deleteRole(roleId) {
    return httpClient.delete(`gateway/v2/common/authProxy/roles/${roleId}`, null, {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  deleteUsers(userId, roleId) {
    return httpClient.delete(`gateway/v2/common/authProxy/users/${userId}/roles/${roleId}`, null, {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  putRole(params) {
    return httpClient.put(`gateway/v2/common/authProxy/roles/${params.roleId}`, params, {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  getAuthRoleList(roleId) {
    return httpClient.get("gateway/v2/common/authProxy/roles/" + roleId + "/permissions", {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  postPermission(roleId, params) {
    return httpClient.post(`gateway/v2/common/authProxy/roles/${roleId}/permissions/${params.permissionId}`, params, {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  deletePermission(roleId, permissionId) {
    return httpClient.delete(`gateway/v2/common/authProxy/roles/${roleId}/permissions/${permissionId}`, null, {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  postUsers(userId, roleId) {
    return httpClient.post(`gateway/v2/common/authProxy/users/${userId}/roles/${roleId}`, null, {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  getUserList(roleId) {
    return httpClient.get("gateway/v2/common/authProxy/roles/" + roleId + "/users", {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  getDepartmentList(roleId) {
    return httpClient.get("gateway/v2/common/authProxy/roles/" + roleId + "/departments", {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  getUserDepartmentList(userId) {
    return httpClient.get("gateway/v2/common/authProxy/users/" + userId + "/departments", {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }

  postRoleDepartment(roleId, params) {
    return httpClient.post(`gateway/v2/common/authProxy/roles/${roleId}/users`, params, {
      headers: {
        "X-Biz-App": util.getNewBizApp(),
      },
    });
  }
}

export default new flyAdminService();