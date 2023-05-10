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

package publish_item

import (
	"archive/zip"
	"bytes"
	"encoding/json"
	"image"
	"image/jpeg"
	"image/png"
	"io/ioutil"
	"mime/multipart"
	"os"
	"path/filepath"
	"regexp"
	"strings"

	"github.com/andrianbdn/iospng"
	"github.com/pkg/errors"
	"github.com/shogo82148/androidbinary/apk"
	"github.com/sirupsen/logrus"
	"howett.net/plist"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	"github.com/erda-project/erda/pkg/template"
)

var (
	reInfoPlist = regexp.MustCompile(`Payload/[^/]+/Info\.plist`)
	ErrNoIcon   = errors.New("icon not found")
)

type IOSPlist struct {
	CFBundleName         string         `plist:"CFBundleName"`
	CFBundleDisplayName  string         `plist:"CFBundleDisplayName"`
	CFBundleVersion      string         `plist:"CFBundleVersion"`
	CFBundleShortVersion string         `plist:"CFBundleShortVersionString"`
	CFBundleIdentifier   string         `plist:"CFBundleIdentifier"`
	CFBundleIcons        *CFBundleIcons `plist:"CFBundleIcons"`
}
type CFBundleIcons struct {
	CFBundlePrimaryIcon *CFBundlePrimaryIcon `plist:"CFBundlePrimaryIcon"`
}

type CFBundlePrimaryIcon struct {
	CFBundleIconFiles []string `plist:"CFBundleIconFiles"`
	CFBundleIconName  string   `plist:"CFBundleIconName"`
}
type IosAppInfo struct {
	Name     string
	BundleId string
	Version  string
	Build    string
	Icon     image.Image
	Size     int64
	IconName string
}

type AndroidAppInfo struct {
	PackageName string
	Version     string
	VersionCode int32
	Icon        image.Image
	DisplayName string
}

// isOffLineVersion 判断版本是不是离线包上传的版本
func isOffLineVersion(version *dbclient.PublishItemVersion) bool {
	var meta map[string]interface{}
	if err := json.Unmarshal([]byte(version.Meta), &meta); err != nil {
		logrus.Errorf("Judge is OffLineVersion err: %v", err)
		return false
	}
	if meta["projectName"].(string) == "OFFLINE" {
		return true
	}

	return false
}

func (i *PublishItem) UploadFileFromReader(fileHeader *multipart.FileHeader) (*apistructs.File, error) {
	fileName := fileHeader.Filename
	if fileName == "" {
		fileName = "file"
	}
	reader, err := fileHeader.Open()
	if err != nil {
		return nil, err
	}
	defer reader.Close()
	resp, err := i.bdl.UploadFile(apistructs.FileUploadRequest{
		From:            "release",
		IsPublic:        true,
		FileReader:      reader,
		FileNameWithExt: fileName,
	})
	if err != nil {
		return nil, err
	}

	// 兖矿临时修改
	v := strings.ReplaceAll(resp.DownloadURL, "http://dice.paas.ykjt.cc", "https://appstore.ykjt.cn")
	v = strings.ReplaceAll(v, "https://dice.paas.ykjt.cc", "https://appstore.ykjt.cn")
	resp.DownloadURL = v

	return resp, nil
}

func (i *PublishItem) UploadFileFromFile(filePath string) (*apistructs.File, error) {
	f, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	resp, err := i.bdl.UploadFile(apistructs.FileUploadRequest{
		From:            "release",
		IsPublic:        true,
		FileReader:      f,
		FileNameWithExt: filepath.Base(filePath),
	})
	if err != nil {
		return nil, err
	}

	// 兖矿临时修改
	v := strings.ReplaceAll(resp.DownloadURL, "http://dice.paas.ykjt.cc", "https://appstore.ykjt.cn")
	v = strings.ReplaceAll(v, "https://dice.paas.ykjt.cc", "https://appstore.ykjt.cn")
	resp.DownloadURL = v

	return resp, nil
}

func GetAndoridInfo(fileHeader *multipart.FileHeader) (*AndroidAppInfo, error) {
	info := &AndroidAppInfo{}
	formFile, err := fileHeader.Open()
	if err != nil {
		return nil, err
	}
	defer formFile.Close()
	pkg, err := apk.OpenZipReader(formFile, fileHeader.Size)
	if err != nil {
		return nil, err
	}
	defer pkg.Close()
	info.Version = pkg.Manifest().VersionName.MustString()
	info.VersionCode = pkg.Manifest().VersionCode.MustInt32()
	info.DisplayName = pkg.Manifest().App.Name.MustString()
	info.PackageName = pkg.PackageName()
	icon, err := pkg.Icon(nil)
	if err != nil {
		return nil, err
	}
	info.Icon = icon

	return info, nil
}

