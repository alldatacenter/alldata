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

package aop

import (
	"sync"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/services/reportsvc"
)

// tuneGroup 保存所有 tune chain
// 根据 类型、触发时机 初始化所有场景下的调用链
var tuneGroup aoptypes.TuneGroup
var once sync.Once
var initialized bool
var globalSDK aoptypes.SDK

func Initialize(bdl *bundle.Bundle, dbClient *dbclient.Client, report *reportsvc.ReportSvc) {
	once.Do(func() {
		initialized = true

		globalSDK.Bundle = bdl
		globalSDK.DBClient = dbClient
		globalSDK.Report = report
	})
}
