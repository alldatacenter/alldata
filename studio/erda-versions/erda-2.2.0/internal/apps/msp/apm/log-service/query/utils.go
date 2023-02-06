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

package log_service

import (
	"fmt"
	"strings"

	"google.golang.org/protobuf/types/known/structpb"
)

type StringList []string

func (l StringList) Any(predicate func(item string) bool) bool {
	for _, s := range l {
		if predicate(s) {
			return true
		}
	}
	return false
}

func (l StringList) All(predicate func(item string) bool) bool {
	for _, s := range l {
		if !predicate(s) {
			return false
		}
	}
	return true
}

func (l StringList) ToInterfaceList() []interface{} {
	var result []interface{}
	for _, s := range l {
		result = append(result, s)
	}
	return result
}

type LogKeyType string

const logServiceKey LogKeyType = LogKeyType("msp_env_id")
const logAnalysisV1Key = LogKeyType("terminus_log_key")
const logAnalysisV2Key = LogKeyType("monitor_log_key")

type LogKeys map[string]LogKeyType

func (m LogKeys) Add(logKeyValue string, logKeyName LogKeyType) {
	m[logKeyValue] = logKeyName
}

func (m LogKeys) Group() LogKeyGroup {
	result := LogKeyGroup{}
	for k, v := range m {
		list := result[v]
		result[v] = append(list, k)
	}
	return result
}

type LogKeyGroup map[LogKeyType]StringList

func (g LogKeyGroup) Contains(keyType LogKeyType) bool {
	_, ok := g[keyType]
	return ok
}

func (g LogKeyGroup) Where(filter func(k LogKeyType, v StringList) bool) LogKeyGroup {
	result := LogKeyGroup{}
	for keyType, list := range g {
		if filter(keyType, list) {
			result[keyType] = list
		}
	}
	return result
}

func (g LogKeyGroup) ToESQueryString() string {
	var terms []string
	for tag, values := range g {
		for _, value := range values {
			terms = append(terms, fmt.Sprintf("tags.%s:%s", tag, value))
		}
	}
	return strings.Join(terms, " OR ")
}

type ListMapConverter map[string][]string

func (lm ListMapConverter) ToPbListMap() map[string]*structpb.ListValue {
	if lm == nil {
		return nil
	}
	result := map[string]*structpb.ListValue{}
	for k, item := range lm {
		list, _ := structpb.NewList(StringList(item).ToInterfaceList())
		result[k] = list
	}
	return result
}
