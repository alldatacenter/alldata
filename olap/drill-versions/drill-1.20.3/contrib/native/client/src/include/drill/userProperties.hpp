/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef USER_PROPERTIES_H
#define USER_PROPERTIES_H

#include <map>
#include "drill/common.hpp"

namespace Drill{

class DECLSPEC_DRILL_CLIENT DrillUserProperties{
    public:
        static const std::map<std::string, uint32_t> USER_PROPERTIES;

        DrillUserProperties(){};
        
        /// @brief Sets the default property value.
        /// 
        /// @param in_propName              The property name.
        /// @param in_propValue             The property value.
        void setDefaultProperty(const std::string& in_propName, const std::string& in_propValue){
            if (!isPropSet(in_propName) || m_properties[in_propName].empty()){
                m_properties[in_propName] = in_propValue;
            }
        }

        void setProperty( const std::string& propName, const std::string& propValue){
            std::pair< std::string, std::string> in = make_pair(propName, propValue);
            m_properties.insert(in);
        }

        size_t size() const { return m_properties.size(); }

        const bool  isPropSet(const std::string& key) const{
            bool isSet=true;
            std::map<std::string, std::string>::const_iterator f=m_properties.find(key);
            if(f==m_properties.end()){
                isSet=false;
            }
            return isSet;
        }

        const std::string&  getProp(const std::string& key, std::string& value) const{
            std::map<std::string, std::string>::const_iterator f=m_properties.find(key);
            if(f!=m_properties.end()){
                value=f->second;
            }
            return value;
        }

        bool validate(std::string& err);

        std::map<std::string,std::string>::const_iterator begin() const{
            return m_properties.begin();
        }

        std::map<std::string,std::string>::const_iterator end() const{
            return m_properties.end();
        }

    private:
        std::map< std::string, std::string > m_properties;
};


} // namespace Drill
#endif // USER_PROPERTIES_H

