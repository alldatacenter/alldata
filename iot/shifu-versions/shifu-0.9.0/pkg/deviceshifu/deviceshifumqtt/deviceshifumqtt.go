package deviceshifumqtt

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/edgenesis/shifu/pkg/deviceshifu/utils"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
)

// DeviceShifu implemented from deviceshifuBase
type DeviceShifu struct {
	base             *deviceshifubase.DeviceShifuBase
	mqttInstructions *MQTTInstructions
}

// HandlerMetaData MetaData for EdgeDevice Setting
type HandlerMetaData struct {
	edgeDeviceSpec v1alpha1.EdgeDeviceSpec
	instruction    string
	properties     *MQTTProtocolProperty
}

// Str and default value
const (
	DefaultUpdateIntervalInMS int64 = 3000
)

var (
	client                         mqtt.Client
	MQTTTopic                      string
	mqttMessageInstructionMap      = map[string]string{}
	mqttMessageReceiveTimestampMap = map[string]time.Time{}
)

// New new MQTT Deviceshifu
func New(deviceShifuMetadata *deviceshifubase.DeviceShifuMetaData) (*DeviceShifu, error) {
	base, mux, err := deviceshifubase.New(deviceShifuMetadata)
	if err != nil {
		return nil, err
	}

	mqttInstructions := CreateMQTTInstructions(&base.DeviceShifuConfig.Instructions)

	if deviceShifuMetadata.KubeConfigPath != deviceshifubase.DeviceKubeconfigDoNotLoadStr {
		switch protocol := *base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolMQTT:
			mqttProtocolSetting := base.EdgeDevice.Spec.ProtocolSettings
			if mqttProtocolSetting != nil {
				if mqttProtocolSetting.MQTTSetting != nil && mqttProtocolSetting.MQTTSetting.MQTTServerSecret != nil {
					logger.Infof("MQTT Server Secret is not empty, currently Shifu does not use MQTT Server Secret")
					// TODO Add MQTT Server secret processing logic
				}
			}

			opts := mqtt.NewClientOptions()
			opts.AddBroker(fmt.Sprintf("tcp://%s", *base.EdgeDevice.Spec.Address))
			opts.SetClientID(base.EdgeDevice.Name)
			opts.SetDefaultPublishHandler(messagePubHandler)
			opts.OnConnect = connectHandler
			opts.OnConnectionLost = connectLostHandler
			client = mqtt.NewClient(opts)
			if token := client.Connect(); token.Wait() && token.Error() != nil {
				panic(token.Error())
			}

			for instruction, properties := range mqttInstructions.Instructions {
				MQTTTopic = properties.MQTTProtocolProperty.MQTTTopic
				sub(client, MQTTTopic)

				HandlerMetaData := &HandlerMetaData{
					base.EdgeDevice.Spec,
					instruction,
					properties.MQTTProtocolProperty,
				}

				handler := DeviceCommandHandlerMQTT{HandlerMetaData}
				mux.HandleFunc("/"+instruction, handler.commandHandleFunc())
			}
		}
	}
	deviceshifubase.BindDefaultHandler(mux)

	ds := &DeviceShifu{
		base:             base,
		mqttInstructions: mqttInstructions,
	}

	ds.base.UpdateEdgeDeviceResourcePhase(v1alpha1.EdgeDevicePending)
	return ds, nil
}

var messagePubHandler mqtt.MessageHandler = func(client mqtt.Client, msg mqtt.Message) {
	logger.Infof("Received message: %v from topic: %v", msg.Payload(), msg.Topic())
	rawMqttMessageStr := string(msg.Payload())
	instructionFuncName, shouldUsePythonCustomProcessing := deviceshifubase.CustomInstructionsPython[msg.Topic()]
	logger.Infof("Topic %v is custom: %v", msg.Topic(), shouldUsePythonCustomProcessing)
	if shouldUsePythonCustomProcessing {
		logger.Infof("Topic %v has a python customized handler configured.\n", msg.Topic())
		mqttMessageInstructionMap[msg.Topic()] = utils.ProcessInstruction(deviceshifubase.PythonHandlersModuleName, instructionFuncName, rawMqttMessageStr, deviceshifubase.PythonScriptDir)
	} else {
		mqttMessageInstructionMap[msg.Topic()] = rawMqttMessageStr
	}
	mqttMessageReceiveTimestampMap[msg.Topic()] = time.Now()
	logger.Infof("MESSAGE_STR updated")
}

var connectHandler mqtt.OnConnectHandler = func(client mqtt.Client) {
	logger.Infof("Connected")
}

var connectLostHandler mqtt.ConnectionLostHandler = func(client mqtt.Client, err error) {
	logger.Infof("Connect lost: %v", err)
}

func sub(client mqtt.Client, topic string) {
	// topic := "topic/test"
	token := client.Subscribe(topic, 1, nil)
	token.Wait()
	logger.Infof("Subscribed to topic: %s", topic)
}

