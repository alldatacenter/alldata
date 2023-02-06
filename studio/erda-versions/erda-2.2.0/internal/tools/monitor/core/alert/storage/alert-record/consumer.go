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

package alert_record

import (
	"encoding/json"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/adapt"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
)

func (p *provider) invoke(key []byte, value []byte, topic *string, timestamp time.Time) error {
	alertRecord := &adapt.AlertRecord{}
	if err := json.Unmarshal(value, alertRecord); err != nil {
		return err
	}
	record := &db.AlertRecord{}
	alertRecord.ToModel(record)
	sqlRecord := &db.AlertRecord{}
	err := p.mysql.Model(&db.AlertRecord{}).Where("group_id = ?", alertRecord.GroupID).First(sqlRecord).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return err
	}
	if sqlRecord.GroupID == "" {
		//create
		record.CreateTime = time.Now()
		record.UpdateTime = time.Now()
		err := p.mysql.Create(record).Error
		return err
	} else {
		//update
		err := p.mysql.Model(&db.AlertRecord{}).Where("group_id = ?", alertRecord.GroupID).Updates(map[string]interface{}{
			"alert_state": record.AlertState,
			"alert_type":  record.AlertType,
			"alert_index": record.AlertIndex,
			"alert_name":  record.AlertName,
			"alert_time":  record.AlertTime,
			"update_time": time.Now(),
		}).Error
		return err
	}
}
