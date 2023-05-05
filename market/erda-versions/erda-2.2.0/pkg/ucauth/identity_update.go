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

import (
	"bytes"
	"fmt"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

func UpdateIdentity(kratosPrivateAddr string, userID string, req OryKratosUpdateIdentitiyRequest) error {
	var body bytes.Buffer
	r, err := httpclient.New(httpclient.WithCompleteRedirect()).
		Put(kratosPrivateAddr).
		Path("/identities/" + userID).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !r.IsOK() {
		return fmt.Errorf("update identity: statuscode: %d, body: %v", r.StatusCode(), body.String())
	}
	return nil
}

func ChangeUserState(kratosPrivateAddr string, userID string, state string) error {
	i, err := getIdentity(kratosPrivateAddr, userID)
	if err != nil {
		return err
	}
	return UpdateIdentity(kratosPrivateAddr, userID, OryKratosUpdateIdentitiyRequest{
		State:  state,
		Traits: i.Traits,
	})
}
