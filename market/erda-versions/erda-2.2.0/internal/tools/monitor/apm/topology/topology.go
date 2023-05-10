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

package topology

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/conv"

	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/i18n"
	apm "github.com/erda-project/erda/internal/tools/monitor/apm/common"
	"github.com/erda-project/erda/internal/tools/monitor/common/db"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
	api "github.com/erda-project/erda/pkg/common/httpapi"
	pkgmath "github.com/erda-project/erda/pkg/math"
)

type Vo struct {
	StartTime   int64    `query:"startTime"`
	EndTime     int64    `query:"endTime"`
	TerminusKey string   `query:"terminusKey" validate:"required"`
	Tags        []string `query:"tags"`
	Debug       bool     `query:"debug"`
}

type Response struct {
	Nodes []*Node `json:"nodes"`
}

func GetTopologyPermission(db *db.DB) httpserver.Interceptor {
	return permission.Intercepter(
		permission.ScopeProject, permission.TkFromParams(db),
		apm.MonitorTopology, permission.ActionGet,
	)
}

const TimeLayout = "2006-01-02 15:04:05"

type Node struct {
	Id              string  `json:"id,omitempty"`
	Name            string  `json:"name,omitempty"`
	Type            string  `json:"type,omitempty"`
	TypeDisplay     string  `json:"typeDisplay,omitempty"`
	AddonId         string  `json:"addonId,omitempty"`
	AddonType       string  `json:"addonType,omitempty"`
	ApplicationId   string  `json:"applicationId,omitempty"`
	ApplicationName string  `json:"applicationName,omitempty"`
	RuntimeId       string  `json:"runtimeId,omitempty"`
	RuntimeName     string  `json:"runtimeName,omitempty"`
	ServiceId       string  `json:"serviceId,omitempty"`
	ServiceName     string  `json:"serviceName,omitempty"`
	DashboardId     string  `json:"dashboardId"`
	Metric          *Metric `json:"metric"`
	Parents         []*Node `json:"parents"`
}

const (
	topologyNodeService   = "topology_node_service"
	topologyNodeGateway   = "topology_node_gateway"
	topologyNodeDb        = "topology_node_db"
	topologyNodeCache     = "topology_node_cache"
	topologyNodeMq        = "topology_node_mq"
	topologyNodeOther     = "topology_node_other"
	processAnalysisNodejs = "process_analysis_nodejs"
	processAnalysisJava   = "process_analysis_java"
)

const (
	JavaProcessType   = "jvm_memory"
	NodeJsProcessType = "nodejs_memory"
)

const (
	TypeService        = "Service"
	TypeMysql          = "Mysql"
	TypeRedis          = "Redis"
	TypeRocketMQ       = "RocketMQ"
	TypeExternal       = "ExternalService"
	TypeInternal       = "InternalService"
	TypeDubbo          = "Dubbo"
	TypeSidecar        = "SideCar"
	TypeGateway        = "APIGateway"
	TypeRegisterCenter = "RegisterCenter"
	TypeConfigCenter   = "ConfigCenter"
	TypeNoticeCenter   = "NoticeCenter"
	TypeElasticsearch  = "Elasticsearch"
)

type SearchTag struct {
	Tag   string `json:"tag"`
	Label string `json:"label"`
	Type  string `json:"type"`
}

var (
	ApplicationSearchTag = SearchTag{
		Tag:   "application",
		Label: "应用名称",
		Type:  "select",
	}

	ServiceSearchTag = SearchTag{
		Tag:   "service",
		Label: "服务名称",
		Type:  "select",
	}
)

var ReqMetricNames = []string{
	"application_http_service",
	"application_rpc_service",
	"application_cache_service",
	"application_db_service",
	"application_mq_service",
}

type Field struct {
	ELapsedCount float64 `json:"elapsed_count,omitempty"`
	ELapsedMax   float64 `json:"elapsed_max,omitempty"`
	ELapsedMean  float64 `json:"elapsed_mean,omitempty"`
	ELapsedMin   float64 `json:"elapsed_min,omitempty"`
	ELapsedSum   float64 `json:"elapsed_sum,omitempty"`
	CountSum     float64 `json:"count_sum,omitempty"`
	ErrorsSum    float64 `json:"errors_sum,omitempty"`
}

type Tag struct {
	Component             string `json:"component,omitempty"`
	DBType                string `json:"db_type,omitempty"`
	DBSystem              string `json:"db_system,omitempty"`
	Host                  string `json:"host,omitempty"`
	HttpUrl               string `json:"http_url,omitempty"`
	PeerServiceScope      string `json:"peer_service_scope,omitempty"`
	PeerAddress           string `json:"peer_address,omitempty"`
	PeerService           string `json:"peer_service,omitempty"`
	DBHost                string `json:"db_host,omitempty"`
	SourceProjectId       string `json:"source_project_id,omitempty"`
	SourceProjectName     string `json:"source_project_name,omitempty"`
	SourceWorkspace       string `json:"source_workspace,omitempty"`
	SourceTerminusKey     string `json:"source_terminus_key,omitempty"`
	SourceApplicationId   string `json:"source_application_id,omitempty"`
	SourceApplicationName string `json:"source_application_name,omitempty"`
	SourceRuntimeId       string `json:"source_runtime_id,omitempty"`
	SourceRuntimeName     string `json:"source_runtime_name,omitempty"`
	SourceServiceName     string `json:"source_service_name,omitempty"`
	SourceServiceId       string `json:"source_service_id,omitempty"`
	SourceAddonID         string `json:"source_addon_id,omitempty"`
	SourceAddonType       string `json:"source_addon_type,omitempty"`
	TargetInstanceId      string `json:"target_instance_id,omitempty"`
	TargetProjectId       string `json:"target_project_id,omitempty"`
	TargetProjectName     string `json:"target_project_name,omitempty"`
	TargetWorkspace       string `json:"target_workspace,omitempty"`
	TargetTerminusKey     string `json:"target_terminus_key,omitempty"`
	TargetApplicationId   string `json:"target_application_id,omitempty"`
	TargetApplicationName string `json:"target_application_name,omitempty"`
	TargetRuntimeId       string `json:"target_runtime_id,omitempty"`
	TargetRuntimeName     string `json:"target_runtime_name,omitempty"`
	TargetServiceName     string `json:"target_service_name,omitempty"`
	TargetServiceId       string `json:"target_service_id,omitempty"`
	TargetAddonID         string `json:"target_addon_id,omitempty"`
	TargetAddonType       string `json:"target_addon_type,omitempty"`
	TerminusKey           string `json:"terminus_key,omitempty"`
	ProjectId             string `json:"project_id,omitempty"`
	ProjectName           string `json:"project_name,omitempty"`
	Workspace             string `json:"workspace,omitempty"`
	ApplicationId         string `json:"application_id,omitempty"`
	ApplicationName       string `json:"application_name,omitempty"`
	RuntimeId             string `json:"runtime_id,omitempty"`
	RuntimeName           string `json:"runtime_name,omitempty"`
	ServiceName           string `json:"service_name,omitempty"`
	ServiceId             string `json:"service_id,omitempty"`
	ServiceInstanceId     string `json:"service_instance_id,omitempty"`
	ServiceIp             string `json:"service_ip,omitempty"`
	Type                  string `json:"type,omitempty"`
}

type TopologyNodeRelation struct {
	Name      string                  `json:"name,omitempty"`
	Timestamp int64                   `json:"timestamp,omitempty"`
	Tags      Tag                     `json:"tags,omitempty"`
	Fields    Field                   `json:"fields,omitempty"`
	Parents   []*TopologyNodeRelation `json:"parents,omitempty"`
	Metric    *Metric                 `json:"metric,omitempty"`
}

type Metric struct {
	Count     int64   `json:"count"`
	HttpError int64   `json:"http_error"`
	RT        float64 `json:"rt"`
	ErrorRate float64 `json:"error_rate"`
	Replicas  float64 `json:"replicas,omitempty"`
	Running   float64 `json:"running"`
	Stopped   float64 `json:"stopped"`
	RPS       float64 `json:"rps"`
	Duration  float64 `json:"duration"`
}

const (
	Application = "application"
	Service     = "service"

	HttpIndex        = apm.Spot + apm.Sep1 + Application + apm.Sep3 + "http" + apm.Sep3 + Service
	RpcIndex         = apm.Spot + apm.Sep1 + Application + apm.Sep3 + "rpc" + apm.Sep3 + Service
	MicroIndex       = apm.Spot + apm.Sep1 + Application + apm.Sep3 + "micro" + apm.Sep3 + Service
	MqIndex          = apm.Spot + apm.Sep1 + Application + apm.Sep3 + "mq" + apm.Sep3 + Service
	DbIndex          = apm.Spot + apm.Sep1 + Application + apm.Sep3 + "db" + apm.Sep3 + Service
	CacheIndex       = apm.Spot + apm.Sep1 + Application + apm.Sep3 + "cache" + apm.Sep3 + Service
	ServiceNodeIndex = apm.Spot + apm.Sep1 + "service_node"
)

var IndexPrefix = []string{
	HttpIndex, RpcIndex, MicroIndex,
	MqIndex, DbIndex, CacheIndex,
	ServiceNodeIndex,
}

type ServiceDashboard struct {
	Id              string  `json:"service_id"`
	Name            string  `json:"service_name"`
	ReqCount        int64   `json:"req_count"`
	ReqErrorCount   int64   `json:"req_error_count"`
	ART             float64 `json:"avg_req_time"`           // avg response time
	RSInstanceCount string  `json:"running_instance_count"` // running / stopped
	RuntimeId       string  `json:"runtime_id"`
	RuntimeName     string  `json:"runtime_name"`
	ApplicationId   string  `json:"application_id"`
	ApplicationName string  `json:"application_name"`
}

func createTypologyIndices(startTimeMs int64, endTimeMs int64) map[string][]string {
	//	HttpRecMircoIndexType = "http-rpc-mirco"
	//	MQDBCacheIndexType    = "mq-db-cache"
	//	ServiceNodeIndexType  = "service-node"
	indices := make(map[string][]string)
	if startTimeMs > endTimeMs {
		indices[apm.EmptyIndex] = []string{apm.EmptyIndex}
	}

	for _, prefix := range IndexPrefix {
		index := prefix + apm.Sep1 + apm.Sep2
		if ReHttpRpcMicro.MatchString(prefix) {
			fillingIndex(indices, index, HttpRecMircoIndexType)
		}

		if ReMqDbCache.MatchString(prefix) {
			fillingIndex(indices, index, MQDBCacheIndexType)
		}

		if ReServiceNode.MatchString(prefix) {
			fillingIndex(indices, index, ServiceNodeIndexType)
		}
	}

	if len(indices) <= 0 {
		indices[apm.EmptyIndex] = []string{apm.EmptyIndex}
	}
	return indices
}

