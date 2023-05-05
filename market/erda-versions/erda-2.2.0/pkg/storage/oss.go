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

package storage

import (
	"io"
	"strings"

	"github.com/aliyun/aliyun-oss-go-sdk/oss"
)

type OSS struct {
	endpoint        string
	accessKeyID     string
	accessKeySecret string
	bucket          string
	clientOptions   []oss.ClientOption
	options         []oss.Option
}

func NewOSS(endpoint, accessKeyID, accessKeySecret, bucket string,
	clientOptions []oss.ClientOption, options []oss.Option) *OSS {
	var o OSS
	o.endpoint = endpoint
	o.accessKeyID = accessKeyID
	o.accessKeySecret = accessKeySecret
	o.bucket = bucket
	o.clientOptions = clientOptions
	o.options = options
	return &o
}

func (o *OSS) Type() Type {
	return TypeOSS
}

func (o *OSS) Read(path string) (io.Reader, error) {
	path = handlePath(path)
	client, err := o.newClient()
	if err != nil {
		return nil, err
	}
	bucket, err := client.Bucket(o.bucket)
	if err != nil {
		return nil, err
	}
	return bucket.GetObject(path, o.options...)
}

func (o *OSS) Write(path string, r io.Reader) error {
	path = handlePath(path)
	client, err := o.newClient()
	if err != nil {
		return err
	}
	bucket, err := client.Bucket(o.bucket)
	if err != nil {
		return err
	}
	return bucket.PutObject(path, r, o.options...)
}

func (o *OSS) Delete(path string) error {
	path = handlePath(path)
	client, err := o.newClient()
	if err != nil {
		return err
	}
	bucket, err := client.Bucket(o.bucket)
	if err != nil {
		return err
	}
	return bucket.DeleteObject(path)
}

func (o *OSS) newClient() (*oss.Client, error) {
	return oss.New(o.endpoint, o.accessKeyID, o.accessKeySecret, o.clientOptions...)
}

// handlePath
// path cannot start with "/" or "\", see: vendor/github.com/aliyun/aliyun-oss-go-sdk/oss/bucket.go:28
func handlePath(path string) string {
	return strings.TrimPrefix(path, "/")
}
