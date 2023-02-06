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

package instanceinfo

import (
	"fmt"

	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

type ServiceReader struct {
	db         *dbengine.DBEngine
	conditions []string
}
type serviceWriter struct {
	db *dbengine.DBEngine
}

func (c *Client) ServiceReader() *ServiceReader {
	return &ServiceReader{db: c.db, conditions: []string{}}
}
func (r *ServiceReader) ByNamespace(ns string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("namespace = \"%s\"", ns))
	return r
}
func (r *ServiceReader) ByName(name string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("name = \"%s\"", name))
	return r
}
func (r *ServiceReader) ByOrgName(name string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("org_name = \"%s\"", name))
	return r
}
func (r *ServiceReader) ByOrgID(id string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("org_id = \"%s\"", id))
	return r
}
func (r *ServiceReader) ByProjectName(name string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("project_name = \"%s\"", name))
	return r
}
func (r *ServiceReader) ByProjectID(id string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("project_id = \"%s\"", id))
	return r
}
func (r *ServiceReader) ByApplicationName(name string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("application_name = \"%s\"", name))
	return r
}
func (r *ServiceReader) ByApplicationID(id string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("application_id = \"%s\"", id))
	return r
}
func (r *ServiceReader) ByRuntimeName(name string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("runtime_name = \"%s\"", name))
	return r
}
func (r *ServiceReader) ByRuntimeID(id string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("runtime_id = \"%s\"", id))
	return r
}
func (r *ServiceReader) ByService(name string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("service_name = \"%s\"", name))
	return r
}
func (r *ServiceReader) ByWorkspace(ws string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("workspace = \"%s\"", ws))
	return r
}
func (r *ServiceReader) ByServiceType(tp string) *ServiceReader {
	r.conditions = append(r.conditions, fmt.Sprintf("service_type = \"%s\"", tp))
	return r
}
func (r *ServiceReader) Do() ([]ServiceInfo, error) {
	serviceinfo := []ServiceInfo{}
	if err := r.db.Where(strutil.Join(r.conditions, " AND ", true)).Find(&serviceinfo).Error; err != nil {
		r.conditions = []string{}
		return nil, err
	}
	r.conditions = []string{}
	return serviceinfo, nil
}

func (c *Client) ServiceWriter() *serviceWriter {
	return &serviceWriter{db: c.db}
}
func (w *serviceWriter) Create(s *ServiceInfo) error {
	return w.db.Save(s).Error
}
func (w *serviceWriter) Update(s ServiceInfo) error {
	return w.db.Model(&s).Updates(s).Error
}
func (w *serviceWriter) Delete(ids ...uint64) error {
	return w.db.Delete(ServiceInfo{}, "id in (?)", ids).Error
}