func fillingIndex(indices map[string][]string, index string, indexType string) {
	i := indices[indexType]
	if i == nil {
		indices[indexType] = []string{index}
	} else {
		indices[indexType] = append(i, index)
	}
}

func (topology *provider) indexExist(indices []string) *elastic.IndicesExistsService {
	exists := topology.es.IndexExists(indices...)
	return exists
}

var (
	ReServiceNode  = regexp.MustCompile("^" + ServiceNodeIndex + "(.*)$")
	ReHttpRpcMicro = regexp.MustCompile("^(" + HttpIndex + "|" + RpcIndex + "|" + MicroIndex + ")(.*)$")
	ReMqDbCache    = regexp.MustCompile("^(" + MqIndex + "|" + DbIndex + "|" + CacheIndex + ")(.*)$")
)

type NodeType struct {
	Type         string
	GroupByField *GroupByField
	SourceFields []string
	Filter       *elastic.BoolQuery
	Aggregation  map[string]*elastic.SumAggregation
}

type GroupByField struct {
	Name     string
	SubField *GroupByField
}

var (
	TargetServiceNodeType   *NodeType
	SourceServiceNodeType   *NodeType
	TargetAddonNodeType     *NodeType
	SourceAddonNodeType     *NodeType
	TargetComponentNodeType *NodeType
	TargetOtherNodeType     *NodeType
	SourceMQNodeType        *NodeType
	TargetMQNodeType        *NodeType
	TargetMQServiceNodeType *NodeType
	OtherNodeType           *NodeType

	ServiceNodeAggregation map[string]*elastic.SumAggregation
	NodeAggregation        map[string]*elastic.SumAggregation
)

type NodeRelation struct {
	Source []*NodeType
	Target *NodeType
}

const (
	TargetServiceNode   = "TargetServiceNode"
	SourceServiceNode   = "SourceServiceNode"
	TargetAddonNode     = "TargetAddonNode"
	SourceAddonNode     = "SourceAddonNode"
	TargetComponentNode = "TargetComponentNode"
	TargetOtherNode     = "TargetOtherNode"
	SourceMQNode        = "SourceMQNode"
	TargetMQNode        = "TargetMQNode"
	TargetMQServiceNode = "TargetMQServiceNode"
	OtherNode           = "OtherNode"
)

func init() {
	ServiceNodeAggregation = map[string]*elastic.SumAggregation{
		apm.FieldsCountSum:  elastic.NewSumAggregation().Field(apm.FieldsCountSum),
		apm.FieldElapsedSum: elastic.NewSumAggregation().Field(apm.FieldElapsedSum),
		apm.FieldsErrorsSum: elastic.NewSumAggregation().Field(apm.FieldsErrorsSum),
	}
	NodeAggregation = map[string]*elastic.SumAggregation{
		apm.FieldsCountSum:  elastic.NewSumAggregation().Field(apm.FieldsCountSum),
		apm.FieldElapsedSum: elastic.NewSumAggregation().Field(apm.FieldElapsedSum),
	}

	TargetServiceNodeType = &NodeType{
		Type:         TargetServiceNode,
		GroupByField: &GroupByField{Name: apm.TagsTargetServiceId, SubField: &GroupByField{Name: apm.TagsTargetServiceName}},
		SourceFields: []string{apm.TagsTargetApplicationId, apm.TagsTargetRuntimeName, apm.TagsTargetServiceName, apm.TagsTargetServiceId, apm.TagsTargetApplicationName, apm.TagsTargetRuntimeId},
		Filter:       elastic.NewBoolQuery().MustNot(elastic.NewExistsQuery(apm.TagsTargetAddonType)),
		Aggregation:  ServiceNodeAggregation,
	}
	SourceServiceNodeType = &NodeType{
		Type:         SourceServiceNode,
		GroupByField: &GroupByField{Name: apm.TagsSourceServiceId, SubField: &GroupByField{Name: apm.TagsSourceServiceName}},
		SourceFields: []string{apm.TagsSourceApplicationId, apm.TagsSourceRuntimeName, apm.TagsSourceServiceName, apm.TagsSourceServiceId, apm.TagsSourceApplicationName, apm.TagsSourceRuntimeId},
		Filter:       elastic.NewBoolQuery().MustNot(elastic.NewExistsQuery(apm.TagsSourceAddonType)),
		Aggregation:  ServiceNodeAggregation,
	}
	TargetAddonNodeType = &NodeType{
		Type:         TargetAddonNode,
		GroupByField: &GroupByField{Name: apm.TagsTargetAddonType, SubField: &GroupByField{Name: apm.TagsTargetAddonId}},
		SourceFields: []string{apm.TagsTargetAddonType, apm.TagsTargetAddonId, apm.TagsTargetAddonGroup},
		Filter:       elastic.NewBoolQuery().Filter(elastic.NewExistsQuery(apm.TagsTargetAddonType)),
		Aggregation:  NodeAggregation,
	}
	SourceAddonNodeType = &NodeType{
		Type:         SourceAddonNode,
		GroupByField: &GroupByField{Name: apm.TagsSourceAddonType, SubField: &GroupByField{Name: apm.TagsSourceAddonId}},
		SourceFields: []string{apm.TagsSourceAddonType, apm.TagsSourceAddonId, apm.TagsSourceAddonGroup},
		Filter:       elastic.NewBoolQuery().Filter(elastic.NewExistsQuery(apm.TagsSourceAddonType)),
		Aggregation:  NodeAggregation,
	}
	TargetComponentNodeType = &NodeType{
		Type:         TargetComponentNode,
		GroupByField: &GroupByField{Name: apm.TagsDBHost, SubField: &GroupByField{Name: apm.TagsDBSystem}},
		SourceFields: []string{apm.TagsComponent, apm.TagsHost, apm.TagsTargetAddonGroup, apm.TagsDBSystem, apm.TagsDBHost},
		Filter:       elastic.NewBoolQuery().MustNot(elastic.NewExistsQuery(apm.TagsTargetAddonType)),
		Aggregation:  NodeAggregation,
	}
	TargetOtherNodeType = &NodeType{
		Type:         TargetOtherNode,
		GroupByField: &GroupByField{Name: apm.TagsHttpUrl, SubField: &GroupByField{Name: apm.TagsPeerServiceScope}},
		SourceFields: []string{apm.TagsPeerServiceScope, apm.TagsHttpUrl},
		Filter:       elastic.NewBoolQuery().MustNot(elastic.NewExistsQuery(apm.TagsTargetAddonType)),
		Aggregation:  NodeAggregation,
	}
	SourceMQNodeType = &NodeType{
		Type:         SourceMQNode,
		GroupByField: &GroupByField{Name: apm.TagsComponent, SubField: &GroupByField{Name: apm.TagsPeerAddress}},
		SourceFields: []string{apm.TagsComponent, apm.TagsHost, apm.TagsPeerAddress},
		Filter: elastic.NewBoolQuery().Filter(elastic.NewTermQuery("name", "application_mq_service")).
			MustNot(elastic.NewExistsQuery(apm.TagsTargetAddonType)),
		Aggregation: NodeAggregation,
	}
	TargetMQNodeType = &NodeType{
		Type:         TargetMQNode,
		GroupByField: &GroupByField{Name: apm.TagsComponent, SubField: &GroupByField{Name: apm.TagsPeerAddress}},
		SourceFields: []string{apm.TagsComponent, apm.TagsHost, apm.TagsPeerAddress},
		Filter: elastic.NewBoolQuery().Filter(elastic.NewTermQuery("name", "application_mq_service")).
			MustNot(elastic.NewExistsQuery(apm.TagsTargetAddonType)),
		Aggregation: NodeAggregation,
	}
	TargetMQServiceNodeType = &NodeType{
		Type:         TargetMQServiceNode,
		GroupByField: &GroupByField{Name: apm.TagsTargetServiceId, SubField: &GroupByField{Name: apm.TagsTargetServiceName}},
		SourceFields: []string{apm.TagsTargetApplicationId, apm.TagsTargetRuntimeName, apm.TagsTargetServiceName, apm.TagsTargetServiceId, apm.TagsTargetApplicationName, apm.TagsTargetRuntimeId},
		Filter:       elastic.NewBoolQuery().MustNot(elastic.NewExistsQuery(apm.TagsTargetAddonType)),
	}
	OtherNodeType = &NodeType{
		Type:         OtherNode,
		GroupByField: &GroupByField{Name: apm.TagsServiceId, SubField: &GroupByField{Name: apm.TagsServiceName}},
		SourceFields: []string{apm.TagsApplicationId, apm.TagsRuntimeName, apm.TagsServiceName, apm.TagsServiceId, apm.TagsApplicationName, apm.TagsRuntimeId},
		Filter:       elastic.NewBoolQuery().Must(elastic.NewExistsQuery(apm.TagsServiceId)),
	}

	NodeRelations = map[string][]*NodeRelation{
		HttpRecMircoIndexType: {
			// Topology Relation (Addon: ApiGateway...)
			// 1.SourceService -> TargetService 2.SourceAddon -> TargetService
			// SourceService -> TargetAddon
			// SourceService -> TargetOther
			{Source: []*NodeType{SourceServiceNodeType, SourceAddonNodeType}, Target: TargetServiceNodeType},
			{Source: []*NodeType{SourceServiceNodeType}, Target: TargetAddonNodeType},
			{Source: []*NodeType{SourceServiceNodeType}, Target: TargetOtherNodeType},
		},
		MQDBCacheIndexType: {
			// Topology Relation (Component: Mysql Redis MQ)
			// SourceMQService  -> TargetMQService (consumer)
			// SourceService  -> TargetMQ (producer)
			// SourceService    -> TargetComponent
			{Source: []*NodeType{SourceMQNodeType}, Target: TargetMQServiceNodeType},
			{Source: []*NodeType{SourceServiceNodeType}, Target: TargetMQNodeType},
			{Source: []*NodeType{SourceServiceNodeType}, Target: TargetComponentNodeType},
		},
		ServiceNodeIndexType: {
			// Topology Relation
			// OtherNode
			{Target: OtherNodeType},
		},
	}

	Aggregations = map[string]*AggregationCondition{
		HttpRecMircoIndexType: {Aggregation: toEsAggregation(NodeRelations[HttpRecMircoIndexType])},
		MQDBCacheIndexType:    {Aggregation: toEsAggregation(NodeRelations[MQDBCacheIndexType])},
		ServiceNodeIndexType:  {Aggregation: toEsAggregation(NodeRelations[ServiceNodeIndexType])},
	}
}

var NodeRelations map[string][]*NodeRelation
var Aggregations map[string]*AggregationCondition

const (
	HttpRecMircoIndexType = "http-rpc-mirco"
	MQDBCacheIndexType    = "mq-db-cache"
	ServiceNodeIndexType  = "service-node"
)

type RequestTransaction struct {
	RequestType      string  `json:"requestType"`
	RequestCount     float64 `json:"requestCount"`
	RequestAvgTime   float64 `json:"requestAvgTime"`
	RequestErrorRate float64 `json:"requestErrorRate"`
}

