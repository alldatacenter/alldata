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

import "github.com/jinzhu/gorm"

// AlertNotifyTemplateDB .
type AlertNotifyTemplateDB struct {
	*gorm.DB
}

// QueryEnabledByTypesAndIndexes .
func (db *AlertNotifyTemplateDB) QueryEnabledByTypesAndIndexes(
	types, indexes []string) ([]*AlertNotifyTemplate, error) {
	var templates []*AlertNotifyTemplate
	if err := db.
		Where("alert_type IN (?)", types).
		Where("alert_index IN (?)", indexes).
		Where("enable=?", true).
		Find(&templates).Error; err != nil {
		return nil, err
	}
	return templates, nil
}
