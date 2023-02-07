package entity

type NpmResponseList struct {
	Total   int         `json:"total"`
	Results []NpmResult `json:"results"`
}

type NpmResult struct {
	Package NpmPackage `json:"package"`
}

type NpmPackage struct {
	Name    string `json:"name"`
	Version string `json:"version"`
}

type NpmListResult struct {
	Dependencies map[string]NpmListPackage `json:"dependencies"`
}

type NpmListPackage struct {
	Version string `json:"version"`
}

type NpmResponseDetail struct {
	Collected NpmCollected `json:"collected"`
}

type NpmCollected struct {
	Metadata NpmPackage `json:"metadata"`
}
