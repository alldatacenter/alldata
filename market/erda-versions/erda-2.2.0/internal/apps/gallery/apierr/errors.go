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

package apierr

import (
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

var (
	ListOpusTypes    = err("ListOpusTypes", "查询 Opus 类型列表失败")
	ListOpus         = err("ListOpus", "查询 Opus 失败")
	ListOpusVersions = err("ListOpusVersions", "查询 Opus 版本详情失败")
	PutOnArtifacts   = err("PutOnArtifacts", "上架项目制品失败")
	PutOffArtifacts  = err("PutOffArtifacts", "下架项目制品失败")
	PutOnExtension   = err("PutOnExtension", "上架 Extension 失败")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