type AggregationCondition struct {
	Aggregation map[string]*elastic.FilterAggregation
}

func toEsAggregation(nodeRelations []*NodeRelation) map[string]*elastic.FilterAggregation {
	m := make(map[string]*elastic.FilterAggregation)
	for _, relation := range nodeRelations {

		nodeType := relation.Target
		key := encodeTypeToKey(nodeType.Type)
		if nodeType != nil {
			childAggregation := elastic.NewFilterAggregation()
			m[key] = childAggregation
			end := overlay(nodeType, childAggregation)

			sources := relation.Source
			if sources != nil {
				for _, source := range sources {
					sourceKey := encodeTypeToKey(source.Type)
					childAggregation := elastic.NewFilterAggregation()
					overlay(source, childAggregation)
					end.SubAggregation(sourceKey, childAggregation)
				}
			}
		}
	}
	return m
}

// encode
func encodeTypeToKey(nodeType string) string {
	md := sha256.New()
	md.Write([]byte(nodeType))
	mdSum := md.Sum(nil)
	key := hex.EncodeToString(mdSum)
	//fmt.Printf("type: %s, key: %s \n", nodeType, key)
	return key
}

func overlay(nodeType *NodeType, childAggregation *elastic.FilterAggregation) *elastic.TermsAggregation {

	// filter
	filter := nodeType.Filter
	if filter != nil {
		childAggregation.Filter(filter)
	}

	// groupBy
	field := nodeType.GroupByField
	start, end := toChildrenAggregation(nodeType.GroupByField, nil)
	childAggregation.SubAggregation(field.Name, start)

	// columns
	sourceFields := nodeType.SourceFields
	if sourceFields != nil {
		end.SubAggregation(apm.Columns,
			elastic.NewTopHitsAggregation().From(0).Size(1).Sort(apm.Timestamp, false).Explain(false).
				FetchSourceContext(elastic.NewFetchSourceContext(true).Include(sourceFields...)))
	}

	// agg
	aggs := nodeType.Aggregation
	if aggs != nil {
		for key, sumAggregation := range aggs {
			end.SubAggregation(key, sumAggregation)
		}
	}
	return end
}

func toChildrenAggregation(field *GroupByField, termEnd *elastic.TermsAggregation) (*elastic.TermsAggregation, *elastic.TermsAggregation) {
	if field == nil {
		log.Fatal("field can't nil")
	}
	termStart := elastic.NewTermsAggregation().Field(field.Name).Size(100)
	if field.SubField != nil {
		start, end := toChildrenAggregation(field.SubField, termEnd)
		termStart.SubAggregation(field.SubField.Name, start).Size(100)
		termEnd = end
	} else {
		termEnd = termStart
	}

	return termStart, termEnd
}

func queryConditions(indexType string, params Vo, tagInfo *TagInfo) *elastic.BoolQuery {
	boolQuery := elastic.NewBoolQuery()
	boolQuery.Filter(elastic.NewRangeQuery(apm.Timestamp).Gte(params.StartTime * 1e6).Lte(params.EndTime * 1e6))
	if ServiceNodeIndexType == indexType {
		boolQuery.Filter(elastic.NewTermQuery(apm.TagsTerminusKey, params.TerminusKey))
	} else {
		boolQuery.Filter(elastic.NewBoolQuery().Should(elastic.NewTermQuery(apm.TagsTargetTerminusKey, params.TerminusKey)).
			Should(elastic.NewTermQuery(apm.TagsSourceTerminusKey, params.TerminusKey)))
	}
	//filter: RegisterCenter ConfigCenter NoticeCenter
	not := elastic.NewBoolQuery().MustNot(elastic.NewTermQuery(apm.TagsComponent, "registerCenter")).
		MustNot(elastic.NewTermQuery(apm.TagsComponent, "configCenter")).
		MustNot(elastic.NewTermQuery(apm.TagsComponent, "noticeCenter")).
		MustNot(elastic.NewTermQuery(apm.TagsTargetAddonType, "registerCenter")).
		MustNot(elastic.NewTermQuery(apm.TagsTargetAddonType, "configCenter")).
		MustNot(elastic.NewTermQuery(apm.TagsTargetAddonType, "noticeCenter"))
	if tagInfo.ApplicationName != "" {
		sbq := elastic.NewBoolQuery()
		sbq.Should(elastic.NewTermQuery(apm.TagsApplicationName, tagInfo.ApplicationName)).
			Should(elastic.NewTermQuery(apm.TagsTargetApplicationName, tagInfo.ApplicationName)).
			Should(elastic.NewTermQuery(apm.TagsSourceApplicationName, tagInfo.ApplicationName))
		boolQuery.Filter(sbq)
	}
	boolQuery.Filter(not)
	return boolQuery
}

type ExceptionDescription struct {
	InstanceId    string `json:"instance_id"`
	ExceptionType string `json:"exception_type"`
	Class         string `json:"class"`
	Method        string `json:"method"`
	Message       string `json:"message"`
	Time          string `json:"time"`
	Count         int64  `json:"count"`
}

type ExceptionDescriptionsCountSort []ExceptionDescription

func (e ExceptionDescriptionsCountSort) Len() int {
	return len(e)
}

//Less() by count
func (e ExceptionDescriptionsCountSort) Less(i, j int) bool {
	return e[i].Count > e[j].Count
}

//Swap()
func (e ExceptionDescriptionsCountSort) Swap(i, j int) {
	e[i], e[j] = e[j], e[i]
}

type ExceptionDescriptionsTimeSort []ExceptionDescription

func (e ExceptionDescriptionsTimeSort) Len() int {
	return len(e)
}

//Less() by time
func (e ExceptionDescriptionsTimeSort) Less(i, j int) bool {
	iTime, err := time.Parse(TimeLayout, e[i].Time)
	jTime, err := time.Parse(TimeLayout, e[j].Time)
	if err != nil {
		return false
	}
	return iTime.UnixNano() > jTime.UnixNano()
}

//Swap()
func (e ExceptionDescriptionsTimeSort) Swap(i, j int) {
	e[i], e[j] = e[j], e[i]
}

const (
	ExceptionTimeSortStrategy  = "time"
	ExceptionCountSortStrategy = "count"
)

func (topology *provider) GetExceptionTypes(language i18n.LanguageCodes, params ServiceParams) ([]string, interface{}) {
	descriptions, err := topology.GetExceptionDescription(language, params, 50, "", "")
	if err != nil {
		return nil, err
	}

	typeMap := make(map[string]string)

	for _, description := range descriptions {
		if typeMap[description.ExceptionType] == "" {
			typeMap[description.ExceptionType] = description.ExceptionType
		}
	}
	types := make([]string, 0)
	for _, _type := range typeMap {
		types = append(types, _type)
	}

	return types, nil
}

func ExceptionOrderByTimeStrategy(exceptions ExceptionDescriptionsTimeSort) []ExceptionDescription {
	sort.Sort(exceptions)
	return exceptions
}

func ExceptionOrderByCountStrategy(exceptions ExceptionDescriptionsCountSort) []ExceptionDescription {
	sort.Sort(exceptions)
	return exceptions
}

func ExceptionOrderByStrategyExecute(exceptionType string, exceptions []ExceptionDescription) []ExceptionDescription {
	switch exceptionType {
	case ExceptionCountSortStrategy:
		return ExceptionOrderByCountStrategy(exceptions)
	case ExceptionTimeSortStrategy:
		return ExceptionOrderByTimeStrategy(exceptions)
	default:
		return ExceptionOrderByTimeStrategy(exceptions)
	}
}

type ReadWriteBytes struct {
	Timestamp  int64   `json:"timestamp"`  // unit: s
	ReadBytes  float64 `json:"readBytes"`  // unit: b
	WriteBytes float64 `json:"writeBytes"` // unit: b
}
type ReadWriteBytesSpeed struct {
	Timestamp       int64   `json:"timestamp"`       // format: yyyy-MM-dd HH:mm:ss
	ReadBytesSpeed  float64 `json:"readBytesSpeed"`  // unit: b/s
	WriteBytesSpeed float64 `json:"writeBytesSpeed"` // unit: b/s
}

func (topology *provider) GetProcessDiskIo(language i18n.LanguageCodes, params ServiceParams) (interface{}, error) {
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(params.EndTime, 10))
	statement := "SELECT parse_time(time(),'2006-01-02T15:04:05Z'),round_float(avg(blk_read_bytes::field), 2),round_float(avg(blk_write_bytes::field), 2) FROM docker_container_summary WHERE terminus_key=$terminus_key AND service_id=$service_id %s GROUP BY time()"
	queryParams := map[string]interface{}{
		"terminus_key": params.ScopeId,
		"service_id":   params.ServiceId,
	}
	if params.InstanceId != "" {
		statement = fmt.Sprintf(statement, "AND instance_id=$instance_id")
		queryParams["instance_id"] = params.InstanceId
	} else {
		statement = fmt.Sprintf(statement, "")
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows := response.ResultSet.Rows
	itemResultSpeed := handleSpeed(rows)
	return itemResultSpeed, nil
}

func (topology *provider) GetProcessNetIo(language i18n.LanguageCodes, params ServiceParams) (interface{}, error) {
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(params.EndTime, 10))
	statement := "SELECT parse_time(time(),'2006-01-02T15:04:05Z'),round_float(avg(rx_bytes::field), 2),round_float(avg(tx_bytes::field), 2) FROM docker_container_summary WHERE terminus_key=$terminus_key AND service_id=$service_id %s GROUP BY time()"
	queryParams := map[string]interface{}{
		"terminus_key": params.ScopeId,
		"service_id":   params.ServiceId,
	}
	if params.InstanceId != "" {
		statement = fmt.Sprintf(statement, "AND instance_id=$instance_id")
		queryParams["instance_id"] = params.InstanceId
	} else {
		statement = fmt.Sprintf(statement, "")
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows := response.ResultSet.Rows
	itemResultSpeed := handleSpeed(rows)

	return itemResultSpeed, nil
}

// handleSpeed The result is processed into ReadWriteBytesSpeed
func handleSpeed(rows [][]interface{}) []ReadWriteBytesSpeed {
	var itemResult []ReadWriteBytes
	for _, row := range rows {
		timeMs := row[1].(time.Time).UnixNano() / 1e6
		rxBytes := conv.ToFloat64(row[2], 0)
		txBytes := conv.ToFloat64(row[3], 0)
		writeBytes := ReadWriteBytes{
			Timestamp:  timeMs,
			ReadBytes:  rxBytes,
			WriteBytes: txBytes,
		}
		itemResult = append(itemResult, writeBytes)
	}
	var itemResultSpeed []ReadWriteBytesSpeed
	for i, curr := range itemResult {
		if i+1 >= len(itemResult) {
			break
		}
		next := itemResult[i+1]
		speed := ReadWriteBytesSpeed{}
		speed.Timestamp = (curr.Timestamp + next.Timestamp) / 2

		speed.ReadBytesSpeed = calculateSpeed(curr.ReadBytes, next.ReadBytes, curr.Timestamp, next.Timestamp)
		speed.WriteBytesSpeed = calculateSpeed(curr.WriteBytes, next.WriteBytes, curr.Timestamp, next.Timestamp)

		itemResultSpeed = append(itemResultSpeed, speed)
	}
	return itemResultSpeed
}

