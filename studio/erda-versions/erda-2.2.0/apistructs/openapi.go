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

type OAuth2Token struct {
	AccessToken string `json:"access_token"`
	ExpiresIn   int64  `json:"expires_in"`
	Scope       string `json:"scope"`
	TokenType   string `json:"token_type"`
}

type OAuth2TokenPayload struct {
	AccessTokenExpiredIn string            `json:"accessTokenExpiredIn"` // such as "300ms", "-1.5h" or "2h45m". "0" means it doesn't expire. Empty string is not allowed.
	AllowAccessAllAPIs   bool              `json:"allowAccessAllApIs,omitempty"`
	AccessibleAPIs       []AccessibleAPI   `json:"accessibleAPIs,omitempty"`
	Metadata             map[string]string `json:"metadata,omitempty"`
}

type AccessibleAPI struct {
	Path   string `json:"path"`
	Method string `json:"method"`
	Schema string `json:"schema"`
}

type OAuth2TokenGetRequest struct {
	ClientID     string
	ClientSecret string
	Payload      OAuth2TokenPayload `json:"payload"`
}

type OAuth2TokenInvalidateRequest struct {
	AccessToken string `json:"access_token"`
}
