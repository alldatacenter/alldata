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

package nexus

import (
	"bytes"
	"encoding/json"
	"net/url"

	"github.com/pkg/errors"
)

const (
	defaultEmailSuffix = "@terminus.io"
)

type UserID string

type UserSource string

var (
	UserSourceDefault UserSource = "default"
)

// {
//   "userId": "anonymous",
//   "firstName": "Anonymous",
//   "lastName": "User",
//   "emailAddress": "anonymous@example.org",
//   "source": "default",
//   "status": "active",
//   "readOnly": false,
//   "roles": [
//     "nx-anonymous"
//   ],
//   "externalRoles": []
// },
type User struct {
	UserID        UserID           `json:"userId"`
	FirstName     string           `json:"firstName"`
	LastName      string           `json:"lastName"`
	EmailAddress  string           `json:"emailAddress"`
	Source        UserSource       `json:"source"`
	Status        string           `json:"status"`
	ReadOnly      bool             `json:"readOnly"`
	Roles         []RoleID         `json:"roles"`
	ExternalRoles []ExternalRoleID `json:"externalRoles"`
}

type UserListRequest struct {
	// +optional
	UserIDPrefix string
	// +optional
	Source string
}

type UserEnsureRequest struct {
	UserCreateRequest
	ForceUpdatePassword bool
}

type UserCreateRequest struct {
	// The userid which is required for login. This value cannot be changed.
	// User-ID must be unique
	UserID UserID `json:"userId"`
	// The first name of the user.
	FirstName string `json:"firstName"`
	// The last name of the user.
	LastName string `json:"lastName"`
	// The email address associated with the user.
	EmailAddress string `json:"emailAddress"`
	// The password for the new user.
	Password string `json:"password"`
	// The user's status, e.g. active or disabled.
	Status UserStatus `json:"status"`
	// The roles which the user has been assigned within Nexus.
	// uniqueItems: true
	Roles []RoleID `json:"roles"`
}

// UserUpdateRequest
// won't update password if field `Password` is empty
type UserUpdateRequest struct {
	UserCreateRequest
	// The user source which is the origin of this user. This value cannot be changed.
	Source UserSource `json:"source"`
	// Indicates whether the user's properties could be modified by Nexus. When false only roles are considered during update.
	ReadOnly bool `json:"readOnly"`
	// The roles which the user has been assigned in an external source, e.g. LDAP group. These cannot be changed within Nexus.
	// uniqueItems: true
	ExternalRoles []ExternalRoleID `json:"externalRoles"`
}

type UserChangePasswordRequest struct {
	UserID   UserID
	Password string
}

type UserStatus string

var (
	UserStatusActive         UserStatus = "ACTIVE"
	UserStatusLocked         UserStatus = "LOCKED"
	UserStatusDisabled       UserStatus = "DISABLED"
	UserStatusChangePassword UserStatus = "CHANGEPASSWORD"
)

type EnsureDeploymentUserRequest struct {
	RepoName   string
	RepoFormat RepositoryFormat
	Password   string
}

type EnsureUserRequest struct {
	// +required
	UserName string
	// +required
	Password string
	// +required
	RepoPrivileges map[RepositoryFormat]map[string][]PrivilegeAction
	// +optional
	// whether update password when option is update, not create
	ForceUpdatePassword bool
}

//////////////////////////////////////////
// http client
//////////////////////////////////////////

// ListUsers Retrieve a list of users. Note if the source is not 'default' the response is limited to 100 users.
// curl -X GET "http://localhost:8081/service/rest/beta/security/users?userId=a&source=default" -H "accept: application/json"
func (n *Nexus) ListUsers(req UserListRequest) ([]User, error) {
	params := make(url.Values)
	if req.UserIDPrefix != "" {
		params["userId"] = []string{req.UserIDPrefix}
	}
	if req.Source != "" {
		params["source"] = []string{req.Source}
	}

	var body bytes.Buffer
	httpResp, err := n.hc.Get(n.Addr).Path("/service/rest/beta/security/users").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Params(params).
		Do().Body(&body)
	if err != nil {
		return nil, err
	}
	if !httpResp.IsOK() {
		return nil, ErrNotOK(httpResp.StatusCode(), body.String())
	}

	var users []User
	if err := json.NewDecoder(&body).Decode(&users); err != nil {
		return nil, err
	}

	return users, nil
}