//calculateSpeed Calculate the speed through the two metric values before and after.
func calculateSpeed(curr, next float64, currTime, nextTime int64) float64 {
	if curr != next {
		if next == 0 || next < curr {
			return 0
		}
		if nextTime-currTime <= 0 { // by zero
			return 0
		}
		return toTwoDecimalPlaces((next - curr) / (float64(nextTime) - float64(currTime)))
	}
	return 0
}

func (topology *provider) GetExceptionMessage(language i18n.LanguageCodes, params ServiceParams, limit int64, sort, exceptionType string) ([]ExceptionDescription, error) {
	result := []ExceptionDescription{}
	descriptions, err := topology.GetExceptionDescription(language, params, limit, sort, exceptionType)
	if exceptionType != "" {
		for _, description := range descriptions {
			if description.ExceptionType == exceptionType {
				result = append(result, description)
			}
		}
	} else {
		result = descriptions
	}

	if err != nil {
		return nil, err
	}
	return result, nil
}

func (topology *provider) GetExceptionDescription(language i18n.LanguageCodes, params ServiceParams, limit int64, sort, exceptionType string) ([]ExceptionDescription, error) {
	if limit <= 0 || limit > 50 {
		limit = 10
	}

	if sort != ExceptionTimeSortStrategy && sort != ExceptionCountSortStrategy {
		sort = ExceptionTimeSortStrategy
	}

	if sort == ExceptionTimeSortStrategy {
		sort = "max(timestamp) DESC"
	}

	if sort == ExceptionCountSortStrategy {
		sort = "sum(count::field) DESC"
	}

	var filter bytes.Buffer
	if exceptionType != "" {
		filter.WriteString(" AND type::tag=$type")
	}
	sql := fmt.Sprintf("SELECT instance_id::tag,method::tag,class::tag,exception_message::tag,type::tag,max(timestamp),sum(count::field) FROM error_alert WHERE service_id::tag=$service_id AND terminus_key::tag=$terminus_key %s GROUP BY error_id::tag ORDER BY %s LIMIT %v", filter.String(), sort, limit)

	paramMap := map[string]interface{}{
		"service_id":   params.ServiceId,
		"type":         exceptionType,
		"terminus_key": params.ScopeId,
	}

	options := url.Values{}
	options.Set("start", strconv.FormatInt(params.StartTime, 10))
	options.Set("end", strconv.FormatInt(params.EndTime, 10))
	source, err := topology.metricq.Query(
		metricq.InfluxQL,
		sql,
		paramMap,
		options)
	if err != nil {
		return nil, err
	}

	var exceptionDescriptions []ExceptionDescription

	for _, detail := range source.ResultSet.Rows {
		var exceptionDescription ExceptionDescription
		exceptionDescription.InstanceId = conv.ToString(detail[0])
		exceptionDescription.Method = conv.ToString(detail[1])
		exceptionDescription.Class = conv.ToString(detail[2])
		exceptionDescription.Message = conv.ToString(detail[3])
		exceptionDescription.ExceptionType = conv.ToString(detail[4])
		exceptionDescription.Time = time.Unix(0, int64(conv.ToFloat64(detail[5], 0))).Format(TimeLayout)
		exceptionDescription.Count = int64(conv.ToFloat64(detail[6], 0))
		exceptionDescriptions = append(exceptionDescriptions, exceptionDescription)
	}

	return exceptionDescriptions, nil
}

type InstanceInfo struct {
	Id     string `json:"instanceId"`
	Ip     string `json:"ip"`
	Status bool   `json:"status"`
	HostIP string `json:"hostIp"`
}

func (topology *provider) GetServiceInstanceIds(language i18n.LanguageCodes, params ServiceParams) (interface{}, interface{}) {
	// instance list
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(params.EndTime, 10))

	statement := "SELECT service_instance_id::tag,service_ip::tag,if(gt(now()-timestamp,300000000000),'false','true'),host_ip::tag FROM application_service_node " +
		"WHERE terminus_key=$terminus_key AND service_id=$service_id GROUP BY service_instance_id::tag"
	queryParams := map[string]interface{}{
		"terminus_key": params.ScopeId,
		"service_id":   params.ServiceId,
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	instanceList := topology.handleInstanceInfo(response)

	// instance status
	metricsParams.Set("end", strconv.FormatInt(time.Now().UnixNano()/1e6, 10))
	response, err = topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	instanceListForStatus := topology.handleInstanceInfo(response)

	filterInstance(instanceList, instanceListForStatus)

	return instanceList, nil
}

func filterInstance(instanceList []*InstanceInfo, instanceListForStatus []*InstanceInfo) {
	for _, instance := range instanceList {
		for i, statusInstance := range instanceListForStatus {
			if instance.Id == statusInstance.Id {
				instance.Status = statusInstance.Status
				instanceListForStatus = append(instanceListForStatus[:i], instanceListForStatus[i+1:]...)
				i--
				break
			}
		}
	}
}

func (topology *provider) handleInstanceInfo(response *query.ResultSet) []*InstanceInfo {
	rows := response.ResultSet.Rows
	instanceIds := []*InstanceInfo{}
	for _, row := range rows {

		status, err := strconv.ParseBool(conv.ToString(row[2]))
		if err != nil {
			status = false
		}
		instance := InstanceInfo{
			Id:     conv.ToString(row[0]),
			Ip:     conv.ToString(row[1]),
			Status: status,
			HostIP: conv.ToString(row[3]),
		}
		instanceIds = append(instanceIds, &instance)
	}
	return instanceIds
}

func (topology *provider) GetServiceInstances(language i18n.LanguageCodes, params ServiceParams) (interface{}, interface{}) {
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(params.EndTime, 10))
	statement := "SELECT service_instance_id::tag,service_agent_platform::tag,format_time(start_time_mean::field*1000000,'2006-01-02 15:04:05') " +
		"AS start_time,format_time(timestamp,'2006-01-02 15:04:05') AS last_heartbeat_time FROM application_service_node " +
		"WHERE terminus_key=$terminus_key AND service_id=$service_id GROUP BY service_instance_id::tag"
	queryParams := map[string]interface{}{
		"terminus_key": params.ScopeId,
		"service_id":   params.ServiceId,
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows := response.ResultSet.Rows
	var result []*ServiceInstance
	for _, row := range rows {
		instance := ServiceInstance{
			ServiceInstanceId: conv.ToString(row[0]),
			PlatformVersion:   conv.ToString(row[1]),
			StartTime:         conv.ToString(row[2]),
			LastHeartbeatTime: conv.ToString(row[3]),
		}
		result = append(result, &instance)
	}
	metricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(time.Now().UnixNano()/1e6, 10))
	statement = "SELECT service_instance_id::tag,if(gt(now()-timestamp,300000000000),'false','true') AS state FROM application_service_node " +
		"WHERE terminus_key=$terminus_key AND service_id=$service_id GROUP BY service_instance_id::tag"
	response, err = topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows = response.ResultSet.Rows
	for _, instance := range result {
		for i, row := range rows {
			if conv.ToString(row[0]) == instance.ServiceInstanceId {
				state, err := strconv.ParseBool(conv.ToString(row[1]))
				if err != nil {
					return nil, err
				}
				if state {
					instance.InstanceState = topology.t.Text(language, "serviceInstanceStateRunning")
				} else {
					instance.InstanceState = topology.t.Text(language, "serviceInstanceStateStopped")
				}
				rows = append(rows[:i], rows[i+1:]...)
				break
			}
		}
	}
	return result, nil
}

func (topology *provider) GetServiceRequest(language i18n.LanguageCodes, params ServiceParams) (interface{}, error) {
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(params.EndTime, 10))
	var translations []RequestTransaction
	for _, metricName := range ReqMetricNames {

		translation, err := topology.serviceReqInfo(metricName, topology.t.Text(language, metricName+"_request"), params, metricsParams)
		if err != nil {
			return nil, err
		}
		translations = append(translations, *translation)
	}
	return translations, nil
}

func (topology *provider) GetServiceOverview(language i18n.LanguageCodes, params ServiceParams) (interface{}, error) {
	dashboardData := make([]map[string]interface{}, 0, 10)
	serviceOverviewMap := make(map[string]interface{})
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(params.EndTime, 10))

	instanceMetricsParams := url.Values{}
	instanceMetricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	instanceMetricsParams.Set("end", strconv.FormatInt(time.Now().UnixNano()/1e6, 10))

	statement := "SELECT service_name::tag,service_instance_id::tag,if(gt(now()-timestamp,300000000000),'stopping','running') FROM application_service_node " +
		"WHERE terminus_key=$terminus_key AND service_name=$service_name AND service_id=$service_id GROUP BY service_instance_id::tag"
	queryParams := map[string]interface{}{
		"terminus_key": params.ScopeId,
		"service_name": params.ServiceName,
		"service_id":   params.ServiceId,
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, instanceMetricsParams)
	if err != nil {
		return nil, err
	}
	rows := response.ResultSet.Rows
	var result []ServiceInstance
	for _, row := range rows {
		instance := ServiceInstance{
			ServiceName:         conv.ToString(row[0]),
			ServiceInstanceName: conv.ToString(row[1]),
			InstanceState:       conv.ToString(row[2]),
		}
		result = append(result, instance)
	}
	runningCount := 0
	stoppedCount := 0
	for _, instance := range result {
		if instance.InstanceState == "running" {
			runningCount += 1
		} else if instance.InstanceState == "stopping" {
			stoppedCount += 1
		}
	}
	serviceOverviewMap["running_instances"] = runningCount
	serviceOverviewMap["stopped_instances"] = stoppedCount

	// error req count
	errorCount := 0.0
	for _, metricName := range ReqMetricNames {
		count, err := topology.serviceReqErrorCount(metricName, params, metricsParams)
		if err != nil {
			return nil, err
		}
		errorCount += count
	}

	serviceOverviewMap["service_error_req_count"] = errorCount

	// exception count
	statement = "SELECT sum(count) FROM error_count WHERE terminus_key=$terminus_key AND service_name=$service_name AND service_id=$service_id"
	response, err = topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows = response.ResultSet.Rows
	expCount := rows[0][0]

	serviceOverviewMap["service_exception_count"] = expCount

	// alert count
	statement = "SELECT count(alert_id::tag) FROM analyzer_alert WHERE terminus_key=$terminus_key AND service_name=$service_name"
	queryParams = map[string]interface{}{
		"terminus_key": params.ScopeId,
		"service_name": params.ServiceName,
	}
	response, err = topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows = response.ResultSet.Rows
	alertCount := rows[0][0]

	serviceOverviewMap["alert_count"] = alertCount
	dashboardData = append(dashboardData, serviceOverviewMap)

	return dashboardData, nil
}

