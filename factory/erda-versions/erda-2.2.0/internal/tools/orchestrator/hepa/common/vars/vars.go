// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package vars

import "github.com/aliyun/alibaba-cloud-sdk-go/services/cloudapi"

const (
	TODO_PARAM = "request error"
)

var (
	ENV_TYPE_PROD = "PROD"
)

var (
	ERR_SQL_FAIL    = "sql failed"
	ERR_INVALID_ARG = "invalid argument"
	ERR_NO_CHANGE   = "no change happen"
	ERR_JSON_FAIL   = "json failed"
)

var (
	CloudapiEndpointType = cloudapi.GetEndpointType()
	CloudapiEndpointMap  = cloudapi.GetEndpointMap()
)

var (
	IS_DELETED_VALUE  = "Y"
	NOT_DELETED_VALUE = "N"
)

var (
	KONG_HTTPS_SERVICE_PORT = 8443
	KONG_SERVICE_PORT       = 8000
	BIND_DOMAIN_INGRESS_TAG = "-domain"
	INNER_INGRESS_TAG       = "-inner"
)

type StandardErrorCode struct {
	Code    string
	Message string
}

func (err StandardErrorCode) GetCode() string {
	return err.Code
}

func (err StandardErrorCode) GetMessage() string {
	return err.Message
}

var (
	UNKNOW_ERROR         = StandardErrorCode{"GW_10000", "未知错误"}
	PARAMS_IS_NULL       = StandardErrorCode{"GW_10001", "参数不能为空"}
	PROJECT_ID_IS_NULL   = StandardErrorCode{"GW_100001", "projectId不能为空"}
	GROUP_NAME_IS_NULL   = StandardErrorCode{"GW_100002", "groupName不能为空"}
	DISPLAY_NAME_IS_NULL = StandardErrorCode{"GW_100003", "displayName不能为空"}
	CATEGORY_IS_NULL     = StandardErrorCode{"GW_100004", "category不能为空"}
	PLUGINNAME_IS_NULL   = StandardErrorCode{"GW_100005", "pluginName不能为空"}
	ORG_ID_IS_NULL       = StandardErrorCode{"GW_100006", "orgId不能为空"}
	GROUP_ID_IS_NULL     = StandardErrorCode{"GW_100007", "groupId不能为空"}
	ANY_API_IN_GROUP     = StandardErrorCode{"GW_100008", "这个group中还有使用的api"}
	INVALID_PATH         = StandardErrorCode{"GW_100009", "错误的路径配置"}

	GROUP_NOT_EXIST    = StandardErrorCode{"GW_100020", "group不存在"}
	CONSUMER_NOT_EXIST = StandardErrorCode{"GW_100021", "consumer不存在"}
	API_NOT_EXIST      = StandardErrorCode{"GW_100022", "api不存在"}
	POLICY_EXIST       = StandardErrorCode{"GW_100023", "policy已存在"}
	POLICY_NOT_EXIST   = StandardErrorCode{"GW_100024", "policy不存在"}
	SERVICE_NOT_EXIST  = StandardErrorCode{"GW_100025", "service不存在"}
	API_EXIST          = StandardErrorCode{"GW_100026", "api已存在, 或者与已存在的api冲突"}

	CREATE_API_SERVICE_FAIL            = StandardErrorCode{"GW_100031", "创建api失败，转发地址错误"}
	CREATE_API_ROUTE_FAIL              = StandardErrorCode{"GW_100032", "创建api失败，API path错误"}
	CREATE_API_PLUGIN_FAIL             = StandardErrorCode{"GW_100033", "创建api失败"}
	UPDATE_API_SERVICE_FAIL            = StandardErrorCode{"GW_100034", "更新api失败，转发地址错误"}
	UPDATE_API_ROUTE_FAIL              = StandardErrorCode{"GW_100035", "更新api失败，API path错误"}
	UPDATE_API_PLUGIN_FAIL             = StandardErrorCode{"GW_100036", "更新api失败"}
	DELETE_POLICY_FAIL                 = StandardErrorCode{"GW_100037", "删除策略失败失败，当前有正在使用该策略的api接口"}
	GET_TRANS_CONFIG_CLUSTER_NAME_MISS = StandardErrorCode{"GW_100038", "clusterName 缺失"}
	RESGITER_MISS_PARAMS               = StandardErrorCode{"GW_100039", "缺失参数,zkUrl or clusterName or envType or runtimeId"}
	DELETE_TRANS_CONFIGS_MISS_PARAMS   = StandardErrorCode{"GW_100040", "缺失参数, clusterName or envType or runtimeId or targetKeyList is empty"}
	RESGITER_MISS_CONFIG_OR_OSS        = StandardErrorCode{"GW_100041", "缺失参数,configJson and ossFileUrl both"}
	SOLUTION_IS_ERR_NULL               = StandardErrorCode{"GW_100042", "solution is err null"}
	CONSUMER_EXIST                     = StandardErrorCode{"GW_100043", "consumer 已经存在"}
	CONSUMER_ID_MISS                   = StandardErrorCode{"GW_100044", "consumerId 缺失"}
	CONSUMER_PARAMS_MISS               = StandardErrorCode{"GW_100045", "参数缺失"}

	//业务网关注册
	TRANSFORM_REGIS_NO_SOLUTION = StandardErrorCode{"GW_200001", "solution 为空"}
	TRANSFORM_CALL_PARAMS_MISS  = StandardErrorCode{"GW_200002", "envType或runtimeId缺失"}

	//mock数据
	MOCK_IS_NOT_EXISTS = StandardErrorCode{"GW_300003", "mock接口不存在"}

	CLUSTER_NOT_EXIST = StandardErrorCode{"GW_400001", "集群查找失败"}
	KONG_NOT_EXIST    = StandardErrorCode{"GW_400002", "kong服务查找失败"}
	CLUSTER_NOT_K8S   = StandardErrorCode{"GW_400003", "非k8s集群不支持此功能"}
	DOMAIN_EXIST      = StandardErrorCode{"GW_400004", "域名已经被占用"}

	PACKAGE_IN_CONSUMER  = StandardErrorCode{"GW_400005", "请取消该流量入口的所有授权后再删除"}
	PACKAGE_EXIST        = StandardErrorCode{"GW_400006", "已存在同名流量入口"}
	DICE_API_NOT_MUTABLE = StandardErrorCode{"GW_400007", "不可修改依赖服务API的路由信息"}
	INVALID_LIMIT_RULE   = StandardErrorCode{"GW_400008", "流量限制参数错误"}
	INVALID_LIMIT_API    = StandardErrorCode{"GW_400009", "只能限制流量入口中已存在的API"}
	API_IN_PACKAGE       = StandardErrorCode{"GW_400010", "API被其他流量入口引用,不可更改或删除"}
	LIMIT_RULE_EXIST     = StandardErrorCode{"GW_400011", "该规则已存在，请直接编辑"}
)

type PolicyCategory struct {
	Name    string
	CnName  string
	Plugin  string
	Carrier string
}

var (
	POLICY_ENUMS                   = map[string]PolicyCategory{}
	TRAFFIC_CONTROL PolicyCategory = PolicyCategory{
		Name:    "trafficControl",
		CnName:  "流控策略",
		Plugin:  "rate-limiting",
		Carrier: "ROUTE,CONSUMER",
	}
)

func GetPolicyCategory(name string) *PolicyCategory {
	category, ok := POLICY_ENUMS[name]
	if !ok {
		return nil
	}
	return &category
}

func init() {
	POLICY_ENUMS[TRAFFIC_CONTROL.Name] = TRAFFIC_CONTROL
}
