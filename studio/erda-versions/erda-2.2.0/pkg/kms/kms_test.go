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

package kms

//import (
//	"context"
//	"encoding/base64"
//	"fmt"
//	"io/ioutil"
//	"testing"
//
//	"github.com/davecgh/go-spew/spew"
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/pkg/kms/kmscrypto"
//	"github.com/erda-project/erda/pkg/kms/kmstypes"
//)
//
//func getDiceKmsWithEtcdStore() kmstypes.Plugin {
//	// 获取一个 data key
//	m, err := GetManager(WithStoreConfigs(map[string]string{
//		"ETCD_ENDPOINTS": "localhost:2379",
//	}))
//	if err != nil {
//		panic(err)
//	}
//
//	plugin, err := m.GetPlugin("DICE_KMS", "ETCD")
//	if err != nil {
//		panic(err)
//	}
//
//	return plugin
//}
//
//func TestKMS(t *testing.T) {
//	m, err := GetManager()
//	assert.NoError(t, err)
//	spew.Dump(m.plugins)
//}
//
//func TestKMS_Dice(t *testing.T) {
//	plugin := getDiceKmsWithEtcdStore()
//	ctx := context.Background()
//
//	// create
//	createKeyResp, err := plugin.CreateKey(ctx,
//		&kmstypes.CreateKeyRequest{
//			PluginKind:            kmstypes.PluginKind_DICE_KMS,
//			CustomerMasterKeySpec: kmstypes.CustomerMasterKeySpec_SYMMETRIC_DEFAULT,
//			KeyUsage:              kmstypes.KeyUsage_ENCRYPT_DECRYPT,
//			Description:           "just test dice kms",
//		})
//	assert.NoError(t, err)
//	spew.Dump("create key", createKeyResp.KeyMetadata)
//
//	// describe
//	keyID := createKeyResp.KeyMetadata.KeyID
//	descKeyResp, err := plugin.KmsDescribeKey(ctx, &kmstypes.DescribeKeyRequest{KeyID: keyID})
//	assert.NoError(t, err)
//	spew.Dump("describe key", descKeyResp.KeyMetadata)
//
//	// list keys
//	listKeysResp, err := plugin.ListKeys(ctx, &kmstypes.ListKeysRequest{})
//	assert.NoError(t, err)
//	spew.Dump("list keys", listKeysResp)
//
//	// encrypt
//	const helloWorld = "hello world"
//	plaintextBase64 := base64.StdEncoding.EncodeToString([]byte(helloWorld))
//	encryptResp, err := plugin.Encrypt(ctx, &kmstypes.EncryptRequest{
//		KeyID:           keyID,
//		PlaintextBase64: plaintextBase64,
//	})
//	assert.NoError(t, err)
//	spew.Dump("ciphertext", encryptResp.CiphertextBase64)
//
//	// decrypt
//	decryptResp, err := plugin.Decrypt(ctx, &kmstypes.DecryptRequest{
//		KeyID:            keyID,
//		CiphertextBase64: encryptResp.CiphertextBase64,
//	})
//	assert.NoError(t, err)
//	assert.Equal(t, plaintextBase64, decryptResp.PlaintextBase64)
//}
//
//func TestKMS_Dice_RotateKeyVersion(t *testing.T) {
//	plugin := getDiceKmsWithEtcdStore()
//	ctx := context.Background()
//
//	// 生成一个 CMK
//	createKeyResp, err := plugin.CreateKey(ctx, &kmstypes.CreateKeyRequest{
//		PluginKind:            kmstypes.PluginKind_DICE_KMS,
//		CustomerMasterKeySpec: kmstypes.CustomerMasterKeySpec_SYMMETRIC_DEFAULT,
//		KeyUsage:              kmstypes.KeyUsage_ENCRYPT_DECRYPT,
//		Description:           "rotate test",
//	})
//	assert.NoError(t, err)
//	keyID := createKeyResp.KeyMetadata.KeyID
//	primaryKeyVersionID := createKeyResp.KeyMetadata.PrimaryKeyVersionID
//	fmt.Println("origin primaryKeyVersionID:", primaryKeyVersionID)
//
//	// 加密数据，轮转密钥版本后再测试解密
//	const testData = "hello world"
//	testDataBase64 := base64.StdEncoding.EncodeToString([]byte(testData))
//	encryptResp, err := plugin.Encrypt(ctx, &kmstypes.EncryptRequest{
//		KeyID:           keyID,
//		PlaintextBase64: testDataBase64,
//	})
//	assert.NoError(t, err)
//	ciphertextBase64 := encryptResp.CiphertextBase64
//
//	// 轮转密钥版本
//	rotateResp, err := plugin.RotateKeyVersion(ctx, &kmstypes.RotateKeyVersionRequest{KeyID: keyID})
//	assert.NoError(t, err)
//	rotatePrimaryKeyVersionID := rotateResp.KeyMetadata.PrimaryKeyVersionID
//	fmt.Println("rotate primaryKeyVersionID:", rotatePrimaryKeyVersionID)
//	assert.NotEqual(t, primaryKeyVersionID, rotatePrimaryKeyVersionID)
//
//	// 解密老的密钥版本加密的数据
//	decryptResp, err := plugin.Decrypt(ctx, &kmstypes.DecryptRequest{
//		KeyID:            keyID,
//		CiphertextBase64: ciphertextBase64,
//	})
//	assert.NoError(t, err)
//	plaintextDecryptByte, err := base64.StdEncoding.DecodeString(string(decryptResp.PlaintextBase64))
//	plaintextDecrypt := string(plaintextDecryptByte)
//	assert.NoError(t, err)
//	fmt.Println("decrypt platintext:", plaintextDecrypt)
//	assert.Equal(t, testData, plaintextDecrypt)
//}
//
//func TestKMS_Dice_EnvelopeEncrypt(t *testing.T) {
//	plugin := getDiceKmsWithEtcdStore()
//	ctx := context.Background()
//
//	// 生成一个 CMK 用于加密 DEK
//	createKeyResp, err := plugin.CreateKey(ctx, &kmstypes.CreateKeyRequest{
//		PluginKind:            kmstypes.PluginKind_DICE_KMS,
//		CustomerMasterKeySpec: kmstypes.CustomerMasterKeySpec_SYMMETRIC_DEFAULT,
//		KeyUsage:              kmstypes.KeyUsage_ENCRYPT_DECRYPT,
//		Description:           "envelope test",
//	})
//	assert.NoError(t, err)
//	keyID := createKeyResp.KeyMetadata.KeyID
//
//	// 生成 DEK
//	generateDataKeyResp, err := plugin.GenerateDataKey(ctx, &kmstypes.GenerateDataKeyRequest{KeyID: keyID})
//	assert.NoError(t, err)
//	dek, err := base64.StdEncoding.DecodeString(generateDataKeyResp.PlaintextBase64)
//	assert.NoError(t, err)
//
//	// 本地使用 DEK 加密一个数据
//	// 加密 /etc/hosts
//	const etcHosts = "/etc/hosts"
//	hosts, err := ioutil.ReadFile(etcHosts)
//	assert.NoError(t, err)
//	additionalData := []byte(etcHosts)
//	ciphertext, err := kmscrypto.AesGcmEncrypt(dek, hosts, additionalData)
//	ioutil.WriteFile("/tmp/hosts_decrypted", ciphertext, 0644)
//	assert.NoError(t, err)
//	// 内存中清除明文 dek
//	dek = nil
//
//	// some time passed
//
//	// 解密
//
//	// 1. 解密 密文 DEK
//	decryptResp, err := plugin.Decrypt(ctx, &kmstypes.DecryptRequest{
//		KeyID:            keyID,
//		CiphertextBase64: generateDataKeyResp.CiphertextBase64,
//	})
//	assert.NoError(t, err)
//	assert.Equal(t, generateDataKeyResp.PlaintextBase64, decryptResp.PlaintextBase64)
//	dek2, err := base64.StdEncoding.DecodeString(decryptResp.PlaintextBase64)
//	assert.NoError(t, err)
//
//	// 2. 使用 DEK 解密 已加密的数据
//	// no additional data
//	_, err = kmscrypto.AesGcmDecrypt(dek2, ciphertext, nil)
//	assert.Error(t, err)
//	decryptHosts, err := kmscrypto.AesGcmDecrypt(dek2, ciphertext, additionalData)
//	assert.NoError(t, err)
//	fmt.Println("/etc/hosts", string(decryptHosts))
//}
