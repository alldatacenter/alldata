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
	"path/filepath"
	"strings"
)

const (
	DefaultBlobNetdataDir = "nexus-netdata/"
)

type BlobStore struct {
	SoftQuota             *BlobStoreSoftQuota `json:"softQuota"`
	Name                  string              `json:"name"`
	Type                  BlobStoreType       `json:"type"`
	Path                  string              `json:"path"`
	BlobCount             int64               `json:"blobCount"`
	TotalSizeInBytes      int64               `json:"totalSizeInBytes"`
	AvailableSpaceInBytes int64               `json:"availableSpaceInBytes"`
}

type BlobStoreType string

var (
	BlobStoreTypeFile BlobStoreType = "file"
	BlobStoreTypeS3   BlobStoreType = "s3"
)

type BlobStoreSoftQuota struct {
	// The type to use such as spaceRemainingQuota, or spaceUsedQuota
	Type string `json:"type"`
	// The limit in MB.
	Limit int64 `json:"limit"`
}

type BlobStoreListRequest struct {
}

type BlobStoreDeleteRequest struct {
	BlobName string
}

type FileBlobStoreCreateRequest struct {
	SoftQuota *BlobStoreSoftQuota `json:"softQuota"`
	// The path to the blobstore contents.
	// This can be an absolute path to anywhere on the system nxrm has access to or it can be a path relative to the sonatype-work directory.
	Path string `json:"path"`
	Name string `json:"name"`

	BlobUseNetdata
}

func (req FileBlobStoreCreateRequest) handlePath(netdataDir string) FileBlobStoreCreateRequest {
	if !req.UseNetdata {
		return req
	}
	if strings.HasPrefix(req.Path, netdataDir) {
		return req
	}
	req.Path = filepath.Join(netdataDir, req.Path)
	return req
}

type FileBlobStoreGetRequest struct {
	Name string
}

type FileBlobStoreUpdateRequest struct {
	FileBlobStoreCreateRequest
}

//////////////////////////////////////////
// http client
//////////////////////////////////////////

func (n *Nexus) ListBlobStore(req BlobStoreListRequest) ([]BlobStore, error) {
	var body bytes.Buffer
	httpResp, err := n.hc.Get(n.Addr).Path("/service/rest/beta/blobstores").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return nil, err
	}
	if !httpResp.IsOK() {
		return nil, ErrNotOK(httpResp.StatusCode(), body.String())
	}

	var blobStores []BlobStore
	if err := json.NewDecoder(&body).Decode(&blobStores); err != nil {
		return nil, err
	}

	return blobStores, nil
}

func (n *Nexus) DeleteBlobStore(req BlobStoreDeleteRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Delete(n.Addr).Path("/service/rest/beta/blobstores/"+req.BlobName).
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

// EnsureFileBlobStore create or update file blob store.
func (n *Nexus) EnsureFileBlobStore(req FileBlobStoreCreateRequest) error {
	_, err := n.GetFileBlobStore(FileBlobStoreGetRequest{Name: req.Name})
	if err != nil {
		if err != ErrNotFound {
			return err
		}
		// not found, create
		return n.CreateFileBlobStore(req)
	}
	// update
	return n.UpdateFileBlobStore(FileBlobStoreUpdateRequest{req})
}

func (n *Nexus) CreateFileBlobStore(req FileBlobStoreCreateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/blobstores/file").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req.handlePath(n.blobNetdataDir)).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) GetFileBlobStore(req FileBlobStoreGetRequest) (*BlobStore, error) {
	var body bytes.Buffer
	httpResp, err := n.hc.Get(n.Addr).Path("/service/rest/beta/blobstores/file/"+req.Name).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return nil, err
	}
	if !httpResp.IsOK() {
		return nil, ErrNotOK(httpResp.StatusCode(), body.String())
	}

	var blobStore BlobStore
	if err := json.NewDecoder(&body).Decode(&blobStore); err != nil {
		return nil, err
	}

	return &blobStore, nil
}

func (n *Nexus) UpdateFileBlobStore(req FileBlobStoreUpdateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/blobstores/file/"+req.Name).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req.handlePath(n.blobNetdataDir)).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}
