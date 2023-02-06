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
	"context"
	"fmt"
	"os"
	"time"

	"gorm.io/gorm/logger"
)

type SQLCollector struct {
	logger.Interface
	filename string
}

func NewSQLCollector(filename string, baseLogger logger.Interface) (*SQLCollector, error) {
	if baseLogger == nil {
		baseLogger = &logger.Recorder
	}

	file, err := os.OpenFile(filename, os.O_CREATE|os.O_TRUNC|os.O_RDWR, 0644)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	return &SQLCollector{
		Interface: baseLogger,
		filename:  filename,
	}, nil
}

func (c SQLCollector) Trace(ctx context.Context, begin time.Time, fc func() (string, int64), err error) {
	c.Interface.Trace(ctx, begin, fc, err)
	sql, _ := fc()
	c.collect(begin, sql)
}

func (c SQLCollector) collect(begin time.Time, sql string) {
	if file, err := os.OpenFile(c.filename, os.O_CREATE|os.O_APPEND|os.O_RDWR, 0644); err == nil {
		_, _ = fmt.Fprintf(file, "/*-BEGIN: %s-*/\n", begin.Format(time.RFC3339))
		_, _ = file.WriteString(sql + "  /*-LINE END-*/\n")
	}
}
