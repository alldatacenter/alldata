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

package pipelineyml

import (
	"fmt"
	"regexp"
	"strings"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/davecgh/go-spew/spew"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/strutil"
)

func TestOutputRegexp(t *testing.T) {
	reg := regexp.MustCompile(`\${([^:]+):OUTPUT:([^:]+)}`)

	assert.True(t, reg.MatchString("${git-checkout:OUTPUT:image}"))
	assert.True(t, reg.MatchString("${java:OUTPUT:image}"))
	assert.True(t, reg.MatchString("${release:OUTPUT:releaseID}"))
	assert.True(t, reg.MatchString("${中文:OUTPUT:releaseID}"))

	assert.False(t, reg.MatchString("${release:OUTPUT1:releaseID}"))
	assert.False(t, reg.MatchString("${:OUTPUT:releaseID}"))
	assert.False(t, reg.MatchString("${release:OUTPUT:}"))

	spew.Dump(reg.FindAllStringSubmatch(`
${release:OUTPUT:releaseID}
${java:OUTPUT:image}
${中文:OUTPUT:image}
${~:OUTPUT:image}
${{:OUTPUT:image}
`, -1))
}

func TestAllRef(t *testing.T) {
	reg := regexp.MustCompile(`\${([^:]+):([^:]+):(.+)}`)

	fmt.Println(strutil.ReplaceAllStringSubmatchFunc(reg, "${git-checkout:OUTPUT:image:xxx:xxx}", func(sub []string) string {
		fmt.Println(sub[0])
		alias := sub[1]
		op := sub[2]
		key := sub[3]
		return alias + "::" + op + "::" + key
	}))

	assert.True(t, reg.MatchString("${git-checkout:OUTPUT:image}"))
	assert.True(t, reg.MatchString("${git-checkout:OUTPUT:image:hello}"))
	assert.True(t, reg.MatchString("${git-check{out:OUTPUT:image:hello}"))
}

