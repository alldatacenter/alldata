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

package cloud_account

import (
	"context"
	"encoding/base64"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
)

type CloudAccount struct {
	db        *dbclient.DBClient
	js        jsonstore.JsonStore
	kmskey    *kmstypes.CreateKeyResponse
	Kmsbundle *bundle.Bundle
}

type VendorAccount struct {
	Org         string
	Vendor      string
	AccessKey   string
	Description string
}

func New(db *dbclient.DBClient, js jsonstore.JsonStore) *CloudAccount {
	return &CloudAccount{db: db, js: js}
}

func (c *CloudAccount) KmsKey() (kmstypes.CreateKeyResponse, error) {
	if c.Kmsbundle == nil {
		c.Kmsbundle = bundle.New(bundle.WithKMS())
	}

	if c.kmskey != nil && c.kmskey.KeyMetadata.KeyID != "" {
		return *c.kmskey, nil
	}
	key := &kmstypes.CreateKeyResponse{}
	if err := c.js.Get(context.Background(), "/dice/ops/kmskey", &key); err != nil {
		if err != jsonstore.NotFoundErr {
			return kmstypes.CreateKeyResponse{}, err
		}
		var err error
		key, err = c.Kmsbundle.KMSCreateKey(apistructs.KMSCreateKeyRequest{
			CreateKeyRequest: kmstypes.CreateKeyRequest{
				PluginKind: kmstypes.PluginKind_DICE_KMS,
			},
		})
		if err != nil {
			return kmstypes.CreateKeyResponse{}, err
		}
		c.kmskey = key
		c.js.Put(context.Background(), "/dice/ops/kmskey", key)
	}
	return *key, nil
}

func (c *CloudAccount) List(org string) ([]VendorAccount, error) {
	akreader := c.db.OrgAKReader()
	aks, err := akreader.ByOrgID(org).Do()
	if err != nil {
		return nil, err
	}
	allaccounts := []VendorAccount{}
	for _, ak := range aks {
		account := VendorAccount{
			Org:         ak.OrgID,
			Vendor:      string(ak.Vendor),
			AccessKey:   ak.AccessKey,
			Description: ak.Description,
		}
		allaccounts = append(allaccounts, account)
	}
	return allaccounts, nil
}

func (c *CloudAccount) Create(org, vendor, ak, secret, desc string) error {
	akreader := c.db.OrgAKReader()

	orgaks, err := akreader.ByOrgID(org).ByVendors(vendor).Do()
	if err != nil {
		return err
	}
	if len(orgaks) != 0 {
		return fmt.Errorf("org: %s, vendor: %s  accessKey exists already", org, vendor)
	}

	kmskey, err := c.KmsKey()
	if err != nil {
		return err
	}
	kmssecret, err := c.Kmsbundle.KMSEncrypt(apistructs.KMSEncryptRequest{
		EncryptRequest: kmstypes.EncryptRequest{
			KeyID:           kmskey.KeyMetadata.KeyID,
			PlaintextBase64: base64.StdEncoding.EncodeToString([]byte(secret)),
		},
	})
	if err != nil {
		return err
	}
	akwriter := c.db.OrgAKWriter()
	orgak := dbclient.OrgAK{
		OrgID:       org,
		Vendor:      dbclient.VendorType(vendor),
		AccessKey:   ak,
		SecretKey:   kmssecret.CiphertextBase64,
		Description: desc,
	}
	if _, err := akwriter.Create(&orgak); err != nil {
		return err
	}
	return nil
}

func (c *CloudAccount) Delete(org, vendor, ak string) error {
	akreader := c.db.OrgAKReader()

	orgaks, err := akreader.ByOrgID(org).ByVendors(vendor).Do()
	if err != nil {
		return err
	}
	for _, ak_ := range orgaks {
		if ak_.AccessKey == ak {
			if err := c.db.OrgAKWriter().Delete(ak_.ID); err != nil {
				return err
			}
		}
	}
	return nil
}
