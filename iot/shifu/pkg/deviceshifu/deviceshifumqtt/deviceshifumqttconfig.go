package deviceshifumqtt

import (
	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/logger"
)

const (
	mqttTopic = "MQTTTopic"
)

// ReturnBody Body of mqtt's reply
type ReturnBody struct {
	MQTTMessage   string `json:"mqtt_message"`
	MQTTTimestamp string `json:"mqtt_receive_timestamp"`
}

// RequestBody Body of mqtt's request by POST method
type RequestBody string

// MQTTInstructions MQTT Instructions
type MQTTInstructions struct {
	Instructions map[string]*MQTTInstruction
}

// MQTTInstruction MQTT Instruction
type MQTTInstruction struct {
	MQTTProtocolProperty *MQTTProtocolProperty
}

// MQTTProtocolProperty MQTT Instruction's Property
type MQTTProtocolProperty struct {
	MQTTTopic string
}

// CreateMQTTInstructions Create MQTT Instructions
func CreateMQTTInstructions(dsInstructions *deviceshifubase.DeviceShifuInstructions) *MQTTInstructions {
	instructions := make(map[string]*MQTTInstruction)

	for key, dsInstruction := range dsInstructions.Instructions {
		if dsInstruction.DeviceShifuProtocolProperties != nil && dsInstruction.DeviceShifuProtocolProperties[mqttTopic] == "" {
			logger.Fatalf("Error when Read MQTTTopic From DeviceShifuInstructions, error: instruction %v has an empty topic", key)
		}
		instruction := &MQTTInstruction{
			&MQTTProtocolProperty{
				MQTTTopic: dsInstruction.DeviceShifuProtocolProperties[mqttTopic],
			},
		}
		instructions[key] = instruction
	}
	return &MQTTInstructions{instructions}
}
