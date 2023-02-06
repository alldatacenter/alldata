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

package main

import (
	"fmt"
	"io/ioutil"
	"os"
	"strings"
)

//go:generate go run gen_auto_register.go
func main() {
	var buf strings.Builder
	fs, err := ioutil.ReadDir("../scenarios")
	if err != nil {
		panic(err)
	}
	comps := make(map[string][]string)
	for _, f := range fs {
		if f.IsDir() {
			scenarioName := f.Name()
			cs, err := ioutil.ReadDir("../scenarios/" + scenarioName + "/components")
			if err != nil {
				panic(err)
			}
			comps[scenarioName] = make([]string, 0)
			for _, c := range cs {
				if c.IsDir() {
					compName := c.Name()
					comps[scenarioName] = append(comps[scenarioName], compName)
				}
			}
		}
	}
	buf.WriteString("//generated file, DO NOT EDIT\n")
	buf.WriteString("package auto_register\n")
	buf.WriteString("import (\n")
	buf.WriteString("\t\"github.com/sirupsen/logrus\"\n")
	buf.WriteString("\t\"gopkg.in/yaml.v3\"\n")
	buf.WriteString("\n")
	buf.WriteString("\t\"github.com/erda-project/erda/apistructs\"\n")
	buf.WriteString("\tprotocol \"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol\"\n")
	for s, v := range comps {
		for _, c := range v {
			buf.WriteString(fmt.Sprintf(
				"\t%s \"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/%s/components/%s\"\n",
				strings.Replace(s, "-", "", -1)+c, s, c))
		}
	}
	buf.WriteString(")\n")
	buf.WriteString("\n")
	buf.WriteString("func RegisterAll() {\n")
	buf.WriteString("\tspecs := []*protocol.CompRenderSpec{\n")
	for s, v := range comps {
		for _, c := range v {
			buf.WriteString(fmt.Sprintf("\t\t{Scenario: \"%s\", CompName: \"%s\", RenderC: %s.RenderCreator},\n",
				s, c, strings.Replace(s, "-", "", -1)+c))
		}
	}
	buf.WriteString("\t}\n")
	buf.WriteString("\n")

	buf.WriteString("\tfor _, s := range specs {\n")
	buf.WriteString("\t\tif err := protocol.Register(s); err != nil {\n")
	buf.WriteString("\t\t\tlogrus.Errorf(\"register render failed, scenario: %v, components: %v, err: %v\", s.Scenario, s.CompName, err)\n")
	buf.WriteString("\t\t\tpanic(err)\n")
	buf.WriteString("\t\t}\n")
	buf.WriteString("\t}\n")
	buf.WriteString("\n")

	// TODO: use go:embed is better
	buf.WriteString("\tvar protocols = map[string]string{\n")
	for s := range comps {
		buf.WriteString(fmt.Sprintf("\t\t\"%s\": `\n", s))
		pData, err := ioutil.ReadFile("../scenarios/" + s + "/protocol.yml")
		if err != nil {
			panic(err)
		}
		buf.Write(pData)
		buf.WriteString("`,\n")
	}
	buf.WriteString("\t}\n")

	buf.WriteString("\tfor pName, pStr := range protocols {\n")
	buf.WriteString("\t\tvar p apistructs.ComponentProtocol\n")
	buf.WriteString("\t\tif err := yaml.Unmarshal([]byte(pStr), &p); err != nil {\n")
	buf.WriteString("\t\t\tpanic(err)\n")
	buf.WriteString("\t\t}\n")
	buf.WriteString("\t\tprotocol.DefaultProtocols[pName] = p\n")
	buf.WriteString("\t\tif protocol.CpPlaceHolderRe.Match([]byte(pStr)) {\n")
	buf.WriteString("\t\t\tprotocol.DefaultProtocolsRaw[pName] = pStr\n")
	buf.WriteString("\t\t}\n")
	buf.WriteString("\t}\n")

	buf.WriteString("}\n")
	f, err := os.OpenFile("./auto_register/auto_register.go", os.O_CREATE|os.O_TRUNC|os.O_RDWR, 0666)
	if err != nil {
		panic(err)
	}
	f.WriteString(buf.String())
}