func (n *Nexus) GetUser(userID UserID) (*User, error) {
	users, err := n.ListUsers(UserListRequest{
		UserIDPrefix: string(userID),
	})
	if err != nil {
		return nil, err
	}
	if len(users) == 0 {
		return nil, ErrNotFound
	}
	if len(users) > 1 {
		return nil, errors.Errorf("found more than one user by userID: %s", userID)
	}
	return &users[0], nil
}

// CreateUser create a new user in the default source.
func (n *Nexus) CreateUser(req UserCreateRequest) error {
	var filterRoles []RoleID
	for _, role := range req.Roles {
		if role != "" {
			filterRoles = append(filterRoles, role)
		}
	}
	req.Roles = filterRoles

	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/security/users").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) UpdateUser(req UserUpdateRequest) error {
	var filterRoles []RoleID
	for _, role := range req.Roles {
		if role != "" {
			filterRoles = append(filterRoles, role)
		}
	}
	req.Roles = filterRoles

	printJSON(req)

	// update properties without password
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/security/users/"+string(req.UserID)).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	// change password
	if req.Password == "" {
		return nil
	}
	return n.ChangeUserPassword(UserChangePasswordRequest{UserID: req.UserID, Password: req.Password})
}

func (n *Nexus) ChangeUserPassword(req UserChangePasswordRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/security/users/"+string(req.UserID)+"/change-password").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Header(HeaderContentType, "text/plain").
		RawBody(bytes.NewBufferString(req.Password)).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

// EnsureUser won't update password if ForceUpdatePassword is false.
func (n *Nexus) EnsureUser(req EnsureUserRequest) (UserID, error) {
	// check
	if req.UserName == "" {
		return "", errors.Errorf("missing username")
	}
	if req.Password == "" {
		return "", errors.Errorf("missing password")
	}
	if len(req.RepoPrivileges) == 0 {
		return "", errors.Errorf("missing repo configs")
	}
	// role
	roleID := req.UserName
	var privileges []PrivilegeID
	for format, reposWithPrivilegeActions := range req.RepoPrivileges {
		for repoName, privilegeActions := range reposWithPrivilegeActions {
			privileges = append(privileges,
				GetNxRepositoryPrivilege(PrivilegeTypeRepositoryView, format, repoName, privilegeActions...)...)
		}
	}
	err := n.EnsureRole(RoleCreateRequest{
		ID:          RoleID(roleID),
		Name:        roleID,
		Description: roleID,
		Privileges:  privileges,
		Roles:       nil,
	})
	if err != nil {
		return "", err
	}

	// ensure user
	err = n.ensureUser(UserEnsureRequest{UserCreateRequest: UserCreateRequest{
		UserID:       UserID(roleID),
		FirstName:    roleID,
		LastName:     roleID,
		EmailAddress: roleID + defaultEmailSuffix,
		Password:     req.Password,
		Status:       UserStatusActive,
		Roles:        []RoleID{RoleID(roleID)},
	}})
	if err != nil {
		return "", err
	}

	return UserID(roleID), nil
}

// ensureUser create or update user.
func (n *Nexus) ensureUser(req UserEnsureRequest) error {
	user, err := n.GetUser(req.UserID)
	if err != nil {
		if err != ErrNotFound {
			return err
		}
		// not found, create
		return n.CreateUser(req.UserCreateRequest)
	}
	// update
	updateReq := UserUpdateRequest{
		UserCreateRequest: req.UserCreateRequest,
		Source:            user.Source,
		ReadOnly:          false,
		ExternalRoles:     user.ExternalRoles,
	}
	if !req.ForceUpdatePassword {
		updateReq.Password = ""
	}
	return n.UpdateUser(updateReq)
}
