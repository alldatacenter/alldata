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

// Package certificate 封装Certificate资源相关操作
package certificate

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/model"
)

// Certificate 资源对象操作封装
type Certificate struct {
	db  *dao.DBClient
	bdl *bundle.Bundle
}

// Option 定义 Certificate 对象的配置选项
type Option func(*Certificate)

// New 新建 Certificate 实例，通过 Certificate 实例操作企业资源
func New(options ...Option) *Certificate {
	p := &Certificate{}
	for _, op := range options {
		op(p)
	}
	return p
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(p *Certificate) {
		p.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(p *Certificate) {
		p.bdl = bdl
	}
}

// Create 创建Certificate
func (c *Certificate) Create(userID string, createReq *apistructs.CertificateCreateRequest) (*apistructs.CertificateDTO, error) {
	var (
		androidMarshal []byte
		iosMarshal     []byte
		err            error
	)
	// 参数合法性检查
	if createReq.Name == "" {
		return nil, errors.Errorf("failed to create certificate(name is empty)")
	}

	defer func() {
		// 删除本地生成的 keystore 文件
		_ = os.Remove("/tmp/debug.keystore")
		_ = os.Remove("/tmp/release.keystore")
	}()

	switch apistructs.CertificateType(createReq.Type) {
	case apistructs.AndroidCertificateType:
		androidInfo := createReq.AndroidInfo
		if androidInfo.IsManualCreate {
			manualInfo := androidInfo.ManualInfo
			if manualInfo.DebugKeyStore.UUID == "" || manualInfo.ReleaseKeyStore.UUID == "" {
				return nil, errors.Errorf("failed to create certificate(debugKeyStore or releaseKeyStore is empty)")
			}
		} else {
			if androidInfo.AutoInfo.Name == "" {
				return nil, errors.Errorf("failed to create certificate(auto create cn is empty)")
			}

			// 调用命令 keytool 自动生成证书
			baseKeyStore := fmt.Sprintf("CN=%s,OU=%s,O=%s,L=%s,ST=%s,C=%s",
				androidInfo.AutoInfo.Name,
				androidInfo.AutoInfo.OU,
				androidInfo.AutoInfo.Org,
				androidInfo.AutoInfo.City,
				androidInfo.AutoInfo.Province,
				androidInfo.AutoInfo.State,
			)

			// debug keystore
			debugCmd := fmt.Sprintf("keytool -genkey -alias %s -keyalg RSA -keystore /tmp/debug.keystore -deststoretype pkcs12 -keypass %s -storepass %s -validity 3650 -dname %s",
				androidInfo.AutoInfo.DebugKeyStore.Alias,
				androidInfo.AutoInfo.DebugKeyStore.KeyPassword,
				androidInfo.AutoInfo.DebugKeyStore.StorePassword,
				baseKeyStore,
			)

			cmd := exec.Command("/bin/sh", "-c", debugCmd)
			out, err := cmd.CombinedOutput()
			if err != nil {
				return nil, errors.Errorf("failed to exec debug keystore cmd, out:%+v, (%+v)", string(out), err)
			}

			// 上传文件
			fileByte, err := ioutil.ReadFile("/tmp/debug.keystore")
			if err != nil {
				return nil, errors.Errorf("failed to read debug.keystore file, (%+v)", err)
			}

			var uploadFileReq = apistructs.FileUploadRequest{
				FileNameWithExt: "debug.keystore",
				Creator:         userID,
				ByteSize:        int64(len(fileByte)),
				FileReader:      ioutil.NopCloser(bytes.NewReader(fileByte)),
			}

			debugFileInfo, err := c.bdl.UploadFile(uploadFileReq)
			if err != nil {
				return nil, errors.Errorf("failed to read upload debug.keystore file, (%+v)", err)
			}

			// release keystore
			releaseCmd := fmt.Sprintf("keytool -genkey -alias %s -keyalg RSA -keystore /tmp/release.keystore -deststoretype pkcs12 -keypass %s -storepass %s -validity 3650 -dname %s",
				androidInfo.AutoInfo.ReleaseKeyStore.Alias,
				androidInfo.AutoInfo.ReleaseKeyStore.KeyPassword,
				androidInfo.AutoInfo.ReleaseKeyStore.StorePassword,
				baseKeyStore,
			)

			cmd = exec.Command("/bin/sh", "-c", releaseCmd)
			out, err = cmd.CombinedOutput()
			if err != nil {
				return nil, errors.Errorf("failed to exec release keystore cmd, out:%+v, (%+v)", string(out), err)
			}

			// 上传文件
			fileByte, err = ioutil.ReadFile("/tmp/release.keystore")
			if err != nil {
				return nil, errors.Errorf("failed to read release.keystore file, (%+v)", err)
			}

			uploadFileReq.FileNameWithExt = "release.keystore"
			uploadFileReq.ByteSize = int64(len(fileByte))
			uploadFileReq.FileReader = ioutil.NopCloser(bytes.NewReader(fileByte))
			releaseFileInfo, err := c.bdl.UploadFile(uploadFileReq)
			if err != nil {
				return nil, errors.Errorf("failed to read upload release.keystore file, (%+v)", err)
			}

			androidInfo.ManualInfo.DebugKeyStore.StorePassword = androidInfo.AutoInfo.DebugKeyStore.StorePassword
			androidInfo.ManualInfo.DebugKeyStore.KeyPassword = androidInfo.AutoInfo.DebugKeyStore.KeyPassword
			androidInfo.ManualInfo.DebugKeyStore.UUID = debugFileInfo.UUID
			androidInfo.ManualInfo.DebugKeyStore.FileName = debugFileInfo.DisplayName
			androidInfo.ManualInfo.DebugKeyStore.Alias = androidInfo.AutoInfo.DebugKeyStore.Alias

			androidInfo.ManualInfo.ReleaseKeyStore.StorePassword = androidInfo.AutoInfo.ReleaseKeyStore.StorePassword
			androidInfo.ManualInfo.ReleaseKeyStore.KeyPassword = androidInfo.AutoInfo.ReleaseKeyStore.KeyPassword
			androidInfo.ManualInfo.ReleaseKeyStore.UUID = releaseFileInfo.UUID
			androidInfo.ManualInfo.ReleaseKeyStore.FileName = releaseFileInfo.DisplayName
			androidInfo.ManualInfo.ReleaseKeyStore.Alias = androidInfo.AutoInfo.ReleaseKeyStore.Alias
		}
		androidMarshal, err = json.Marshal(&androidInfo)
		if err != nil {
			return nil, err
		}
	case apistructs.IOSCertificateType:
		iosInfo := createReq.IOSInfo
		if iosInfo.KeyChainP12.UUID == "" ||
			iosInfo.DebugProvisionFile.UUID == "" ||
			iosInfo.ReleaseProvisionFile.UUID == "" {
			return nil, errors.Errorf("failed to create certificate(keyChainP12 or " +
				"debugProvisionFile or releaseProvisionFile is empty)")
		}
		iosMarshal, err = json.Marshal(&iosInfo)
		if err != nil {
			return nil, err
		}
	case apistructs.MessageCertificateType:
		if createReq.MessageInfo.UUID == "" {
			return nil, errors.Errorf("need messageInfo uuid")
		}
	default:
		return nil, errors.Errorf("failed to create certificate(type error)")
	}

	certificate, err := c.db.GetCertificateByOrgAndName(int64(createReq.OrgID), createReq.Name)
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
	}
	if certificate != nil {
		return nil, errors.Errorf("failed to create certificate(name already exists)")
	}

	// 添加Certificate至DB
	certificate = &model.Certificate{
		Name:     createReq.Name,
		Desc:     createReq.Desc,
		OrgID:    int64(createReq.OrgID),
		Android:  string(androidMarshal),
		Ios:      string(iosMarshal),
		Creator:  userID,
		Operator: userID,
		Type:     createReq.Type,
	}
	if err = c.db.CreateCertificate(certificate); err != nil {
		return nil, errors.Errorf("failed to insert certificate to db")
	}

	return c.convertToCertificateDTO(certificate), nil
}

