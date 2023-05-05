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
	"context"
	"fmt"
	"sync"
	"time"

	"github.com/coreos/etcd/clientv3"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
)

var initKmsOnce sync.Once
var kmsKey string

const etcdKmsFilesCMK = "/dice/cmdb/files/kms/key"

type FileService struct {
	db         *dao.DBClient
	bdl        *bundle.Bundle
	etcdClient *etcd.Store
}

// Option 定义 FileService 配置选项
type Option func(*FileService)

// New 新建 Issue 实例
func New(options ...Option) *FileService {
	svc := &FileService{}
	for _, op := range options {
		op(svc)
	}

	initKmsOnce.Do(func() {
		// 检查 etcd 是否有 key
		kv, err := svc.etcdClient.Get(context.Background(), etcdKmsFilesCMK)
		if err != nil {
			if err.Error() == "not found" {
				ApplyKmsCmk(svc)
				return
			}
			panic(err)
		}
		kmsKey = string(kv.Value)
		if len(kmsKey) == 0 {
			ApplyKmsCmk(svc)
			return
		} else {
			_, err = svc.bdl.KMSRotateKeyVersion(apistructs.KMSRotateKeyVersionRequest{
				RotateKeyVersionRequest: kmstypes.RotateKeyVersionRequest{
					KeyID: kmsKey,
				},
			})
			if err != nil {
				panic(fmt.Errorf("dop files kms cmk rotate key version failed, keyID: %s, err: %v", kmsKey, err))
			}
		}
		_, err = svc.bdl.KMSDescribeKey(apistructs.KMSDescribeKeyRequest{
			DescribeKeyRequest: kmstypes.DescribeKeyRequest{
				KeyID: kmsKey,
			},
		})
		if err != nil {
			logrus.Errorf("dop files kms cmk describe failed, keyID: %s, err: %v", kmsKey, err)
			ApplyKmsCmk(svc)
			return
		}
		logrus.Infof("cmdb files kms cmk: %s", kmsKey)
		return
	})

	// clean expired files
	go func() {
		ticker := time.NewTicker(time.Minute)
		for range ticker.C {
			_ = svc.CleanExpiredFiles()
		}
	}()

	return svc
}

// WithDBClient 配置 FileService 数据库选项
func WithDBClient(db *dao.DBClient) Option {
	return func(svc *FileService) {
		svc.db = db
	}
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(svc *FileService) {
		svc.bdl = bdl
	}
}

func WithEtcdClient(etcdClient *etcd.Store) Option {
	return func(svc *FileService) {
		svc.etcdClient = etcdClient
	}
}

func GetKMSKey() string {
	return kmsKey
}

func ApplyKmsCmk(svc *FileService) {
	kmsResp, err := svc.bdl.KMSCreateKey(apistructs.KMSCreateKeyRequest{
		CreateKeyRequest: kmstypes.CreateKeyRequest{
			PluginKind:            kmstypes.PluginKind_DICE_KMS,
			CustomerMasterKeySpec: kmstypes.CustomerMasterKeySpec_SYMMETRIC_DEFAULT,
			KeyUsage:              kmstypes.KeyUsage_ENCRYPT_DECRYPT,
			Description:           "cmdb files kms key",
		},
	})
	if err != nil {
		panic(err)
	}
	keyID := kmsResp.KeyMetadata.KeyID
	defer func() {
		kmsKey = keyID
		logrus.Infof("cmdb files kms cmk: %s", kmsKey)
	}()
	_, err = svc.etcdClient.GetClient().Txn(context.Background()).
		If(clientv3.Compare(clientv3.Version(etcdKmsFilesCMK), "=", 0)).
		Then(clientv3.OpPut(etcdKmsFilesCMK, keyID)).
		Commit()
	if err != nil {
		panic(err)
	}
	return
}
