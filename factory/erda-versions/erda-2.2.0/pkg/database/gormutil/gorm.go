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

package gormutil

import (
	"fmt"
	"reflect"
	"strings"

	"github.com/jinzhu/gorm"
)

// GetFieldToColumnMap .
func GetFieldToColumnMap(typ reflect.Type) map[string]string {
	if typ.Kind() != reflect.Struct {
		return nil
	}
	m := make(map[string]string)
loop:
	for i, n := 0, typ.NumField(); i < n; i++ {
		field := typ.Field(i)
		dbtag := field.Tag.Get("gorm")
		if len(dbtag) > 0 {
			for _, item := range strings.Split(dbtag, ";") {
				kv := strings.SplitN(item, ":", 2)
				if len(kv) == 2 {
					k, v := strings.TrimSpace(kv[0]), strings.TrimSpace(kv[1])
					if k == "column" && len(v) > 0 {
						m[field.Name] = v
						continue loop
					}
				}
			}
		}
		m[field.Name] = strings.ToLower(field.Name)
	}
	return m
}

// GetQueryFilterByFields .
func GetQueryFilterByFields(db *gorm.DB, fieldColumns map[string]string, fields map[string]interface{}) (*gorm.DB, error) {
	for name, value := range fields {
		col, ok := fieldColumns[name]
		if !ok {
			return nil, fmt.Errorf("unknown %q", name)
		}
		db = db.Where(fmt.Sprintf("`%s`=?", col), value)
	}
	return db, nil
}

// GetQueryLikeFilterByFields .
func GetQueryLikeFilterByFields(db *gorm.DB, fieldColumns map[string]string, fields map[string]interface{}) (*gorm.DB, error) {
	for name, value := range fields {

		col, ok := fieldColumns[name]
		if !ok {
			return nil, fmt.Errorf("unknown %q", name)
		}
		db = db.Where(fmt.Sprintf("`%s` like ?", col), "%"+value.(string)+"%")
	}
	return db, nil
}

// GetQueryFilterByFields .
func GetQueryFilterByTypeFields(db *gorm.DB, typ interface{}, fields map[string]interface{}) (*gorm.DB, error) {
	type TableNamer interface {
		TableName() string
	}
	if table, ok := typ.(TableNamer); ok {
		db = db.Table(table.TableName())
	}
	return GetQueryFilterByFields(db, GetFieldToColumnMap(reflect.TypeOf(typ)), fields)
}
