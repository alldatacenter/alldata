package deviceshifuopcua

import (
	"os"
	"path"
	"reflect"
	"testing"

	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/logger"

	"gopkg.in/yaml.v3"
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

func TestNewDeviceShifuConfig(t *testing.T) {
	var (
		InstructionNameGetValue               = "get_value"
		InstructionNameGetTime                = "get_time"
		InstructionNameGetServerVersion       = "get_server"
		InstructionNodeIDValue                = "ns=2;i=2"
		InstructionNodeIDTime                 = "i=2258"
		InstructionNodeIDServerVersion        = "i=2261"
		TelemetryMs1000Int64            int64 = 1000
		TelemetryMs3000Int64            int64 = 3000
		TelemetryBoolFalse                    = false
	)

	var DriverProperties = deviceshifubase.DeviceShifuDriverProperties{
		DriverSku:       "Edgenesis Mock Device",
		DriverImage:     "edgenesis/mockdevice:v0.0.1",
		DriverExecution: "python mock_driver.py",
	}

	var mockDeviceInstructions = map[string]*OPCUAInstruction{
		InstructionNameGetValue: {
			&OPCUAInstructionProperty{
				OPCUANodeID: InstructionNodeIDValue,
			},
		},
		InstructionNameGetTime: {
			&OPCUAInstructionProperty{
				OPCUANodeID: InstructionNodeIDTime,
			},
		},
		InstructionNameGetServerVersion: {
			&OPCUAInstructionProperty{
				OPCUANodeID: InstructionNodeIDServerVersion,
			},
		},
	}

	var mockDeviceTelemetries = &deviceshifubase.DeviceShifuTelemetries{
		DeviceShifuTelemetrySettings: &deviceshifubase.DeviceShifuTelemetrySettings{
			DeviceShifuTelemetryUpdateIntervalInMilliseconds: &TelemetryMs1000Int64,
			DeviceShifuTelemetryTimeoutInMilliseconds:        &TelemetryMs3000Int64,
			DeviceShifuTelemetryInitialDelayInMilliseconds:   &TelemetryMs3000Int64,
			DeviceShifuTelemetryDefaultPushToServer:          &TelemetryBoolFalse,
		},
		DeviceShifuTelemetries: map[string]*deviceshifubase.DeviceShifuTelemetry{
			"device_health": {
				DeviceShifuTelemetryProperties: deviceshifubase.DeviceShifuTelemetryProperties{
					DeviceInstructionName: &InstructionNameGetServerVersion,
					InitialDelayMs:        &TelemetryMs1000Int64,
					IntervalMs:            &TelemetryMs1000Int64,
				},
			},
		},
	}

	mockdsc, err := deviceshifubase.NewDeviceShifuConfig(MockDeviceConfigFolder)
	if err != nil {
		t.Errorf(err.Error())
	}

	mockintructions := CreateOPCUAInstructions(&mockdsc.Instructions)

	eq := reflect.DeepEqual(DriverProperties, mockdsc.DriverProperties)
	if !eq {
		t.Errorf("DriverProperties mismatch")
	}

	eq = reflect.DeepEqual(mockDeviceInstructions, mockintructions.Instructions)
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
