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

package adapt

import (
	"reflect"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
)

// OperatorType .
const (
	OperatorTypeNone = "none"
	OperatorTypeOne  = "one"
	OperatorTypeMore = "more"

	OrgNotifyTemplateSample = `
    【机器负载异常告警】

    Load5: {{load5_avg}}

    集群: {{cluster_name}}

    机器: {{host_ip}}

    时间: {{timestamp}}

    [查看详情]({{display_url}})

    [查看记录]({{record_url}})
`

	OrgNotifyTemplateSampleEn = `
    【System load average alarm】

    Load5: {{load5_avg}}

    Cluster: {{cluster_name}}

    IP: {{host_ip}}

    Time: {{timestamp}}

    [Details]({{display_url}})

    [History]({{record_url}})
`

	fixedSliencePolicy = "fixed"
)

// units
const (
	Seconds = "seconds"
	Minutes = "minutes"
	Hours   = "hours"
)

const (
	ClusterName   = "cluster_name"
	ApplicationId = "application_id"
	HostIP        = "host_ip"
)

const (
	SMS                = "sms"
	dingtalkWorkNotice = "dingtalk_work_notice"
)

type (
	// DisplayKey .
	DisplayKey struct {
		Key     string `json:"key"`
		Display string `json:"display"`
	}
	// Operator .
	Operator struct {
		DisplayKey
		Type string `json:"type"`
	}
	// NotifySilence .
	NotifySilence struct {
		Value int64       `json:"value"`
		Unit  *DisplayKey `json:"unit"`
	}
)

var (
	// duration
	windowKeys = []int64{1, 3, 5, 10, 15, 30}
	// filter operation
	filterOperatorRel = map[string]string{
		"any":      OperatorTypeNone,
		"eq":       OperatorTypeOne,
		"false":    OperatorTypeNone,
		"in":       OperatorTypeMore,
		"notIn":    OperatorTypeMore,
		"like":     OperatorTypeOne,
		"neq":      OperatorTypeOne,
		"null":     OperatorTypeNone,
		"match":    OperatorTypeOne,
		"notMatch": OperatorTypeOne,
		"all":      OperatorTypeOne,
	}
	functionOperatorRel = map[string]string{
		"all":      OperatorTypeOne,
		"any":      OperatorTypeNone,
		"contains": OperatorTypeOne,
		"eq":       OperatorTypeOne,
		"gt":       OperatorTypeOne,
		"gte":      OperatorTypeOne,
		"lt":       OperatorTypeOne,
		"lte":      OperatorTypeOne,
		"like":     OperatorTypeOne,
		"neq":      OperatorTypeOne,
	}
	aggregators, aggregatorSet     = getListAndSet("sum", "avg", "max", "min", "distinct", "count", "value", "values", "distinct_count", "p99", "p95", "p90", "p75", "p50")
	notifyTargets, notifyTargetSet = getListAndSet("dingding", "webhook", "email", "mbox", "ticket", "sms", "vms", "dingtalk_work_notice")
	//notifyTargets, notifyTargetSet = getListAndSet("dingding", "webhook", "email", "mbox", "ticket", "vms", "sms")
	notifySilences = []*struct {
		Value int64  `json:"value"`
		Unit  string `json:"unit"`
	}{
		{Value: 5, Unit: Minutes},
		{Value: 10, Unit: Minutes},
		{Value: 15, Unit: Minutes},
		{Value: 30, Unit: Minutes},
		{Value: 60, Unit: Minutes},
		{Value: 3, Unit: Hours},
	}
)

func getListAndSet(list ...string) ([]string, map[string]bool) {
	set := make(map[string]bool)
	for _, item := range list {
		set[item] = true
	}
	return list, set
}

// FilterOperatorKeys .
func (a *Adapt) FilterOperatorKeys(lang i18n.LanguageCodes) []*pb.Operator {
	return a.getOperatorKeys(lang, filterOperatorRel)
}

// FilterOperatorKeysMap .
func (a *Adapt) FilterOperatorKeysMap() map[string]string {
	return filterOperatorRel
}

// FunctionOperatorKeys .
func (a *Adapt) FunctionOperatorKeys(lang i18n.LanguageCodes) []*pb.Operator {
	return a.getOperatorKeys(lang, functionOperatorRel)
}

// FunctionOperatorKeysMap .
func (a *Adapt) FunctionOperatorKeysMap() map[string]string {
	return functionOperatorRel
}

func (a *Adapt) getOperatorKeys(lang i18n.LanguageCodes, m map[string]string) []*pb.Operator {
	var list []*pb.Operator
	for k, v := range m {
		list = append(list, &pb.Operator{
			Key:     k,
			Display: a.t.Text(lang, k),
			Type:    v,
		})
	}
	return list
}

