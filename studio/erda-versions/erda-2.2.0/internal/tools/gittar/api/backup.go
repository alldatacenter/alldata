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

package api

import (
	"errors"
	"os"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/helper"
	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
)

// Backup 添加到备份
func Backup(ctx *webcontext.Context) {
	branch := ctx.Param("*")
	format := "zip"
	request := &apistructs.RepoFiles{}

	err := ctx.BindJSON(&request)
	ref := request.CommitID
	if err != nil {
		ctx.AbortWithStatus(400, errors.New("request body parse failed"))
		return
	}
	if ref == "" {
		ctx.AbortWithString(400, "invalid ref ")
		return
	}
	_, err = ctx.Repository.GetCommitByAny(ref)
	if err != nil {
		ctx.AbortWithString(404, "ref not found "+ref)
		return
	}

	res, err := ctx.Service.GetBackupList(1, 20, ctx.Repository)
	if res.Total >= 3 {
		ctx.Abort(errors.New("备份数已达上限3"))
		return
	}
	path := helper.OutPutArchive(ctx, branch, format)
	f, err := os.Open(path)
	if err != nil {
		ctx.Abort(err)
		return
	}
	req := apistructs.FileUploadRequest{
		FileNameWithExt: branch + "." + format,
		FileReader:      f,
		From:            "backup",
		Creator:         ctx.User.Id,
	}
	_, err = ctx.Bundle.UploadFile(req, 300)
	if err != nil {
		ctx.Abort(err)
		helper.OutPutArchiveDelete(ctx, path)
		return
	}
	ctx.Service.AddBackupRecording(ctx.Repository, request.CommitID, request.Remark)
	helper.OutPutArchiveDelete(ctx, path)
	ctx.Success("")
}

// BackupList 获取备份列表
func BackupList(ctx *webcontext.Context) {
	pagReq := &PagingRequest{}
	pagReq.PageNo = ctx.GetQueryInt32("pageNo", 1)
	pagReq.PageSize = ctx.GetQueryInt32("pageSize", 10)
	resquest, err := ctx.Service.GetBackupList(pagReq.PageNo, pagReq.PageSize, ctx.Repository)
	if err != nil {
		ctx.Abort(err)
		return
	}
	ctx.Success(resquest)
}

// DeleteBackup 删除备份
func DeleteBackup(ctx *webcontext.Context) {
	uuid := ctx.Param("*")
	_, err := ctx.Service.DeleteBackupRecording(uuid)
	if err != nil {
		ctx.Abort(err)
		return
	}
	err = ctx.Bundle.DeleteDiceFile(uuid)
	if err != nil {
		ctx.Abort(err)
		return
	}
	ctx.Success("")
}
