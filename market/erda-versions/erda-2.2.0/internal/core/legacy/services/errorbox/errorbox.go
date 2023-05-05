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

package errorbox

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

// ErrorBox 错误日志操作封装
type ErrorBox struct {
	db *dao.DBClient
}

// Option 定义 Member 对象配置选项
type Option func(*ErrorBox)

// New 新建 ErrorBox 实例
func New(options ...Option) *ErrorBox {
	eb := &ErrorBox{}
	for _, op := range options {
		op(eb)
	}
	return eb
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(eb *ErrorBox) {
		eb.db = db
	}
}

// CreateOrUpdate 记录或更新错误日志
func (eb *ErrorBox) CreateOrUpdate(req apistructs.ErrorLogCreateRequest) error {
	occurrenceTime, err := req.FormartTime()
	if err != nil {
		return err
	}

	// 更新
	if req.DedupID != "" {
		errorLog, err := eb.db.GetErrorLogByDedupIndex(req.ResourceID, req.ResourceType, req.DedupID)
		if err != nil {
			return err
		}

		if errorLog != nil {
			errorLog.Level = req.Level
			errorLog.OccurrenceTime = *occurrenceTime
			errorLog.HumanLog = req.HumanLog
			errorLog.PrimevalLog = req.PrimevalLog
			if err := eb.db.UpdateErrorLog(errorLog); err != nil {
				return err
			}
			return nil
		}
	}

	// 创建
	if err := eb.db.CreateErrorLog(&model.ErrorLog{
		ResourceType:   req.ResourceType,
		ResourceID:     req.ResourceID,
		Level:          req.Level,
		OccurrenceTime: *occurrenceTime,
		HumanLog:       req.HumanLog,
		PrimevalLog:    req.PrimevalLog,
		DedupID:        req.DedupID,
	}); err != nil {
		return err
	}

	return nil
}

// BatchCreateErrorLogs 批量创建错误日志
func (eb *ErrorBox) BatchCreateErrorLogs(reqs []apistructs.Audit) error {
	return nil
}
