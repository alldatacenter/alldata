package fs

import (
	"crypto/md5"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"io"
)

func getConfigPathFromOptions(opts ...Option) (path string) {
	svc := &Service{}
	for _, opt := range opts {
		opt(svc)
	}
	return svc.cfgPath
}

func getHashStringFromIdAndConfigPath(id primitive.ObjectID, cfgPath string) (str string) {
	h := md5.New()
	_, _ = io.WriteString(h, id.Hex())
	_, _ = io.WriteString(h, cfgPath)
	return string(h.Sum(nil))
}
