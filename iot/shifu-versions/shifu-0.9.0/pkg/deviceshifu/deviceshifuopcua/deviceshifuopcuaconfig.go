package deviceshifuopcua

import (
	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
)

const (
	opcuaID = "OPCUANodeID"
)

// OPCUAInstructions OPCUA Instructions
type OPCUAInstructions struct {
	Instructions map[string]*OPCUAInstruction
}

// OPCUAInstruction OPCUA Instruction
type OPCUAInstruction struct {
	OPCUAInstructionProperty *OPCUAInstructionProperty `yaml:"instructionProperties,omitempty"`
}

// OPCUAInstructionProperty OPCUA Instruction's Property
type OPCUAInstructionProperty struct {
	OPCUANodeID string `yaml:"OPCUANodeID"`
}

// CreateOPCUAInstructions Create OPCUA Instructions
func CreateOPCUAInstructions(dsInstructions *deviceshifubase.DeviceShifuInstructions) *OPCUAInstructions {
	instructions := make(map[string]*OPCUAInstruction)

	for key, dsInstruction := range dsInstructions.Instructions {
		instruction := &OPCUAInstruction{
			&OPCUAInstructionProperty{
				OPCUANodeID: dsInstruction.DeviceShifuProtocolProperties[opcuaID],
			},
		}
		instructions[key] = instruction
	}
	return &OPCUAInstructions{instructions}
}