func (topology *provider) GetOverview(language i18n.LanguageCodes, params GlobalParams) (interface{}, error) {
	result := make(map[string]interface{})
	dashboardData := make([]map[string]interface{}, 0, 10)
	overviewMap := make(map[string]interface{})

	// service count
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(params.EndTime, 10))
	statement := "SELECT distinct(service_name::tag) FROM application_service_node WHERE terminus_key=$terminus_key GROUP BY service_id::tag"
	queryParams := map[string]interface{}{
		"terminus_key": params.ScopeId,
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows := response.ResultSet.Rows
	serviceCount := float64(0)
	for _, row := range rows {
		count := conv.ToFloat64(row[0], 0)
		serviceCount += count
	}
	overviewMap["service_count"] = serviceCount

	// running service instance count
	instanceMetricsParams := url.Values{}
	instanceMetricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	instanceMetricsParams.Set("end", strconv.FormatInt(time.Now().UnixNano()/1e6, 10))
	statement = "SELECT service_instance_id::tag,if(gt(now()-timestamp,300000000000),'stopping','running') FROM application_service_node WHERE terminus_key=$terminus_key GROUP BY service_instance_id::tag"
	queryParams = map[string]interface{}{
		"terminus_key": params.ScopeId,
	}
	response, err = topology.metricq.Query("influxql", statement, queryParams, instanceMetricsParams)
	if err != nil {
		return nil, err
	}
	rows = response.ResultSet.Rows
	serviceRunningInstanceCount := float64(0)
	for _, row := range rows {
		if row[1] == "running" {
			serviceRunningInstanceCount += 1
		}
	}
	overviewMap["service_running_instance_count"] = serviceRunningInstanceCount

	// error request count
	errorCount := 0.0
	for _, errorReqMetricName := range ReqMetricNames {
		count, err := topology.globalReqCount(errorReqMetricName, params, metricsParams)
		if err != nil {
			return nil, err
		}
		errorCount += count
	}
	overviewMap["service_error_req_count"] = errorCount

	// service exception count
	statement = "SELECT sum(count) FROM error_count WHERE terminus_key=$terminus_key"
	queryParams = map[string]interface{}{
		"terminus_key": params.ScopeId,
	}
	response, err = topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows = response.ResultSet.Rows
	expCount := rows[0][0]
	overviewMap["service_exception_count"] = expCount

	// alert count
	statement = "SELECT count(alert_id::tag) FROM analyzer_alert WHERE terminus_key=$terminus_key"
	queryParams = map[string]interface{}{
		"terminus_key": params.ScopeId,
	}
	response, err = topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows = response.ResultSet.Rows
	alertCount := rows[0][0]
	overviewMap["alert_count"] = alertCount
	dashboardData = append(dashboardData, overviewMap)

	result["data"] = dashboardData

	return result, nil
}

func (topology *provider) globalReqCount(metricScopeName string, params GlobalParams, metricsParams url.Values) (float64, error) {
	statement := fmt.Sprintf("SELECT sum(errors_sum::field) FROM %s WHERE target_terminus_key::tag=$terminus_key", metricScopeName)
	queryParams := map[string]interface{}{
		"metric":       metricScopeName,
		"terminus_key": params.ScopeId,
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return 0, err
	}
	rows := conv.ToFloat64(response.ResultSet.Rows[0][0], 0)
	return rows, nil
}

//toTwoDecimalPlaces Two decimal places
func toTwoDecimalPlaces(num float64) float64 {
	temp, err := strconv.ParseFloat(fmt.Sprintf("%.2f", num), 64)
	if err != nil {
		temp = 0
	}
	return temp
}

func (topology *provider) serviceReqInfo(metricScopeName, metricScopeNameDesc string, params ServiceParams, metricsParams url.Values) (*RequestTransaction, error) {
	var requestTransaction RequestTransaction
	metricType := "target_service_name"
	tkType := "target_terminus_key"
	serviceIdType := "target_service_id"
	if metricScopeName == ReqMetricNames[2] || metricScopeName == ReqMetricNames[3] || metricScopeName == ReqMetricNames[4] {
		metricType = "source_service_name"
		serviceIdType = "source_service_id"
		tkType = "source_terminus_key"
	}
	statement := fmt.Sprintf("SELECT sum(count_sum),sum(elapsed_sum)/sum(count_sum),sum(errors_sum)/sum(count_sum) FROM %s WHERE %s=$terminus_key AND %s=$service_name AND %s=$service_id",
		metricScopeName, tkType, metricType, serviceIdType)
	queryParams := map[string]interface{}{
		"terminus_key": params.ScopeId,
		"service_name": params.ServiceName,
		"service_id":   params.ServiceId,
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}

	row := response.ResultSet.Rows
	requestTransaction.RequestCount = conv.ToFloat64(row[0][0], 0)
	if row[0][1] != nil {
		requestTransaction.RequestAvgTime = toTwoDecimalPlaces(conv.ToFloat64(row[0][1], 0) / 1e6)
	} else {
		requestTransaction.RequestAvgTime = 0
	}
	if row[0][2] != nil {
		requestTransaction.RequestErrorRate = toTwoDecimalPlaces(conv.ToFloat64(row[0][2], 0) * 100)
	} else {
		requestTransaction.RequestErrorRate = 0
	}
	requestTransaction.RequestType = metricScopeNameDesc
	return &requestTransaction, nil
}

func (topology *provider) serviceReqErrorCount(metricScopeName string, params ServiceParams, metricsParams url.Values) (float64, error) {
	metricType := "target_service_name"
	tkType := "target_terminus_key"
	serviceIdType := "target_service_id"
	if metricScopeName == ReqMetricNames[2] || metricScopeName == ReqMetricNames[3] || metricScopeName == ReqMetricNames[4] {
		metricType = "source_service_name"
		serviceIdType = "source_service_id"
		tkType = "source_terminus_key"
	}
	statement := fmt.Sprintf("SELECT sum(errors_sum) FROM %s WHERE %s=$terminus_key AND %s=$service_name AND %s=$service_id",
		metricScopeName, tkType, metricType, serviceIdType)
	queryParams := map[string]interface{}{
		"terminus_key": params.ScopeId,
		"service_name": params.ServiceName,
		"service_id":   params.ServiceId,
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return 0, err
	}
	rows := conv.ToFloat64(response.ResultSet.Rows[0][0], 0)
	return rows, nil
}

func (topology *provider) GetSearchTags(r *http.Request) []SearchTag {
	lang := api.Language(r)
	label := topology.t.Text(lang, ApplicationSearchTag.Tag)
	if label != "" {
		ApplicationSearchTag.Label = label
	}
	return []SearchTag{
		ApplicationSearchTag,
	}
}

func searchApplicationTag(topology *provider, scopeId string, startTime, endTime int64) ([]string, error) {
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(startTime, 10))
	metricsParams.Set("end", strconv.FormatInt(endTime, 10))
	statement := "SELECT application_name::tag FROM application_service_node WHERE terminus_key=$terminus_key GROUP BY application_name::tag"
	queryParams := map[string]interface{}{
		"terminus_key": scopeId,
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows := response.ResultSet.Rows
	var itemResult []string
	for _, name := range rows {
		itemResult = append(itemResult, conv.ToString(name[0]))
	}
	return itemResult, nil
}

func (topology *provider) ComposeTopologyNode(r *http.Request, params Vo) ([]*Node, error) {
	lang := api.Language(r)

	nodes := topology.GetTopology(lang, params)

	// instance count info
	instances, err := topology.GetInstances(api.Language(r), params)
	if err != nil {
		return nil, err
	}

	for _, node := range nodes {
		key := node.ServiceId
		serviceInstances := instances[key]
		for _, instance := range serviceInstances {
			if instance.ServiceId == node.ServiceId {
				if instance.InstanceState == "running" {
					node.Metric.Running += 1
				} else {
					node.Metric.Stopped += 1
				}
			}
		}
	}
	return nodes, nil
}

func (topology *provider) Services(serviceName string, nodes []*Node) []ServiceDashboard {
	var serviceDashboards []ServiceDashboard
	for _, node := range nodes {
		if node.ServiceName == "" {
			continue
		}

		if serviceName != "" && !strings.Contains(node.ServiceName, serviceName) {
			continue
		}

		var serviceDashboard ServiceDashboard
		serviceDashboard.Name = node.ServiceName
		serviceDashboard.ReqCount = node.Metric.Count
		serviceDashboard.ReqErrorCount = node.Metric.HttpError
		serviceDashboard.ART = toTwoDecimalPlaces(node.Metric.RT)
		serviceDashboard.RSInstanceCount = fmt.Sprintf("%v", node.Metric.Running)
		serviceDashboard.RuntimeId = node.RuntimeId
		serviceDashboard.Id = node.ServiceId
		serviceDashboard.RuntimeName = node.RuntimeName
		serviceDashboard.ApplicationId = node.ApplicationId
		serviceDashboard.ApplicationName = node.ApplicationName
		serviceDashboards = append(serviceDashboards, serviceDashboard)
	}
	return serviceDashboards
}

type ServiceInstance struct {
	ApplicationName     string `json:"applicationName,omitempty"`
	ServiceId           string `json:"serviceId,omitempty"`
	ServiceName         string `json:"serviceName,omitempty"`
	ServiceInstanceName string `json:"serviceInstanceName,omitempty"`
	ServiceInstanceId   string `json:"serviceInstanceId,omitempty"`
	InstanceState       string `json:"instanceState,omitempty"`
	PlatformVersion     string `json:"platformVersion,omitempty"`
	StartTime           string `json:"startTime,omitempty"`
	LastHeartbeatTime   string `json:"lastHeartbeatTime,omitempty"`
}

func (topology *provider) GetInstances(language i18n.LanguageCodes, params Vo) (map[string][]ServiceInstance, error) {
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(params.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(time.Now().UnixNano()/1e6, 10))
	statement := "SELECT service_id::tag,service_instance_id::tag,if(gt(now()-timestamp,300000000000),'stopping','running') FROM application_service_node WHERE terminus_key=$terminus_key GROUP BY service_instance_id::tag"
	queryParams := map[string]interface{}{
		"terminus_key": params.TerminusKey,
	}
	response, err := topology.metricq.Query("influxql", statement, queryParams, metricsParams)
	if err != nil {
		return nil, err
	}
	rows := response.ResultSet.Rows
	var result []ServiceInstance
	for _, row := range rows {
		instance := ServiceInstance{
			ServiceId:           conv.ToString(row[0]),
			ServiceInstanceName: conv.ToString(row[1]),
			InstanceState:       conv.ToString(row[2]),
		}
		result = append(result, instance)
	}
	instanceResult := make(map[string][]ServiceInstance)
	for _, instance := range result {
		key := instance.ServiceId
		if instanceResult[key] == nil {
			var serviceInstance []ServiceInstance
			serviceInstance = append(serviceInstance, instance)
			instanceResult[key] = serviceInstance
		} else {
			serviceInstances := instanceResult[key]
			serviceInstances = append(serviceInstances, instance)
			instanceResult[key] = serviceInstances
		}
	}

	return instanceResult, nil
}

