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

package notify_record

import (
	"encoding/json"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/tools/monitor/notify/template/db"
	"github.com/erda-project/erda/internal/tools/monitor/notify/template/model"
	"github.com/erda-project/erda/internal/tools/monitor/notify/template/query"
)

func (p *provider) invoke(key []byte, value []byte, topic *string, timestamp time.Time) error {
	notifyRecord := model.NotifyRecord{}
	if err := json.Unmarshal(value, notifyRecord); err != nil {
		return err
	}
	record := query.ToNotifyRecord(&notifyRecord)
	sqlRecord := &db.NotifyRecord{}
	err := p.mysql.Model(&db.NotifyRecord{}).Where("notify_id = ?", notifyRecord.NotifyId).First(sqlRecord).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return err
	}
	if sqlRecord.NotifyId == "" {
		err := p.mysql.Create(record).Error
		return err
	} else {
		err := p.mysql.Model(&db.NotifyRecord{}).Where("notify_id = ?", notifyRecord.NotifyId).Updates(map[string]interface{}{
			"notify_id":   record.NotifyId,
			"notify_name": record.NotifyName,
			"notify_time": record.NotifyTime,
			"title":       record.Title,
		}).Error
		return err
	}
}
