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

// Store the key information storage interface
type Store interface {
	// PluginKind is key store type
	GetKind() StoreKind

	// Create create and store new CMK
	CreateKey(info KeyInfo) error

	// GetKey use keyID to find CMK
	GetKey(keyID string) (KeyInfo, error)

	// ListByKind use plugin type to list CMKs
	ListKeysByKind(kind PluginKind) ([]string, error)

	// DeleteByKeyID use keyID to delete CMK
	DeleteByKeyID(keyID string) error

	// GetKeyVersion use keyID and keyVersionID to find keyVersion
	GetKeyVersion(keyID, keyVersionID string) (KeyVersionInfo, error)

	// RotateKeyVersion rotate key version
	RotateKeyVersion(keyID string, newKeyVersionInfo KeyVersionInfo) (KeyVersionInfo, error)
}
