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

package collector

import (
	"bytes"
	"compress/gzip"
	"encoding/base64"
	"errors"
	"io"
	"io/ioutil"
	"net/http"

	"github.com/sirupsen/logrus"
)

func isJSONArray(b []byte) bool {
	x := bytes.TrimLeft(b, " \t\r\n")
	return len(x) > 0 && x[0] == '['
}

// ReadGzip .
func ReadGzip(body io.ReadCloser) ([]byte, error) {
	gzipReader, err := gzip.NewReader(body)
	if err != nil {
		return []byte{}, err
	}
	defer gzipReader.Close()
	return ioutil.ReadAll(gzipReader)
}

// ReadRequestBody .
func ReadRequestBody(req *http.Request) ([]byte, error) {
	defer func() {
		req.Body.Close()
	}()

	reader, err := getBodyReader(req)
	if err != nil {
		return []byte{}, err
	}

	reader, err = getCustomEncodingReader(req, reader)
	if err != nil {
		return []byte{}, err
	}

	res, err := ioutil.ReadAll(reader)
	return res, err
}

// ReadRequestBodyReader .
func ReadRequestBodyReader(req *http.Request) (io.Reader, error) {
	reader, err := getBodyReader(req)
	if err != nil {
		return nil, err
	}

	return getCustomEncodingReader(req, reader)
}

func getCustomEncodingReader(req *http.Request, reader io.Reader) (io.Reader, error) {
	ccEncoding := req.Header.Get("Custom-Content-Encoding")
	logrus.Debugf("read Custom-Content-Encoding: %s", ccEncoding)
	if ccEncoding == "" {
		return reader, nil
	} else if ccEncoding == "base64" {
		return base64.NewDecoder(base64.StdEncoding, reader), nil
	}
	return nil, errors.New("unsupported custom-content-encoding")
}

func getBodyReader(req *http.Request) (io.Reader, error) {
	contentEncoding := req.Header.Get("Content-Encoding")
	if contentEncoding == "gzip" {
		gzipReader, err := gzip.NewReader(req.Body)
		if err != nil {
			return nil, err
		}
		return gzipReader, nil
	}
	return req.Body, nil
}
