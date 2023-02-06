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

// MetricExpression
type MetricExpressionDB struct {
	*gorm.DB
}

func (db *MetricExpressionDB) GetAllMetricExpression(pageNo, pageSize int64) ([]*MetricExpression, error) {
	var expressions []*MetricExpression
	err := db.Where("enable = ?", true).
		Offset((pageNo - 1) * pageSize).Limit(pageSize).
		Find(&expressions).Error
	if err != nil {
		return nil, err
	}
	return expressions, nil
}