func (topology *provider) GetSearchTagv(r *http.Request, tag, scopeId string, startTime, endTime int64) ([]string, error) {
	switch tag {
	case ApplicationSearchTag.Tag:
		return searchApplicationTag(topology, scopeId, startTime, endTime)
	default:
		return nil, errors.New("search tag not support")
	}
}

type TagInfo struct {
	ApplicationId   string
	ApplicationName string
	ServiceId       string
	ServiceName     string
}

func (topology *provider) GetTopology(lang i18n.LanguageCodes, param Vo) []*Node {

	indices := createTypologyIndices(param.StartTime, param.EndTime)
	timeRange := (param.EndTime - param.StartTime) / 1e3 // second
	ctx := context.Background()

	nodes := make([]*Node, 0)
	tagInfo := parserTag(param)

	for key, typeIndices := range indices {

		aggregationConditions, relations := selectRelation(key)
		conditions := queryConditions(key, param, tagInfo)
		searchSource := elastic.NewSearchSource()
		searchSource.Query(conditions).Size(0)
		if aggregationConditions == nil {
			log.Fatal("aggregation conditions can't nil")
		}
		for key, aggregation := range aggregationConditions.Aggregation {
			searchSource.Aggregation(key, aggregation)
		}

		searchResult, err := topology.es.Search(typeIndices...).
			Header("content-type", "application/json").
			SearchSource(searchSource).
			Do(ctx)
		if err != nil {
			continue
		}
		//debug
		if param.Debug {
			source, _ := searchSource.Source()
			data, _ := json.Marshal(source)
			topology.Log.Infof("indices: %s \n request body: %s \n", typeIndices, string(data))
		}
		topology.parseToTypologyNode(lang, timeRange, searchResult, relations, &nodes)
	}

	if tagInfo.ServiceId != "" {
		return filterNodesByServiceId(tagInfo.ServiceId, nodes)
	}
	return nodes
}

func filterNodesByServiceId(serviceId string, nodes []*Node) []*Node {
	relatedServiceNode := make([]*Node, 0)
	currentServiceNode := getServiceNode(serviceId, nodes)
	serviceParentNodeIds := getNodeParentNodeIds(currentServiceNode)

	for _, node := range nodes {
		// filter service
		if node.ServiceId == serviceId {
			relatedServiceNode = append(relatedServiceNode, node)
			continue
		}
		// filter service upstream
		if _, ok := serviceParentNodeIds[node.Id]; ok {
			pNodes := make([]*Node, 0, 10)
			for _, parent := range node.Parents {
				if parent.ServiceId == serviceId {
					pNodes = append(pNodes, parent)
					break
				}
			}
			node.Parents = pNodes
			relatedServiceNode = append(relatedServiceNode, node)
			continue
		}
		// filter service downstream
		for _, parent := range node.Parents {
			if parent.ServiceId == serviceId {
				var pNodes []*Node
				pNodes = append(pNodes, parent)
				node.Parents = pNodes
				relatedServiceNode = append(relatedServiceNode, node)
				break
			}
		}
	}
	return relatedServiceNode
}

func getNodeParentNodeIds(node *Node) map[string]struct{} {
	ids := map[string]struct{}{}
	if node == nil {
		return ids
	}
	for _, parent := range node.Parents {
		ids[parent.Id] = struct{}{}
	}
	return ids
}

func getServiceNode(serviceId string, nodes []*Node) *Node {
	for _, node := range nodes {
		if node.ServiceId == serviceId {
			return node
		}
	}
	return nil
}

func parserTag(param Vo) *TagInfo {
	tagInfo := &TagInfo{}
	if param.Tags != nil && len(param.Tags) > 0 {
		for _, v := range param.Tags {
			tag := strings.Split(v, ":")
			key := tag[0]
			value := tag[1]
			switch key {
			case ApplicationSearchTag.Tag:
				tagInfo.ApplicationName = value
			case ServiceSearchTag.Tag:
				tagInfo.ServiceId = value
			}
		}
	}
	return tagInfo
}

func selectRelation(indexType string) (*AggregationCondition, []*NodeRelation) {
	var aggregationConditions *AggregationCondition
	var relations []*NodeRelation
	aggregationConditions = Aggregations[indexType]
	relations = NodeRelations[indexType]
	return aggregationConditions, relations
}

func (topology *provider) isExistTopologyNode(node *Node, topologyNodes *[]*Node) bool {
	for _, n := range *topologyNodes {
		if n.Id == node.Id {
			return true
		}
	}
	return false
}

func (topology *provider) parseToTypologyNode(lang i18n.LanguageCodes, timeRange int64, searchResult *elastic.SearchResult, relations []*NodeRelation, topologyNodes *[]*Node) {
	for _, nodeRelation := range relations {
		targetNodeType := nodeRelation.Target
		sourceNodeTypes := nodeRelation.Source

		key := encodeTypeToKey(targetNodeType.Type) // targetNodeType key
		aggregations := searchResult.Aggregations
		if aggregations != nil {
			filter, b := aggregations.Filter(key)
			if b {
				field := targetNodeType.GroupByField
				buckets := findDataBuckets(&filter.Aggregations, field)
				// handler
				for _, item := range *buckets {
					// node
					target := item.Aggregations

					// columns
					termsColumns, ok := target.TopHits(apm.Columns)
					if !ok {
						continue
					}
					if len(termsColumns.Hits.Hits) <= 0 && termsColumns.Hits.Hits[0].Source != nil {
						continue
					}
					targetNode := &TopologyNodeRelation{}
					err := json.Unmarshal(*termsColumns.Hits.Hits[0].Source, &targetNode)
					if err != nil {
						log.Println("parser error")
					}

					node := columnsParser(targetNodeType.Type, targetNode)
					node.TypeDisplay = topology.t.Text(lang, strings.ToLower(node.Type))
					if targetNodeType.Type == TargetOtherNode && node.Type == TypeInternal {
						continue
					}

					// aggs
					metric := metricParser(targetNodeType, target)

					node.Metric = metric

					// merge same node
					exist := false
					for _, n := range *topologyNodes {
						if n.Id == node.Id {
							n.Metric.Count += node.Metric.Count
							n.Metric.HttpError += node.Metric.HttpError
							n.Metric.Duration += node.Metric.Duration
							n.Metric.RT += node.Metric.RT
							if n.RuntimeId == "" {
								n.RuntimeId = node.RuntimeId
							}
							exist = true
							node = n
							break
						}
					}
					if !exist {
						*topologyNodes = append(*topologyNodes, node)
						node.Parents = []*Node{}
					}

					// sourceNodeTypes
					for _, nodeType := range sourceNodeTypes {
						key := encodeTypeToKey(nodeType.Type) // sourceNodeTypes key
						bucket, found := target.Filter(key)
						if !found {
							continue
						}
						a := bucket.Aggregations
						items := findDataBuckets(&a, nodeType.GroupByField)

						for _, keyItem := range *items {
							// node
							source := keyItem.Aggregations

							// columns
							sourceTermsColumns, ok := source.TopHits(apm.Columns)
							if !ok {
								continue
							}
							if len(sourceTermsColumns.Hits.Hits) <= 0 && sourceTermsColumns.Hits.Hits[0].Source != nil {
								continue
							}
							sourceNodeInfo := &TopologyNodeRelation{}
							err := json.Unmarshal(*sourceTermsColumns.Hits.Hits[0].Source, &sourceNodeInfo)
							if err != nil {
								log.Println("parser error")
							}

							sourceNode := columnsParser(nodeType.Type, sourceNodeInfo)
							if !topology.isExistTopologyNode(sourceNode, topologyNodes) {
								*topologyNodes = append(*topologyNodes, sourceNode)
								sourceNode.Parents = []*Node{}
							}
							// aggs
							sourceMetric := metricParser(nodeType, source)

							sourceNode.Metric = sourceMetric
							sourceNode.Parents = []*Node{}
							node.Parents = append(node.Parents, sourceNode)
						}
					}
				}
			}
		}
	}

	for _, node := range *topologyNodes {
		if node.Metric.Count != 0 { // by zero
			node.Metric.RT = pkgmath.DecimalPlacesWithDigitsNumber(node.Metric.Duration/float64(node.Metric.Count)/1e6, 2)
			node.Metric.ErrorRate = pkgmath.DecimalPlacesWithDigitsNumber(float64(node.Metric.HttpError)/float64(node.Metric.Count)*100, 2)
		}
		node.Metric.RPS = pkgmath.DecimalPlacesWithDigitsNumber(float64(node.Metric.Count)/float64(timeRange), 2)
	}
}

func metricParser(targetNodeType *NodeType, target elastic.Aggregations) *Metric {
	aggregation := targetNodeType.Aggregation
	metric := Metric{}

	inner := make(map[string]*float64)
	field := Field{}
	if aggregation == nil {
		return &metric
	}
	for key := range aggregation {
		sum, _ := target.Sum(key)
		split := strings.Split(key, ".")
		s2 := split[1]
		value := sum.Value
		inner[s2] = value
	}
	marshal, err := json.Marshal(inner)
	if err != nil {
		return &metric
	}
	err = json.Unmarshal(marshal, &field)
	if err != nil {
		return &metric
	}

	countSum := field.CountSum
	metric.Count = int64(countSum)
	metric.HttpError = int64(field.ErrorsSum)
	metric.Duration = field.ELapsedSum

	return &metric
}

func getDashboardId(nodeType string) string {

	switch strings.ToLower(nodeType) {
	case strings.ToLower(TypeService):
		return topologyNodeService
	case strings.ToLower(TypeGateway):
		return topologyNodeGateway
	case strings.ToLower(TypeMysql):
		return topologyNodeDb
	case strings.ToLower(TypeRedis):
		return topologyNodeCache
	case strings.ToLower(TypeRocketMQ):
		return topologyNodeMq
	case strings.ToLower(JavaProcessType):
		return processAnalysisJava
	case strings.ToLower(NodeJsProcessType):
		return processAnalysisNodejs
	case strings.ToLower(TypeExternal):
		return topologyNodeOther
	default:
		return ""
	}
}

