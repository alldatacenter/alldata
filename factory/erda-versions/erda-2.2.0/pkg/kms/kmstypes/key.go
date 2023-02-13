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

import (
	"fmt"
	"time"
)

type KeyInfo interface {
	New() KeyInfo

	GetPluginKind() PluginKind
	SetPluginKind(PluginKind)

	GetKeyID() string
	SetKeyID(string)

	GetPrimaryKeyVersion() KeyVersionInfo
	SetPrimaryKeyVersion(KeyVersionInfo)

	GetKeySpec() CustomerMasterKeySpec
	SetKeySpec(CustomerMasterKeySpec)
	GetKeyUsage() KeyUsage
	SetKeyUsage(KeyUsage)
	GetKeyState() KeyState
	SetKeyState(KeyState)

	GetDescription() string
	SetDescription(string)

	GetCreatedAt() *time.Time
	SetCreatedAt(time.Time)
	GetUpdatedAt() *time.Time
	SetUpdatedAt(time.Time)
}

func GetKeyMetadata(keyInfo KeyInfo) KeyMetadata {
	return KeyMetadata{
		KeyID:                 keyInfo.GetKeyID(),
		PrimaryKeyVersionID:   keyInfo.GetPrimaryKeyVersion().GetVersionID(),
		CustomerMasterKeySpec: keyInfo.GetKeySpec(),
		KeyUsage:              keyInfo.GetKeyUsage(),
		KeyState:              keyInfo.GetKeyState(),
		Description:           keyInfo.GetDescription(),
	}
}

type KeyVersionInfo interface {
	New() KeyVersionInfo

	GetVersionID() string
	SetVersionID(string)

	GetSymmetricKeyBase64() string
	SetSymmetricKeyBase64(string)

	GetPublicKeyBase64() string
	SetPublicKeyBase64(string)
	GetPrivateKeyBase64() string
	SetPrivateKeyBase64(string)

	GetCreatedAt() *time.Time
	SetCreatedAt(time.Time)

	GetUpdatedAt() *time.Time
	SetUpdatedAt(time.Time)
}

func CheckKeyForCreate(keyInfo KeyInfo) error {
	if keyInfo.GetKeyID() == "" {
		return fmt.Errorf("not found: keyID")
	}
	if keyInfo.GetPluginKind() == "" {
		return fmt.Errorf("not found: plugin kind")
	}
	if keyInfo.GetKeySpec() == "" {
		return fmt.Errorf("not found: key spec")
	}
	if keyInfo.GetKeyUsage() == "" {
		return fmt.Errorf("not found: key usage")
	}
	if keyInfo.GetPrimaryKeyVersion() == nil {
		return fmt.Errorf("not found: key version")
	}
	if keyInfo.GetPrimaryKeyVersion().GetVersionID() == "" {
		return fmt.Errorf("not found: key version id")
	}
	return nil
}

type Key struct {
	PluginKind        PluginKind            `json:"pluginKind,omitempty"`
	KeyID             string                `json:"keyID,omitempty"`
	PrimaryKeyVersion KeyVersion            `json:"primaryKeyVersion,omitempty"`
	KeySpec           CustomerMasterKeySpec `json:"keySpec,omitempty"`
	KeyUsage          KeyUsage              `json:"keyUsage,omitempty"`
	KeyState          KeyState              `json:"keyState,omitempty"`
	Description       string                `json:"description,omitempty"`
	CreatedAt         *time.Time            `json:"createdAt,omitempty"`
	UpdatedAt         *time.Time            `json:"updatedAt,omitempty"`
}

func (k *Key) New() KeyInfo                          { return &Key{} }
func (k *Key) GetPluginKind() PluginKind             { return k.PluginKind }
func (k *Key) SetPluginKind(pluginKind PluginKind)   { k.PluginKind = pluginKind }
func (k *Key) GetKeyID() string                      { return k.KeyID }
func (k *Key) SetKeyID(keyID string)                 { k.KeyID = keyID }
func (k *Key) GetKeySpec() CustomerMasterKeySpec     { return k.KeySpec }
func (k *Key) SetKeySpec(spec CustomerMasterKeySpec) { k.KeySpec = spec }
func (k *Key) GetKeyUsage() KeyUsage                 { return k.KeyUsage }
func (k *Key) SetKeyUsage(usage KeyUsage)            { k.KeyUsage = usage }
func (k *Key) GetKeyState() KeyState                 { return k.KeyState }
func (k *Key) SetKeyState(state KeyState)            { k.KeyState = state }
func (k *Key) GetDescription() string                { return k.Description }
func (k *Key) SetDescription(desc string)            { k.Description = desc }
func (k *Key) GetCreatedAt() *time.Time              { return k.CreatedAt }
func (k *Key) SetCreatedAt(t time.Time)              { k.CreatedAt = &t }
func (k *Key) GetUpdatedAt() *time.Time              { return k.UpdatedAt }
func (k *Key) SetUpdatedAt(t time.Time)              { k.UpdatedAt = &t }
func (k *Key) GetPrimaryKeyVersion() KeyVersionInfo  { return &k.PrimaryKeyVersion }
func (k *Key) SetPrimaryKeyVersion(version KeyVersionInfo) {
	k.PrimaryKeyVersion = KeyVersion{
		VersionID:          version.GetVersionID(),
		SymmetricKeyBase64: version.GetSymmetricKeyBase64(),
		PublicKeyBase64:    version.GetPublicKeyBase64(),
		PrivateKeyBase64:   version.GetPrivateKeyBase64(),
		CreatedAt:          version.GetCreatedAt(),
		UpdatedAt:          version.GetUpdatedAt(),
	}
}

type KeyVersion struct {
	VersionID string `json:"versionID,omitempty"`
	// base64 encoded
	SymmetricKeyBase64 string     `json:"symmetricKeyBase64,omitempty"`
	PublicKeyBase64    string     `json:"publicKeyBase64,omitempty"`
	PrivateKeyBase64   string     `json:"privateKeyBase64,omitempty"`
	CreatedAt          *time.Time `json:"createdAt,omitempty"`
	UpdatedAt          *time.Time `json:"updatedAt,omitempty"`
}

func (k *KeyVersion) New() KeyVersionInfo            { return &KeyVersion{} }
func (k *KeyVersion) GetVersionID() string           { return k.VersionID }
func (k *KeyVersion) SetVersionID(s string)          { k.VersionID = s }
func (k *KeyVersion) GetSymmetricKeyBase64() string  { return k.SymmetricKeyBase64 }
func (k *KeyVersion) SetSymmetricKeyBase64(s string) { k.SymmetricKeyBase64 = s }
func (k *KeyVersion) GetCreatedAt() *time.Time       { return k.CreatedAt }
func (k *KeyVersion) SetCreatedAt(t time.Time)       { k.CreatedAt = &t }
func (k *KeyVersion) GetUpdatedAt() *time.Time       { return k.UpdatedAt }
func (k *KeyVersion) SetUpdatedAt(t time.Time)       { k.UpdatedAt = &t }
func (k *KeyVersion) GetPublicKeyBase64() string     { return k.PublicKeyBase64 }
func (k *KeyVersion) SetPublicKeyBase64(s string)    { k.PublicKeyBase64 = s }
func (k *KeyVersion) GetPrivateKeyBase64() string    { return k.PrivateKeyBase64 }
func (k *KeyVersion) SetPrivateKeyBase64(s string)   { k.PrivateKeyBase64 = s }
