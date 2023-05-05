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
)

type ContentSelector struct {
	Name        string `json:"name"`
	Type        string `json:"type"`
	Description string `json:"description"`
	Expression  string `json:"expression"`
}

type ContentSelectorListRequest struct {
}

type ContentSelectorCreateRequest struct {
	// The content selector name cannot be changed after creation.
	// pattern: ^[a-zA-Z0-9\-]{1}[a-zA-Z0-9_\-\.]*$
	Name string `json:"name"`
	// A human-readable description
	// allowEmptyValue: true
	Description string `json:"description"`
	// The expression used to identify content
	// example: format == "maven2" and path =^ "/org/sonatype/nexus"
	Expression string `json:"expression"`
}
type ContentSelectorGetRequest struct {
	ContentSelectorName string
}
type ContentSelectorUpdateRequest struct {
	ContentSelectorName string `json:"-"`
	Description         string `json:"description"`
	Expression          string `json:"expression"`
}
type ContentSelectorDeleteRequest struct {
	ContentSelectorName string
}

//////////////////////////////////////////
// http client
//////////////////////////////////////////

func (n *Nexus) ContentSelectorListRequest(req ContentSelectorListRequest) ([]ContentSelector, error) {
	var body bytes.Buffer
	httpResp, err := n.hc.Get(n.Addr).Path("/service/rest/beta/security/content-selectors").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return nil, err
	}
	if !httpResp.IsOK() {
		return nil, ErrNotOK(httpResp.StatusCode(), body.String())
	}

	var contentSelectors []ContentSelector
	if err := json.NewDecoder(&body).Decode(&contentSelectors); err != nil {
		return nil, err
	}

	return contentSelectors, nil
}

func (n *Nexus) ContentSelectorCreateRequest(req ContentSelectorCreateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/security/content-selectors").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(&req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) ContentSelectorGetRequest(req ContentSelectorGetRequest) (*ContentSelector, error) {
	var body bytes.Buffer
	httpResp, err := n.hc.Get(n.Addr).Path("/service/rest/beta/security/content-selectors/"+req.ContentSelectorName).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return nil, err
	}
	if !httpResp.IsOK() {
		return nil, ErrNotOK(httpResp.StatusCode(), body.String())
	}

	var selector ContentSelector
	if err := json.NewDecoder(&body).Decode(&selector); err != nil {
		return nil, err
	}

	return &selector, nil
}

func (n *Nexus) UpdateContentSelector(req ContentSelectorUpdateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/security/content-selectors/"+req.ContentSelectorName).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(&req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) DeleteContentSelector(req ContentSelectorDeleteRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Delete(n.Addr).Path("/service/rest/beta/security/content-selectors/"+req.ContentSelectorName).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}
