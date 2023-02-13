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

package apierrors

import (
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

var (
	ErrCreateRelease                   = err("ErrCreateRelease", "创建Release失败")
	ErrUpdateRelease                   = err("ErrUpdateRelease", "更新Release失败")
	ErrDeleteRelease                   = err("ErrDeleteRelease", "删除Release失败")
	ErrDownloadRelease                 = err("ErrDownloadRelease", "下载Release Yaml失败")
	ErrGetRelease                      = err("ErrGetRelease", "获取Release失败")
	ErrListRelease                     = err("ErrListRelease", "获取Release列表失败")
	ErrFormalRelease                   = err("ErrFormalRelease", "转正Release失败")
	ErrParseReleaseFile                = err("ErrParseReleaseFile", "解析Release文件失败")
	ErrCheckReleaseVersion             = err("ErrCheckReleaseVersion", "校验Version失败")
	ErrGetYAML                         = err("ErrGetYAML", "获取Dice YAML失败")
	ErrGetIosPlist                     = err("ErrGetIosPlist", "获取Ios Plist文件失败")
	ErrPutOnRelease                    = err("ErrPutOnRelease", "上架制品失败")
	ErrPutOffRelease                   = err("ErrPutOffRelease", "下架制品失败")
	ErrCreateImage                     = err("ErrCreateImage", "添加镜像失败")
	ErrUpdateImage                     = err("ErrUpdateImage", "更新镜像失败")
	ErrDeleteImage                     = err("ErrDeleteImage", "删除镜像失败")
	ErrGetImage                        = err("ErrGetImage", "获取镜像失败")
	ErrListImage                       = err("ErrListImage", "获取镜像列表失败")
	ErrCreateExtension                 = err("ErrCreateExtension", "添加扩展失败")
	ErrQueryExtension                  = err("ErrQueryExtension", "查询扩展失败")
	ErrCreateExtensionVersion          = err("ErrCreateExtensionVersion", "添加扩展版本失败")
	ErrQueryExtensionVersion           = err("ErrQueryExtensionVersion", "查询扩展版本失败")
	ErrCreatePipelineTemplate          = err("ErrCreateTemplate", "添加模板失败")
	ErrQueryPipelineTemplate           = err("ErrQueryTemplate", "查询模板失败")
	ErrCreatePipelineTemplateVersion   = err("ErrCreateTemplateVersion", "添加模板版本失败")
	ErrQueryPipelineTemplateVersion    = err("ErrQueryTemplateVersion", "查询模板版本失败")
	ErrRenderPipelineTemplate          = err("ErrQueryTemplateVersion", "模板渲染失败")
	ErrQueryPublishItem                = err("ErrQueryPublishItem", "查询发布内容失败")
	ErrCreatePublishItem               = err("ErrCreatePublishItem", "创建发布内容失败")
	ErrGetPublishItem                  = err("ErrGetPublishItem", "获取发布内容详情失败")
	ErrUpdatePublishItem               = err("ErrUpdatePublishItem", "更新发布内容失败")
	ErrDeletePublishItem               = err("ErrDeletePublishItem", "删除发布内容失败")
	ErrQueryPublishItemVersion         = err("ErrQueryPublishItemVersion", "查询发布版本失败")
	ErrCreatePublishItemVersion        = err("ErrCreatePublishItemVersion", "创建发布版本失败")
	ErrCreateOffLinePublishItemVersion = err("ErrCreateOffLinePublishItemVersion", "推送离线包失败")
	ErrUpdatePublishItemVersion        = err("ErrUpdatePublishItemVersion", "更新发布版本失败")
	ErrDeletePublishItemVersion        = err("ErrDeletePublishItemVersion", "删除发布版本失败")
	ErrSetPublishItemVersionStatus     = err("ErrSetPublishItemVersionStatus", "更新版本状态失败")
	ErrGetMonitorKeys                  = err("ErrGetMonitorKeys", "获取监控key失败")

	ErrCreateBlacklist = err("ErrCreateBlacklist", "添加黑名单失败")
	ErrGetBlacklist    = err("ErrCreateBlacklist", "查询黑名单失败")
	ErrDeleteBlacklist = err("ErrDeleteBlacklist", "删除黑名单失败")

	ErrCreateEraselist = err("ErrCreateEraselist", "添加数据擦除失败")

	ErrSecurity    = err("ErrSecurity", "security error")
	ErrUpdateErase = err("ErrUpdateErase", "request error")

	ErrSratisticsErrList       = err("ErrSratisticsErrList", "获取错误列表失败")
	ErrSratisticsErrTrend      = err("ErrSratisticsErrTrend", "获取错误趋势失败")
	ErrSratisticsTotalTrend    = err("ErrSratisticsTotalTrend", "获取整体趋势失败")
	ErrSratisticsVersionDetail = err("ErrSratisticsVersionDetail", "获取版本详情明细数据失败")
	ErrSratisticsChannelDetail = err("ErrSratisticsChannelDetail", "获取渠道详情明细数据失败")

	ErrCrashRateList = err("ErrCrashRateList", "获取崩溃率失败")

	ErrCreateReleaseRule = err("ErrCreateReleaseRule", "创建制品规则失败")
	ErrListReleaseRule   = err("ErrListReleaseRule", "列举制品规则失败")
	ErrUpdateReleaseRule = err("ErrUpdateReleaseRule", "更新制品规则失败")
	ErrDeleteReleaseRule = err("ErrDeleteReleaseRule", "删除制品规则失败")
	ErrAuthReleaseRule   = err("ErrAuthReleaseRule", "制品规则鉴权失败")
	ErrGetReleaseRule    = err("ErrGetReleaseRule", "获取制品规则失败")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
