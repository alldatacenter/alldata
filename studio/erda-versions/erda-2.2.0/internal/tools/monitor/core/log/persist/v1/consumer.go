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

package storage

import (
	"context"
	"strings"
	"time"

	jsoniter "github.com/json-iterator/go"
	"github.com/sirupsen/logrus"

	logmodule "github.com/erda-project/erda/internal/tools/monitor/core/log"
)

var json = jsoniter.ConfigCompatibleWithStandardLibrary

func (p *provider) invoke(key []byte, value []byte, topic *string, timestamp time.Time) error {
	log := &logmodule.Log{}
	if err := json.Unmarshal(value, log); err != nil {
		return err
	}
	p.processLog(log)
	if !p.validate(log) {
		logInvalidCounter.WithLabelValues(log.Tags[diceOrgNameKey]).Inc()
		return nil
	}

	cacheKey := log.Source + "_" + log.ID
	if !p.cache.Has(cacheKey) {
		// store meta
		meta := &logmodule.Meta{
			ID:     log.ID,
			Source: log.Source,
			Tags:   log.Tags,
		}
		p.output.Write(meta)
		p.cache.SetWithExpire(cacheKey, meta, time.Hour)
	}

	count(log)
	return p.output.Write(log)
}

func (p *provider) validate(log *logmodule.Log) bool {
	orgName, ok := log.Tags[diceOrgNameKey]
	if ok {
		return p.schema.ValidateOrg(orgName)
	}
	return true
}

func (p *provider) processLog(log *logmodule.Log) {
	if log.Tags == nil {
		log.Tags = make(map[string]string)
	}

	if log.Time != nil {
		log.Timestamp = log.Time.UnixNano()
		log.Time = nil
	}

	level, ok := log.Tags["level"]
	if !ok {
		level = "INFO" // default log level
	} else {
		level = strings.ToUpper(level)
	}
	log.Tags["level"] = level

	for _, key := range p.Cfg.Output.IDKeys {
		if val, ok := log.Tags[key]; ok {
			log.ID = val
			break
		}
	}

	if log.Stream == "" {
		log.Stream = "stdout" // default log stream
	}
}

func (p *provider) storeMetaCache() {
	for _, meta := range p.cache.GetALL(true) {
		if err := p.output.Write(meta); err != nil {
			logrus.Errorf("fail to write log meta: %s", err)
		}
	}
}

func (p *provider) startStoreMetaCache(ctx context.Context) {
	ticker := time.NewTicker(p.Cfg.Output.Cassandra.CacheStoreInterval)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			p.storeMetaCache()
		case <-ctx.Done():
			return
		}
	}
}

const (
	platformKey        = "platform"
	componentKey       = "component"
	componentNameKey   = "component_name"
	componentTypeKey   = "component_type"
	orgIDKey           = "org_id"
	orgNameKey         = "org_name"
	clusterNameKey     = "cluster_name"
	projectIDKey       = "project_id"
	projectNameKey     = "project_name"
	applicationIDKey   = "application_id"
	applicationNameKey = "application_name"
	workspaceKey       = "workspace"
	levelKey           = "level"

	dicePrefix             = "dice_"
	diceComponentKey       = dicePrefix + componentKey
	diceOrgIDKey           = dicePrefix + orgIDKey
	diceOrgNameKey         = dicePrefix + orgNameKey
	diceClusterNameKey     = dicePrefix + clusterNameKey
	diceProjectIDKey       = dicePrefix + projectIDKey
	diceProjectNameKey     = dicePrefix + projectNameKey
	diceApplicationIDKey   = dicePrefix + applicationIDKey
	diceApplicationNameKey = dicePrefix + applicationNameKey
	diceWorkspaceKey       = dicePrefix + workspaceKey

	srcKey                = "src"
	srcPrefix             = "src_"
	srcComponentNameKey   = srcPrefix + componentNameKey
	srcComponentTypeKey   = srcPrefix + componentTypeKey
	srcOrgNameKey         = srcPrefix + orgNameKey
	srcClusterNameKey     = srcPrefix + clusterNameKey
	srcProjectIDKey       = srcPrefix + projectIDKey
	srcProjectNameKey     = srcPrefix + projectNameKey
	srcApplicationIDKey   = srcPrefix + applicationIDKey
	srcApplicationNameKey = srcPrefix + applicationNameKey
	srcWorkspaceKey       = srcPrefix + workspaceKey
)

func count(log *logmodule.Log) {
	componentName := log.Tags[diceComponentKey]
	var componentType string
	if componentName != "" {
		componentType = platformKey
	}
	logBytesCounter.WithLabelValues(
		log.Tags[levelKey],
		log.Source,
		componentType,
		componentName,
		log.Tags[diceClusterNameKey],
		log.Tags[diceOrgNameKey],
		log.Tags[diceProjectIDKey],
		log.Tags[diceProjectNameKey],
		log.Tags[diceApplicationIDKey],
		log.Tags[diceApplicationNameKey],
		log.Tags[diceWorkspaceKey],
	).Add(float64(len(log.Content)))
}
