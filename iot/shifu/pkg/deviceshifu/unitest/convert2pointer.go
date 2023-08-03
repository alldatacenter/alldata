package unitest

// SO FAR ONLY FOR UNIT TESTING USAGE
// DO NOT USE IN NONE UNIT TESTING CODE
// if there are common use case, let's move it to an much common package or even another code repo

func ToPointer[T any](v T) *T {
	return &v
}
