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

package httpclient

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"mime/multipart"
	"net"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/http/customhttp"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/terminal/loading"
)

type Request struct {
	path     string
	header   map[string]string
	cookie   []*http.Cookie
	params   url.Values
	body     io.Reader
	err      error
	method   string
	host     string
	proto    string
	cli      *http.Client
	internal *http.Request

	option *Option

	tracer Tracer

	// retry
	retryOption RetryOption
}
type AfterDo struct{ *Request }

type RetryFn func() (*http.Response, error)

type MultipartItem struct {
	Reader io.ReadCloser
	// Filename 当 Reader 为 *os.File 时，该值在 Content-Disposition 中生效；默认取 *os.File 的 baseName
	// Content-Disposition: form-data; name=${fieldName}; filename=${Filename}
	// +optional
	Filename string
}

const (
	MAX_RETRY_TIMES_FOR_TIMEOUT = 2
)

func (r *Request) Do() AfterDo {
	if r.err != nil {
		return AfterDo{r}
	}
	if r.host == "" {
		r.err = errors.New("not found host")
		return AfterDo{r}
	}

	if strings.HasPrefix(r.host, "https://") {
		r.proto = "https"
		r.host = strings.TrimPrefix(r.host, "https://")
	} else if strings.HasPrefix(r.host, "http://") {
		r.proto = "http"
		r.host = strings.TrimPrefix(r.host, "http://")
	} else if strings.HasPrefix(r.host, "inet://") {
		r.proto = "inet"
		r.host = strings.TrimPrefix(r.host, "inet://")
	}

	_url := fmt.Sprintf("%s://%s%s", r.proto, r.host, r.path)
	if len(r.params) > 0 {
		_url += "?" + r.params.Encode()
	}

	req, err := customhttp.NewRequest(r.method, _url, r.body)
	if err != nil {
		r.err = err
		return AfterDo{r}
	}

	for k, v := range r.header {
		req.Header.Set(k, v)
	}

	// get goroutine context il8n header to bdl request
	localeName := i18n.GetGoroutineBindLang()
	if localeName != "" {
		req.Header.Set(i18n.LangHeader, localeName)
	}

	for _, v := range r.cookie {
		req.AddCookie(v)
	}

	if len(req.Header.Get("Accept")) == 0 {
		req.Header.Set("Accept", "application/json, */*")
	}

	r.internal = req

	return AfterDo{r}
}

func (r *Request) GetUrl() string {
	return fmt.Sprintf("%s://%s%s?%s", r.proto, r.host, r.path, r.params.Encode())
}

// 构造2个一样的 request
func dupRequest(req *http.Request) (*http.Request, *http.Request, error) {
	var bodybuf []byte
	if req.Body != nil {
		var err error
		bodybuf, err = ioutil.ReadAll(req.Body)
		if err != nil {
			return nil, nil, err
		}
	}
	dupbody1 := bytes.NewReader(bodybuf)
	dupbody2 := bytes.NewReader(bodybuf)
	req1, err := http.NewRequest(req.Method, req.URL.String(), dupbody1)
	if err != nil {
		return nil, nil, err
	}
	req2, err := http.NewRequest(req.Method, req.URL.String(), dupbody2)
	if err != nil {
		return nil, nil, err
	}
	req1.Header = req.Header
	req2.Header = req.Header

	return req1, req2, nil
}

func doRequest(r AfterDo) (*http.Response, error) {
	if r.err != nil {
		return nil, r.err
	}
	if r.tracer != nil {
		r.tracer.TraceRequest(r.internal)
	}
	var resp *http.Response
	var err error
	f := func() {
		fn := func() (*http.Response, error) {
			req1, req2, err := dupRequest(r.internal)
			if err != nil {
				return nil, err
			}
			r.internal = req1
			return r.cli.Do(req2)
		}
		if strings.Index(r.internal.Header.Get("Content-Type"), "multipart/form-data;") == 0 {
			//上传文件不复制request重试,防止OOM
			resp, err = r.cli.Do(r.internal)
		} else {
			resp, err = retry(r, fn)
		}
	}
	if r.option != nil && r.option.loadingPrint {
		if r.option.loadingDesc != "" {
			loading.Loading(r.option.loadingDesc, f, true, false)
		} else {
			loading.Loading(r.path, f, true, false)
		}
	} else {
		f()
	}
	if err != nil {
		return nil, err
	}
	if r.tracer != nil {
		r.tracer.TraceResponse(resp)
	}
	return resp, nil
}

func (r AfterDo) RAW() (*http.Response, error) {
	return doRequest(r)
}

