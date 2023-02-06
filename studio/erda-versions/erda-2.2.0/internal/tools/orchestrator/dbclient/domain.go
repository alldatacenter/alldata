// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dbclient

func (db *DBClient) GetDefaultDomainOrCreate(runtimeId uint64, serviceName string, domain string) (string, error) {
	var dom RuntimeDomain
	result := db.
		Where("runtime_id = ? AND endpoint_name = ? AND domain_type = 'DEFAULT'", runtimeId, serviceName).
		Take(&dom)
	isNoRecord := false
	if result.Error != nil {
		if result.RecordNotFound() {
			isNoRecord = true
		} else {
			return "", result.Error
		}
	}
	if isNoRecord {
		dom = RuntimeDomain{
			RuntimeId:    runtimeId,
			EndpointName: serviceName,
			Domain:       domain,
			DomainType:   "DEFAULT",
			UseHttps:     false,
		}
		if err := db.Save(&dom).Error; err != nil {
			return "", err
		}
		return domain, nil
	} else {
		return dom.Domain, nil
	}
}

func (db *DBClient) FindDomainsByRuntimeIdAndServiceName(runtimeId uint64, serviceName string) ([]RuntimeDomain, error) {
	var domains []RuntimeDomain
	if err := db.
		Where("runtime_id = ? AND endpoint_name = ?", runtimeId, serviceName).
		Find(&domains).Error; err != nil {
		return nil, err
	}
	return domains, nil
}

func (db *DBClient) FindDomainsByRuntimeId(runtimeId uint64) ([]RuntimeDomain, error) {
	var domains []RuntimeDomain
	if err := db.
		Where("runtime_id = ?", runtimeId).
		Find(&domains).Error; err != nil {
		return nil, err
	}
	return domains, nil
}

func (db *DBClient) FindDomains(domainValues []string) ([]RuntimeDomain, error) {
	var domains []RuntimeDomain
	if err := db.
		Where("domain in (?)", domainValues).
		Find(&domains).Error; err != nil {
		return nil, err
	}
	return domains, nil
}

func (db *DBClient) DeleteDomainsByRuntimeId(runtimeId uint64) error {
	if err := db.
		Where("runtime_id = ?", runtimeId).
		Delete(&RuntimeDomain{}).Error; err != nil {
		return err
	}
	return nil
}

func (db *DBClient) SaveDomain(domain *RuntimeDomain) error {
	return db.Save(domain).Error
}

func (db *DBClient) DeleteDomain(domainValue string) error {
	if len(domainValue) == 0 {
		return nil
	}
	return db.
		Where("domain = ?", domainValue).
		Delete(&RuntimeDomain{}).Error
}
