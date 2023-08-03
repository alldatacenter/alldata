package deviceshifuhttp

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
	MockDeviceCmStr2             = "configmap_snippet2.yaml"
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
		TelemetryInstructionNameGetStatus         = "get_status"
		TelemetryInstructionNameGetReading        = "get_reading"
		InstructionValueTypeInt32                 = "Int32"
		InstructionReadWriteW                     = "W"
		TelemetryMs1000                           = int64(1000)
		TelemetryMs3000                           = int64(3000)
		TelemetryMs6000                           = int64(6000)
		TelemetrySettingsDefaultPushToServer      = true
		TelemetrySettingsDefaultCollectionService = "push-endpoint-1"
		TelemetrySettingsPushToServerFalse        = false
		TelmeetrySettingsCollectionService2       = "push-endpoint-2"
	)

	var DriverProperties = deviceshifubase.DeviceShifuDriverProperties{
		DriverSku:       "Edgenesis Mock Device",
		DriverImage:     "edgenesis/mockdevice:v0.0.1",
		DriverExecution: "python mock_driver.py",
	}

	var mockDeviceInstructions = map[string]*deviceshifubase.DeviceShifuInstruction{
		"get_reading": nil,
		"get_status":  nil,
		"set_reading": {
			DeviceShifuInstructionProperties: []deviceshifubase.DeviceShifuInstructionProperty{
				{
					ValueType:    InstructionValueTypeInt32,
					ReadWrite:    InstructionReadWriteW,
					DefaultValue: nil,
				},
			},
			DeviceShifuProtocolProperties: nil,
		},
		"start": nil,
		"stop":  nil,
	}

	var mockDeviceTelemetries = map[string]*deviceshifubase.DeviceShifuTelemetry{
		"device_health": {
			DeviceShifuTelemetryProperties: deviceshifubase.DeviceShifuTelemetryProperties{
				DeviceInstructionName: &TelemetryInstructionNameGetStatus,
				InitialDelayMs:        &TelemetryMs1000,
				IntervalMs:            &TelemetryMs1000,
				PushSettings: &deviceshifubase.DeviceShifuTelemetryPushSettings{
					DeviceShifuTelemetryCollectionService: &TelmeetrySettingsCollectionService2,
				},
			},
		},
		"get_reading": {
			DeviceShifuTelemetryProperties: deviceshifubase.DeviceShifuTelemetryProperties{
				DeviceInstructionName: &TelemetryInstructionNameGetReading,
				InitialDelayMs:        &TelemetryMs1000,
				IntervalMs:            &TelemetryMs1000,
			},
		},
		"device_health2": {
			DeviceShifuTelemetryProperties: deviceshifubase.DeviceShifuTelemetryProperties{
				DeviceInstructionName: &TelemetryInstructionNameGetReading,
				InitialDelayMs:        &TelemetryMs1000,
				IntervalMs:            &TelemetryMs1000,
				PushSettings: &deviceshifubase.DeviceShifuTelemetryPushSettings{
					DeviceShifuTelemetryPushToServer: &TelemetrySettingsPushToServerFalse,
				},
			},
		},
	}

	var mockDeviceTelemetrySettings = deviceshifubase.DeviceShifuTelemetrySettings{
		DeviceShifuTelemetryUpdateIntervalInMilliseconds: &TelemetryMs6000,
		DeviceShifuTelemetryTimeoutInMilliseconds:        &TelemetryMs3000,
		DeviceShifuTelemetryInitialDelayInMilliseconds:   &TelemetryMs3000,
		DeviceShifuTelemetryDefaultPushToServer:          &TelemetrySettingsDefaultPushToServer,
		DeviceShifuTelemetryDefaultCollectionService:     &TelemetrySettingsDefaultCollectionService,
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

	eq = reflect.DeepEqual(&mockDeviceTelemetrySettings, mockdsc.Telemetries.DeviceShifuTelemetrySettings)
	if !eq {
		t.Errorf("TelemetrySettings mismatch")
	}

	eq = reflect.DeepEqual(mockDeviceTelemetries, mockdsc.Telemetries.DeviceShifuTelemetries)
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
