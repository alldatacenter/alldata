package test

import (
	"github.com/apex/log"
	"github.com/stretchr/testify/require"
	"strconv"
	"testing"
)

func TestStatsService_InsertLogs(t *testing.T) {
	var err error
	T.Setup(t)

	nt := int64(1e2)
	nl := int64(1e2)
	for i := int64(0); i < nt; i++ {
		task := T.NewTask()
		log.Infof("i: %d", i)
		for j := int64(0); j < nl; j++ {
			logs := []string{"log_" + strconv.Itoa(int(i+j)+1)}
			err = T.statsSvc.InsertLogs(task.GetId(), logs...)
			require.Nil(t, err)
		}
	}
}
