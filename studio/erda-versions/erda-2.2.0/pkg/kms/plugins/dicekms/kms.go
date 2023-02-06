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

package dicekms

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/kms/kmscrypto"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
	"github.com/erda-project/erda/pkg/kms/log"
	"github.com/erda-project/erda/pkg/strutil"
)

type additionalData struct {
	KeyID string
}

func init() {
	logrus.Debugf("begin register dice kms plugin...")
	err := kmstypes.RegisterPlugin(kmstypes.PluginKind_DICE_KMS, func(ctx context.Context) kmstypes.Plugin {
		return &Dice{}
	})
	if err != nil {
		logrus.Errorf("[alert] failed to register dice kms plugin, err: %v", err)
		os.Exit(1)
	}
}

type Dice struct {
	store kmstypes.Store
}

func (d *Dice) Kind() kmstypes.PluginKind {
	return kmstypes.PluginKind_DICE_KMS
}

func (d *Dice) SetStore(store kmstypes.Store) {
	d.store = store
}

func (d *Dice) CreateKey(ctx context.Context, req *kmstypes.CreateKeyRequest) (*kmstypes.CreateKeyResponse, error) {
	// plugin kind
	if req.PluginKind != kmstypes.PluginKind_DICE_KMS {
		return nil, fmt.Errorf("invalid pluginKind: %s, expect: %s", req.PluginKind, kmstypes.PluginKind_DICE_KMS)
	}

	// key spec
	if !req.CustomerMasterKeySpec.IsValid() {
		return nil, fmt.Errorf("not supported key spec: %s", req.CustomerMasterKeySpec)
	}

	// key usage
	if req.KeyUsage != kmstypes.KeyUsage_ENCRYPT_DECRYPT {
		return nil, fmt.Errorf("not supported key usage: %s", req.KeyUsage)
	}

	// write key to store
	primaryKeyVersion := kmstypes.KeyVersion{
		VersionID: uuid.UUID(),
	}
	switch req.CustomerMasterKeySpec {
	case kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_4096:
		if err := d.fillRsaKeyVersionByBits(&primaryKeyVersion, 4096); err != nil {
			return nil, err
		}
	case kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_3072:
		if err := d.fillRsaKeyVersionByBits(&primaryKeyVersion, 3072); err != nil {
			return nil, err
		}
	case kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_2048:
		if err := d.fillRsaKeyVersionByBits(&primaryKeyVersion, 2048); err != nil {
			return nil, err
		}
	case kmstypes.CustomerMasterKeySpec_SYMMETRIC_DEFAULT:
		symmetricKeyBytes, err := kmscrypto.GenerateAes256Key()
		if err != nil {
			return nil, fmt.Errorf("failed to generate symmetric key, err: %v", err)
		}
		symmetricKeyBase64 := base64.StdEncoding.EncodeToString(symmetricKeyBytes)
		primaryKeyVersion.SymmetricKeyBase64 = symmetricKeyBase64
	}
	key := kmstypes.Key{
		PluginKind:        kmstypes.PluginKind_DICE_KMS,
		KeyID:             uuid.UUID(),
		PrimaryKeyVersion: primaryKeyVersion,
		KeySpec:           req.CustomerMasterKeySpec,
		KeyUsage:          req.KeyUsage,
		KeyState:          kmstypes.KeyStateEnabled,
		Description:       req.Description,
	}
	err := d.store.CreateKey(&key)
	if err != nil {
		return nil, fmt.Errorf("failed to create key in store, err: %v", err)
	}

	metadata := kmstypes.GetKeyMetadata(&key)
	resp := kmstypes.CreateKeyResponse{KeyMetadata: metadata}

	return &resp, nil
}

func (d *Dice) DescribeKey(ctx context.Context, req *kmstypes.DescribeKeyRequest) (*kmstypes.DescribeKeyResponse, error) {
	keyInfo, err := d.store.GetKey(req.KeyID)
	if err != nil {
		return nil, err
	}
	resp := kmstypes.DescribeKeyResponse{KeyMetadata: kmstypes.GetKeyMetadata(keyInfo)}
	return &resp, nil
}

func (d *Dice) ListKeys(ctx context.Context, req *kmstypes.ListKeysRequest) (*kmstypes.ListKeysResponse, error) {
	keyIDs, err := d.store.ListKeysByKind(kmstypes.PluginKind_DICE_KMS)
	if err != nil {
		return nil, err
	}
	var resp kmstypes.ListKeysResponse
	for _, id := range keyIDs {
		resp.Keys = append(resp.Keys, kmstypes.KeyListEntry{KeyID: id})
	}
	return &resp, nil
}

