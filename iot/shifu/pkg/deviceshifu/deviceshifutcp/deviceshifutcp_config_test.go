package deviceshifutcp

import (
	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/logger"
	"k8s.io/client-go/tools/clientcmd"
	"os"
	"path"
	"reflect"
	"testing"

	"gopkg.in/yaml.v3"
	clientcmdapi "k8s.io/client-go/tools/clientcmd/api"
)

// Str and default value
const (
	MockDeviceCmStr              = "configmap_snippet.yaml"
	MockDeviceWritFilePermission = 0644
	MockConfigFile               = "mockconfig"
	UnitTestAddress              = "localhost:23266"
	UnitTestProxyAddress         = "localhost:23288"
)

var MockDeviceConfigFolder = path.Join("etc", "edgedevice", "config")

type ConfigMapData struct {
	Data struct {
		DriverProperties string `yaml:"driverProperties"`
		Instructions     string `yaml:"instructions"`
		Telemetries      string `yaml:"telemetries"`
	} `yaml:"data"`
}

func TestNewDeviceShifuConfig(t *testing.T) {
	var (
		TelemetryMs1000Int64 int64 = 1000
		TelemetryMs3000Int64 int64 = 3000
		TelemetryBoolFalse   bool  = false
	)

	var DriverProperties = deviceshifubase.DeviceShifuDriverProperties{
		DriverSku:       "Edgenesis Mock Device",
		DriverImage:     "edgenesis/mockdevice:v0.0.1",
		DriverExecution: "defaultExecution",
	}

	var mockDeviceInstructions = map[string]*deviceshifubase.DeviceShifuInstruction{
		"cmd": nil,
	}

	var mockDeviceTelemetries = &deviceshifubase.DeviceShifuTelemetries{
		DeviceShifuTelemetrySettings: &deviceshifubase.DeviceShifuTelemetrySettings{
			DeviceShifuTelemetryUpdateIntervalInMilliseconds: &TelemetryMs1000Int64,
			DeviceShifuTelemetryTimeoutInMilliseconds:        &TelemetryMs3000Int64,
			DeviceShifuTelemetryInitialDelayInMilliseconds:   &TelemetryMs3000Int64,
			DeviceShifuTelemetryDefaultPushToServer:          &TelemetryBoolFalse,
		},
		DeviceShifuTelemetries: make(map[string]*deviceshifubase.DeviceShifuTelemetry),
	}

	mockdsc, err := deviceshifubase.NewDeviceShifuConfig(MockDeviceConfigFolder)
	if err != nil {
		t.Errorf(err.Error())
	}

	eq := reflect.DeepEqual(DriverProperties, mockdsc.DriverProperties)
	if !eq {
		t.Errorf("DriverProperties mismatch")
	}

	eq = reflect.DeepEqual(mockDeviceInstructions, mockdsc.Instructions.Instructions)
	if !eq {
		t.Errorf("Instruction mismatch")
	}

	eq = reflect.DeepEqual(mockDeviceTelemetries, mockdsc.Telemetries)
	if !eq {
		t.Errorf("Telemetries mismatch")
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

func writeMockConfigFile(t *testing.T, serverURL string) {
	fakeConfig := clientcmdapi.NewConfig()
	fakeConfig.APIVersion = "v1"
	fakeConfig.CurrentContext = "alpha"

	fakeConfig.Clusters["alpha"] = &clientcmdapi.Cluster{
		Server:                serverURL,
		InsecureSkipTLSVerify: true,
	}

	fakeConfig.Contexts["alpha"] = &clientcmdapi.Context{
		Cluster: "alpha",
	}

	err := clientcmd.WriteToFile(*fakeConfig, MockConfigFile)
	if err != nil {
		t.Errorf("write mock file failed")
	}

}
