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

package json

import (
	"bytes"
	"encoding/json"
	"fmt"
	"regexp"
	"strings"
	"unicode"

	"github.com/recallsong/go-utils/reflectx"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
	queryv1 "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query/v1"
)

// Form .
type Form struct {
	Start     string              `json:"start"`
	End       string              `json:"end"`
	Timestamp string              `json:"timestamp"`
	Latest    string              `json:"latest"`
	Filter    map[string]string   `json:"filter"`
	Match     map[string]string   `json:"match"`
	Field     map[string]string   `json:"field"`
	GField    map[string]string   `json:"gfield"`
	In        map[string][]string `json:"in"`
	Group     []string            `json:"group"`
	Sort      []string            `json:"sort"`
	Range     []RangeAgg          `json:"range"`
	Limit     []int               `json:"limit"`
	Debug     bool                `json:"debug"`
	Points    float64             `json:"points"`
	Align     string              `json:"align"`
	Function  map[string][]string `json:"function"`
}

// RangeAgg .
type RangeAgg struct {
	Name   string `json:"name"`
	Ranges []struct {
		Form float64 `json:"form"`
		To   float64 `json:"to"`
	} `json:"ranges"`
	Split     int     `json:"split"`
	RangeSize float64 `json:"rangesize"`
}

// Parser .
type Parser struct{}

// Parse .
func (p *Parser) Parse(statement string) (*queryv1.Request, error) {
	parts := strings.SplitN(statement, "?", 2)
	if len(parts) != 2 {
		return nil, fmt.Errorf("invalid query statement: %s", statement)
	}

	var form Form
	if err := json.Unmarshal([]byte(parts[1]), &form); err != nil {
		return nil, fmt.Errorf("json unmarshal failed: %+v", form)
	}

	mparts := strings.SplitN(parts[0], "/", 2)
	req := &queryv1.Request{
		ExistKeys: make(map[string]struct{}),
	}
	req.Metrics = strings.Split(mparts[0], ",")
	req.Name = mparts[0]
	req.Limit = form.Limit

	err := req.InitTimestamp(form.Start, form.End, form.Timestamp, form.Latest)
	if err != nil {
		return nil, err
	}

	for _, group := range form.Group {
		var script string
		if _, ok := getScript(group); ok {
			script = group
		}
		req.GroupBy = append(req.GroupBy, &queryv1.Group{
			Property: queryv1.Property{
				Name:   group,
				Script: script,
			},
			Limit: 20,
		})
	}

	for _, val := range form.Sort {
		sort := "DESC"
		if strings.HasPrefix(val, "histogram_") {
			val = val[len("histogram_"):]
		}
		idx := strings.LastIndex(val, ":")
		if idx >= 0 {
			sort = val[idx+1:]
			val = val[:idx]
		}
		if val == "count" {
			req.OrderBy = append(req.OrderBy, &queryv1.Order{
				FuncName: val,
				Sort:     sort,
			})
			continue
		}
		preIdx := strings.Index(val, ".")
		idx = strings.Index(val, "_")
		if idx < 0 || (preIdx >= 0 && idx > preIdx) {
			if val == "timestamp" {
				val = "." + val
			}
			req.OrderBy = append(req.OrderBy, &queryv1.Order{
				Property: queryv1.Property{
					Name: val,
				},
				FuncName: "",
				Sort:     sort,
			})
		} else {
			if val[idx+1:] == "" || val[:idx] == "" {
				return nil, fmt.Errorf("invalid sort %s", val)
			}
			req.OrderBy = append(req.OrderBy, &queryv1.Order{
				Property: queryv1.Property{
					Name: val[idx+1:],
				},
				FuncName: val[:idx],
				Sort:     sort,
			})
		}
	}

	for key, val := range form.Filter {
		req.Where = append(req.Where, &query.Filter{
			Key:      queryv1.NormalizeKey(key, query.TagKey),
			Operator: "=",
			Value:    val,
		})
	}

	for key, val := range form.Match {
		req.Where = append(req.Where, &query.Filter{
			Key:      queryv1.NormalizeKey(key, query.TagKey),
			Operator: "match",
			Value:    val,
		})
	}

	for key, val := range form.Field {
		var value interface{}
		err := json.Unmarshal([]byte(val), &value)
		if err != nil {
			return nil, fmt.Errorf("invalid params field_%s=%s", key, val)
		}
		req.Where = append(req.Where, &query.Filter{
			Key:      queryv1.NormalizeKey(key, query.TagKey),
			Operator: "field",
			Value:    value,
		})
	}

	for key, vals := range form.In {
		for _, val := range vals {
			values := make([]interface{}, 0, len(val))
			for _, val := range vals {
				values = append(values, val)
			}
			req.Where = append(req.Where, &query.Filter{
				Key:      queryv1.NormalizeKey(key, query.TagKey),
				Operator: "in",
				Value:    values,
			})
		}
	}

	for _, val := range form.Range {
		var script string
		if s, ok := getScript(val.Name); ok {
			var keys map[string]struct{}
			script, keys, err = parseScript(s, query.FieldKey)
			if err != nil {
				return nil, fmt.Errorf("invalid script %v", val)
			}

			if keys != nil {
				for k := range keys {
					req.ExistKeys[k] = struct{}{}
				}
			}
		}
		values, err := getRangeParams(val)
		if err != nil {
			return nil, err
		}

		req.Select = append(req.Select, &queryv1.Column{
			Property: queryv1.Property{
				Name:   val.Name,
				Script: script,
			},
			FuncName: "range",
			Params:   values,
		})
	}

	for key, vals := range form.Function {
		if _, ok := queryv1.Functions[key]; ok {
			for _, val := range vals {
				var script string
				if s, ok := getScript(val); ok {
					var keys map[string]struct{}
					script, keys, err = parseScript(s, query.FieldKey)
					if err != nil {
						return nil, fmt.Errorf("invalid script %s", val)
					}
					if keys != nil {
						for k := range keys {
							req.ExistKeys[k] = struct{}{}
						}
					}
				}
				req.Select = append(req.Select, &queryv1.Column{
					Property: queryv1.Property{
						Name:   val,
						Script: script,
					},
					FuncName: key,
				})
			}
		}
	}

	align := strings.ToLower(form.Align)
	if align == "true" {
		req.TimeAlign = queryv1.TimeAlignAuto
	} else if align == "false" {
		req.TimeAlign = queryv1.TimeAlignNone
	} else {
		req.TimeAlign = queryv1.TimeAlignUnset
	}

	if len(mparts) > 1 {
		switch mparts[1] {
		case "histogram":
			req.Aggregate = &queryv1.Column{
				FuncName: mparts[1],
			}
			req.Points = form.Points
			req.AlignEnd = req.TimeAlign != queryv1.TimeAlignNone
		case "range":
		case "apdex":
		}
	}
	if form.Debug {
		req.Debug = true
	}
	return req, nil
}

