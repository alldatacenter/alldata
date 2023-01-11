package deviceshifuplc4x

import (
	"os"
	"path"
	"testing"

	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/logger"
	"gopkg.in/yaml.v3"

	"k8s.io/apimachinery/pkg/util/wait"
)

// Str and default value
const (
	MockDeviceCmStr              = "configmap_snippet.yaml"
	MockDeviceWritFilePermission = 0644
	MockDeviceConfigPath         = "etc"
)

var MockDeviceConfigFolder = path.Join("etc", "edgedevice", "config")

type ConfigMapData struct {
	Data struct {
		DriverProperties string `yaml:"driverProperties"`
		Instructions     string `yaml:"instructions"`
		Telemetries      string `yaml:"telemetries"`
	} `yaml:"data"`
}

func TestMain(m *testing.M) {
	err := GenerateConfigMapFromSnippet(MockDeviceCmStr, MockDeviceConfigFolder)
	if err != nil {
		logger.Errorf("error when generateConfigmapFromSnippet,err: %v", err)
		os.Exit(-1)
	}
	m.Run()
	err = os.RemoveAll(MockDeviceConfigPath)
	if err != nil {
		logger.Fatal(err)
	}
}

func TestStart(t *testing.T) {
	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
		Name:           "TestStart",
		ConfigFilePath: "etc/edgedevice/config",
		KubeConfigPath: deviceshifubase.DeviceKubeconfigDoNotLoadStr,
	}

	mockds, err := New(deviceShifuMetadata)
	if err != nil {
		t.Errorf("Failed creating new deviceshifu")
	}

	if err := mockds.Start(wait.NeverStop); err != nil {
		t.Errorf("DeviceShifu.Start failed due to: %v", err.Error())
	}

	if err := mockds.Stop(); err != nil {
		t.Errorf("unable to stop mock deviceShifu, error: %+v", err)
	}
}

func GenerateConfigMapFromSnippet(fileName string, folder string) error {
	snippetFile, err := os.ReadFile(fileName)
	if err != nil {
		return err
	}

	var cmData ConfigMapData
	err = yaml.Unmarshal(snippetFile, &cmData)
	if err != nil {
		logger.Fatalf("Error parsing ConfigMap %v, error: %v", fileName, err)
		return err
	}

	var MockDeviceConfigMapping = map[string]string{
		path.Join(MockDeviceConfigFolder, deviceshifubase.ConfigmapDriverPropertiesStr): cmData.Data.DriverProperties,
		path.Join(MockDeviceConfigFolder, deviceshifubase.ConfigmapInstructionsStr):     cmData.Data.Instructions,
		path.Join(MockDeviceConfigFolder, deviceshifubase.ConfigmapTelemetriesStr):      cmData.Data.Telemetries,
	}

	err = os.MkdirAll(MockDeviceConfigFolder, os.ModePerm)
	if err != nil {
		logger.Fatalf("Error creating path for: %v", MockDeviceConfigFolder)
		return err
	}

	for outputDir, data := range MockDeviceConfigMapping {
		err = os.WriteFile(outputDir, []byte(data), MockDeviceWritFilePermission)
		if err != nil {
			logger.Fatalf("Error creating configFile for: %v", outputDir)
			return err
		}
	}
	return nil
}
