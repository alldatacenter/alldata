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

package bundle

//import (
//	"encoding/base64"
//	"os"
//	"testing"
//
//	"github.com/davecgh/go-spew/spew"
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/apistructs"
//	"github.com/erda-project/erda/pkg/kms/kmstypes"
//)
//
//func TestBundle_KMS(t *testing.T) {
//	_ = os.Setenv("KMS_ADDR", "localhost:3082")
//	b := New(WithKMS())
//
//	// create key
//	key, err := b.KMSCreateKey(apistructs.KMSCreateKeyRequest{
//		CreateKeyRequest: kmstypes.CreateKeyRequest{
//			PluginKind: kmstypes.PluginKind_DICE_KMS,
//		},
//	})
//	assert.NoError(t, err)
//	spew.Dump("create key data", key)
//
//	const plaintext = "hello world"
//	plaintextBase64 := base64.StdEncoding.EncodeToString([]byte(plaintext))
//
//	// describe key
//	descData, err := b.KMSDescribeKey(apistructs.KMSDescribeKeyRequest{
//		DescribeKeyRequest: kmstypes.DescribeKeyRequest{
//			KeyID: key.KeyMetadata.KeyID,
//		},
//	})
//	assert.NoError(t, err)
//	spew.Dump("describe data", descData)
//	assert.Equal(t, descData.KeyMetadata.KeyID, key.KeyMetadata.KeyID)
//
//	// encrypt
//	encryptData, err := b.KMSEncrypt(apistructs.KMSEncryptRequest{
//		EncryptRequest: kmstypes.EncryptRequest{
//			KeyID:           key.KeyMetadata.KeyID,
//			PlaintextBase64: plaintextBase64,
//		},
//	})
//	assert.NoError(t, err)
//	spew.Dump("encrypt data", encryptData)
//
//	// decrypt
//	decryptData, err := b.KMSDecrypt(apistructs.KMSDecryptRequest{
//		DecryptRequest: kmstypes.DecryptRequest{
//			KeyID:            key.KeyMetadata.KeyID,
//			CiphertextBase64: encryptData.CiphertextBase64,
//		},
//	})
//	assert.NoError(t, err)
//	spew.Dump("decrypt data", decryptData)
//	assert.Equal(t, plaintextBase64, decryptData.PlaintextBase64)
//
//	// generate data key
//	generateData, err := b.KMSGenerateDataKey(apistructs.KMSGenerateDataKeyRequest{
//		GenerateDataKeyRequest: kmstypes.GenerateDataKeyRequest{
//			KeyID: key.KeyMetadata.KeyID,
//		},
//	})
//	assert.NoError(t, err)
//	spew.Dump("generate resp", generateData)
//
//	// rotate key version
//	rotateData, err := b.KMSRotateKeyVersion(apistructs.KMSRotateKeyVersionRequest{
//		RotateKeyVersionRequest: kmstypes.RotateKeyVersionRequest{
//			KeyID: key.KeyMetadata.KeyID,
//		},
//	})
//	assert.NoError(t, err)
//	spew.Dump("rotate data", rotateData)
//	assert.Equal(t, rotateData.KeyMetadata.KeyID, key.KeyMetadata.KeyID)
//	assert.NotEqual(t, rotateData.KeyMetadata.PrimaryKeyVersionID, key.KeyMetadata.PrimaryKeyVersionID)
//
//	// decrypt old data
//	decryptData2, err := b.KMSDecrypt(apistructs.KMSDecryptRequest{
//		DecryptRequest: kmstypes.DecryptRequest{
//			KeyID:            key.KeyMetadata.KeyID,
//			CiphertextBase64: encryptData.CiphertextBase64,
//		},
//	})
//	assert.NoError(t, err)
//	spew.Dump("decrypt data 2", decryptData)
//	assert.Equal(t, plaintextBase64, decryptData2.PlaintextBase64)
//}
