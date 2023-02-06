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

package ucauth

type OryKratosSession struct {
	ID       string            `json:"id"`
	Active   bool              `json:"active"`
	Identity OryKratosIdentity `json:"identity"`
}

type OryKratosIdentity struct {
	ID       USERID                  `json:"id"`
	SchemaID string                  `json:"schema_id"`
	State    string                  `json:"state"`
	Traits   OryKratosIdentityTraits `json:"traits"`
}

type OryKratosIdentityTraits struct {
	Email  string `json:"email"`
	Name   string `json:"username"`
	Nick   string `json:"nickname"`
	Phone  string `json:"phone"`
	Avatar string `json:"avatar"`
}

type OryKratosFlowResponse struct {
	ID string                  `json:"id"`
	UI OryKratosFlowResponseUI `json:"ui"`
}

type OryKratosReadyResponse struct {
	Status string `json:"status"`
}

type OryKratosFlowResponseUI struct {
	Action string `json:"action"`
}

type OryKratosRegistrationRequest struct {
	Traits   OryKratosIdentityTraits `json:"traits"`
	Password string                  `json:"password"`
	Method   string                  `json:"method"`
}

type OryKratosRegistrationResponse struct {
	Identity OryKratosIdentity `json:"identity"`
}

type OryKratosUpdateIdentitiyRequest struct {
	State  string                  `json:"state"`
	Traits OryKratosIdentityTraits `json:"traits"`
}

type OryKratosCreateIdentitiyRequest struct {
	SchemaID    string                                  `json:"schema_id"`
	Traits      OryKratosIdentityTraits                 `json:"traits"`
	Credentials OryKratosAdminIdentityImportCredentials `json:"credentials"`
}

type OryKratosAdminIdentityImportCredentials struct {
	Password *OryKratosAdminIdentityImportCredentialsPassword `json:"password"`
}

type OryKratosAdminIdentityImportCredentialsPassword struct {
	Config OryKratosIdentityCredentialsPasswordConfig `json:"config"`
}

type OryKratosIdentityCredentialsPasswordConfig struct {
	HashedPassword string `json:"hashed_password"`
	Password       string `json:"password"`
}

const (
	UserActive   = "active"
	UserInActive = "inactive"
)

var oryKratosStateMap = map[int]string{
	0: UserActive,
	1: UserInActive,
}

func identityToUser(i OryKratosIdentity) User {
	return User{
		ID:        string(i.ID),
		Name:      i.Traits.Name,
		Nick:      i.Traits.Nick,
		Email:     i.Traits.Email,
		Phone:     i.Traits.Phone,
		AvatarURL: i.Traits.Avatar,
		State:     i.State,
	}
}

func IdentityToUserInfo(i OryKratosIdentity) UserInfo {
	return userToUserInfo(identityToUser(i))
}

func userToUserInfo(u User) UserInfo {
	return UserInfo{
		ID:        USERID(u.ID),
		Email:     u.Email,
		Phone:     u.Phone,
		AvatarUrl: u.AvatarURL,
		UserName:  u.Name,
		NickName:  u.Nick,
		Enabled:   true,
	}
}

func userToUserInPaging(u User) userInPaging {
	return userInPaging{
		Id:       u.ID,
		Avatar:   u.AvatarURL,
		Username: u.Name,
		Nickname: u.Nick,
		Mobile:   u.Phone,
		Email:    u.Email,
		Enabled:  true,
		Locked:   u.State == UserInActive,
		// TODO: LastLoginAt PwdExpireAt
	}
}