func TestRefOpVisitor(t *testing.T) {
	// test ${{ outputs.8230.aa.escape }}

	yml := `version: "1.1"
stages:
  - stage:
      - api-test:
          alias: "8231"
          version: "2.0"
          params:
            asserts:
              - arg: aa
                operator: not_empty
                value: ""
            body:
              content: null
              type: ""
            headers: []
            id: ""
            method: GET
            name: 获取yml文件
            out_params:
              - expression: data.meta.pipelineYml
                key: aa
                source: body:json
            params:
              - desc: ""
                key: scope
                value: project-app
              - desc: ""
                key: scopeID
                value: "2"
            url: /api/project-pipeline/filetree/Mi8yL3RyZWUvbWFzdGVyL3BpcGVsaW5lLnltbA%3D%3D?scopeID=2&scope=project-app
          labels:
            AUTOTESTTYPE: STEP
            STEP: eyJpZCI6ODIzMSwidHlwZSI6IkFQSSIsIm1ldGhvZCI6IiIsInZhbHVlIjoie1wiYXBpU3BlY1wiOntcImFzc2VydHNcIjpbe1wiYXJnXCI6XCJhYVwiLFwib3BlcmF0b3JcIjpcIm5vdF9lbXB0eVwiLFwidmFsdWVcIjpcIlwifV0sXCJib2R5XCI6e1wiY29udGVudFwiOm51bGwsXCJ0eXBlXCI6XCJcIn0sXCJoZWFkZXJzXCI6W10sXCJpZFwiOlwiXCIsXCJtZXRob2RcIjpcIkdFVFwiLFwibmFtZVwiOlwi6I635Y+WeW1s5paH5Lu2XCIsXCJvdXRfcGFyYW1zXCI6W3tcImV4cHJlc3Npb25cIjpcImRhdGEubWV0YS5waXBlbGluZVltbFwiLFwia2V5XCI6XCJhYVwiLFwic291cmNlXCI6XCJib2R5Ompzb25cIn1dLFwicGFyYW1zXCI6W3tcImRlc2NcIjpcIlwiLFwia2V5XCI6XCJzY29wZVwiLFwidmFsdWVcIjpcInByb2plY3QtYXBwXCJ9LHtcImRlc2NcIjpcIlwiLFwia2V5XCI6XCJzY29wZUlEXCIsXCJ2YWx1ZVwiOlwiMlwifV0sXCJ1cmxcIjpcIi9hcGkvcHJvamVjdC1waXBlbGluZS9maWxldHJlZS9NaTh5TDNSeVpXVXZiV0Z6ZEdWeUwzQnBjR1ZzYVc1bExubHRiQSUzRCUzRD9zY29wZUlEPTJcXHUwMDI2c2NvcGU9cHJvamVjdC1hcHBcIn0sXCJsb29wXCI6bnVsbH0iLCJuYW1lIjoi6I635Y+WeW1s5paH5Lu2IiwicHJlSUQiOjAsInByZVR5cGUiOiJTZXJpYWwiLCJzY2VuZUlEIjo5NTEsInNwYWNlSUQiOjcsImNyZWF0b3JJRCI6IiIsInVwZGF0ZXJJRCI6IiIsIkNoaWxkcmVuIjpudWxsLCJhcGlTcGVjSUQiOjB9
          if: ${{ 1 == 1 }}
  - stage:
      - api-test:
          alias: "8230"
          version: "2.0"
          params:
            asserts: []
            body:
              content: |-
                {
                  "pipelineYmlContent": ${{ outputs.8231.aa.escape }}
                }
              type: application/json
            headers:
              - desc: ""
                key: Content-Type
                value: application/json
            id: ""
            method: POST
            name: 图形展示
            out_params: []
            params: []
            url: http://pipeline.project-387-dev.svc.cluster.local:3081/api/pipelines/actions/pipeline-yml-graph
          labels:
            AUTOTESTTYPE: STEP
            STEP: eyJpZCI6ODIzMCwidHlwZSI6IkFQSSIsIm1ldGhvZCI6IiIsInZhbHVlIjoie1wiYXBpU3BlY1wiOntcImFzc2VydHNcIjpbXSxcImJvZHlcIjp7XCJjb250ZW50XCI6XCJ7XFxuICBcXFwicGlwZWxpbmVZbWxDb250ZW50XFxcIjogJHt7IG91dHB1dHMuODIzMS5hYS5xdW90ZSB9fVxcbn1cIixcInR5cGVcIjpcImFwcGxpY2F0aW9uL2pzb25cIn0sXCJoZWFkZXJzXCI6W3tcImRlc2NcIjpcIlwiLFwia2V5XCI6XCJDb250ZW50LVR5cGVcIixcInZhbHVlXCI6XCJhcHBsaWNhdGlvbi9qc29uXCJ9XSxcImlkXCI6XCJcIixcIm1ldGhvZFwiOlwiUE9TVFwiLFwibmFtZVwiOlwi5Zu+5b2i5bGV56S6XCIsXCJvdXRfcGFyYW1zXCI6W10sXCJwYXJhbXNcIjpbXSxcInVybFwiOlwiaHR0cDovL3BpcGVsaW5lLnByb2plY3QtMzg3LWRldi5zdmMuY2x1c3Rlci5sb2NhbDozMDgxL2FwaS9waXBlbGluZXMvYWN0aW9ucy9waXBlbGluZS15bWwtZ3JhcGhcIn0sXCJsb29wXCI6bnVsbH0iLCJuYW1lIjoi5Zu+5b2i5bGV56S6IiwicHJlSUQiOjgyMzEsInByZVR5cGUiOiJTZXJpYWwiLCJzY2VuZUlEIjo5NTEsInNwYWNlSUQiOjcsImNyZWF0b3JJRCI6IiIsInVwZGF0ZXJJRCI6IiIsIkNoaWxkcmVuIjpudWxsLCJhcGlTcGVjSUQiOjB9
          if: ${{ 1 == 1 }}
`
	outputs := Outputs{
		ActionAlias("8231"): map[string]string{
			"aa": `version: "1.1"
stages:
  - stage:
      - git-checkout:
          alias: git-checkout
          description: 代码仓库克隆
  - stage:
      - golang:
          alias: go-demo
          description: golang action
          params:
            command: go build -o web-server main.go
            context: ${git-checkout}
            service: web-server
            target: web-server
  - stage:
      - release:
          alias: release
          description: 用于打包完成时，向dicehub 提交完整可部署的dice.yml。用户若没在pipeline.yml里定义该action，CI会自动在pipeline.yml里插入该action
          params:
            dice_yml: ${git-checkout}/dice.yml
            image:
              go-demo: ${go-demo:OUTPUT:image}
  - stage:
      - dice:
          alias: dice
          description: 用于 dice 平台部署应用服务
          params:
            release_id: ${release:OUTPUT:releaseID}`,
		},
	}
	pipelineYml, err := New([]byte(yml), WithRefOpOutputs(outputs), WithAliasesToCheckRefOp(map[string]string{}, []ActionAlias{ActionAlias("8230"), ActionAlias("8231")}...))
	assert.Equal(t, nil, err)
	assert.Equal(t, true, strings.Contains(fmt.Sprintf("%v", pipelineYml.s.Stages[1].Actions[0]["api-test"].Params["body"]), "version:"))
}

