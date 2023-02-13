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

package db

import (
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/pkg/database/gormutil"
)

// MetricDB .
type MetricDB struct {
	*gorm.DB
}

func (db *MetricDB) query() *gorm.DB {
	return db.Table(TableMetric).Where("`is_deleted`=?", "N")
}

func (db *MetricDB) GetByFields(fields map[string]interface{}) (*Metric, error) {
	query, err := gormutil.GetQueryFilterByFields(db.query(), metricFieldColumns, fields)
	if err != nil {
		return nil, err
	}
	var list []*Metric
	if err := query.Limit(1).Find(&list).Error; err != nil {
		return nil, err
	}
	if len(list) <= 0 {
		return nil, nil
	}
	return list[0], nil
}

func (db *MetricDB) GetByID(id int64) (*Metric, error) {
	return db.GetByFields(map[string]interface{}{
		"ID": id,
	})
}

func (db *MetricDB) ListIDs() ([]int64, error) {
	var list []int64
	if err := db.query().Select("`id`").Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}

func (db *MetricDB) ListByIDs(ids ...int64) ([]*Metric, error) {
	var list []*Metric
	if err := db.query().Where("`id` IN ?", ids).Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}

func (db *MetricDB) Create(m *Metric) error {
	m.ID = 0
	m.IsDeleted = "N"
	return db.Table(TableMetric).Create(m).Error
}

func (db *MetricDB) Update(m *Metric) error {
	m.UpdateTime = time.Now()
	return db.Table(TableMetric).Save(m).Error
}

func (db *MetricDB) Delete(id int64) error {
	return db.Table(TableMetric).Where("`id`=?", id).Updates(map[string]interface{}{
		"is_deleted":  "Y",
		"update_time": time.Now(),
	}).Error
}

func (db *MetricDB) ListByProjectIDAndEnv(projectID int64, env string) ([]*Metric, error) {
	var list []*Metric
	if err := db.query().Where("`project_id`=? AND `env`=?", projectID, env).Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}
