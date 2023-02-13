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

//import (
//	"testing"
//
//	"github.com/sirupsen/logrus"
//	"github.com/stretchr/testify/assert"
//)
//
//const (
//	BUCKET = "terminus-dice"
//)
//
//var c *OssClient
//
//func TestNewClient(t *testing.T) {
//	_, err := New("oss-cn-hangzhou.aliyuncs.com", "xxx", "xx")
//	assert.Nil(t, err)
//}
//
//func TestClient_GetFileUrl(t *testing.T) {
//	url, err := c.GetFileUrl(BUCKET, "test.yml")
//	assert.Nil(t, err)
//	logrus.Info(">>", url)
//}
//
//func TestClient_UploadFile(t *testing.T) {
//	url, err := c.UploadFile(BUCKET, "yhp-test-alert.sh", "/Users/yuhaiping/Desktop/alert.sh")
//	assert.Nil(t, err)
//	logrus.Info(">> url:", url)
//}
//
//func TestOssClient_DownloadFile(t *testing.T) {
//	bs, err := c.DownloadFile(BUCKET, "test.yml")
//	assert.Nil(t, err)
//	logrus.Info(string(bs))
//}