func TestHandleOneRefOpOutput(t *testing.T) {
	v := &RefOpVisitor{
		availableOutputs: Outputs{
			ActionAlias("8231"): map[string]string{
				"aa": "version: \"1.1\"\n        stages:\n          - stage:\n              - git-checkout:\n                  alias: git-checkout\n                  description: 代码仓库克隆\n          - stage:\n              - golang:\n                  alias: go-demo\n                  description: golang action\n                  params:\n                    command: go build -o web-server main.go\n                    context: ${git-checkout}\n                    service: web-server\n                    target: web-server\n          - stage:\n              - release:\n                  alias: release\n                  description: 用于打包完成时，向dicehub 提交完整可部署的dice.yml。用户若没在pipeline.yml里定义该action，CI会自动在pipeline.yml里插入该action\n                  params:\n                    dice_yml: ${git-checkout}/dice.yml\n                    image:\n                      go-demo: ${go-demo:OUTPUT:image}\n          - stage:\n              - dice:\n                  alias: dice\n                  description: 用于 dice 平台部署应用服务\n                  params:\n                    release_id: ${release:OUTPUT:releaseID}",
			},
		},
	}
	ref := RefOp{
		Ref: "8231",
		Key: "aa",
		Ex:  "escape",
	}
	replaced := v.handleOneRefOpOutput(ref)
	assert.Equal(t, 0, len(v.result.Errs))
	assert.Equal(t, true, strings.Contains(replaced, "version:"))

	noExistEx := RefOp{
		Ref: "8231",
		Key: "aa",
		Ex:  "json",
	}
	v.handleOneRefOp(noExistEx)
	assert.Equal(t, 1, len(v.result.Errs))
}

func Test_handleRefEx(t *testing.T) {
	tests := []struct {
		name string
		arg  string
		ex   string
		want string
	}{
		{
			name: "single quote",
			arg:  "aaa",
			ex:   RefOpExWrapSingleQuotes,
			want: `'aaa'`,
		},
		{
			name: "double quote",
			arg:  "aaa",
			ex:   RefOpExWrapDoubleQuotes,
			want: `"aaa"`,
		},
	}
	v := &RefOpVisitor{}
	for _, tt := range tests {
		got := v.handleRefEx(tt.arg, RefOp{Ex: tt.ex})
		if got != tt.want {
			t.Errorf("got : %s but want: %s", got, tt.want)
		}
	}
}

func Test_handleOneParamOrCmdV2(t *testing.T) {
	yml := `version: "1.1"
stages:
  - stage:
      - api-test:
          alias: "8231"
          version: "2.0"
          params:
            headers: []
            id: ""
            method: GET
            params:
              - desc: ""
                key: scope
                value: project-app
              - desc: ""
                key: scopeID
                value: "2"
			  - desc: ""
				key: start
				value: ${{ random.datetime_hour.wrap_double_quotes }}
            url: /api/pipelines
`
	want := `version: "1.1"
stages:
  - stage:
      - api-test:
          alias: "8231"
          version: "2.0"
          params:
            headers: []
            id: ""
            method: GET
            params:
              - desc: ""
                key: scope
                value: project-app
              - desc: ""
                key: scopeID
                value: "2"
			  - desc: ""
				key: start
				value: %s
            url: /api/pipelines
`
	now := time.Now()
	nowSubtractHourStr := now.Add(-1 * time.Hour).Format("2006-01-02 15:04:05")
	pm1 := monkey.Patch(time.Now, func() time.Time {
		return now
	})
	defer pm1.Unpatch()
	visitor := &RefOpVisitor{
		currentAction: &indexedAction{
			stageIndex: 1,
		},
	}
	replaced := visitor.handleOneParamOrCmdV2(yml)
	assert.Equal(t, fmt.Sprintf(want, fmt.Sprintf(`"%s"`, nowSubtractHourStr)), replaced)
}

