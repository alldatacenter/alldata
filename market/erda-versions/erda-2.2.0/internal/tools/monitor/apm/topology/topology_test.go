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
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"reflect"
	"regexp"
	"strconv"
	"testing"

	"bou.ke/monkey"
	"github.com/gofrs/uuid"
	"github.com/olivere/elastic"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/tools/monitor/common/db"
	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func TestSpliceIndexByTime(t *testing.T) {
	startTimeMs := 1603148400544 // 2020-10-20 07:00:00
	endTimeMs := 1603753200543   // 2020-10-27 07:00:00

	indices := createTypologyIndices(int64(startTimeMs), int64(endTimeMs))
	for _, index := range indices {
		fmt.Println(index)
	}
}

func TestJsonStrToStruct(t *testing.T) {
	str := `{"name":"application_micro_service","timestamp":1603671150000000000,"tags":{"_meta":"true","_metric_scope":"micro_service","_metric_scope_id":"z341b9c025b914180877ad7dbb9d80d9f","cluster_name":"terminus-dev","host":"node-010000006205","host_ip":"10.0.6.205","org_name":"terminus","source_application_id":"4","source_application_name":"apm-demo","source_org_id":"1","source_project_id":"1","source_project_name":"test","source_runtime_id":"48","source_runtime_name":"feature/simple_with_nacos","source_service_id":"4_feature/simple_with_nacos_apm-demo-dubbo","source_service_instance_id":"fae63126-78f8-4ddd-9756-c9d363211e5f","source_service_name":"apm-demo-dubbo","source_terminus_key":"z341b9c025b914180877ad7dbb9d80d9f","source_workspace":"DEV","target_addon_id":"registerCenter","target_addon_type":"registerCenter"},"fields":{"elapsed_count":1,"elapsed_max":13945,"elapsed_mean":13945,"elapsed_min":13945,"elapsed_sum":13945},"@timestamp":1603671150000}`
	tnr := TopologyNodeRelation{}
	err := json.Unmarshal([]byte(str), &tnr)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println(tnr)
}

func TestRegexp(t *testing.T) {
	re := regexp.MustCompile("^" + ServiceNodeIndex + "(.*)$")
	matchString := re.MatchString("spot-service_node-*-1603065600000")
	if !matchString {
		log.Fatal("not match")
	}
}

func TestCreateAggregation(t *testing.T) {

	NodeRelations := map[string][]*NodeRelation{}

	NodeRelations["mq-db-cache"] = []*NodeRelation{
		// Topology Relation (Component: Mysql Redis MQ)
		// SourceMQService  -> TargetMQService
		// SourceService    -> TargetComponent
		{Source: []*NodeType{SourceMQNodeType}, Target: TargetMQServiceNodeType},
		{Source: []*NodeType{SourceServiceNodeType}, Target: TargetComponentNodeType},
	}

	//aggregation := elastic.NewFilterAggregation()
	//allQuery := elastic.NewMatchAllQuery()
	aggregation := elastic.NewFilterAggregation().Filter(elastic.NewMatchAllQuery()) // 1级索引过滤
	for _, relation := range NodeRelations["http-rpc-mirco"] {
		// target
		if relation.Target != nil {
			uuid, _ := uuid.NewV4()
			childAggregation := elastic.NewFilterAggregation()
			aggregation.SubAggregation(uuid.String(), childAggregation)
			if relation.Target.Filter != nil {
				not := elastic.NewBoolQuery().MustNot(relation.Target.Filter)
				childAggregation.Filter(not)
			}
		}
	}

	source, _ := aggregation.Source()
	marshal, _ := json.Marshal(source)
	fmt.Println(string(marshal))

}

func TestToEsAggregation(t *testing.T) {
	NodeRelations := map[string][]*NodeRelation{}

	NodeRelations["mq-db-cache"] = []*NodeRelation{
		// Topology Relation (Component: Mysql Redis MQ)
		// SourceMQService  -> TargetMQService
		// SourceService    -> TargetComponent
		//{Source: []*NodeType{SourceMQNodeType}, Target: TargetMQServiceNodeType},
		{Source: []*NodeType{SourceServiceNodeType}, Target: TargetComponentNodeType},
	}
}

