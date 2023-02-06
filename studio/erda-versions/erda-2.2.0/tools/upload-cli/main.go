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
	"os"

	"github.com/aliyun/aliyun-oss-go-sdk/oss"
)

func main() {
	keyID := os.Args[1]
	keySecret := os.Args[2]
	objName := os.Args[3]
	file := os.Args[4]
	// Create an OSSClient instance.
	client, err := oss.New("oss-cn-hangzhou.aliyuncs.com", keyID, keySecret)
	if err != nil {
		fmt.Println("Error:", err)
		os.Exit(-1)
	}

	bucket, err := client.Bucket("erda-release")
	if err != nil {
		fmt.Println("Error:", err)
		os.Exit(-1)
	}

	// Set the ACL and upload the object.
	objectAcl := oss.ObjectACL(oss.ACLPublicRead)
	err = bucket.PutObjectFromFile(objName, file, objectAcl)
	if err != nil {
		fmt.Println("Error:", err)
		os.Exit(-1)
	}

	fmt.Println("Upload success!")
}