func GetIosInfo(fileHeader *multipart.FileHeader) (*IosAppInfo, error) {
	formFile, err := fileHeader.Open()
	if err != nil {
		return nil, err
	}
	defer formFile.Close()
	reader, err := zip.NewReader(formFile, fileHeader.Size)
	if err != nil {
		return nil, err
	}

	var plistFile *zip.File
	for _, f := range reader.File {
		if reInfoPlist.MatchString(f.Name) {
			plistFile = f
			break
		}
	}
	info, err := parseIpaFile(plistFile)
	if err != nil {
		return nil, err
	}

	var iosIconFile *zip.File
	for _, f := range reader.File {
		if strings.Contains(f.Name, info.IconName) {
			iosIconFile = f
			break
		}
	}
	icon, err := parseIpaIcon(iosIconFile)
	if err != nil {
		return nil, err
	}

	info.Icon = icon
	info.Size = fileHeader.Size

	return info, nil
}

func parseIpaFile(plistFile *zip.File) (*IosAppInfo, error) {
	if plistFile == nil {
		return nil, errors.New("info.plist not found")
	}

	rc, err := plistFile.Open()
	if err != nil {
		return nil, err
	}
	defer rc.Close()

	buf, err := ioutil.ReadAll(rc)
	if err != nil {
		return nil, err
	}

	p := new(IOSPlist)
	decoder := plist.NewDecoder(bytes.NewReader(buf))
	if err := decoder.Decode(p); err != nil {
		return nil, err
	}

	info := new(IosAppInfo)
	if p.CFBundleDisplayName == "" {
		info.Name = p.CFBundleName
	} else {
		info.Name = p.CFBundleDisplayName
	}
	info.BundleId = p.CFBundleIdentifier
	info.Version = p.CFBundleShortVersion
	info.Build = p.CFBundleVersion
	if p.CFBundleIcons != nil &&
		p.CFBundleIcons.CFBundlePrimaryIcon != nil &&
		p.CFBundleIcons.CFBundlePrimaryIcon.CFBundleIconFiles != nil &&
		len(p.CFBundleIcons.CFBundlePrimaryIcon.CFBundleIconFiles) > 0 {
		files := p.CFBundleIcons.CFBundlePrimaryIcon.CFBundleIconFiles
		info.IconName = files[len(files)-1]
	} else {
		info.IconName = "Icon.png"
	}
	return info, nil
}

func parseIpaIcon(iconFile *zip.File) (image.Image, error) {
	if iconFile == nil {
		return nil, ErrNoIcon
	}

	rc, err := iconFile.Open()
	if err != nil {
		return nil, err
	}
	defer rc.Close()

	var w bytes.Buffer
	iospng.PngRevertOptimization(rc, &w)

	return png.Decode(bytes.NewReader(w.Bytes()))
}

func GenerateInstallPlist(info *IosAppInfo, downloadUrl string) string {
	plistTemplate := `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
   <key>items</key>
   <array>
       <dict>
           <key>assets</key>
           <array>
               <dict>
                   <key>kind</key>
                   <string>software-package</string>
                   <key>url</key>
                   <string>{{appUrl}}</string>
               </dict>
           </array>
           <key>metadata</key>
           <dict>
               <key>bundle-identifier</key>
               <string>{{bundleId}}</string>
               <key>bundle-version</key>
               <string>{{version}}</string>
               <key>kind</key>
               <string>software</string>
               <key>subtitle</key>
               <string>{{displayName}}</string>
               <key>title</key>
               <string>{{displayName}}</string>
           </dict>
       </dict>
   </array>
</dict>
</plist>`
	plistContent := template.Render(plistTemplate, map[string]string{
		"bundleId":    info.BundleId,
		"version":     info.Version,
		"displayName": info.Name,
		"appUrl":      downloadUrl,
	})
	return plistContent
}

func GenrateTmpImagePath(id string) string {
	return "/tmp/logo-" + id + ".jpg"
}

func SaveImageToFile(icon image.Image, path string) error {
	out, err := os.Create(path)
	if err != nil {
		return err
	}
	defer out.Close()
	var opt jpeg.Options
	opt.Quality = 80
	err = jpeg.Encode(out, icon, &opt) // put quality to 80%
	return err
}
