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

package template

//func ChangeString() {
//	aa := "\ncron: \"*/10 * * * *\"\n\n\ncron_compensator:\n  enable: true\n  latest_first: true\n\n\nversion: \"1.0\"\nstages:\n  - stage:\n      - git-checkout:\n          alias: git-checkout\n          resources:\n            cpu: 0.2\n            mem: 512\n          params:\n            depth: 1\n\n  - stage:\n      - java-build:\n          alias: java-build\n          version: \"1.0\"\n          resources:\n            cpu: 0.2\n            mem: 512\n          params:\n            build_cmd:\n               - \"mvn clean install -Dmaven.test.skip=true\"\n              \n            jdk_version: 8\n            workdir: ${git-checkout}\n\n  - stage:\n      - release:\n          params:\n            dice_yml: ${git-checkout}/dice.yml\n            services:\n              java-demo:\n                # 图形界面基于选择，当然可以自选\n                image: openjdk:8-jre-alpine\n                copys:\n                  - ${java-build:OUTPUT:buildPath}/target/docker-java-app-example.jar:/target\n                  - ${java-build:OUTPUT:buildPath}/spot-agent/spot-agent.jar:/spot-agent\n                cmd: java ${java-build:OUTPUT:JAVA_OPTS} -jar /target/docker-java-app-example.jar\n\n  - stage:\n      - dice:\n          params:\n            release_id: ${release:OUTPUT:releaseID}\n"
//	str := "\ncron: \"*/10 * * * *\"\n\n\ncron_compensator:\n  enable: true\n  latest_first: true\n\n\nversion: \"1.0\"\nstages:\n  - stage:\n      - git-checkout:\n          alias: git-checkout\n          resources:\n            cpu: 0.2\n            mem: 512\n          params:\n            depth: 1\n\n  - stage:\n      - java-build:\n          alias: java-build\n          version: \"1.0\"\n          resources:\n            cpu: 0.2\n            mem: 512\n          params:\n            build_cmd:\n               - \"mvn clean install -Dmaven.test.skip=true\"\n              \n            jdk_version: 8\n            workdir: ${git-checkout}\n\n  - stage:\n      - release:\n          params:\n            dice_yml: ${git-checkout}/dice.yml\n            services:\n              java-demo:\n                # 图形界面基于选择，当然可以自选\n                image: openjdk:8-jre-alpine\n                copys:\n                  - ${java-build:OUTPUT:buildPath}/target/docker-java-app-example.jar:/target\n                  - ${java-build:OUTPUT:buildPath}/spot-agent/spot-agent.jar:/spot-agent\n                cmd: java ${java-build:OUTPUT:JAVA_OPTS} -jar /target/docker-java-app-example.jar\n\n  - stage:\n      - dice:\n          params:\n            release_id: ${release:OUTPUT:releaseID}\n"
//	str = strings.ReplaceAll(str, "\n", "\r\n")
//	ma := make(map[string]string)
//	ma["spec"] = str
//	aaa, _ := json.Marshal(ma)
//	fmt.Println(string(aaa))
//}

// func TestRenderTemplate(t *testing.T) {
//ChangeString()
// specYaml, err := ioutil.ReadFile("/Users/terminus/go/src/terminus.io/dice/extensions/template/custom-script-output/1.0/spec.yml")
// if err != nil {
// 	fmt.Println(err)
// 	t.Fail()
// 	return
// }
// ma := make(map[string]string)
// ma["spec"] = string(specYaml)

// str, err := json.Marshal(ma)
// if err != nil {
// 	fmt.Println(err)
// 	fmt.Println("----------")
// }
// fmt.Println(string(str))

//tests := []struct {
//	params map[string]interface{}
//}{
//	{
//		//params: map[string]interface{}{
//		//	"jdk_version":         "8",
//		//	"build_cmd":           []string{"mvn clean install -Dmaven.test.skip=true"},
//		//	"jar_name":            "docker-java-app-example.jar",
//		//	"pipeline_cron":       "*/10 * * * *",
//		//	"pipeline_scheduling": "new_to_old",
//		//},
//		params: map[string]interface{}{
//			"sleep_time":          1,
//			"sleep_time_one":      2,
//			"jar_name":            "docker-java-app-example.jar",
//			"pipeline_cron":       "*/10 * * * *",
//			"pipeline_scheduling": "new_to_old",
//		},
//	},
//}
//
//for _, v := range tests {
//	template, err := RenderTemplate(string(specYaml), v.params, "git-java-boot-release")
//	if err != nil {
//		fmt.Println(err)
//		t.Fail()
//	}
//
//	fmt.Println(template)
//}

// }