// DeviceCommandHandlerMQTT handler for Mqtt
type DeviceCommandHandlerMQTT struct {
	// client                         *rest.RESTClient
	HandlerMetaData *HandlerMetaData
}

func (handler DeviceCommandHandlerMQTT) commandHandleFunc() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		// handlerEdgeDeviceSpec := handler.HandlerMetaData.edgeDeviceSpec
		reqType := r.Method

		if reqType == http.MethodGet {
			returnMessage := ReturnBody{
				MQTTMessage:   mqttMessageInstructionMap[handler.HandlerMetaData.properties.MQTTTopic],
				MQTTTimestamp: mqttMessageReceiveTimestampMap[handler.HandlerMetaData.properties.MQTTTopic].String(),
			}

			w.WriteHeader(http.StatusOK)
			w.Header().Set("Content-Type", "application/json")
			err := json.NewEncoder(w).Encode(returnMessage)
			if err != nil {
				http.Error(w, "Cannot Encode message to json", http.StatusInternalServerError)
				logger.Errorf("Cannot Encode message to json")
				return
			}
		} else if reqType == http.MethodPost {
			body, err := io.ReadAll(r.Body)
			if err != nil {
				logger.Errorf("Error when Read Data From Body, error: %v", err)
				http.Error(w, err.Error(), http.StatusBadRequest)
				return
			}

			mqttTopic := handler.HandlerMetaData.properties.MQTTTopic
			requestBody := RequestBody(body)
			logger.Infof("requestBody: %v", requestBody)

			// TODO handle error asynchronously
			token := client.Publish(mqttTopic, 1, false, body)
			if token.Error() != nil {
				logger.Errorf("Error when publish Data to MQTTServer,%v", token.Error())
				http.Error(w, "Error to publish a message to server", http.StatusBadRequest)
				return
			}
			logger.Infof("Info: Success To publish a message %v to MQTTServer!", requestBody)
			return
		} else {
			http.Error(w, "must be GET or POST method", http.StatusBadRequest)
			logger.Errorf("Request type %v is not supported yet!", reqType)
			return
		}

	}
}

func (ds *DeviceShifu) getMQTTTopicFromInstructionName(instructionName string) (string, error) {
	if instructionProperties, exists := ds.mqttInstructions.Instructions[instructionName]; exists {
		return instructionProperties.MQTTProtocolProperty.MQTTTopic, nil
	}

	return "", fmt.Errorf("Instruction %v not found in list of deviceshifu instructions", instructionName)
}

// TODO: update configs
// TODO: update status based on telemetry

func (ds *DeviceShifu) collectMQTTTelemetry() (bool, error) {

	if ds.base.EdgeDevice.Spec.Protocol != nil {
		switch protocol := *ds.base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolMQTT:
			telemetrySettings := ds.base.DeviceShifuConfig.Telemetries.DeviceShifuTelemetrySettings
			if ds.base.EdgeDevice.Spec.Address == nil {
				return false, fmt.Errorf("device %v does not have an address", ds.base.Name)
			}

			if interval := telemetrySettings.DeviceShifuTelemetryUpdateIntervalInMilliseconds; interval == nil {
				var telemetryUpdateIntervalInMilliseconds = DefaultUpdateIntervalInMS
				telemetrySettings.DeviceShifuTelemetryUpdateIntervalInMilliseconds = &telemetryUpdateIntervalInMilliseconds
			}

			telemetries := ds.base.DeviceShifuConfig.Telemetries.DeviceShifuTelemetries
			for telemetry, telemetryProperties := range telemetries {
				if telemetryProperties.DeviceShifuTelemetryProperties.DeviceInstructionName == nil {
					return false, fmt.Errorf("Device %v telemetry %v does not have an instruction name", ds.base.Name, telemetry)
				}

				instruction := *telemetryProperties.DeviceShifuTelemetryProperties.DeviceInstructionName
				mqttTopic, err := ds.getMQTTTopicFromInstructionName(instruction)
				if err != nil {
					logger.Errorf("%v", err.Error())
					return false, err
				}

				// use mqtttopic to get the mqttMessageReceiveTimestampMap
				// determine whether the message interval exceed DeviceShifuTelemetryUpdateIntervalInMilliseconds
				// return true if there is a topic message interval is normal
				// return false if the time interval of all topics is abnormal
				nowTime := time.Now()
				if int64(nowTime.Sub(mqttMessageReceiveTimestampMap[mqttTopic]).Milliseconds()) < *telemetrySettings.DeviceShifuTelemetryUpdateIntervalInMilliseconds {
					return true, nil
				}
			}
		default:
			logger.Warnf("EdgeDevice protocol %v not supported in deviceshifu", protocol)
			return false, nil
		}
	}

	return false, nil
}

// Start start Mqtt Telemetry
func (ds *DeviceShifu) Start(stopCh <-chan struct{}) error {
	return ds.base.Start(stopCh, ds.collectMQTTTelemetry)
}

// Stop Stop Http Server
func (ds *DeviceShifu) Stop() error {
	return ds.base.Stop()
}
