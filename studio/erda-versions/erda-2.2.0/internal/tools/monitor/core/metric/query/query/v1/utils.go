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

package queryv1

import (
	"encoding/hex"
	"fmt"
	"strings"
	"time"

	md5simd "github.com/minio/md5-simd"
	"github.com/mitchellh/mapstructure"
	"github.com/pkg/errors"

	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
)

// NormalizeColumn .
func NormalizeColumn(col, typ string) string {
	idx := strings.Index(col, ".")
	if idx > 0 {
		return col
	}
	return typ + "." + col
}

// MapToRawQuery .
func MapToRawQuery(scope, agg string, params map[string]string) (string, error) {
	if scope == "" {
		return "", errors.New("metric scope can't empty")
	}

	path := scope
	if len(agg) > 0 {
		path += "/" + agg
	}
	path += "?"

	for k, v := range params {
		path += fmt.Sprintf("%s=%s&", k, v)
	}
	path = path[:len(path)-1]

	return path, nil
}

// NormalizeRequest .
func NormalizeRequest(req *Request) error {
	if req.TimeAlign != TimeAlignNone {
		now := time.Now()
		nowms := now.Add(-1*time.Minute).UnixNano() / int64(time.Millisecond)
		if req.End > nowms {
			offset := req.End - nowms
			req.End = nowms
			req.Start -= offset
		}
		alignValue := int64(time.Minute) / int64(time.Millisecond)
		req.End = req.End - req.End%alignValue
		req.Start = req.Start - req.Start%alignValue
	}
	if req.Start < 0 {
		req.Start = 0
	}
	if req.End <= 0 {
		req.End = 1
	}
	if req.End <= req.Start {
		// Blank check data.
		req.Start = 0
		req.End = 1
	}
	if len(req.TimeKey) <= 0 {
		req.TimeKey = tsql.TimestampKey
	}
	if req.TimeKey == tsql.TimestampKey {
		req.OriginalTimeUnit = tsql.Nanosecond
	} else if req.OriginalTimeUnit == tsql.UnsetTimeUnit {
		req.OriginalTimeUnit = tsql.Millisecond
	}

	if len(req.Metrics) == 0 {
		req.Metrics = strings.Split(req.Name, ",")
	}
	req.Columns = make(map[string]*Column)
	if req.ExistKeys == nil {
		req.ExistKeys = make(map[string]struct{})
	}

	for _, col := range req.Select {
		col.Property.Normalize(query.FieldKey)
		col.ID = NormalizeID(col.FuncName, &col.Property)
		req.Columns[col.ID] = col
		req.ExistKeys[col.Property.Key] = struct{}{}
	}
	clusters := make(map[string]struct{})
	for _, filter := range req.Where {
		if filter.Key == query.ClusterNameKey {
			if filter.Operator == "in" {
				if values, ok := filter.Value.([]interface{}); ok {
					for _, value := range values {
						clusters[fmt.Sprint(value)] = struct{}{}
					}
				}
			} else if cluster, ok := filter.Value.(string); ok {
				clusters[cluster] = struct{}{}
			}
		}
	}
	for cluster := range clusters {
		req.ClusterNames = append(req.ClusterNames, cluster)
	}

	for _, group := range req.GroupBy {
		group.Property.Normalize(query.TagKey)
		group.ID = NormalizeID("groupby", &group.Property)
		for _, filter := range group.Filters {
			filter.Property.Normalize(query.FieldKey)
			filter.ID = NormalizeID(filter.FuncName, &filter.Property)
		}
	}
	if len(req.Limit) <= 0 {
		req.Limit = append(req.Limit, 20)
	}
	for i, order := range req.OrderBy {
		order.Sort = strings.ToUpper(order.Sort)
		if order.Sort != "DESC" && order.Sort != "ASC" {
			return fmt.Errorf("invalid order by")
		}
		if order.FuncName == "count" && len(order.Property.Name) <= 0 {
			continue
		}
		order.Property.Normalize(query.FieldKey)
		if i < len(req.GroupBy)-1 && order.FuncName == "" {
			return fmt.Errorf("invalid order by")
		}
		order.ID = NormalizeID(order.FuncName, &order.Property)
	}
	if len(req.GroupBy) > 0 {
		for i, ody := range req.OrderBy {
			if i < len(req.GroupBy) {
				req.GroupBy[i].Sort = ody
			}
		}
		if len(req.OrderBy) > len(req.GroupBy) {
			req.OrderBy = req.OrderBy[len(req.GroupBy):]
		}
		for i, limit := range req.Limit {
			if i < len(req.GroupBy) {
				req.GroupBy[i].Limit = limit
			}
		}
	}

	if req.Aggregate != nil {
		if req.Aggregate.FuncName == "histogram" {
			if len(req.Aggregate.Property.Name) == 0 {
				req.Aggregate.Property.Name = ".timestamp"
			}
			switch {
			case req.Interval > 0:
				req.Points = float64(req.End-req.Start) * 1000000 / req.Interval
				if req.Points > 120 {
					req.Points = 120
					req.Interval = float64(req.End-req.Start) * 1000000 / 120
				}
			case req.Points == -1:
				points, interval, err := dynamicPoints(req)
				if err != nil {
					return errors.Wrap(err, "dynamic generate failed")
				}
				req.Interval = float64(interval.Nanoseconds())
				req.Points = float64(points)
			case req.Points > 0:
				req.Interval = float64(req.End-req.Start) * 1000000 / req.Points
			default:
				return fmt.Errorf("invalid points")
			}
			req.EndOffset = 2 * int64(time.Minute)
		}
		req.Aggregate.Property.Normalize(query.FieldKey)
		req.Aggregate.ID = NormalizeID(req.Aggregate.FuncName, &req.Aggregate.Property)
	}
	if req.Interval <= 0 {
		req.Interval = float64((req.End - req.Start) * int64(time.Millisecond))
	}
	return nil
}

