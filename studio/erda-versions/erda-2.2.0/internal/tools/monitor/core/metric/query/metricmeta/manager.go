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

package metricmeta

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/providers/i18n"
	indexloader "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

// MetaSource .
type MetaSource string

// MetaSource values
const (
	MappingsMetaSource MetaSource = "mappings"
	IndexMetaSource    MetaSource = "index"
	FileMetaSource     MetaSource = "file"
	DatabaseMetaSource MetaSource = "database"
)

// Manager .
type Manager struct {
	sources         map[MetaSource]bool
	groupProviders  []func() GroupProvider
	metricProviders []func() MetricMetaProvider

	db         *gorm.DB
	index      indexloader.Interface
	metaPath   string
	groupFiles []string

	i18n i18n.I18n
	log  logs.Logger
}

// NewManager .
func NewManager(ms []string, db *gorm.DB, index indexloader.Interface, metaPath string, groupFiles []string, i18n i18n.I18n, log logs.Logger) *Manager {
	sources := make(map[MetaSource]bool)
	for _, item := range ms {
		sources[MetaSource(item)] = true
	}
	return &Manager{
		sources:    sources,
		db:         db,
		index:      index,
		metaPath:   metaPath,
		groupFiles: groupFiles,
		i18n:       i18n,
		log:        log,
	}
}

// Init .
func (m *Manager) Init() error {
	if m.sources[MappingsMetaSource] {
		gp, err := NewIndexMappingsGroupProvider(m.index)
		if err != nil {
			return err
		}
		m.groupProviders = append(m.groupProviders, func() GroupProvider { return gp })
		mp, err := NewIndexMappingsMetricMetaProvider(m.index)
		if err != nil {
			return err
		}
		m.metricProviders = append(m.metricProviders, func() MetricMetaProvider { return mp })
	}
	if m.sources[IndexMetaSource] {
		gp, err := NewMetaIndexGroupProvider(m.index)
		if err != nil {
			return err
		}
		m.groupProviders = append(m.groupProviders, func() GroupProvider { return gp })
		mp, err := NewMetaIndexMetricMetaProvider(m.index, m.log)
		if err != nil {
			return err
		}
		m.metricProviders = append(m.metricProviders, func() MetricMetaProvider { return mp })
	}
	if m.sources[FileMetaSource] {
		gp, err := NewFileGroupProvider(m.groupFiles, m.log)
		if err != nil {
			return err
		}
		m.groupProviders = append(m.groupProviders, func() GroupProvider { return gp })
		mp, err := NewFileMetricMetaProvider(m.metaPath, m.log)
		if err != nil {
			return err
		}
		m.metricProviders = append(m.metricProviders, func() MetricMetaProvider { return mp })
	}
	if m.sources[DatabaseMetaSource] {
		gp, err := NewDatabaseGroupProvider(m.db, m.log)
		if err != nil {
			return err
		}
		m.groupProviders = append(m.groupProviders, func() GroupProvider { return gp })
		mp, err := NewDatabaseMetaProvider(m.db, m.log)
		if err != nil {
			return err
		}
		m.metricProviders = append(m.metricProviders, func() MetricMetaProvider { return mp })
	}
	return nil
}