func columnsParser(nodeType string, nodeRelation *TopologyNodeRelation) *Node {
	//	TypeService        = "Service"
	//	TypeMysql          = "Mysql"
	//	TypeRedis          = "Redis"
	//	TypeHttp           = "Http"
	//	TypeDubbo          = "Dubbo"
	//	TypeSidecar        = "SideCar"
	//	TypeGateway        = "APIGateway"
	//	TypeRegisterCenter = "RegisterCenter"
	//	TypeConfigCenter   = "ConfigCenter"
	//	TypeNoticeCenter   = "NoticeCenter"
	//	TypeElasticsearch  = "Elasticsearch"

	node := Node{}
	tags := nodeRelation.Tags

	switch nodeType {
	case TargetServiceNode:
		node.Type = TypeService
		node.ApplicationId = tags.TargetApplicationId
		node.ApplicationName = tags.TargetApplicationName
		node.ServiceName = tags.TargetServiceName
		node.ServiceId = tags.TargetServiceId
		node.Name = node.ServiceName
		node.RuntimeId = tags.TargetRuntimeId
		node.RuntimeName = tags.TargetRuntimeName
		node.Id = encodeTypeToKey(node.ServiceId + apm.Sep1 + node.ServiceName)
	case SourceServiceNode:
		node.Type = TypeService
		node.ApplicationId = tags.SourceApplicationId
		node.ApplicationName = tags.SourceApplicationName
		node.ServiceId = tags.SourceServiceId
		node.ServiceName = tags.SourceServiceName
		node.Name = node.ServiceName
		node.RuntimeId = tags.SourceRuntimeId
		node.RuntimeName = tags.SourceRuntimeName
		node.Id = encodeTypeToKey(node.ServiceId + apm.Sep1 + node.ServiceName)
	case TargetAddonNode:
		if strings.ToLower(tags.Component) == strings.ToLower("Http") {
			node.Type = TypeElasticsearch
		} else {
			node.Type = tags.TargetAddonType
		}
		node.AddonId = tags.TargetAddonID
		node.Name = tags.TargetAddonID
		node.AddonType = tags.TargetAddonType
		node.Id = encodeTypeToKey(node.AddonId + apm.Sep1 + node.AddonType)
	case SourceAddonNode:
		node.Type = tags.SourceAddonType
		node.AddonId = tags.SourceAddonID
		node.AddonType = tags.SourceAddonType
		node.Name = tags.SourceAddonID
		node.Id = encodeTypeToKey(node.AddonId + apm.Sep1 + node.AddonType)
	case TargetComponentNode:
		node.Type = tags.Component
		node.Name = tags.DBHost
		if tags.DBSystem != "" {
			node.Type = tags.DBSystem
		}
		node.Id = encodeTypeToKey(node.Type + apm.Sep1 + node.Name)
	case SourceMQNode:
		node.Type = tags.Component
		node.Name = tags.PeerAddress
		node.Id = encodeTypeToKey(node.Type + apm.Sep1 + node.Name)
	case TargetMQNode:
		node.Type = tags.Component
		node.Name = tags.PeerAddress
		node.Id = encodeTypeToKey(node.Type + apm.Sep1 + node.Name)
	case TargetMQServiceNode:
		node.Type = TypeService
		node.ApplicationId = tags.TargetApplicationId
		node.ApplicationName = tags.TargetApplicationName
		node.ServiceId = tags.TargetServiceId
		node.ServiceName = tags.TargetServiceName
		node.Name = node.ServiceName
		node.RuntimeId = tags.TargetRuntimeId
		node.RuntimeName = tags.TargetRuntimeName
		node.Id = encodeTypeToKey(node.ServiceId + apm.Sep1 + node.ServiceName)
	case TargetOtherNode:
		if strings.ToLower(tags.Component) == strings.ToLower("Http") && strings.HasPrefix(tags.Host, "terminus-elasticsearch") {
			node.Type = TypeElasticsearch
		} else {
			node.Type = tags.Component
		}
		if tags.PeerServiceScope == "external" {
			node.Type = TypeExternal
		}
		if tags.PeerServiceScope == "internal" {
			node.Type = TypeInternal
		}
		node.Name = tags.HttpUrl
		node.Id = encodeTypeToKey(node.Name + apm.Sep1 + node.Type)
	case OtherNode:
		node.Type = TypeService
		node.ApplicationId = tags.ApplicationId
		node.ApplicationName = tags.ApplicationName
		node.ServiceId = tags.ServiceId
		node.ServiceName = tags.ServiceName
		node.Name = node.ServiceName
		node.RuntimeId = tags.RuntimeId
		node.RuntimeName = tags.RuntimeName
		node.Id = encodeTypeToKey(node.ServiceId + apm.Sep1 + node.ServiceName)
	}
	node.DashboardId = getDashboardId(node.Type)

	return &node
}

func findDataBuckets(filter *elastic.Aggregations, field *GroupByField) *[]*elastic.AggregationBucketKeyItem {

	var nodeBuckets []*elastic.AggregationBucketKeyItem
	termAggs, _ := filter.Terms(field.Name)
	findNodeBuckets(termAggs.Buckets, field, &nodeBuckets)
	return &nodeBuckets
}

func findNodeBuckets(bucketKeyItems []*elastic.AggregationBucketKeyItem, field *GroupByField, nodeBuckets *[]*elastic.AggregationBucketKeyItem) {
	for _, buckets := range bucketKeyItems {
		if field != nil && field.SubField != nil {
			aggregations := buckets.Aggregations
			bucket, _ := aggregations.Terms(field.SubField.Name)
			findNodeBuckets(bucket.Buckets, field.SubField.SubField, nodeBuckets)
			continue
		}
		if field == nil {
			*nodeBuckets = append(*nodeBuckets, buckets)
		} else {
			bucketsAgg := buckets.Aggregations
			terms, _ := bucketsAgg.Terms(field.Name)
			*nodeBuckets = append(*nodeBuckets, terms.Buckets...)
		}
	}
}

// http/rpc
func (topology *provider) translation(r *http.Request, params translation) interface{} {
	if params.Layer != "http" && params.Layer != "rpc" {
		return api.Errors.Internal(errors.New("not supported layer name"))
	}
	options := url.Values{}
	options.Set("start", strconv.FormatInt(params.Start, 10))
	options.Set("end", strconv.FormatInt(params.End, 10))
	var where bytes.Buffer

	param := map[string]interface{}{
		"terminusKey":       params.TerminusKey,
		"filterServiceName": params.FilterServiceName,
		"serviceId":         params.ServiceId,
	}
	field, orderBy, err := handlerTranslationConditions(params, param, where)
	if err != nil {
		return api.Errors.Internal(err)
	}

	sql := fmt.Sprintf("SELECT %s,sum(elapsed_count::field),count(error::tag),format_duration(avg(elapsed_mean::field),'',2) "+
		"FROM application_%s WHERE target_service_id::tag=$serviceId AND target_service_name::tag=$filterServiceName "+
		"AND target_terminus_key::tag=$terminusKey %s GROUP BY %s", field, params.Layer, where.String(), field+orderBy)
	source, err := topology.metricq.Query(
		metricq.InfluxQL,
		sql,
		param,
		options)
	if err != nil {
		return api.Errors.Internal(err)
	}

	result := make(map[string]interface{}, 0)
	cols := []map[string]interface{}{
		{"flag": "tag|gropuby", "key": "translation_name", "_key": "tags.http_path"},
		{"flag": "field|func|agg", "key": "elapsed_count", "_key": ""},
		{"flag": "tag|func|agg", "key": "error_count", "_key": ""},
		{"flag": "tag|func|agg", "key": "slow_elapsed_count", "_key": ""},
		{"flag": "tag|func|agg", "key": "avg_elapsed", "_key": ""},
	}
	result["cols"] = cols
	data := make([]map[string]interface{}, 0)
	for _, r := range source.ResultSet.Rows {
		itemResult := make(map[string]interface{})
		itemResult["translation_name"] = r[0]
		itemResult["elapsed_count"] = r[1]
		itemResult["error_count"] = r[2]
		itemResult["avg_elapsed"] = r[3]
		sql = fmt.Sprintf("SELECT sum(elapsed_count::field) FROM application_%s_slow WHERE target_service_id::tag=$serviceId "+
			"AND target_service_name::tag=$filterServiceName AND %s=$field AND target_terminus_key::tag=$terminusKey ", params.Layer, field)
		slowElapsedCount, err := topology.metricq.Query(
			metricq.InfluxQL,
			sql,
			map[string]interface{}{
				"field":             conv.ToString(r[0]),
				"terminusKey":       params.TerminusKey,
				"filterServiceName": params.FilterServiceName,
				"serviceId":         params.ServiceId,
			},
			options)
		if err != nil {
			return api.Errors.Internal(err)
		}
		for _, item := range slowElapsedCount.ResultSet.Rows {
			itemResult["slow_elapsed_count"] = item[0]
		}
		data = append(data, itemResult)
	}
	result["data"] = data
	return api.Success(result)
}

func handlerTranslationConditions(params translation, param map[string]interface{}, where bytes.Buffer) (string, string, error) {
	var orderBy string
	var field string
	switch params.Layer {
	case "http":
		field = "http_path::tag"
		if params.Search != "" {
			param["field"] = map[string]interface{}{"regex": ".*" + params.Search + ".*"}
			where.WriteString(" AND http_path::tag=~$field")
		}
	case "rpc":
		field = "rpc_target::tag"
		if params.Search != "" {
			param["field"] = map[string]interface{}{
				"regex": ".*" + params.Search + ".*",
			}
			where.WriteString(" AND rpc_target::tag=~$field")
		}
	default:
		return "", "", errors.New("not support layer name")
	}

	if params.Sort == 0 {
		orderBy = " ORDER BY avg(elapsed_mean::field) DESC"
	}
	if params.Sort == 1 {
		orderBy = " ORDER BY sum(elapsed_count::field) DESC"
	}
	if params.Sort == 2 {
		orderBy = " ORDER BY count(error::tag) DESC"
	}
	return field, orderBy, nil
}

// db/cache
func (topology *provider) middlewareTransaction(r *http.Request, params translation) interface{} {
	if params.Layer != "db" && params.Layer != "cache" && params.Layer != "mq" {
		return api.Errors.Internal(errors.New("not supported layer name"))
	}
	lang := api.Language(r)
	result, err := topology.middlewareStrategy(lang, params)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(result)
}

func (topology *provider) middlewareStrategy(lang i18n.LanguageCodes, params translation) (map[string]interface{}, error) {
	switch params.Layer {
	case "db":
		return topology.dbOrCacheTranslation(lang, params)
	case "cache":
		return topology.dbOrCacheTranslation(lang, params)
	case "mq":
		return topology.mqTranslation(lang, params)
	}
	return nil, errors.New("no support middleware type")
}