func TestFloat64ToString(t *testing.T) {
	f := 12.33
	float := strconv.FormatFloat(f, 'f', 2, 64)
	fmt.Println(float)
}

func Test_filterInstance(t *testing.T) {
	type args struct {
		instanceList          []*InstanceInfo
		instanceListForStatus []*InstanceInfo
	}
	var instanceListCase []*InstanceInfo
	var instanceListForStatusCase []*InstanceInfo
	for i := 0; i < 100; i++ {
		info := InstanceInfo{
			Id:     fmt.Sprintf("instance-%d", i),
			Ip:     "127.0.0.1",
			Status: false,
		}
		instanceListCase = append(instanceListCase, &info)
		infoForStatus := InstanceInfo{
			Id:     fmt.Sprintf("instance-%d", i),
			Ip:     "127.0.0.1",
			Status: false,
		}
		if i%2 == 0 {
			infoForStatus.Status = true
		}
		instanceListForStatusCase = append(instanceListForStatusCase, &infoForStatus)
	}
	tests := []struct {
		name string
		args args
	}{
		{name: "case1", args: args{instanceList: instanceListCase, instanceListForStatus: nil}},
		{name: "case2", args: args{instanceList: nil, instanceListForStatus: instanceListForStatusCase}},
		{name: "case3", args: args{instanceList: instanceListCase, instanceListForStatus: instanceListForStatusCase}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			filterInstance(tt.args.instanceList, tt.args.instanceListForStatus)
			count := 0
			for _, info := range tt.args.instanceList {
				if info.Status == true {
					count++
				}
			}
			if tt.name == "case1" {
				assert.Equal(t, 0, count)
			}
			if tt.name == "case2" {
				assert.Equal(t, 0, count)
			}
			if tt.name == "case3" {
				assert.Equal(t, 50, count)
			}
		})
	}
}

