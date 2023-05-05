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

package ossclient

import (
	"io"
	"io/ioutil"
	"strings"

	"github.com/aliyun/aliyun-oss-go-sdk/oss"
	"github.com/pkg/errors"
)

type OssClient struct {
	endpoint  string
	accessKey string
	secretKey string
	client    *oss.Client
}

func New(endpoint, accessKey, secretKey string) (*OssClient, error) {
	ossclient, err := oss.New(endpoint, accessKey, secretKey)
	if err != nil {
		return nil, err
	}
	client := OssClient{
		endpoint:  endpoint,
		accessKey: accessKey,
		secretKey: secretKey,
		client:    ossclient,
	}

	return &client, nil
}

func (c *OssClient) UploadFile(bucketName, objectName, file string) (string, error) {
	bucket, err := c.client.Bucket(bucketName)
	if err != nil {
		return "", errors.Wrap(err, "get bucket")
	}
	if err := bucket.PutObjectFromFile(objectName, file); err != nil {
		return "", err
	}
	url, err := c.GetFileUrl(bucketName, objectName)
	if err != nil {
		return "", errors.Wrap(err, "get url")
	}
	return url, nil
}

func (c *OssClient) DownloadFile(bucketName, objectName string) ([]byte, error) {
	var err error

	var bucket *oss.Bucket
	if bucket, err = c.client.Bucket(bucketName); err != nil {
		return nil, err
	}

	var reader io.ReadCloser
	defer func() {
		if reader != nil {
			reader.Close()
		}
	}()
	if reader, err = bucket.GetObject(objectName); err != nil {
		return nil, err
	}

	var data []byte
	if data, err = ioutil.ReadAll(reader); err != nil {
		return nil, err
	}

	return data, nil
}

func (c *OssClient) GetFileUrl(bucketName, objectName string) (string, error) {
	bucket, err := c.client.Bucket(bucketName)
	if err != nil {
		return "", err
	}
	exist, err := bucket.IsObjectExist(objectName)
	if err != nil {
		return "", err
	}
	if !exist {
		return "", errors.Errorf("file=%s not exist in bucket=%s", objectName, bucketName)
	}
	return strings.Join([]string{c.endpoint, bucketName, objectName}, "/"), nil
}

func (c *OssClient) HealthCheck() error {
	panic("not implement")
}