func (topology *provider) dbOrCacheTranslation(lang i18n.LanguageCodes, params translation) (map[string]interface{}, error) {
	options := url.Values{}
	options.Set("start", strconv.FormatInt(params.Start, 10))
	options.Set("end", strconv.FormatInt(params.End, 10))
	var where bytes.Buffer
	var orderby string
	param := make(map[string]interface{})
	param["terminusKey"] = params.TerminusKey
	param["serviceId"] = params.ServiceId
	if params.Search != "" {
		where.WriteString(" AND db_statement::tag=~$field")
		param["field"] = map[string]interface{}{"regex": ".*" + params.Search + ".*"}
	}

	// elapsed_mean desc
	if params.Sort == 0 {
		orderby = " ORDER BY avg(elapsed_mean::field) DESC"
	}
	// error elapsed_count desc
	if params.Sort == 1 {
		orderby = " ORDER BY sum(elapsed_count::field) DESC"
	}

	sql := fmt.Sprintf("SELECT db_statement::tag,db_system::tag,db_name::tag,db_host::tag,sum(elapsed_count::field),"+
		"format_duration(avg(elapsed_mean::field),'',2) FROM application_%s WHERE source_service_id::tag=$serviceId AND "+
		"source_terminus_key::tag=$terminusKey %s GROUP BY db_statement::tag %s",
		params.Layer, where.String(), orderby)
	source, err := topology.metricq.Query(
		metricq.InfluxQL,
		sql,
		param,
		options)
	if err != nil {
		return nil, err
	}

	result := make(map[string]interface{}, 0)
	cols := []map[string]interface{}{
		{"_key": "tags.db_statement", "flag": "tag|groupby", "key": "operation"},
		{"_key": "tags.db_system", "flag": "tag", "key": "db_system"},
		{"_key": "tags.db_name", "flag": "tag", "key": "db_name"},
		{"_key": "tags.db_host", "flag": "tag", "key": "db_host"},
		{"_key": "", "flag": "field|func|agg", "key": "call_count"},
		{"_key": "", "flag": "field|func|agg", "key": "avg_elapsed"},
		{"_key": "", "flag": "field|func|agg", "key": "slow_elapsed_count"},
	}
	result["cols"] = cols
	data := make([]map[string]interface{}, 0)
	for _, r := range source.ResultSet.Rows {
		itemResult := make(map[string]interface{})
		itemResult["operation"] = r[0]
		itemResult["db_system"] = r[1]
		itemResult["db_name"] = r[2]
		itemResult["db_host"] = r[3]
		itemResult["call_count"] = r[4]
		itemResult["avg_elapsed"] = r[5]
		sql := fmt.Sprintf("SELECT sum(elapsed_count::field) FROM application_%s_slow WHERE source_service_id::tag=$serviceId "+
			"AND db_statement::tag=$field AND source_terminus_key::tag=$terminusKey", params.Layer)
		slowElapsedCount, err := topology.metricq.Query(
			metricq.InfluxQL,
			sql,
			map[string]interface{}{
				"field":       conv.ToString(r[0]),
				"terminusKey": params.TerminusKey,
				"serviceId":   params.ServiceId,
			},
			options)
		if err != nil {
			return nil, err
		}
		for _, item := range slowElapsedCount.ResultSet.Rows {
			itemResult["slow_elapsed_count"] = item[0]
		}
		data = append(data, itemResult)
	}
	result["data"] = data
	return result, nil
}

func (topology *provider) mqTranslation(lang i18n.LanguageCodes, params translation) (map[string]interface{}, error) {
	options, param, sql := topology.composeMqTranslationCondition(params)

	response, err := topology.metricq.Query(metricq.InfluxQL, sql, param, options)
	result := make(map[string]interface{}, 0)
	data, err := topology.handleMQTranslationResponse(lang, params, response, options)
	if err != nil {
		return nil, err
	}
	result["data"] = data
	return result, nil
}

func (topology *provider) composeMqTranslationCondition(params translation) (url.Values, map[string]interface{}, string) {
	options := url.Values{}
	options.Set("start", strconv.FormatInt(params.Start, 10))
	options.Set("end", strconv.FormatInt(params.End, 10))
	var where bytes.Buffer
	var orderby string
	param := make(map[string]interface{})
	param["terminusKey"] = params.TerminusKey
	param["serviceId"] = params.ServiceId
	if params.Search != "" {
		where.WriteString(fmt.Sprintf(" message_bus_destination::tag=~/.*%s.*/ AND ", params.Search))
	}

	// elapsed_mean desc
	if params.Sort == 0 {
		orderby = " ORDER BY avg(elapsed_mean::field) DESC"
	}
	// error elapsed_count desc
	if params.Sort == 1 {
		orderby = " ORDER BY sum(elapsed_count::field) DESC"
	}

	producerCondition := "source_service_id::tag=$serviceId AND span_kind::tag='producer' AND source_terminus_key::tag=$terminusKey"
	consumerCondition := "target_service_id::tag=$serviceId AND span_kind::tag='consumer' AND target_terminus_key::tag=$terminusKey"
	if params.Type == "producer" {
		where.WriteString(producerCondition)
	} else if params.Type == "consumer" {
		where.WriteString(consumerCondition)
	} else {
		where.WriteString(fmt.Sprintf("((%s) OR (%s))", producerCondition, consumerCondition))
	}

	sql := fmt.Sprintf("SELECT message_bus_destination::tag,span_kind::tag,component::tag,host::tag,sum(elapsed_count::field),"+
		"format_duration(avg(elapsed_mean::field),'',2) FROM application_%s WHERE %s GROUP BY message_bus_destination::tag,span_kind::tag %s", params.Layer, where.String(), orderby)
	return options, param, sql
}

func (topology *provider) handleMQTranslationResponse(lang i18n.LanguageCodes, params translation, result *query.ResultSet, options url.Values) ([]map[string]interface{}, error) {
	data := make([]map[string]interface{}, 0)
	if result.ResultSet == nil {
		return []map[string]interface{}{}, nil
	}
	for _, r := range result.ResultSet.Rows {
		sqlProducer := fmt.Sprintf("SELECT sum(elapsed_count::field) FROM application_%s_slow WHERE source_service_id::tag=$serviceId "+
			"AND message_bus_destination::tag=$field AND span_kind::tag='producer' AND source_terminus_key::tag=$terminusKey", params.Layer)
		sqlConsumer := fmt.Sprintf("SELECT sum(elapsed_count::field) FROM application_%s_slow WHERE target_service_id::tag=$serviceId "+
			"AND message_bus_destination::tag=$field AND span_kind::tag='consumer' AND target_terminus_key::tag=$terminusKey", params.Layer)

		paramsM := map[string]interface{}{
			"field":       conv.ToString(r[0]),
			"terminusKey": params.TerminusKey,
			"serviceId":   params.ServiceId,
		}
		slowCount := 0
		slowElapsedCountProducer, err := topology.metricq.Query(metricq.InfluxQL, sqlProducer, paramsM, options)
		slowElapsedCountConsumer, err := topology.metricq.Query(metricq.InfluxQL, sqlConsumer, paramsM, options)
		if err != nil {
			return nil, err
		}
		cCount := slowElapsedCountProducer.ResultSet.Rows[0][0].(float64)
		pCount := slowElapsedCountConsumer.ResultSet.Rows[0][0].(float64)
		slowCount = int(cCount + pCount)
		itemResult := topology.handleResult(lang, r, slowCount)
		data = append(data, itemResult)
	}
	return data, nil
}

func (topology *provider) handleResult(lang i18n.LanguageCodes, r []interface{}, slowCount int) map[string]interface{} {
	itemResult := make(map[string]interface{})
	itemResult["operation"] = r[0]
	if lang == nil {
		itemResult["type"] = r[1]
	} else {
		itemResult["type"] = topology.t.Text(lang, r[1].(string))
	}
	itemResult["component"] = r[2]
	itemResult["host"] = r[3]
	itemResult["call_count"] = r[4]
	itemResult["avg_elapsed"] = r[5]
	itemResult["slow_elapsed_count"] = slowCount
	return itemResult
}

func (topology *provider) slowTranslationTrace(r *http.Request, params struct {
	Start       int64  `query:"start" validate:"required"`
	End         int64  `query:"end" validate:"required"`
	ServiceName string `query:"serviceName" validate:"required"`
	TerminusKey string `query:"terminusKey" validate:"required"`
	Operation   string `query:"operation" validate:"required"`
	ServiceId   string `query:"serviceId" validate:"required"`
	Limit       int64  `query:"limit" default:"100"`
	Sort        string `default:"duration:DESC" query:"sort"`
}) interface{} {
	sortCondition := ""
	if params.Sort == "timestamp:DESC" {
		sortCondition = "timestamp DESC"
	} else if params.Sort == "timestamp:ASC" {
		sortCondition = "timestamp ASC"
	} else if params.Sort == "duration:DESC" {
		sortCondition = "trace_duration DESC"
	} else if params.Sort == "duration:ASC" {
		sortCondition = "trace_duration ASC"
	} else {
		sortCondition = "trace_duration DESC"
	}
	if params.Limit < 100 {
		params.Limit = 100
	}
	if params.Limit > 1000 {
		params.Limit = 1000
	}
	lang := api.Language(r)
	options := url.Values{}
	options.Set("start", strconv.FormatInt(params.Start, 10))
	options.Set("end", strconv.FormatInt(params.End, 10))
	sql := fmt.Sprintf("SELECT trace_id::tag,format_time(timestamp,'2006-01-02 15:04:05'),format_duration(trace_duration::field,'',2) "+
		"FROM trace WHERE service_ids::field=$serviceId AND service_names::field=$serviceName AND terminus_keys::field=$terminusKey "+
		"AND (http_paths::field=$operation OR rpc_methods::field=$operation OR db_statements::field=$operation OR topics::field=$operation) ORDER BY %s Limit %v", sortCondition, params.Limit)
	details, err := topology.metricq.Query(metricq.InfluxQL, sql,
		map[string]interface{}{
			"serviceName": params.ServiceName,
			"terminusKey": params.TerminusKey,
			"operation":   params.Operation,
			"serviceId":   params.ServiceId,
		}, options)
	if err != nil {
		return api.Errors.Internal(err)
	}
	var data []map[string]interface{}
	for _, detail := range details.ResultSet.Rows {
		detailMap := make(map[string]interface{})
		detailMap["requestId"] = detail[0]
		detailMap["time"] = detail[1]
		detailMap["avgElapsed"] = detail[2]
		data = append(data, detailMap)
	}
	result := handleSlowTranslationTraceResult(topology, lang, data)
	return api.Success(result)
}

func handleSlowTranslationTraceResult(topology *provider, lang i18n.LanguageCodes, data []map[string]interface{}) map[string]interface{} {
	result := map[string]interface{}{
		"cols": []map[string]interface{}{
			{"title": topology.t.Text(lang, "request_id"), "index": "requestId"},
			{"title": topology.t.Text(lang, "time"), "index": "time"},
			{"title": topology.t.Text(lang, "avg_elapsed"), "index": "avgElapsed"},
		},
		"data": data,
	}
	return result
}