func Test_getDashboardId(t *testing.T) {

	type args struct {
		nodeType string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{name: TypeService, args: args{TypeService}, want: "topology_node_service"},
		{name: TypeGateway, args: args{TypeGateway}, want: "topology_node_gateway"},
		{name: TypeMysql, args: args{TypeMysql}, want: "topology_node_db"},
		{name: TypeRedis, args: args{TypeRedis}, want: "topology_node_cache"},
		{name: TypeRocketMQ, args: args{TypeRocketMQ}, want: "topology_node_mq"},
		{name: TypeExternal, args: args{TypeExternal}, want: "topology_node_other"},
		{name: JavaProcessType, args: args{JavaProcessType}, want: "process_analysis_java"},
		{name: NodeJsProcessType, args: args{NodeJsProcessType}, want: "process_analysis_nodejs"},
		{name: "not", args: args{"not"}, want: ""},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getDashboardId(tt.args.nodeType); got != tt.want {
				t.Errorf("getDashboardId() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_provider_handleResult(t *testing.T) {
	itemResult := make(map[string]interface{})
	itemResult["operation"] = "test-topic"
	itemResult["type"] = "consumer"
	itemResult["component"] = "mq"
	itemResult["host"] = "xxx:8080"
	itemResult["call_count"] = 10
	itemResult["avg_elapsed"] = 1000
	itemResult["slow_elapsed_count"] = 2
	type fields struct {
		Cfg     *config
		Log     logs.Logger
		db      *db.DB
		es      *elastic.Client
		ctx     servicehub.Context
		metricq metricq.Queryer
		t       i18n.Translator
	}
	type args struct {
		r         []interface{}
		slowCount int
		lang      i18n.LanguageCodes
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   map[string]interface{}
	}{
		{"case", fields{
			Cfg:     nil,
			Log:     nil,
			db:      nil,
			es:      nil,
			ctx:     nil,
			metricq: nil,
			t:       nil,
		}, args{
			r: []interface{}{
				"test-topic",
				"consumer",
				"mq",
				"xxx:8080",
				10,
				1000,
			},
			slowCount: 2,
		}, itemResult,
		}}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			topology := &provider{
				Cfg:     tt.fields.Cfg,
				Log:     tt.fields.Log,
				db:      tt.fields.db,
				es:      tt.fields.es,
				ctx:     tt.fields.ctx,
				metricq: tt.fields.metricq,
				t:       tt.fields.t,
			}
			if got := topology.handleResult(nil, tt.args.r, tt.args.slowCount); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("handleResult() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_provider_mqTranslation(t *testing.T) {
	type fields struct {
		Cfg     *config
		Log     logs.Logger
		db      *db.DB
		es      *elastic.Client
		ctx     servicehub.Context
		metricq metricq.Queryer
		t       i18n.Translator
	}
	type args struct {
		lang   i18n.LanguageCodes
		params translation
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    map[string]interface{}
		wantErr bool
	}{}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			topology := &provider{
				Cfg:     tt.fields.Cfg,
				Log:     tt.fields.Log,
				db:      tt.fields.db,
				es:      tt.fields.es,
				ctx:     tt.fields.ctx,
				metricq: tt.fields.metricq,
				t:       tt.fields.t,
			}
			got, err := topology.mqTranslation(tt.args.lang, tt.args.params)
			if (err != nil) != tt.wantErr {
				t.Errorf("mqTranslation() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("mqTranslation() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_provider_composeMqTranslationCondition(t *testing.T) {
	type fields struct {
		Cfg     *config
		Log     logs.Logger
		db      *db.DB
		es      *elastic.Client
		ctx     servicehub.Context
		metricq metricq.Queryer
		t       i18n.Translator
	}
	type args struct {
		params translation
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want2  string
	}{
		{"case1", fields{Cfg: nil, Log: nil, db: nil, es: nil, ctx: nil, metricq: nil, t: nil}, args{params: translation{
			Start:             1630971092821,
			End:               1630981892821,
			Limit:             0,
			Search:            "topic",
			Layer:             "mq",
			FilterServiceName: "apm-demo-api-test",
			TerminusKey:       "58ee69adccbb4a42a638b2de6b8eac7c",
			Sort:              0,
			ServiceId:         "15_feature/simple_apm-demo-api-test",
			Type:              "",
		}}, "SELECT message_bus_destination::tag,span_kind::tag,component::tag,host::tag,sum(elapsed_count::field),format_duration(avg(elapsed_mean::field),'',2) " +
			"FROM application_mq WHERE  message_bus_destination::tag=~/.*topic.*/ AND ((source_service_id::tag=$serviceId AND span_kind::tag='producer' " +
			"AND source_terminus_key::tag=$terminusKey) OR (target_service_id::tag=$serviceId AND span_kind::tag='consumer' " +
			"AND target_terminus_key::tag=$terminusKey)) GROUP BY message_bus_destination::tag,span_kind::tag  ORDER BY avg(elapsed_mean::field) DESC"},
		{"case2", fields{Cfg: nil, Log: nil, db: nil, es: nil, ctx: nil, metricq: nil, t: nil}, args{params: translation{
			Start:             1630971092821,
			End:               1630981892821,
			Limit:             0,
			Search:            "topic",
			Layer:             "mq",
			FilterServiceName: "apm-demo-api-test",
			TerminusKey:       "58ee69adccbb4a42a638b2de6b8eac7c",
			Sort:              0,
			ServiceId:         "15_feature/simple_apm-demo-api-test",
			Type:              "producer",
		}}, "SELECT message_bus_destination::tag,span_kind::tag,component::tag,host::tag,sum(elapsed_count::field),format_duration(avg(elapsed_mean::field),'',2) " +
			"FROM application_mq WHERE  message_bus_destination::tag=~/.*topic.*/ AND source_service_id::tag=$serviceId AND span_kind::tag='producer' " +
			"AND source_terminus_key::tag=$terminusKey GROUP BY message_bus_destination::tag,span_kind::tag  ORDER BY avg(elapsed_mean::field) DESC"},
		{"case3", fields{
			Cfg:     nil,
			Log:     nil,
			db:      nil,
			es:      nil,
			ctx:     nil,
			metricq: nil,
			t:       nil,
		}, args{params: translation{
			Start:             1630971092821,
			End:               1630981892821,
			Limit:             0,
			Search:            "topic",
			Layer:             "mq",
			FilterServiceName: "apm-demo-api-test",
			TerminusKey:       "58ee69adccbb4a42a638b2de6b8eac7c",
			Sort:              0,
			ServiceId:         "15_feature/simple_apm-demo-api-test",
			Type:              "consumer",
		}}, "SELECT message_bus_destination::tag,span_kind::tag,component::tag,host::tag,sum(elapsed_count::field)," +
			"format_duration(avg(elapsed_mean::field),'',2) FROM application_mq WHERE  message_bus_destination::tag=~/.*topic.*/ " +
			"AND target_service_id::tag=$serviceId AND span_kind::tag='consumer' AND target_terminus_key::tag=$terminusKey " +
			"GROUP BY message_bus_destination::tag,span_kind::tag  ORDER BY avg(elapsed_mean::field) DESC"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			topology := &provider{
				Cfg:     tt.fields.Cfg,
				Log:     tt.fields.Log,
				db:      tt.fields.db,
				es:      tt.fields.es,
				ctx:     tt.fields.ctx,
				metricq: tt.fields.metricq,
				t:       tt.fields.t,
			}
			_, _, got := topology.composeMqTranslationCondition(tt.args.params)

			if got != tt.want2 {
				t.Errorf("composeMqTranslationCondition() got = %v, want %v", got, tt.want2)
			}
		})
	}
}

type translator struct {
	common map[string]map[string]string
	dic    map[string]map[string]string
}

func (t *translator) Text(lang i18n.LanguageCodes, key string) string {
	return key
}

func (t *translator) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return key
}

func (t *translator) Get(lang i18n.LanguageCodes, key, def string) string {
	return def
}

func Test_handleSlowTranslationTraceResult(t *testing.T) {

	type args struct {
		topology *provider
		lang     i18n.LanguageCodes
		data     []map[string]interface{}
	}
	tests := []struct {
		name string
		args args
	}{
		{"case1", args{topology: &provider{t: &translator{}}, lang: i18n.LanguageCodes{{Code: "zh", Quality: 0.9}}}},
		{"case2", args{topology: &provider{t: &translator{}}, lang: i18n.LanguageCodes{{Code: "en", Quality: 0.9}}}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := handleSlowTranslationTraceResult(tt.args.topology, tt.args.lang, tt.args.data)
			if got == nil {
				t.Errorf("handleSlowTranslationTraceResult() = %v", got)
			}
		})
	}
}

func Test_handlerTranslationConditions(t *testing.T) {
	type args struct {
		params translation
		param  map[string]interface{}
		where  bytes.Buffer
	}
	tests := []struct {
		name        string
		args        args
		wantField   string
		wantOrderBy string
		wantErr     bool
	}{
		{"case1", args{params: translation{Layer: "xxx"}, where: bytes.Buffer{}}, "", "", true},
		{"case2", args{params: translation{Layer: ""}, where: bytes.Buffer{}}, "", "", true},
		{"case3", args{params: translation{Layer: "http"}, where: bytes.Buffer{}}, "http_path::tag", " ORDER BY avg(elapsed_mean::field) DESC", false},
		{"case4", args{params: translation{Layer: "rpc"}, where: bytes.Buffer{}}, "rpc_target::tag", " ORDER BY avg(elapsed_mean::field) DESC", false},
		{"case5", args{params: translation{Layer: "http", Sort: 0}, where: bytes.Buffer{}}, "http_path::tag", " ORDER BY avg(elapsed_mean::field) DESC", false},
		{"case6", args{params: translation{Layer: "rpc", Sort: 1}, where: bytes.Buffer{}}, "rpc_target::tag", " ORDER BY sum(elapsed_count::field) DESC", false},
		{"case7", args{params: translation{Layer: "rpc", Sort: 2}, where: bytes.Buffer{}}, "rpc_target::tag", " ORDER BY count(error::tag) DESC", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotField, gotOrderBy, err := handlerTranslationConditions(tt.args.params, tt.args.param, tt.args.where)
			if (err != nil) != tt.wantErr {
				t.Errorf("handlerTranslationConditions() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if gotField != tt.wantField {
				t.Errorf("handlerTranslationConditions() got = %v, want %v", gotField, tt.wantField)
			}
			if gotOrderBy != tt.wantOrderBy {
				t.Errorf("handlerTranslationConditions() got = %v, want %v", gotOrderBy, tt.wantOrderBy)
			}
		})
	}
}

func Test_columnsParser(t *testing.T) {
	type args struct {
		nodeType     string
		nodeRelation *TopologyNodeRelation
	}
	tests := []struct {
		name string
		args args
		want *Node
	}{
		{"case1", args{TargetServiceNode, &TopologyNodeRelation{}}, &Node{Type: TypeService}},
		{"case2", args{SourceServiceNode, &TopologyNodeRelation{}}, &Node{Type: TypeService}},
		{"case3-1", args{TargetAddonNode, &TopologyNodeRelation{Tags: Tag{Component: "Http"}}}, &Node{Type: TypeElasticsearch}},
		{"case3-2", args{TargetAddonNode, &TopologyNodeRelation{Tags: Tag{TargetAddonType: "Test"}}}, &Node{Type: "Test"}},
		{"case4", args{SourceAddonNode, &TopologyNodeRelation{Tags: Tag{SourceAddonType: "Test"}}}, &Node{Type: "Test"}},
		{"case5", args{TargetComponentNode, &TopologyNodeRelation{Tags: Tag{Component: "Test"}}}, &Node{Type: "Test"}},
		{"case6-1", args{TargetOtherNode, &TopologyNodeRelation{Tags: Tag{Component: "Http", Host: "terminus-elasticsearch"}}}, &Node{Type: TypeElasticsearch}},
		{"case6-2", args{TargetOtherNode, &TopologyNodeRelation{Tags: Tag{Component: "Test"}}}, &Node{Type: "Test"}},
		{"case6-3", args{TargetOtherNode, &TopologyNodeRelation{Tags: Tag{PeerServiceScope: "external"}}}, &Node{Type: TypeExternal}},
		{"case6-4", args{TargetOtherNode, &TopologyNodeRelation{Tags: Tag{PeerServiceScope: "internal"}}}, &Node{Type: TypeInternal}},
		{"case7", args{SourceMQNode, &TopologyNodeRelation{Tags: Tag{Component: "Test"}}}, &Node{Type: "Test"}},
		{"case8", args{TargetMQNode, &TopologyNodeRelation{Tags: Tag{Component: "Test"}}}, &Node{Type: "Test"}},
		{"case9", args{TargetMQServiceNode, &TopologyNodeRelation{}}, &Node{Type: TypeService}},
		{"case10", args{OtherNode, &TopologyNodeRelation{}}, &Node{Type: TypeService}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := columnsParser(tt.args.nodeType, tt.args.nodeRelation)
			if got.Type != tt.want.Type {
				t.Errorf("columnsParser() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_provider_slowTranslationTrace(t *testing.T) {
	type params struct {
		Start       int64  `query:"start" validate:"required"`
		End         int64  `query:"end" validate:"required"`
		ServiceName string `query:"serviceName" validate:"required"`
		TerminusKey string `query:"terminusKey" validate:"required"`
		Operation   string `query:"operation" validate:"required"`
		ServiceId   string `query:"serviceId" validate:"required"`
		Limit       int64  `query:"limit" default:"100"`
		Sort        string `default:"duration:DESC" query:"sort"`
	}

	type args struct {
		r      *http.Request
		params params
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{r: &http.Request{}, params: params{Sort: "duration:DESC"}}, false},
		{"case2", args{r: &http.Request{}, params: params{Sort: "timestamp:ASC"}}, false},
		{"case3", args{r: &http.Request{}, params: params{Sort: "duration:ASC"}}, false},
		{"case4", args{r: &http.Request{}, params: params{Sort: "timestamp DESC"}}, false},
		{"case5", args{r: &http.Request{}, params: params{Limit: 15000}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			topology := &provider{
				metricq: &metricq.Metricq{},
				t:       &MockTran{},
			}
			monkey.Patch(api.Language, func(r *http.Request) i18n.LanguageCodes {
				return i18n.LanguageCodes{{Code: "zh"}}
			})
			var m *metricq.Metricq
			monkey.PatchInstanceMethod(reflect.TypeOf(m), "Query", func(m *metricq.Metricq, ql, statement string, params map[string]interface{}, options url.Values) (*query.ResultSet, error) {
				return &query.ResultSet{ResultSet: &tsql.ResultSet{Rows: [][]interface{}{}}}, nil
			})
			topology.slowTranslationTrace(tt.args.r, tt.args.params)
		})
	}
}

type MockTran struct {
	i18n.Translator
}

func (m *MockTran) Text(lang i18n.LanguageCodes, key string) string {
	return ""
}

func Test_provider_handleInstanceInfo(t *testing.T) {
	type args struct {
		response *query.ResultSet
	}
	tests := []struct {
		name string
		args args
		want []*InstanceInfo
	}{
		{"case1", args{response: &query.ResultSet{
			ResultSet: &tsql.ResultSet{Rows: [][]interface{}{{"id", "172.0.0.0", "true", "127.0.0.1"}}},
		}}, []*InstanceInfo{{Id: "id", Ip: "172.0.0.0", Status: true, HostIP: "127.0.0.1"}}},
		{"case2", args{response: &query.ResultSet{
			ResultSet: &tsql.ResultSet{Rows: [][]interface{}{{"id", "172.0.0.0", "xxx", "127.0.0.1"}}},
		}}, []*InstanceInfo{{Id: "id", Ip: "172.0.0.0", Status: false, HostIP: "127.0.0.1"}}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			topology := &provider{}
			if got := topology.handleInstanceInfo(tt.args.response); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("handleInstanceInfo() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_parserTag(t *testing.T) {
	type args struct {
		param Vo
	}
	tests := []struct {
		name string
		args args
		want *TagInfo
	}{
		{"case1", args{param: Vo{Tags: []string{"service:service_id"}}}, &TagInfo{ServiceId: "service_id"}},
		{"case2", args{param: Vo{Tags: []string{"application:application_name"}}}, &TagInfo{ApplicationName: "application_name"}},
		{"case3", args{param: Vo{Tags: []string{"unknown:xx"}}}, &TagInfo{}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := parserTag(tt.args.param); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("parserTag() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getServiceNode(t *testing.T) {
	type args struct {
		serviceId string
		nodes     []*Node
	}
	tests := []struct {
		name string
		args args
		want *Node
	}{
		{"case1", args{serviceId: "service_id", nodes: []*Node{{Id: "id", ServiceId: "service_id"}}}, &Node{Id: "id", ServiceId: "service_id"}},
		{"case2", args{serviceId: "unknown", nodes: []*Node{{Id: "id", ServiceId: "service_id"}}}, nil},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getServiceNode(tt.args.serviceId, tt.args.nodes); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getServiceNode() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getNodeParentNodeIds(t *testing.T) {
	type args struct {
		node *Node
	}
	tests := []struct {
		name string
		args args
		want map[string]struct{}
	}{
		{"case1", args{node: &Node{Id: "id", ServiceId: "service_id", Parents: []*Node{}}}, map[string]struct{}{}},
		{"case2", args{node: &Node{Id: "id", ServiceId: "service_id", Parents: []*Node{{Id: "pid", ServiceId: "pservice_id"}}}}, map[string]struct{}{"pid": {}}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getNodeParentNodeIds(tt.args.node); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getNodeParentNodeIds() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_provider_isExistTopologyNode(t *testing.T) {
	type args struct {
		node          *Node
		topologyNodes *[]*Node
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{"case1", args{
			node: &Node{Id: "test1"},
			topologyNodes: &[]*Node{
				{Id: "test1"},
			},
		}, true},
		{"case2", args{
			node: &Node{Id: "test2"},
			topologyNodes: &[]*Node{
				{Id: "test1"},
			},
		}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			topology := &provider{}
			assert.Equalf(t, tt.want, topology.isExistTopologyNode(tt.args.node, tt.args.topologyNodes), "isExistTopologyNode(%v, %v)", tt.args.node, tt.args.topologyNodes)
		})
	}
}
