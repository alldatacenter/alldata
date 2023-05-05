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

package endpoints

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"path/filepath"
	"strconv"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/core/legacy/services/filesvc"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	headerContentType          = "Content-Type"
	headerValueApplicationJSON = "application/json"
)

// UploadFile 上传文件至存储
func (e *Endpoints) UploadFile(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// check the user login
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUploadFile.NotLogin().ToResp(), nil
	}

	// check the size
	if r.ContentLength > int64(conf.FileMaxUploadSize()) {
		return nil, apierrors.ErrUploadTooLargeFile.InvalidParameter(errors.Errorf("max file size: %s", conf.FileMaxUploadSize().String()))
	}

	// get the file
	if err := r.ParseMultipartForm(int64(conf.FileMaxMemorySize())); err != nil {
		return nil, apierrors.ErrUploadFile.InvalidParameter(errors.Errorf("err: %s", err))
	}
	formFile, fileHeader, err := r.FormFile("file")
	if err != nil {
		return nil, apierrors.ErrUploadFile.InternalError(err)
	}
	defer formFile.Close()

	fileExtension := filepath.Ext(fileHeader.Filename)
	if !conf.FileTypeCarryActiveContentAllowed() && strutil.Exist(conf.FileTypesCanCarryActiveContent(), strutil.TrimPrefixes(fileExtension, ".")) {
		return nil, apierrors.ErrUploadFile.InvalidParameter(errors.Errorf("cannot upload file with type: %s", fileExtension))
	}
	// get params
	const (
		paramFileFrom  = "fileFrom"
		paramFileFrom2 = "from"
		paramPublic    = "public"
		paramEncrypt   = "encrypt"
		paramExpiredIn = "expiredIn" // such as "300ms", "-1.5h" or "2h45m". "0" means it doesn't expire.
	)
	fileFrom := r.URL.Query().Get(paramFileFrom)
	if fileFrom == "" {
		fileFrom = r.URL.Query().Get(paramFileFrom2)
	}
	public, _ := strconv.ParseBool(r.URL.Query().Get(paramPublic))
	encrypt, _ := strconv.ParseBool(r.URL.Query().Get(paramEncrypt))
	expiredInStr := r.URL.Query().Get(paramExpiredIn)
	var expiredAt *time.Time
	if expiredInStr != "" {
		expiredIn, err := time.ParseDuration(expiredInStr)
		if err != nil {
			return nil, apierrors.ErrUploadFile.InvalidParameter(fmt.Sprintf("invalid expiredIn: %s", expiredIn))
		}
		if expiredIn != 0 {
			t := time.Now().Add(expiredIn)
			expiredAt = &t
		}
	}

	// 处理文件元信息
	if unescapeFilename, err := url.QueryUnescape(fileHeader.Filename); err == nil {
		fileHeader.Filename = unescapeFilename
	}

	// 上传文件
	file, err := e.fileSvc.UploadFile(apistructs.FileUploadRequest{
		FileNameWithExt: fileHeader.Filename,
		ByteSize:        fileHeader.Size,
		FileReader:      formFile,
		From:            fileFrom,
		IsPublic:        public,
		Creator:         identityInfo.UserID,
		Encrypt:         encrypt,
		ExpiredAt:       expiredAt,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(file, []string{file.Creator})
}

// DownloadFile 根据文件链接返回文件内容
func (e *Endpoints) DownloadFile(ctx context.Context, w http.ResponseWriter, r *http.Request, vars map[string]string) (err error) {
	statusCode := http.StatusInternalServerError
	defer func() {
		if err != nil {
			// set Content-Type before statusCode, or we will get text/plain
			w.Header().Set(headerContentType, headerValueApplicationJSON)
			w.WriteHeader(statusCode)
			var jsonResp apistructs.FileDownloadFailResponse
			jsonResp.Success = false
			jsonResp.Error.Code = "ErrDownloadFile"
			if apiErr, ok := err.(*errorresp.APIError); ok {
				jsonResp.Error.Msg = apiErr.Error()
				if jsonResp.Error.Msg == "" {
					jsonResp.Error.Msg = apiErr.Code()
				}
			} else {
				jsonResp.Error.Msg = err.Error()
			}
			if wErr := json.NewEncoder(w).Encode(&jsonResp); wErr != nil {
				logrus.Errorf("failed to write http response while download file failed, downloadErr: %v, writeRespErr: %v", err, wErr)
			}
			// err 已经在 response body 中返回，不需要 writerHandler 再次处理
			err = nil
		}
	}()

	// get from path variable
	uuid := vars["uuid"]
	if uuid == "" {
		// get from query param
		uuid = r.URL.Query().Get("file")
		if uuid == "" {
			statusCode = http.StatusBadRequest
			return errors.Errorf("no file specified")
		}
	}

	file, err := e.db.GetFileByUUID(uuid)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return apierrors.ErrDownloadFile.NotFound()
		}
		return apierrors.ErrDownloadFile.InvalidParameter(uuid)
	}

	// 校验用户登录
	if !file.Extra.IsPublic && !conf.DisableFileDownloadPermissionValidate() {
		_, err = user.GetIdentityInfo(r)
		if err != nil {
			return apierrors.ErrDownloadFile.NotLogin()
		}
	}

	if _, err := e.fileSvc.DownloadFile(w, file); err != nil {
		return err
	}

	return nil
}

// HeadFile 文件 HEAD 请求
func (e *Endpoints) HeadFile(ctx context.Context, w http.ResponseWriter, r *http.Request, vars map[string]string) (err error) {
	// get from path variable
	uuid := vars["uuid"]
	if uuid == "" {
		// get from query param
		uuid = r.URL.Query().Get("file")
		if uuid == "" {
			w.WriteHeader(http.StatusBadRequest)
			return nil
		}
	}

	file, err := e.db.GetFileByUUID(uuid)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			w.WriteHeader(http.StatusNotFound)
			return nil
		}
		w.WriteHeader(http.StatusBadRequest)
		return nil
	}

	// 校验用户登录
	if file.Extra.IsPublic == false {
		_, err = user.GetIdentityInfo(r)
		if err != nil {
			w.WriteHeader(http.StatusUnauthorized)
			return nil
		}
	}

	w.Header().Set(filesvc.HeaderContentLength, strconv.FormatInt(file.ByteSize, 10))

	return nil
}

// DeleteFile 删除文件
func (e *Endpoints) DeleteFile(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteFile.NotLogin().ToResp(), nil
	}
	// 只有内部调用允许删除文件
	if !CheckInternalPermission(identityInfo) {
		return apierrors.ErrDeleteFile.AccessDenied().ToResp(), nil
	}

	// 获取文件
	uuid := vars["uuid"]
	file, err := e.db.GetFileByUUID(uuid)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return apierrors.ErrDeleteFile.NotFound().ToResp(), nil
		}
		return apierrors.ErrDeleteFile.InvalidParameter(err).ToResp(), nil
	}

	// delete
	if err := e.fileSvc.DeleteFile(file); err != nil {
		return apierrors.ErrDeleteFile.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(nil)
}

func CheckInternalPermission(identityInfo apistructs.IdentityInfo) bool {
	if identityInfo.IsInternalClient() {
		return true
	}
	return isReservedInternalServiceAccount(identityInfo.UserID)
}

// isReservedInternalServiceAccount 是否为内部服务账号
func isReservedInternalServiceAccount(userID string) bool {
	// TODO: ugly code
	// all (1000,5000) users is reserved as internal service account
	if v, err := strutil.Atoi64(userID); err == nil {
		if v > 1000 && v < 5000 && userID != apistructs.SupportID {
			return true
		}
	}
	return false
}