func (r AfterDo) JSON(o interface{}) (*Response, error) {
	resp, err := doRequest(r)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	// check content-type before decode body
	contentType := resp.Header.Get("Content-Type")
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}
	if len(contentType) > 0 && !contentTypeIsJson(contentType) {
		return &Response{
			body:     body,
			internal: resp,
		}, nil
	}

	if err := json.Unmarshal(body, o); err != nil {
		return nil, fmt.Errorf("failed to Unmarshal JSON, err:%s, body :%s", err, string(body))
	}

	return &Response{
		body:     body,
		internal: resp,
	}, nil
}

// 适用于a) 如果成功，不关心body内容及其结构体；
//   并且b) 如果失败，需要把body内容封装进error里返回给上层定位错误
func (r AfterDo) Body(b *bytes.Buffer) (*Response, error) {
	resp, err := doRequest(r)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if _, err = io.Copy(b, resp.Body); err != nil {
		return nil, err
	}
	return &Response{
		internal: resp,
	}, nil
}

// StreamBody 返回 response body, 用于流式读取。
// 场景：k8s & marathon event.
// 注意：使用完后，需要自己 close body.
func (r AfterDo) StreamBody() (io.ReadCloser, *Response, error) {
	resp, err := doRequest(r)
	if err != nil {
		return nil, nil, err
	}

	return resp.Body, &Response{
		internal: resp,
	}, nil
}

func (r AfterDo) DiscardBody() (*Response, error) {
	resp, err := doRequest(r)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	io.Copy(ioutil.Discard, resp.Body)
	return &Response{
		internal: resp,
	}, nil
}

func (r *Request) Path(path string) *Request {
	r.path = path
	return r
}
func (r *Request) Param(k, v string) *Request {
	if r.params == nil {
		r.params = make(url.Values)
	}
	r.params.Add(k, v)
	return r
}

func (r *Request) Params(kvs url.Values) *Request {
	if r.params == nil {
		r.params = make(url.Values)
	}
	for k, v := range kvs {
		r.params[k] = append(r.params[k], v...)
	}
	return r
}

func (r *Request) JSONBody(o interface{}) *Request {
	b, err := json.Marshal(o)
	if err != nil {
		r.err = err
	}
	r.body = bytes.NewReader(b)
	r.Header("Content-Type", "application/json")
	return r
}

func (r *Request) RawBody(body io.Reader) *Request {
	r.body = body
	return r
}

func (r *Request) FormBody(form url.Values) *Request {
	r.body = strings.NewReader(form.Encode())
	r.Header("Content-Type", "application/x-www-form-urlencoded")
	return r
}

func (r *Request) MultipartFormDataBody(fields map[string]MultipartItem) *Request {
	pipeReader, pipeWriter := io.Pipe()
	w := multipart.NewWriter(pipeWriter)
	go func() {
		defer func() {
			for _, field := range fields {
				field.Reader.Close()
			}
		}()
		for field, item := range fields {
			var fw io.Writer
			var err error
			switch item.Reader.(type) {
			case *os.File:
				if item.Filename == "" {
					item.Filename = filepath.Base(item.Reader.(*os.File).Name())
				}
				fw, err = w.CreateFormFile(field, item.Filename)
			default:
				fw, err = w.CreateFormFile(field, item.Filename)
			}

			if err != nil {
				r.err = err
				return
			}
			if _, err := io.Copy(fw, item.Reader); err != nil {
				r.err = err
				return
			}
		}
		w.Close()
		pipeWriter.Close()
	}()
	r.body = pipeReader
	r.Header("Content-Type", w.FormDataContentType())
	return r
}

func (r *Request) Header(k, v string) *Request {
	r.header[k] = v
	return r
}
func (r *Request) Headers(hs http.Header) *Request {
	for k, v := range hs {
		if len(v) > 0 {
			r.header[k] = v[0]
		}
	}
	return r

}
func (r *Request) Cookie(v *http.Cookie) *Request {
	r.cookie = append(r.cookie, v)
	return r
}

func contentTypeIsJson(contentType string) bool {
	return strings.Contains(contentType, "json")
}

func retryIfTimeout(fn RetryFn) (*http.Response, error) {
	resp, err := fn()
	for i := 0; i < MAX_RETRY_TIMES_FOR_TIMEOUT; i++ {
		if nerr, ok := err.(net.Error); ok && nerr.Timeout() {
			resp, err = fn()
		} else {
			break
		}
	}
	return resp, err
}

func retry(r AfterDo, fn RetryFn) (*http.Response, error) {
	maxtime := r.retryOption.MaxTime
	fns := r.retryOption.Fns
	interval := r.retryOption.Interval

	var resp *http.Response
	var err error
	for i := 0; i < maxtime; i++ {
		resp, err = retryIfTimeout(fn)
		retry := false
		for _, fn := range fns {
			if fn(r.internal, resp, err) {
				retry = true
				time.Sleep(time.Duration(interval) * time.Second)
				break
			}
		}
		if !retry {
			return resp, err
		}
	}
	return resp, err
}
