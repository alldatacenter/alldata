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

package i18n

import (
	"testing"
)

func TestLocaleLoader(t *testing.T) {
	loader := NewLoader()
	err := loader.LoadFile("test-locale1.json", "test-locale2.json", "test-locale.yml")
	if err != nil {
		panic(err)
	}
	println(loader.Locale(ZH).Get("dice.not_login"))
	println(loader.Locale(EN).Get("dice.not_login"))
	println(loader.Locale(ZH).Get("dice.multiline"))

}

func TestLocaleLoaderTemplate(t *testing.T) {
	loader := NewLoader()
	err := loader.LoadFile("test-locale1.json", "test-locale2.json", "test-locale.yml")
	if err != nil {
		panic(err)
	}
	template := loader.Locale(ZH).GetTemplate("dice.resource_not_found")
	println(template.RenderByKey(map[string]string{
		"name": "11",
	}))
}
