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

type orgakReader struct {
	db         *dbengine.DBEngine
	conditions []string
	limit      int
	offset     int
}
type orgakWriter struct {
	db *dbengine.DBEngine
}

func (c *DBClient) OrgAKReader() *orgakReader {
	return &orgakReader{db: c.DBEngine, conditions: []string{}, limit: 0, offset: -1}
}

func (r *orgakReader) ByOrgID(org string) *orgakReader {
	r.conditions = append(r.conditions, fmt.Sprintf("org_id = \"%s\"", org))
	return r
}

func (r *orgakReader) ByVendors(vendors ...string) *orgakReader {
	vendorsStr := strutil.Map(vendors, func(s string) string { return "\"" + s + "\"" })
	r.conditions = append(r.conditions, fmt.Sprintf("vendor in (%s)", strutil.Join(vendorsStr, ",")))
	return r
}
func (r *orgakReader) Do() ([]OrgAK, error) {
	orgaks := []OrgAK{}
	expr := r.db.Where(strutil.Join(r.conditions, " AND ", true)).Order("created_at desc")
	if r.limit != 0 {
		expr = expr.Limit(r.limit)
	}
	if r.offset != -1 {
		expr = expr.Offset(r.offset)
	}
	if err := expr.Find(&orgaks).Error; err != nil {
		r.conditions = []string{}
		return nil, err
	}
	r.conditions = []string{}
	return orgaks, nil
}

func (c *DBClient) OrgAKWriter() *orgakWriter {
	return &orgakWriter{db: c.DBEngine}
}

func (w *orgakWriter) Create(s *OrgAK) (uint64, error) {
	db := w.db.Save(s)
	return s.ID, db.Error
}

func (w *orgakWriter) Update(s OrgAK) error {
	return w.db.Model(&s).Updates(s).Error
}

func (w *orgakWriter) Delete(ids ...uint64) error {
	return w.db.Delete(OrgAK{}, "id in (?)", ids).Error
}
