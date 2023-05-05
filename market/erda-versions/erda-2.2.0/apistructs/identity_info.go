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

package apistructs

// IdentityInfo represents operator identity info.
// Fields will not be json marshal/unmarshal.
type IdentityInfo struct {
	// UserID is user id. It must be provided in some cases.
	// Cannot be null if InternalClient is null.
	// +optional
	UserID string `json:"userID"`

	// InternalClient records the internal client, such as: bundle.
	// Cannot be null if UserID is null.
	// +optional
	InternalClient string `json:"-"`
}

func (info *IdentityInfo) IsInternalClient() bool {
	return info.InternalClient != ""
}

func (info *IdentityInfo) Empty() bool {
	return info.UserID == "" && info.InternalClient == ""
}