// Update 更新Certificate
func (c *Certificate) Update(certificateID int64, updateReq *apistructs.CertificateUpdateRequest) error {
	// 检查待更新的certificate是否存在
	certificate, err := c.db.GetCertificateByID(certificateID)
	if err != nil {
		return errors.Wrap(err, "not exist certificate")
	}

	certificate.Desc = updateReq.Desc
	if err = c.db.UpdateCertificate(&certificate); err != nil {
		logrus.Errorf("failed to update certificate, (%v)", err)
		return errors.Errorf("failed to update certificate")
	}

	return nil
}

// Delete 删除Certificate
func (c *Certificate) Delete(certificateID, orgID int64) error {
	// 获取是否被引用
	count, err := c.db.GetCountByCertificateID(certificateID)
	if err != nil {
		return err
	}

	if count > 0 {
		return errors.New("certificate already referenced, can not delete!!!")
	}

	if err := c.db.DeleteCertificate(certificateID); err != nil {
		return errors.Errorf("failed to delete certificate, (%v)", err)
	}
	logrus.Infof("deleted certificate %d success", certificateID)

	return nil
}

// Get 获取Certificate
func (c *Certificate) Get(certificateID int64) (*apistructs.CertificateDTO, error) {
	certificate, err := c.db.GetCertificateByID(certificateID)
	if err != nil {
		return nil, errors.Wrap(err, "failed to get certificate info")
	}
	return c.convertToCertificateDTO(&certificate), nil
}

// ListAllCertificates 企业管理员可查看当前企业下所有Certificate，包括未加入的Certificate
func (c *Certificate) ListAllCertificates(params *apistructs.CertificateListRequest) (
	*apistructs.PagingCertificateDTO, error) {
	total, certificates, err := c.db.GetCertificatesByOrgIDAndName(int64(params.OrgID), params)
	if err != nil {
		return nil, errors.Errorf("failed to get certificates, (%v)", err)
	}

	// 转换成所需格式
	certificateDTOs := make([]apistructs.CertificateDTO, 0, len(certificates))
	for i := range certificates {
		certificateDTOs = append(certificateDTOs, *(c.convertToCertificateDTO(&certificates[i])))
	}

	return &apistructs.PagingCertificateDTO{Total: total, List: certificateDTOs}, nil
}

func (p *Certificate) convertToCertificateDTO(certificate *model.Certificate) *apistructs.CertificateDTO {
	var (
		androidInfo apistructs.AndroidCertificateDTO
		iosInfo     apistructs.IOSCertificateDTO
		messageInfo apistructs.CertificateFileDTO
	)
	_ = json.Unmarshal([]byte(certificate.Android), &androidInfo)
	_ = json.Unmarshal([]byte(certificate.Ios), &iosInfo)
	_ = json.Unmarshal([]byte(certificate.Message), &messageInfo)
	return &apistructs.CertificateDTO{
		ID:          uint64(certificate.ID),
		Type:        certificate.Type,
		Name:        certificate.Name,
		AndroidInfo: androidInfo,
		IOSInfo:     iosInfo,
		MessageInfo: messageInfo,
		Desc:        certificate.Desc,
		OrgID:       uint64(certificate.OrgID),
		Creator:     certificate.Creator,
		Operator:    certificate.Operator,
		CreatedAt:   certificate.CreatedAt,
		UpdatedAt:   certificate.UpdatedAt,
	}
}
