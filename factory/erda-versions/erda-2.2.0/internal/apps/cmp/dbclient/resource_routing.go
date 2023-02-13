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

	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

type resourceRoutingReader struct {
	db         *dbengine.DBEngine
	conditions []string
	limit      int
	offset     int
}

type resourceRoutingWriter struct {
	db *dbengine.DBEngine
}

func (c *DBClient) ResourceRoutingReader() *resourceRoutingReader {
	return &resourceRoutingReader{db: c.DBEngine, conditions: []string{}, limit: 0, offset: -1}
}

func (r *resourceRoutingReader) ByResourceIDs(rids ...string) *resourceRoutingReader {
	render := strutil.Map(rids, func(s string) string { return "\"" + s + "\"" })
	r.conditions = append(r.conditions, fmt.Sprintf("resource_id in (%s)", strutil.Join(render, ",")))
	return r
}

func (r *resourceRoutingReader) ByResourceTypes(types ...string) *resourceRoutingReader {
	render := strutil.Map(types, func(s string) string { return "\"" + s + "\"" })
	r.conditions = append(r.conditions, fmt.Sprintf("resource_type in (%s)", strutil.Join(render, ",")))
	return r
}

func (r *resourceRoutingReader) ByProjectIDs(pids ...string) *resourceRoutingReader {
	render := strutil.Map(pids, func(s string) string { return "\"" + s + "\"" })
	r.conditions = append(r.conditions, fmt.Sprintf("project_id in (%s)", strutil.Join(render, ",")))
	return r
}

func (r *resourceRoutingReader) ByRecordIDs(rids ...string) *resourceRoutingReader {
	r.conditions = append(r.conditions, fmt.Sprintf("record_id in (%s)", strutil.Join(rids, ",")))
	return r
}

func (r *resourceRoutingReader) ByAddonIDs(aids ...string) *resourceRoutingReader {
	render := strutil.Map(aids, func(s string) string { return "\"" + s + "\"" })
	r.conditions = append(r.conditions, fmt.Sprintf("addon_id in (%s)", strutil.Join(render, ",")))
	return r
}

func (r *resourceRoutingReader) ByClusterName(clusters ...string) *resourceRoutingReader {
	render := strutil.Map(clusters, func(s string) string { return "\"" + s + "\"" })
	r.conditions = append(r.conditions, fmt.Sprintf("cluster_name in (%s)", strutil.Join(render, ",")))
	return r
}

func (r *resourceRoutingReader) Do() ([]ResourceRouting, error) {
	rRoutings := []ResourceRouting{}
	expr := r.db.Where(strutil.Join(r.conditions, " AND ", true)).Order("created_at desc")
	if r.limit != 0 {
		expr = expr.Limit(r.limit)
	}
	if r.offset != -1 {
		expr = expr.Offset(r.offset)
	}
	if err := expr.Find(&rRoutings).Error; err != nil {
		r.conditions = []string{}
		return nil, err
	}
	r.conditions = []string{}
	return rRoutings, nil
}

func (c *DBClient) ResourceRoutingWriter() *resourceRoutingWriter {
	return &resourceRoutingWriter{db: c.DBEngine}
}

func (w *resourceRoutingWriter) Create(r *ResourceRouting) (uint64, error) {
	db := w.db.Save(r)
	return r.ID, db.Error
}

func (w *resourceRoutingWriter) Update(r ResourceRouting) error {
	return w.db.Model(&r).Updates(r).Error
}

func (w *resourceRoutingWriter) Delete(ids ...uint64) error {
	return w.db.Delete(ResourceRouting{}, "id in (?)", ids).Error
}
