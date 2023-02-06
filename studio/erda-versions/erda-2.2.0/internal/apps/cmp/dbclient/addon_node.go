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

package dbclient

import (
	"fmt"
	"strings"
	"time"

	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

// AddonNode Addon node info
type AddonNode struct {
	ID         string `gorm:"type:varchar(64)"`
	InstanceID string `gorm:"type:varchar(64)"` // AddonInstance primary key
	Namespace  string `gorm:"type:text"`
	NodeName   string
	CPU        float64
	Mem        uint64
	Deleted    string    `gorm:"column:is_deleted"` // Y: deleted N: not delete
	CreatedAt  time.Time `gorm:"column:create_time"`
	UpdatedAt  time.Time `gorm:"column:update_time"`
}

func (AddonNode) TableName() string {
	return "tb_middle_node"
}

type addonNodeReader struct {
	db         *dbengine.DBEngine
	conditions []string
	limit      int
	offset     int
}

type addonNodeWriter struct {
	db *dbengine.DBEngine
}

func (c *DBClient) AddonNodeReader() *addonNodeReader {
	return &addonNodeReader{db: c.DBEngine, conditions: []string{}, limit: 0, offset: -1}
}

func (r *addonNodeReader) ByAddonIDs(aids ...string) *addonNodeReader {
	render := strutil.Map(aids, func(s string) string { return "\"" + s + "\"" })
	r.conditions = append(r.conditions, fmt.Sprintf("instance_id in (%s)", strutil.Join(render, ",")))
	return r
}

func (r *addonNodeReader) ByDeleteStatus(status ...string) *addonNodeReader {
	render := strutil.Map(status, func(s string) string { return "\"" + s + "\"" })
	r.conditions = append(r.conditions, fmt.Sprintf("is_deleted in (%s)", strutil.Join(render, ",")))
	return r
}

func (r *addonNodeReader) Do() ([]AddonNode, error) {
	ams := []AddonNode{}
	expr := r.db.Where(strutil.Join(r.conditions, " AND ", true)).Order("create_time desc")
	if r.limit != 0 {
		expr = expr.Limit(r.limit)
	}
	if r.offset != -1 {
		expr = expr.Offset(r.offset)
	}
	if err := expr.Find(&ams).Error; err != nil {
		r.conditions = []string{}
		return nil, err
	}
	r.conditions = []string{}
	return ams, nil
}

func (c *DBClient) AddonNodeWriter() *addonNodeWriter {
	return &addonNodeWriter{db: c.DBEngine}
}

func (w *addonNodeWriter) Create(a *AddonNode) (string, error) {
	db := w.db.Save(a)
	return a.ID, db.Error
}

func (w *addonNodeWriter) Update(a AddonNode) error {
	return w.db.Model(&a).Updates(a).Error
}

func (w *addonNodeWriter) Updates(cpu float64, mem uint64, addonIDs ...string) error {
	render := strutil.Map(addonIDs, func(s string) string { return "\"" + s + "\"" })
	return w.db.Model(AddonNode{}).Where(fmt.Sprintf("instance_id in (%s)", strutil.Join(render, ","))).
		Updates(map[string]interface{}{"cpu": cpu, "mem": mem}).Error
}

func (w *addonNodeWriter) Delete(ids ...string) error {
	render := strutil.Map(ids, func(s string) string { return "\"" + s + "\"" })
	return w.db.Delete(AddonNode{}, fmt.Sprintf("id in (%s)", strutil.Join(render, ","))).Error
}

type AddonNodeList []AddonNode

func (r AddonNodeList) Len() int {
	return len(r)
}

func (r AddonNodeList) Less(i, j int) bool {
	return strings.Compare(r[i].NodeName, r[j].NodeName) < 0
}

func (r AddonNodeList) Swap(i, j int) {
	r[i], r[j] = r[j], r[i]
}
