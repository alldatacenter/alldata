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

package analysis

import (
	"strings"
	"time"

	jsoniter "github.com/json-iterator/go"
	"github.com/recallsong/go-utils/errorx"

	"github.com/erda-project/erda/internal/apps/msp/apm/log-service/analysis/processors"
	logs "github.com/erda-project/erda/internal/tools/monitor/core/log"
	metrics "github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

var json = jsoniter.ConfigCompatibleWithStandardLibrary

func (p *provider) invoke(key []byte, value []byte, topic *string, timestamp time.Time) error {
	pv := p.processors.Load()
	if pv == nil {
		// processors not ready, so return
		return nil
	}
	log := &logs.Log{}
	if err := json.Unmarshal(value, log); err != nil {
		return err
	}

	// do filter
	if log.Tags == nil {
		return nil
	}
	if log.Timestamp == 0 && log.Time != nil {
		log.Timestamp = log.Time.UnixNano()
	}

	// 兼容
	// todo: use processor-pipeline pattern to simple the code
	tagVal, ok := log.Tags["monitor_log_key"]
	if !ok {
		tagVal, ok = log.Tags["terminus_log_key"]
		if ok {
			log.Tags["monitor_log_key"] = tagVal
		}
	}
	if !ok {
		tagVal, ok = log.Tags["msp_env_id"]
		if ok {
			log.Tags["monitor_log_key"] = tagVal
		}
	}

	if len(tagVal) == 0 {
		return nil
	}

	if p.C.Filters != nil {
		for k, v := range p.C.Filters {
			val, ok := log.Tags[k]
			if !ok {
				return nil
			}
			if len(v) > 0 && v != val {
				return nil
			}
		}
	}

	level, ok := log.Tags["level"]
	if !ok {
		level = "INFO" // default log level
	} else {
		level = strings.ToUpper(level)
	}
	log.Tags["level"] = level
	log.Tags["org_name"] = log.Tags["dice_org_name"]
	log.Tags["cluster_name"] = log.Tags["dice_cluster_name"]
	log.Tags["_meta"] = "true"
	log.Tags["_metric_scope"] = p.C.Processors.Scope
	scopeID := p.C.Processors.ScopeID
	if len(scopeID) <= 0 {
		if len(p.C.Processors.ScopeIDKey) > 0 {
			scopeID = log.Tags[p.C.Processors.ScopeIDKey]
			log.Tags["_metric_scope_id"] = scopeID
		} else if p.C.Processors.Scope == "org" {
			scopeID = log.Tags["dice_org_name"]
			log.Tags["_metric_scope_id"] = scopeID
		}
	} else {
		log.Tags["_metric_scope_id"] = scopeID
	}

	// fmt.Println(jsonx.MarshalAndIndent(log.Tags))

	ps := (pv.(*processors.Processors)).Find("", scopeID, log.Tags)
	var errs errorx.Errors
	for _, processor := range ps {
		name, fields, appendTags, replaceKey, err := processor.Process(log.Content)
		if err != nil {
			// invalid processor or not match content
			continue
		}
		for k, v := range fields {
			if s, ok := v.(string); ok {
				if _, ok := log.Tags[k]; !ok {
					// 直接在 Tags 上修改，因为这里 len(ps) == 1，不会混淆
					// 后面大盘支持 field 过滤了，再调整
					log.Tags[k] = s
				}
			}
		}
		if len(appendTags) > 0 {
			for k, v := range appendTags {
				log.Tags[k] = v
			}
		}
		for k, v := range replaceKey {
			value := log.Tags[k]
			delete(log.Tags, k)
			log.Tags[v] = value
		}
		metric := &metrics.Metric{
			Name:      name,
			Timestamp: log.Timestamp,
			Tags:      log.Tags,
			Fields:    fields,
		}
		err = p.output.Write(metric)
		if err != nil {
			errs = append(errs, err)
		}
	}
	return errs.MaybeUnwrap()
}