func (d *Dice) Encrypt(ctx context.Context, req *kmstypes.EncryptRequest) (*kmstypes.EncryptResponse, error) {
	// base64 decode
	plaintextBytes, err := base64.StdEncoding.DecodeString(req.PlaintextBase64)
	if err != nil {
		return nil, err
	}

	// check length
	maxLen := 1024 * 64 // 64 KB
	err = strutil.Validate(string(plaintextBytes), strutil.MaxLenValidator(maxLen))
	if err != nil {
		return nil, err
	}

	// key info
	keyInfo, err := d.store.GetKey(req.KeyID)
	if err != nil {
		return nil, err
	}

	// encrypt
	var ciphertext []byte
	switch keyInfo.GetKeySpec() {
	case kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_4096, kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_2048,
		kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_3072:
		rsaCrypt := kmscrypto.NewRSACrypt(kmscrypto.RSASecret{
			PublicKey:          keyInfo.GetPrimaryKeyVersion().GetPublicKeyBase64(),
			PublicKeyDataType:  kmscrypto.Base64,
			PrivateKey:         keyInfo.GetPrimaryKeyVersion().GetPrivateKeyBase64(),
			PrivateKeyDataType: kmscrypto.Base64,
			PrivateKeyType:     kmscrypto.PKCS1,
		})
		encryptedV, err := rsaCrypt.Encrypt(string(plaintextBytes), kmscrypto.String)
		if err != nil {
			return nil, err
		}
		ciphertext = []byte(encryptedV)
	case kmstypes.CustomerMasterKeySpec_SYMMETRIC_DEFAULT:
		additionalData := additionalData{
			KeyID: keyInfo.GetKeyID(),
		}
		additionalDataJSON, err := json.Marshal(&additionalData)
		if err != nil {
			return nil, err
		}
		symmetricKeyBytes, err := base64.StdEncoding.DecodeString(keyInfo.GetPrimaryKeyVersion().GetSymmetricKeyBase64())
		if err != nil {
			return nil, err
		}
		ciphertext, err = kmscrypto.AesGcmEncrypt(symmetricKeyBytes, plaintextBytes, additionalDataJSON)
		if err != nil {
			return nil, err
		}
	}
	// prefix append keyVersionID into ciphertext
	keyVersionIDPrefix, err := kmscrypto.PrefixAppend000Length([]byte(keyInfo.GetPrimaryKeyVersion().GetVersionID()))
	if err != nil {
		return nil, err
	}
	wrappedCiphertextBytes := append(keyVersionIDPrefix, ciphertext...)
	wrappedCiphertextBase64 := base64.StdEncoding.EncodeToString(wrappedCiphertextBytes)

	return &kmstypes.EncryptResponse{
		KeyID:            req.KeyID,
		CiphertextBase64: wrappedCiphertextBase64,
	}, nil

}

func (d *Dice) Decrypt(ctx context.Context, req *kmstypes.DecryptRequest) (resp *kmstypes.DecryptResponse, err error) {
	log.WithTraceID(ctx).Infof("decrypt request: %+v", req)

	// key info
	keyInfo, kerr := d.store.GetKey(req.KeyID)
	if kerr != nil {
		return nil, kerr
	}

	defer func() {
		// not expose concrete error to frontend, log err and return `broken ciphertext`
		if err != nil {
			log.WithTraceID(ctx).Errorf("parse ciphertext failed, err: %v", err)
			resp = nil
			err = fmt.Errorf("broken ciphertext")
		}
	}()

	ciphertextBytes, cerr := base64.StdEncoding.DecodeString(req.CiphertextBase64)
	if cerr != nil {
		return nil, err
	}

	// unwrap ciphertext
	keyVersionIDBytes, ciphertext, err := kmscrypto.PrefixUnAppend000Length(ciphertextBytes)
	if err != nil {
		return nil, err
	}
	keyVersionID := string(keyVersionIDBytes)

	// get keyVersionID info
	keyVersionInfo, err := d.store.GetKeyVersion(keyInfo.GetKeyID(), keyVersionID)
	if err != nil {
		return nil, err
	}

	// decrypt ciphertext
	var plaintextBytes []byte
	switch keyInfo.GetKeySpec() {
	case kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_4096, kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_2048,
		kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_3072:
		rsaCrypt := kmscrypto.NewRSACrypt(kmscrypto.RSASecret{
			PublicKey:          keyInfo.GetPrimaryKeyVersion().GetPublicKeyBase64(),
			PublicKeyDataType:  kmscrypto.Base64,
			PrivateKey:         keyInfo.GetPrimaryKeyVersion().GetPrivateKeyBase64(),
			PrivateKeyDataType: kmscrypto.Base64,
			PrivateKeyType:     kmscrypto.PKCS1,
		})
		plaintext, err := rsaCrypt.Decrypt(string(ciphertext), kmscrypto.String)
		if err != nil {
			return nil, err
		}
		plaintextBytes = []byte(plaintext)
	case kmstypes.CustomerMasterKeySpec_SYMMETRIC_DEFAULT:
		symmetricKey, err := base64.StdEncoding.DecodeString(keyVersionInfo.GetSymmetricKeyBase64())
		if err != nil {
			return nil, err
		}
		additionalDataJSON, err := json.Marshal(&additionalData{KeyID: keyInfo.GetKeyID()})
		if err != nil {
			return nil, err
		}
		plaintextBytes, err = kmscrypto.AesGcmDecrypt(symmetricKey, ciphertext, additionalDataJSON)
		if err != nil {
			return nil, err
		}
	}
	if err != nil {
		return nil, err
	}
	plaintextBase64 := base64.StdEncoding.EncodeToString(plaintextBytes)

	resp = &kmstypes.DecryptResponse{PlaintextBase64: plaintextBase64}

	return resp, nil
}