//func TestRef(t *testing.T) {
//	re := regexp.MustCompile(`\$\{\{.*\\}\\}`)
//
//	s := `${{java}}`
//	allMatch := re.FindAllString(s, -1)
//	assert.True(t, len(allMatch) == 1)
//	assert.Equal(t, s, allMatch[0])
//
//	s = `${HOME1:-xxx}`
//	allMatch = re.FindAllString(s, -1)
//	assert.True(t, len(allMatch) == 1)
//	assert.Equal(t, s, allMatch[0])
//
//	s = `${java:OUTPUT:image}`
//	allMatch = re.FindAllString(s, -1)
//	assert.True(t, len(allMatch) == 1)
//	assert.Equal(t, s, allMatch[0])
//
//	s1 := `${java}`
//	s2 := `${java:OUTPUT:image}`
//	s = strutil.Concat(s1, " ", s2)
//	allMatch = re.FindAllString(s, -1)
//	assert.True(t, len(allMatch) == 2)
//	assert.Equal(t, s1, allMatch[0])
//	assert.Equal(t, s2, allMatch[1])
//
//	s = `version: 1.1
//stages:
//- stage:
//  - custom-script:
//      commands:
//      - echo maintainer=lj > ${METAFILE}
//  - js:
//- stage:
//  - java:
//      params:
//        invalidref: ${js:OUTPUTS:image} ${js:OUTPUTSS:image}
//        ref: ${js:OUTPUT:image}
//        unknownop: ${js:XXX:images}
//        cs: ${custom-script:OUTPUT:maintainer}
//`
//	allMatch = re.FindAllString(s, -1)
//	spew.Dump(allMatch)
//}
//
//func TestReV2(t *testing.T) {
//	re := expression.Re
//
//	s := `${{ java }}`
//	allMatch := re.FindStringSubmatch(s)
//	assert.True(t, len(allMatch) == 1)
//	assert.Equal(t, s, allMatch[0])
//
//	s = `${{ HOME1:-xxx }}`
//	allMatch = re.FindStringSubmatch(s)
//	assert.True(t, len(allMatch) == 1)
//	assert.Equal(t, s, allMatch[0])
//
//	s = `${{ java:OUTPUT:image }}`
//	allMatch = re.FindStringSubmatch(s)
//	assert.True(t, len(allMatch) == 1)
//	assert.Equal(t, s, allMatch[0])
//
//	s1 := `${{ java }}`
//	s2 := `${{ java:OUTPUT:image }}`
//	s = strutil.Concat(s1, " ", s2)
//	allMatch = re.FindStringSubmatch(s)
//	assert.True(t, len(allMatch) == 2)
//	assert.Equal(t, s1, allMatch[0])
//	assert.Equal(t, s2, allMatch[1])
//
//	s = `version: 1.1
//stages:
//- stage:
//  - custom-script:
//      commands:
//      - echo maintainer=lj > ${{METAFILE}}
//  - js:
//- stage:
//  - java:
//      params:
//        invalidref: ${{js:OUTPUTS:image}} ${{js:OUTPUTSS:image}}
//        ref: ${{js:OUTPUT:image}}
//        ${{}}
//        unknownop: ${{js:XXX:images}}
//        cs: ${{custom-script:OUTPUT:maintainer}}
//`
//
//	strutil.ReplaceAllStringSubmatchFunc(re, s, func(strings []string) string {
//		for _, v := range strings {
//			fmt.Println(v)
//		}
//		return ""
//	})
//	allMatch = re.FindAllString(s, -1)
//	spew.Dump(allMatch)
//}