func getRangeParams(rangeAgg RangeAgg) (values []interface{}, err error) {
	if len(rangeAgg.Ranges) > 0 {
		for _, item := range rangeAgg.Ranges {
			values = append(values, &queryv1.ValueRange{
				From: item.Form,
				To:   item.To,
			})
		}
	} else {
		rangeSize := rangeAgg.RangeSize
		split := rangeAgg.Split
		if split > 1000 {
			return nil, fmt.Errorf("invalid split %d", split)
		}
		for i, last := 0, split-1; i < split; i++ {
			if i >= last {
				values = append(values, &queryv1.ValueRange{
					From: float64(i) * rangeSize,
					To:   nil,
				})
			} else {
				values = append(values, &queryv1.ValueRange{
					From: float64(i) * rangeSize,
					To:   float64(i+1) * rangeSize,
				})
			}
		}
	}
	return values, nil
}

func getScript(script string) (string, bool) {
	if strings.HasPrefix(script, "(") && strings.HasSuffix(script, ")") {
		return script[1 : len(script)-1], true
	} else if strings.ContainsAny(script, "+-*/") {
		return script, true
	}
	return "", false
}

func parseScript(script, keyType string) (string, map[string]struct{}, error) {
	if strings.HasPrefix(script, "(") && strings.HasSuffix(script, ")") {
		if keyType == query.TagKey {
			if match, _ := regexp.Match("doc\\[\\'[a-zA-Z0-9_.]+\\'\\]", reflectx.StringToBytes(script)); match {
				// As the original elasticsearch script.
				return script, nil, nil
			}
		} else {
			if match, _ := regexp.Match("return .*", reflectx.StringToBytes(script)); match {
				// 作为原始的es脚本
				return script, nil, nil
			}
		}
	}
	result := &bytes.Buffer{}
	fields := make(map[string]struct{})
	var field []rune
	for i, c := range script {
		if unicode.IsSpace(c) {
			continue
		}
		if unicode.IsLetter(c) || c == '_' || c == '.' {
			field = append(field, c)
		} else {
			if len(field) > 0 {
				fieldName := string(field)
				if !strings.HasPrefix(fieldName, "tags.") && !strings.HasPrefix(fieldName, "fields.") &&
					fieldName != "@timestamp" && fieldName != "timestamp" && fieldName != "name" {
					result.WriteString("doc['" + keyType + "." + fieldName + "'].value")
					fields[keyType+"."+fieldName] = struct{}{}
				} else {
					result.WriteString("doc['" + fieldName + "'].value")
					fields[fieldName] = struct{}{}
				}
				field = nil
			}
			if c == ',' {
				if i == len(script)-2 { // (xxx,)
					result.WriteString("+','")
				} else {
					result.WriteString("+','+")
				}
			} else {
				result.WriteRune(c)
			}
		}
	}
	if len(field) > 0 {
		fieldName := string(field)
		if !strings.HasPrefix(fieldName, "tags.") && !strings.HasPrefix(fieldName, "fields.") &&
			fieldName != "@timestamp" && fieldName != "timestamp" && fieldName != "name" {
			result.WriteString("doc['" + keyType + "." + fieldName + "'].value")
			fields[keyType+"."+fieldName] = struct{}{}
		} else {
			result.WriteString("doc['" + fieldName + "'].value")
			fields[fieldName] = struct{}{}
		}
	}
	return result.String(), fields, nil
}

func init() {
	queryv1.RegisterQueryParser("json", &Parser{})
}