func (d *Dice) GenerateDataKey(ctx context.Context, req *kmstypes.GenerateDataKeyRequest) (*kmstypes.GenerateDataKeyResponse, error) {
	// get CMK
	keyInfo, err := d.store.GetKey(req.KeyID)
	if err != nil {
		return nil, err
	}

	// generate AES 256 key
	symmetricKey, err := kmscrypto.GenerateAes256Key()
	if err != nil {
		return nil, err
	}
	symmetricKeyBase64 := base64.StdEncoding.EncodeToString(symmetricKey)

	// encrypt AES 256 key by CMK
	encryptResp, err := d.Encrypt(ctx, &kmstypes.EncryptRequest{
		KeyID:           req.KeyID,
		PlaintextBase64: symmetricKeyBase64,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to generate data key, err: %v", err)
	}

	resp := kmstypes.GenerateDataKeyResponse{
		KeyID:            req.KeyID,
		KeyVersionID:     keyInfo.GetPrimaryKeyVersion().GetVersionID(),
		CiphertextBase64: encryptResp.CiphertextBase64,
		PlaintextBase64:  symmetricKeyBase64,
	}

	return &resp, nil
}

func (d *Dice) RotateKeyVersion(ctx context.Context, req *kmstypes.RotateKeyVersionRequest) (*kmstypes.RotateKeyVersionResponse, error) {
	keyInfo, err := d.store.GetKey(req.KeyID)
	if err != nil {
		return nil, err
	}
	newKeyVersion := kmstypes.KeyVersion{
		VersionID: uuid.UUID(),
	}
	switch keyInfo.GetKeySpec() {
	case kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_4096:
		if err := d.fillRsaKeyVersionByBits(&newKeyVersion, 4096); err != nil {
			return nil, err
		}
	case kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_3072:
		if err := d.fillRsaKeyVersionByBits(&newKeyVersion, 3072); err != nil {
			return nil, err
		}
	case kmstypes.CustomerMasterKeySpec_ASYMMETRIC_RSA_2048:
		if err := d.fillRsaKeyVersionByBits(&newKeyVersion, 2048); err != nil {
			return nil, err
		}
	case kmstypes.CustomerMasterKeySpec_SYMMETRIC_DEFAULT:
		symmetricKey, err := kmscrypto.GenerateAes256Key()
		if err != nil {
			return nil, err
		}
		symmetricKeyBase64 := base64.StdEncoding.EncodeToString(symmetricKey)
		newKeyVersion.SymmetricKeyBase64 = symmetricKeyBase64
	}

	// rotate key version
	_, err = d.store.RotateKeyVersion(req.KeyID, &newKeyVersion)
	if err != nil {
		return nil, err
	}
	keyInfo, err = d.store.GetKey(req.KeyID)
	if err != nil {
		return nil, err
	}

	resp := kmstypes.RotateKeyVersionResponse{KeyMetadata: kmstypes.GetKeyMetadata(keyInfo)}
	return &resp, nil
}

func (d *Dice) fillRsaKeyVersionByBits(keyVersion *kmstypes.KeyVersion, bits int) error {
	publicKey, privateKey, err := kmscrypto.GenRsaKey(bits)
	if err != nil {
		return fmt.Errorf("failed to create rsa key pair, err: %v", err)
	}
	publicKeyBase64 := base64.StdEncoding.EncodeToString(publicKey)
	privateKeyBase64 := base64.StdEncoding.EncodeToString(privateKey)
	keyVersion.PublicKeyBase64 = publicKeyBase64
	keyVersion.PrivateKeyBase64 = privateKeyBase64
	return nil
}

func (d *Dice) GetPublicKey(ctx context.Context, req *kmstypes.GetPublicKeyRequest) (*kmstypes.PublicKey, error) {
	panic("implement me")
}

func (d *Dice) AsymmetricDecrypt(ctx context.Context, req *kmstypes.AsymmetricDecryptRequest) (*kmstypes.AsymmetricDecryptResponse, error) {
	panic("implement me")
}
