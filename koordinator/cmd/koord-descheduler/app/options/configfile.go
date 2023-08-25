/*
Copyright 2022 The Koordinator Authors.

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

package options

import (
	"bytes"
	"fmt"
	"io"
	"os"

	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/klog/v2"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	deschedulerconfigscheme "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/scheme"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/v1alpha2"
)

func loadConfigFromFile(file string) (*deschedulerconfig.DeschedulerConfiguration, error) {
	data, err := os.ReadFile(file)
	if err != nil {
		return nil, err
	}

	return loadConfig(data)
}

func loadConfig(data []byte) (*deschedulerconfig.DeschedulerConfiguration, error) {
	// The UniversalDecoder runs defaulting and returns the internal type by default.
	obj, gvk, err := deschedulerconfigscheme.Codecs.UniversalDecoder().Decode(data, nil, nil)
	if err != nil {
		return nil, err
	}
	if cfgObj, ok := obj.(*deschedulerconfig.DeschedulerConfiguration); ok {
		// the field will be cleared later by API machinery during
		// conversion. See DeschedulerConfiguration internal type definition for
		// more details.
		cfgObj.TypeMeta.APIVersion = gvk.GroupVersion().String()
		return cfgObj, nil
	}
	return nil, fmt.Errorf("couldn't decode as DeschedulerConfiguration, got %s: ", gvk)
}

func encodeConfig(cfg *deschedulerconfig.DeschedulerConfiguration) (*bytes.Buffer, error) {
	buf := new(bytes.Buffer)
	const mediaType = runtime.ContentTypeYAML
	info, ok := runtime.SerializerInfoForMediaType(deschedulerconfigscheme.Codecs.SupportedMediaTypes(), mediaType)
	if !ok {
		return buf, fmt.Errorf("unable to locate encoder -- %q is not a supported media type", mediaType)
	}

	var encoder runtime.Encoder
	switch cfg.TypeMeta.APIVersion {
	case v1alpha2.SchemeGroupVersion.String():
		encoder = deschedulerconfigscheme.Codecs.EncoderForVersion(info.Serializer, v1alpha2.SchemeGroupVersion)
	default:
		encoder = deschedulerconfigscheme.Codecs.EncoderForVersion(info.Serializer, v1alpha2.SchemeGroupVersion)
	}
	if err := encoder.Encode(cfg, buf); err != nil {
		return buf, err
	}
	return buf, nil
}

// LogOrWriteConfig logs the completed component config and writes it into the given file name as YAML, if either is enabled
func LogOrWriteConfig(fileName string, cfg *deschedulerconfig.DeschedulerConfiguration, completedProfiles []deschedulerconfig.DeschedulerProfile) error {
	klogV := klog.V(2)
	if !klogV.Enabled() && len(fileName) == 0 {
		return nil
	}
	cfg.Profiles = completedProfiles

	buf, err := encodeConfig(cfg)
	if err != nil {
		return err
	}

	if klogV.Enabled() {
		klogV.InfoS("Using component config", "config", buf.String())
	}

	if len(fileName) > 0 {
		configFile, err := os.Create(fileName)
		if err != nil {
			return err
		}
		defer configFile.Close()
		if _, err := io.Copy(configFile, buf); err != nil {
			return err
		}
		klog.InfoS("Wrote configuration", "file", fileName)
		os.Exit(0)
	}
	return nil
}