// AggregatorKeys .
func (a *Adapt) AggregatorKeys(lang i18n.LanguageCodes) []*pb.DisplayKey {
	var keys []*pb.DisplayKey
	for _, item := range aggregators {
		keys = append(keys, &pb.DisplayKey{Key: item, Display: a.t.Text(lang, item)})
	}
	return keys
}

// AggregatorKeysSet .
func (a *Adapt) AggregatorKeysSet() map[string]bool {
	return aggregatorSet
}

// NotifyTargetsKeys .
func (a *Adapt) NotifyTargetsKeys(code i18n.LanguageCodes, config map[string]bool) []*pb.DisplayKey {
	var keys []*pb.DisplayKey
	for item := range config {
		keys = append(keys, &pb.DisplayKey{Key: item, Display: a.t.Text(code, item)})
	}
	return keys
}

// NotifySilences .
func (a *Adapt) NotifySilences(lang i18n.LanguageCodes) []*pb.NotifySilence {
	var silenceKeys []*pb.NotifySilence
	for _, item := range notifySilences {
		silenceKeys = append(silenceKeys, &pb.NotifySilence{
			Value: item.Value,
			Unit:  &pb.DisplayKey{Key: item.Unit, Display: a.t.Text(lang, item.Unit)},
		})
	}
	return silenceKeys
}

// data types .
const (
	StringType  = "string"
	NumberType  = "number"
	BoolType    = "bool"
	UnknownType = "unknown"
)

// TypeOf .
func TypeOf(obj interface{}) string {
	if obj == nil {
		return ""
	}
	switch obj.(type) {
	case []byte, string:
		return StringType
	case int, int8, int16, int32, int64, uint, uint8, uint16, uint32, uint64, float32, float64:
		return NumberType
	case bool:
		return BoolType
	default:
		val := reflect.ValueOf(obj)
		switch val.Kind() {
		case reflect.Array, reflect.Slice:
			return TypeOf(val.Index(0).Interface())
		default:
			return UnknownType
		}
	}
}

func convertDataByType(obj interface{}, dataType string) (interface{}, error) {
	switch TypeOf(obj) {
	case StringType:
		value, _ := convertString(obj)
		switch dataType {
		case StringType:
			return value, nil
		case NumberType:
			return strconv.ParseFloat(value, 10)
		case BoolType:
			return strconv.ParseBool(value)
		}
	case NumberType:
		value, _ := convertFloat64(obj)
		switch dataType {
		case StringType:
			return strconv.FormatFloat(value, 'f', -1, 64), nil
		case NumberType:
			return value, nil
		case BoolType:
			if value == 0 {
				return false, nil
			}
			return true, nil
		}
	case BoolType:
		value, _ := convertBool(obj)
		switch dataType {
		case StringType:
			return strconv.FormatBool(value), nil
		case NumberType:
			if value {
				return 1, nil
			}
			return 0, nil
		case BoolType:
			return value, nil
		}
	}
	return nil, nil
}

func convertString(obj interface{}) (string, bool) {
	switch val := obj.(type) {
	case string:
		return val, true
	case []byte:
		return string(val), true
	}
	return "", false
}

func convertFloat64(obj interface{}) (float64, bool) {
	switch val := obj.(type) {
	case int:
		return float64(val), true
	case int8:
		return float64(val), true
	case int16:
		return float64(val), true
	case int32:
		return float64(val), true
	case int64:
		return float64(val), true
	case uint:
		return float64(val), true
	case uint8:
		return float64(val), true
	case uint16:
		return float64(val), true
	case uint32:
		return float64(val), true
	case uint64:
		return float64(val), true
	case float32:
		return float64(val), true
	case float64:
		return float64(val), true
	}
	return 0, false
}

func convertBool(obj interface{}) (bool, bool) {
	switch val := obj.(type) {
	case bool:
		return val, true
	case int, int8, int16, int32, int64, uint, uint8, uint16, uint32, uint64, float32, float64:
		return val != 0, true
	}
	return false, false
}

func convertMillisecondByUnit(value int64, unit string) int64 {
	switch unit {
	case Minutes:
		return value * time.Minute.Nanoseconds() / time.Millisecond.Nanoseconds()
	case Hours:
		return value * time.Hour.Nanoseconds() / time.Millisecond.Nanoseconds()
	case Seconds:
		return value * time.Second.Nanoseconds() / time.Millisecond.Nanoseconds()
	default:
		return -1
	}
}

func convertMillisecondToUnit(t int64) (value int64, unit string) {
	ns := t * time.Millisecond.Nanoseconds()
	if ns > time.Hour.Nanoseconds() {
		return ns / time.Hour.Nanoseconds(), Hours
	} else if ns > time.Minute.Nanoseconds() {
		return ns / time.Minute.Nanoseconds(), Minutes
	}
	return ns / time.Second.Nanoseconds(), Seconds
}