func dynamicPoints(req *Request) (points int, interval time.Duration, err error) {
	if req.Start >= req.End {
		return 0, 0, fmt.Errorf("start must less than end")
	}
	duration := time.Duration((req.End - req.Start) * 1000000)
	switch {
	case duration < time.Minute:
		interval = time.Second * 30
		return int(duration / interval), interval, nil
	case duration < time.Minute*1*120:
		interval = time.Minute * 1
		return int(duration / interval), interval, nil
	default: // max is 120 points
		return 120, duration / time.Duration(120), nil
	}
}

// NormalizeKey .
func NormalizeKey(keys, typ string) string {
	if len(keys) == 0 || (strings.HasPrefix(keys, "{") && strings.HasSuffix(keys, "}")) {
		return keys
	}
	var list []string
	for _, key := range strings.Split(keys, ",") {
		if strings.Contains(key, ".") || key == "_name" || len(typ) == 0 {
			if key[0] == '.' || key == "_name" { // Compatible with group=_name query for historical reasons.
				list = append(list, key[1:])
				continue
			}
			list = append(list, key)
			continue
		}
		list = append(list, typ+"."+key)
	}
	return strings.Join(list, ",")
}

// NormalizeID .
func NormalizeID(fn string, p *Property) string {
	if p.IsScript() {
		server := md5simd.NewServer()
		defer server.Close()
		md5Hash := server.NewHash()
		defer md5Hash.Close()
		md5Hash.Write([]byte(fn + "_" + p.Name))
		return hex.EncodeToString(md5Hash.Sum(nil))[8:24]
	}
	return strings.Replace(fn+"_"+p.Key, ".", "_", -1)
}

// NormalizeName .
func NormalizeName(key string) string {
	if len(key) == 0 {
		return key
	}
	if key[0] == '.' || key[0] == '_' {
		return key[1:]
	}
	return key
}

// Unmarshal .
func Unmarshal(input, output interface{}) error {
	decoder, err := mapstructure.NewDecoder(&mapstructure.DecoderConfig{
		Metadata:         nil,
		Result:           output,
		WeaklyTypedInput: true,
		TagName:          "json",
		DecodeHook: mapstructure.ComposeDecodeHookFunc(
			mapstructure.StringToTimeDurationHookFunc(),
			mapstructure.StringToSliceHookFunc(","),
		),
	})
	if err != nil {
		return err
	}
	return decoder.Decode(input)
}

func getMapValue(key string, data map[string]interface{}) interface{} {
	keys := strings.Split(key, ".")
	last := len(keys) - 1
	for i, k := range keys {
		if i >= last {
			return data[k]
		}
		d, ok := data[k]
		if !ok {
			return nil
		}
		data, ok = d.(map[string]interface{})
		if !ok {
			return nil
		}
	}
	return nil
}
