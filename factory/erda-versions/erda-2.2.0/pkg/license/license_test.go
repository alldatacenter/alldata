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

package license

import (
	"fmt"
	"io/ioutil"
	"os"
	"testing"
)

func TestParseLicense(t *testing.T) {
	bytes, err := ioutil.ReadFile("license.json")
	if err != nil {
		panic(err)
	}
	licenseKey, err := AesEncrypt(string(bytes), aesKey)
	if err != nil {
		panic(err)
	}
	ioutil.WriteFile("license_key.txt", []byte(licenseKey), os.ModePerm)
	println(licenseKey)
	license, err := ParseLicense(licenseKey)
	if err != nil {
		panic(err)
	}
	fmt.Printf("%v", license.Data)
}
