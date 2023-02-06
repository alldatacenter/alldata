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

package dao

var dbClient *DBClient

// func init() {
// 	os.Setenv("MYSQL_HOST", "localhost")
// 	os.Setenv("MYSQL_PORT", "3306")
// 	os.Setenv("MYSQL_USERNAME", "")
// 	os.Setenv("MYSQL_PASSWORD", "")
// 	os.Setenv("MYSQL_DATABASE", "")
// 	url := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?charset=utf8&parseTime=True&loc=%s",
// 		os.Getenv("MYSQL_USERNAME"), os.Getenv("MYSQL_PASSWORD"), os.Getenv("MYSQL_HOST"),
// 		os.Getenv("MYSQL_PORT"), os.Getenv("MYSQL_DATABASE"), "Local")
//
// 	logrus.Debugf("Initialize db with %s, url: %s", DIALECT, url)
//
// 	db, err := gorm.Open(DIALECT, url)
// 	if err != nil {
// 		logrus.Fatal(err)
// 	}
// 	dbClient = &DBClient{
// 		db,
// 	}
// }

// func TestDBClient_ListNexusRepositories(t *testing.T) {
// 	repos, err := dbClient.ListNexusRepositories(apistructs.NexusRepositoryListRequest{
// 		Formats: []nexus.RepositoryFormat{nexus.RepositoryFormatNpm},
// 	})
// 	assert.NoError(t, err)
// 	for _, repo := range repos {
// 		b, _ := json.MarshalIndent(repo, "", "  ")
// 		fmt.Println(string(b))
// 	}
//
// 	fmt.Println("=============")
//
// 	repos, err = dbClient.ListNexusRepositories(apistructs.NexusRepositoryListRequest{
// 		NameContains: []string{"-proxy-org", "maven2-proxy-org-", "maven2-hosted-"},
// 	})
// 	assert.NoError(t, err)
// 	for _, repo := range repos {
// 		b, _ := json.MarshalIndent(repo, "", "  ")
// 		fmt.Println(string(b))
// 	}
// }
