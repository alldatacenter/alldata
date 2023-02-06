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

package filesvc

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"io/ioutil"
	"path/filepath"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/kms/kmscrypto"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
	"github.com/erda-project/erda/pkg/storage"
	"github.com/erda-project/erda/pkg/strutil"
)

var (
	headerValueDispositionInline = func(fileType, filename string) string {
		if !conf.FileTypeCarryActiveContentAllowed() && strutil.Exist(conf.FileTypesCanCarryActiveContent(), strutil.TrimPrefixes(fileType, ".")) {
			return fmt.Sprintf("attachment; filename=%s", filename)
		}
		return fmt.Sprintf("inline; filename=%s", filename)
	}
)

func (svc *FileService) UploadFile(req apistructs.FileUploadRequest) (*apistructs.File, error) {
	// 校验文件大小
	if req.ByteSize > int64(conf.FileMaxUploadSize()) {
		return nil, apierrors.ErrUploadTooLargeFile.InvalidParameter(errors.Errorf("max file size: %s", conf.FileMaxUploadSize().String()))
	}

	// 处理文件元信息
	ext := filepath.Ext(req.FileNameWithExt)
	fileUUID := uuid.UUID()
	destFileName := strutil.Concat(fileUUID, ext)
	handledPath, err := svc.handleFilePath(destFileName)
	if err != nil {
		return nil, apierrors.ErrUploadFile.InvalidParameter(err)
	}

	// storager
	storager := svc.GetStorage()
	fileReader := req.FileReader
	var DEKCiphertextBase64 string // 使用 CMK 加密后的 DEK 密文

	// 加密存储（信封加密）
	if req.Encrypt {
		fileBytes, err := ioutil.ReadAll(req.FileReader)
		if err != nil {
			return nil, apierrors.ErrUploadFileEncrypt.InternalError(err)
		}
		// get DEK
		generateDEKResp, err := svc.bdl.KMSGenerateDataKey(apistructs.KMSGenerateDataKeyRequest{
			GenerateDataKeyRequest: kmstypes.GenerateDataKeyRequest{
				KeyID: GetKMSKey(),
			},
		})
		if err != nil {
			return nil, apierrors.ErrUploadFileEncrypt.InternalError(err)
		}
		DEKCiphertextBase64 = generateDEKResp.CiphertextBase64
		DEK, err := base64.StdEncoding.DecodeString(generateDEKResp.PlaintextBase64)
		if err != nil {
			return nil, apierrors.ErrUploadFileEncrypt.InternalError(err)
		}
		// encrypt file content
		ciphertext, err := kmscrypto.AesGcmEncrypt(DEK, fileBytes, generateAesGemAdditionalData(req.From))
		if err != nil {
			return nil, apierrors.ErrUploadFileEncrypt.InternalError(err)
		}
		fileReader = ioutil.NopCloser(bytes.NewBuffer(ciphertext))
	}

	if err := storager.Write(handledPath, fileReader); err != nil {
		return nil, apierrors.ErrUploadFile.InternalError(err)
	}

	// store to db
	file := dao.File{
		UUID:             fileUUID,
		DisplayName:      req.FileNameWithExt,
		Ext:              ext,
		ByteSize:         req.ByteSize,
		StorageType:      storager.Type(),
		FullRelativePath: handledPath,
		From:             req.From,
		Creator:          req.Creator,
		ExpiredAt:        req.ExpiredAt,
	}
	file.Extra = handleFileExtra(file)
	file.Extra.IsPublic = req.IsPublic
	if req.Encrypt {
		file.Extra.Encrypt = req.Encrypt
		file.Extra.KMSKeyID = GetKMSKey()
		file.Extra.DEKCiphertextBase64 = DEKCiphertextBase64
	}
	if err := svc.db.CreateFile(&file); err != nil {
		return nil, apierrors.ErrUploadFile.InternalError(err)
	}

	return convert(&file), nil
}

func (svc *FileService) GetStorage(typ ...storage.Type) storage.Storager {
	var storageType storage.Type
	if len(typ) > 0 {
		storageType = typ[0]
	}

	switch storageType {
	case storage.TypeOSS:
		goto createOSS
	case storage.TypeFileSystem:
		goto createFS
	default:
		if conf.OSSEndpoint() != "" {
			goto createOSS
		}
		goto createFS
	}

createOSS:
	// TODO 这里统一用环境变量中的 oss 配置初始化客户端，如果 oss endpoint 或 bucket 发生过改变，之前通过 oss 上传的文件会下载不到。
	// 有两个方案：
	// 1. 数据迁移，将 oss 数据从老 bucket 移动到新 bucket
	// 2. 环境变量维护多份 oss 配置，根据 file 记录里的 endpoint 和 bucket 查询正确的 oss 配置
	return storage.NewOSS(conf.OSSEndpoint(), conf.OSSAccessID(), conf.OSSAccessSecret(), conf.OSSBucket(), nil, nil)
createFS:
	return storage.NewFS()
}

func checkPath(path string) error {
	// 校验 path
	// 防止恶意 path 获取到任何文件
	if strutil.Contains(path, "../", "./") {
		return errors.Errorf("invalid path: %s", path)
	}
	return nil
}

func (svc *FileService) handleFilePath(path string) (string, error) {

	if err := checkPath(path); err != nil {
		return "", err
	}

	// 加上指定前缀，限制文件访问路径
	switch svc.GetStorage().Type() {
	case storage.TypeFileSystem:
		path = filepath.Join(conf.StorageMountPointInContainer(), path)
	case storage.TypeOSS:
		path = filepath.Join(conf.OSSPathPrefix(), path)
		path = strings.TrimPrefix(path, "/")
	}

	return path, nil
}

func getFileDownloadLink(uuid string) string {
	return fmt.Sprintf("%s/api/files/%s", conf.UIPublicURL(), uuid)
}

func handleFileExtra(file dao.File) dao.FileExtra {
	var extra dao.FileExtra
	if file.StorageType == storage.TypeOSS {
		extra.OSSSnapshot.OSSEndpoint = conf.OSSEndpoint()
		extra.OSSSnapshot.OSSBucket = conf.OSSBucket()
	}
	return extra
}

func convert(file *dao.File) *apistructs.File {
	return &apistructs.File{
		ID:          uint64(file.ID),
		UUID:        file.UUID,
		DisplayName: file.DisplayName,
		ByteSize:    file.ByteSize,
		DownloadURL: getFileDownloadLink(file.UUID),
		Type:        apistructs.GetFileTypeByExt(file.Ext),
		From:        file.From,
		Creator:     file.Creator,
		CreatedAt:   file.CreatedAt,
		UpdatedAt:   file.UpdatedAt,
		ExpiredAt:   file.ExpiredAt,
	}
}

func generateAesGemAdditionalData(fileFrom string) []byte {
	return []byte(fileFrom)
}
