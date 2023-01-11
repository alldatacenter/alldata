package deviceshifusocket

// RequestBody Socket Request Body
type RequestBody struct {
	Command string `json:"command"`
	Timeout int64  `json:"timeout"`
}

// ReturnBody Socket Reply Body
type ReturnBody struct {
	Message string `json:"message"`
	Status  int64  `json:"status"`
}

const (
	DefaultBufferLength = 1024
)
