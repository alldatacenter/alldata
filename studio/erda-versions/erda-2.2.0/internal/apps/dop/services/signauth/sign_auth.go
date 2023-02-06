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

// 文档地址
// https://dice-docs.app.terminus.io/3.19/manual/microservice/sign-auth.html#%E5%9F%BA%E4%BA%8E-body-%E7%9A%84%E7%AD%BE%E5%90%8D
//
// 本页实现的签名方法基于以上文档

package signauth

import (
	"bytes"
	"crypto/sha512"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"sort"
	"strings"
)

type Signer interface {
	// 计算签名, 并更新 r *http.Request
	// 注意: 调用 Sign() 方法后, 相应的 r *http.Request 已经更新, 无需再次更新
	Sign() (sign string, err error)
}

func NewSigner(r *http.Request, key, secret string) (Signer, error) {
	// 对于没有 Body 的方法, 根据 url 参数签名
	switch r.Method {
	case http.MethodGet, http.MethodHead:
		r.Header.Del("content-type")
		return &urlParamsSigner{
			r:      r,
			key:    key,
			secret: secret,
		}, nil
	}

	// 对于有 body 的方法, 根据 content-type 参数签名
	switch contentType := strings.ToLower(r.Header.Get("content-type")); {
	case strings.Contains(contentType, "application/x-www-form-urlencoded"):
		r.Header.Set("content-type", "application/x-www-form-urlencoded")
		return &urlEncodedSigner{
			r:      r,
			key:    key,
			secret: secret,
		}, nil
	case strings.Contains(contentType, "application/json"):
		r.Header.Set("content-type", "application/json")
		return &jsonSigner{
			r:      r,
			key:    key,
			secret: secret,
		}, nil
	default:
		r.Header.Del("content-type")
		return &urlParamsSigner{
			r:      r,
			key:    key,
			secret: secret,
		}, nil
	}
}

type urlParamsSigner struct {
	r      *http.Request
	key    string
	secret string
}

// 计算签名
func (a *urlParamsSigner) Sign() (string, error) {
	values := a.r.URL.Query()
	values.Set("appKey", a.key)
	sign := sortAndHash(values, a.secret)

	// 更新请求
	values.Set("sign", sign)
	a.r.URL.RawQuery = values.Encode()

	return sign, nil
}

type urlEncodedSigner struct {
	r      *http.Request
	key    string
	secret string
}

func (a *urlEncodedSigner) Sign() (string, error) {
	bodyData, err := ioutil.ReadAll(a.r.Body)
	if err != nil {
		return "", err
	}
	values, err := url.ParseQuery(string(bodyData))
	if err != nil {
		return "", err
	}
	values.Set("appKey", a.key)

	sign := sortAndHash(values, a.secret)

	// 更新请求
	values.Set("sign", sign)
	bodyS := values.Encode()
	a.r.Body = ioutil.NopCloser(bytes.NewBufferString(bodyS))
	a.r.ContentLength = int64(len(bodyS))

	return sign, nil
}

type jsonSigner struct {
	r      *http.Request
	key    string
	secret string
}

func (a *jsonSigner) Sign() (string, error) {
	var values = make(url.Values)
	values.Set("appKey", a.key)

	var dataS = "{}"
	if a.r.Body != nil {
		data, err := ioutil.ReadAll(a.r.Body)
		if err != nil {
			return "", err
		}
		dataS = string(data)
	}

	values.Set("data", dataS)

	sign := sortAndHash(values, a.secret)

	// 更新请求
	m := map[string]string{
		"data":   dataS,
		"appKey": a.key,
		"sign":   sign,
	}
	marshal, _ := json.Marshal(m)
	a.r.Body = ioutil.NopCloser(bytes.NewBuffer(marshal))
	a.r.ContentLength = int64(len(marshal))

	return sign, nil
}

func sortAndHash(values url.Values, secret string) string {
	sortedURLValues := NewSortedURLValues(values)
	keyValues := sortedURLValues.SortedKeyValues()
	queryURI := strings.Join(keyValues, "&")
	queryURI += secret

	hash := sha512.New()
	_, _ = hash.Write([]byte(queryURI))
	sign := hash.Sum(nil)
	return fmt.Sprintf("%x", string(sign))
}

type SortedURLValues struct {
	url.Values
	keys []string
}

func NewSortedURLValues(values url.Values) *SortedURLValues {
	sv := SortedURLValues{
		Values: values,
		keys:   nil,
	}
	for k := range sv.Values {
		sv.keys = append(sv.keys, k)
	}
	sort.Strings(sv.keys)
	return &sv
}

func (sv *SortedURLValues) SortedKeyValues() []string {
	var result []string
	for _, k := range sv.keys {
		values := sv.Values[k]
		sort.Strings(values)
		for _, v := range values {
			result = append(result, k+"="+v)
		}
	}
	return result
}
