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

package kmstypes

import "fmt"

type (
	CustomerMasterKeySpec string
	KeyUsage              string
	KeyState              string
)

func (s *CustomerMasterKeySpec) IsValid() bool {
	switch *s {
	case CustomerMasterKeySpec_SYMMETRIC_DEFAULT, CustomerMasterKeySpec_ASYMMETRIC_RSA_2048,
		CustomerMasterKeySpec_ASYMMETRIC_RSA_3072, CustomerMasterKeySpec_ASYMMETRIC_RSA_4096:
		return true
	default:
		return false
	}
}

type (
	KeyMetadata struct {
		KeyID                 string                `json:"keyID,omitempty"`
		PrimaryKeyVersionID   string                `json:"primaryKeyVersionID,omitempty"`
		CustomerMasterKeySpec CustomerMasterKeySpec `json:"customerMasterKeySpec,omitempty"`
		KeyUsage              KeyUsage              `json:"keyUsage,omitempty"`
		KeyState              KeyState              `json:"keyState,omitempty"`
		Description           string                `json:"description,omitempty"`
	}

	KeyListEntry struct {
		KeyID string `json:"keyID,omitempty"`
	}
)

type CreateKeyRequest struct {
	PluginKind            PluginKind            `json:"pluginKind,omitempty"`
	CustomerMasterKeySpec CustomerMasterKeySpec `json:"customerMasterKeySpec,omitempty"`
	KeyUsage              KeyUsage              `json:"keyUsage,omitempty"`
	Description           string                `json:"description,omitempty"`
}

func (req *CreateKeyRequest) ValidateRequest() error {
	if req.PluginKind == "" {
		req.PluginKind = PluginKind_DICE_KMS
	}
	if req.CustomerMasterKeySpec == "" {
		req.CustomerMasterKeySpec = CustomerMasterKeySpec_SYMMETRIC_DEFAULT
	}
	if req.KeyUsage == "" {
		req.KeyUsage = KeyUsage_ENCRYPT_DECRYPT
	}
	return nil
}

type CreateKeyResponse struct {
	KeyMetadata KeyMetadata `json:"keyMetadata,omitempty"`
}

type GenerateDataKeyRequest struct {
	KeyID string `json:"keyID,omitempty"`
}

func (req *GenerateDataKeyRequest) ValidateRequest() error {
	if req.KeyID == "" {
		return fmt.Errorf("missing keyID")
	}
	return nil
}

type GenerateDataKeyResponse struct {
	KeyID            string `json:"keyID,omitempty"`
	KeyVersionID     string `json:"keyVersionID,omitempty"`
	CiphertextBase64 string `json:"ciphertextBase64,omitempty"`
	PlaintextBase64  string `json:"plaintextBase64,omitempty"`
}

type DescribeKeyRequest struct {
	KeyID string `json:"keyID,omitempty"`
}

func (req *DescribeKeyRequest) ValidateRequest() error {
	if req.KeyID == "" {
		return fmt.Errorf("missing keyID")
	}
	return nil
}

type DescribeKeyResponse struct {
	KeyMetadata KeyMetadata `json:"keyMetadata,omitempty"`
}

type ListKeysRequest struct {
}

type ListKeysResponse struct {
	Keys []KeyListEntry `json:"keys,omitempty"`
}
