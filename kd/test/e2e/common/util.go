/*
Copyright 2022 The Koordinator Authors.
Copyright 2016 The Kubernetes Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package common

import (
	"bytes"
	"text/template"

	"k8s.io/apimachinery/pkg/util/sets"
	imageutils "k8s.io/kubernetes/test/utils/image"

	"github.com/koordinator-sh/koordinator/test/e2e/framework"
)

// TODO: Cleanup this file.

// Suite represents test suite.
type Suite string

const (
	// E2E represents a test suite for e2e.
	E2E Suite = "e2e"
)

// CurrentSuite represents current test suite.
var CurrentSuite Suite

// PrePulledImages are a list of images used in e2e/common tests. These images should be prepulled
// before tests starts, so that the tests won't fail due image pulling flakes.
// Currently, this is only used by node e2e test.
// See also updateImageAllowList() in ../../e2e_node/image_list.go
// TODO(random-liu): Change the image puller pod to use similar mechanism.
var PrePulledImages = sets.NewString(
	imageutils.GetE2EImage(imageutils.Agnhost),
	imageutils.GetE2EImage(imageutils.BusyBox),
	imageutils.GetE2EImage(imageutils.IpcUtils),
	imageutils.GetE2EImage(imageutils.Nginx),
	imageutils.GetE2EImage(imageutils.Httpd),
	imageutils.GetE2EImage(imageutils.VolumeNFSServer),
	imageutils.GetE2EImage(imageutils.VolumeGlusterServer),
	imageutils.GetE2EImage(imageutils.NonRoot),
)

type testImagesStruct struct {
	AgnhostImage  string
	BusyBoxImage  string
	KittenImage   string
	NautilusImage string
	NginxImage    string
	NginxNewImage string
	HttpdImage    string
	HttpdNewImage string
	PauseImage    string
	RedisImage    string
}

var testImages testImagesStruct

func init() {
	testImages = testImagesStruct{
		imageutils.GetE2EImage(imageutils.Agnhost),
		imageutils.GetE2EImage(imageutils.BusyBox),
		imageutils.GetE2EImage(imageutils.Kitten),
		imageutils.GetE2EImage(imageutils.Nautilus),
		imageutils.GetE2EImage(imageutils.Nginx),
		imageutils.GetE2EImage(imageutils.NginxNew),
		imageutils.GetE2EImage(imageutils.Httpd),
		imageutils.GetE2EImage(imageutils.HttpdNew),
		imageutils.GetE2EImage(imageutils.Pause),
		imageutils.GetE2EImage(imageutils.Redis),
	}
}

// SubstituteImageName replaces image name in content.
func SubstituteImageName(content string) []byte {
	contentWithImageName := new(bytes.Buffer)
	tmpl, err := template.New("imagemanifest").Parse(content)
	if err != nil {
		framework.Failf("Failed Parse the template: %v", err)
	}
	err = tmpl.Execute(contentWithImageName, testImages)
	if err != nil {
		framework.Failf("Failed executing template: %v", err)
	}
	return contentWithImageName.Bytes()
}
