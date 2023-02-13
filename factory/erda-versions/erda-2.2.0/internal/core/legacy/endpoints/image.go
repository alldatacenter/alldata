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
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"strings"

	"github.com/gorilla/mux"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/core/legacy/utils"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	maxUploadSize = 10 << 20 // 10M
	avatarPrefix  = "avatars"
)

// UploadImage 上传图片
func (e *Endpoints) UploadImage(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 限制最大上传文件大小
	if err := r.ParseMultipartForm(maxUploadSize); err != nil {
		return apierrors.ErrUploadImage.InternalError(err).ToResp(), nil
	}

	// 获取上传文件
	file, header, err := r.FormFile("file")
	if err != nil {
		return apierrors.ErrUploadImage.InternalError(err).ToResp(), nil
	}
	defer file.Close()

	// 文件名改为uuid foo => uuid  foo.ext => uuid.ext
	name := uuid.UUID()
	if strutil.Contains(header.Filename, ".") {
		tmp := strutil.Split(header.Filename, ".")
		name = strutil.Concat(name, ".", tmp[len(tmp)-1])
	}

	if utils.IsOSS(conf.AvatarStorageURL()) { // 上传文件至OSS
		ossURL, err := url.Parse(conf.AvatarStorageURL())
		if err != nil {
			return apierrors.ErrUploadImage.InternalError(err).ToResp(), nil
		}
		bucketStr := strings.TrimPrefix(ossURL.Path, "/")
		bucket, err := e.ossClient.Bucket(bucketStr)
		if err != nil {
			return apierrors.ErrUploadImage.InternalError(err).ToResp(), nil
		}

		if err := bucket.PutObject(strutil.Concat(avatarPrefix, "/", name), file); err != nil {
			return apierrors.ErrUploadImage.InternalError(err).ToResp(), nil
		}
		url := fmt.Sprintf("https://%s.%s/%s/%s", bucketStr, ossURL.Host, avatarPrefix, name)

		return httpserver.OkResp(apistructs.ImageUploadResponseData{URL: url})
	}

	// 上传文件至网盘
	fileBytes, err := ioutil.ReadAll(file)
	if err != nil {
		return apierrors.ErrUploadImage.InternalError(err).ToResp(), nil
	}
	newFile, err := os.Create(filepath.Join("/", avatarPrefix, "/", name))
	if err != nil {
		return apierrors.ErrUploadImage.InternalError(err).ToResp(), nil
	}
	defer newFile.Close()
	if _, err := newFile.Write(fileBytes); err != nil {
		return apierrors.ErrUploadImage.InternalError(err).ToResp(), nil
	}
	uiPublicURL := conf.UIPublicURL()
	url := fmt.Sprintf("%s/api/images/%s", uiPublicURL, name)

	return httpserver.OkResp(apistructs.ImageUploadResponseData{URL: url})
}

// GetImage 获取image
func GetImage(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	imgExp := regexp.MustCompile("^[a-zA-Z0-9]+[a-zA-Z0-9.]*.(jpg|jpeg|png|gif)")
	if !imgExp.MatchString(vars["imageName"]) {
		w.WriteHeader(http.StatusNotFound)
		return
	}
	filename := strutil.Concat("/", avatarPrefix, "/", vars["imageName"])
	fileBytes, err := ioutil.ReadFile(filename)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		return
	}
	w.Header().Set("Content-Type", "image/jpeg")
	w.WriteHeader(http.StatusOK)
	w.Write(fileBytes)
	return
}
