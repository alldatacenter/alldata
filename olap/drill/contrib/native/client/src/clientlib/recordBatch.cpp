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
#include "drill/common.hpp"
#include "drill/fieldmeta.hpp"
#include "drill/recordBatch.hpp"
#include "utils.hpp"
#include "../protobuf/User.pb.h"

const int32_t YEARS_TO_MONTHS=12;
const int32_t DAYS_TO_MILLIS=24*60*60*1000;
const int32_t HOURS_TO_MILLIS=60*60*1000;
const int32_t MINUTES_TO_MILLIS=60*1000;
const int32_t SECONDS_TO_MILLIS=1000;
extern "C"
{
    #include "y2038/time64.h"
}

namespace Drill{

static char timezoneMap[][36]={
    "Africa/Abidjan", "Africa/Accra", "Africa/Addis_Ababa", "Africa/Algiers", "Africa/Asmara", "Africa/Asmera",
    "Africa/Bamako", "Africa/Bangui", "Africa/Banjul", "Africa/Bissau", "Africa/Blantyre", "Africa/Brazzaville",
    "Africa/Bujumbura", "Africa/Cairo", "Africa/Casablanca", "Africa/Ceuta", "Africa/Conakry", "Africa/Dakar",
    "Africa/Dar_es_Salaam", "Africa/Djibouti", "Africa/Douala", "Africa/El_Aaiun",
    "Africa/Freetown", "Africa/Gaborone",
    "Africa/Harare", "Africa/Johannesburg", "Africa/Juba", "Africa/Kampala", "Africa/Khartoum", "Africa/Kigali",
    "Africa/Kinshasa", "Africa/Lagos", "Africa/Libreville", "Africa/Lome", "Africa/Luanda", "Africa/Lubumbashi",
    "Africa/Lusaka", "Africa/Malabo", "Africa/Maputo", "Africa/Maseru", "Africa/Mbabane", "Africa/Mogadishu",
    "Africa/Monrovia", "Africa/Nairobi", "Africa/Ndjamena", "Africa/Niamey",
    "Africa/Nouakchott", "Africa/Ouagadougou",
    "Africa/Porto-Novo", "Africa/Sao_Tome", "Africa/Timbuktu", "Africa/Tripoli",
    "Africa/Tunis", "Africa/Windhoek",
    "America/Adak", "America/Anchorage", "America/Anguilla", "America/Antigua", "America/Araguaina",
    "America/Argentina/Buenos_Aires", "America/Argentina/Catamarca", "America/Argentina/ComodRivadavia",
    "America/Argentina/Cordoba", "America/Argentina/Jujuy", "America/Argentina/La_Rioja",
    "America/Argentina/Mendoza", "America/Argentina/Rio_Gallegos", "America/Argentina/Salta",
    "America/Argentina/San_Juan", "America/Argentina/San_Luis", "America/Argentina/Tucuman",
    "America/Argentina/Ushuaia", "America/Aruba", "America/Asuncion", "America/Atikokan", "America/Atka",
    "America/Bahia", "America/Bahia_Banderas", "America/Barbados", "America/Belem", "America/Belize",
    "America/Blanc-Sablon", "America/Boa_Vista", "America/Bogota", "America/Boise", "America/Buenos_Aires",
    "America/Cambridge_Bay", "America/Campo_Grande", "America/Cancun", "America/Caracas", "America/Catamarca",
    "America/Cayenne", "America/Cayman", "America/Chicago", "America/Chihuahua", "America/Coral_Harbour",
    "America/Cordoba", "America/Costa_Rica", "America/Cuiaba", "America/Curacao", "America/Danmarkshavn",
    "America/Dawson", "America/Dawson_Creek", "America/Denver", "America/Detroit", "America/Dominica",
    "America/Edmonton", "America/Eirunepe", "America/El_Salvador", "America/Ensenada", "America/Fort_Wayne",
    "America/Fortaleza", "America/Glace_Bay", "America/Godthab", "America/Goose_Bay", "America/Grand_Turk",
    "America/Grenada", "America/Guadeloupe", "America/Guatemala", "America/Guayaquil", "America/Guyana",
    "America/Halifax", "America/Havana", "America/Hermosillo",
    "America/Indiana/Indianapolis", "America/Indiana/Knox",
    "America/Indiana/Marengo", "America/Indiana/Petersburg", "America/Indiana/Tell_City",
    "America/Indiana/Vevay", "America/Indiana/Vincennes", "America/Indiana/Winamac",
    "America/Indianapolis", "America/Inuvik",
    "America/Iqaluit", "America/Jamaica", "America/Jujuy", "America/Juneau", "America/Kentucky/Louisville",
    "America/Kentucky/Monticello", "America/Knox_IN", "America/Kralendijk", "America/La_Paz", "America/Lima",
    "America/Los_Angeles", "America/Louisville", "America/Lower_Princes", "America/Maceio", "America/Managua",
    "America/Manaus", "America/Marigot", "America/Martinique", "America/Matamoros", "America/Mazatlan",
    "America/Mendoza", "America/Menominee", "America/Merida", "America/Metlakatla", "America/Mexico_City",
    "America/Miquelon", "America/Moncton", "America/Monterrey", "America/Montevideo", "America/Montreal",
    "America/Montserrat", "America/Nassau", "America/New_York", "America/Nipigon",
    "America/Nome", "America/Noronha",
    "America/North_Dakota/Beulah", "America/North_Dakota/Center", "America/North_Dakota/New_Salem",
    "America/Ojinaga", "America/Panama", "America/Pangnirtung",
    "America/Paramaribo", "America/Phoenix", "America/Port-au-Prince",
    "America/Port_of_Spain", "America/Porto_Acre", "America/Porto_Velho",
    "America/Puerto_Rico", "America/Rainy_River", "America/Rankin_Inlet",
    "America/Recife", "America/Regina", "America/Resolute", "America/Rio_Branco",
    "America/Rosario", "America/Santa_Isabel",
    "America/Santarem", "America/Santiago", "America/Santo_Domingo",
    "America/Sao_Paulo", "America/Scoresbysund", "America/Shiprock", "America/Sitka",
    "America/St_Barthelemy", "America/St_Johns",
    "America/St_Kitts", "America/St_Lucia", "America/St_Thomas",
    "America/St_Vincent", "America/Swift_Current", "America/Tegucigalpa",
    "America/Thule", "America/Thunder_Bay", "America/Tijuana", "America/Toronto",
    "America/Tortola", "America/Vancouver",
    "America/Virgin", "America/Whitehorse", "America/Winnipeg", "America/Yakutat",
    "America/Yellowknife", "Antarctica/Casey",
    "Antarctica/Davis", "Antarctica/DumontDUrville", "Antarctica/Macquarie",
    "Antarctica/Mawson", "Antarctica/McMurdo", "Antarctica/Palmer",
    "Antarctica/Rothera", "Antarctica/South_Pole", "Antarctica/Syowa",
    "Antarctica/Vostok", "Arctic/Longyearbyen", "Asia/Aden", "Asia/Almaty", "Asia/Amman", "Asia/Anadyr",
    "Asia/Aqtau", "Asia/Aqtobe", "Asia/Ashgabat", "Asia/Ashkhabad", "Asia/Baghdad", "Asia/Bahrain",
    "Asia/Baku", "Asia/Bangkok", "Asia/Beirut", "Asia/Bishkek", "Asia/Brunei", "Asia/Calcutta",
    "Asia/Choibalsan", "Asia/Chongqing", "Asia/Chungking", "Asia/Colombo", "Asia/Dacca", "Asia/Damascus",
    "Asia/Dhaka", "Asia/Dili", "Asia/Dubai", "Asia/Dushanbe", "Asia/Gaza", "Asia/Harbin",
    "Asia/Hebron", "Asia/Ho_Chi_Minh", "Asia/Hong_Kong", "Asia/Hovd", "Asia/Irkutsk", "Asia/Istanbul",
    "Asia/Jakarta", "Asia/Jayapura", "Asia/Jerusalem", "Asia/Kabul", "Asia/Kamchatka", "Asia/Karachi",
    "Asia/Kashgar", "Asia/Kathmandu", "Asia/Katmandu", "Asia/Kolkata", "Asia/Krasnoyarsk", "Asia/Kuala_Lumpur",
    "Asia/Kuching", "Asia/Kuwait", "Asia/Macao", "Asia/Macau", "Asia/Magadan", "Asia/Makassar",
    "Asia/Manila", "Asia/Muscat", "Asia/Nicosia", "Asia/Novokuznetsk", "Asia/Novosibirsk", "Asia/Omsk",
    "Asia/Oral", "Asia/Phnom_Penh", "Asia/Pontianak", "Asia/Pyongyang", "Asia/Qatar", "Asia/Qyzylorda",
    "Asia/Rangoon", "Asia/Riyadh", "Asia/Saigon", "Asia/Sakhalin", "Asia/Samarkand", "Asia/Seoul",
    "Asia/Shanghai", "Asia/Singapore", "Asia/Taipei", "Asia/Tashkent", "Asia/Tbilisi", "Asia/Tehran",
    "Asia/Tel_Aviv", "Asia/Thimbu", "Asia/Thimphu", "Asia/Tokyo", "Asia/Ujung_Pandang", "Asia/Ulaanbaatar",
    "Asia/Ulan_Bator", "Asia/Urumqi", "Asia/Vientiane", "Asia/Vladivostok", "Asia/Yakutsk", "Asia/Yekaterinburg",
    "Asia/Yerevan", "Atlantic/Azores", "Atlantic/Bermuda", "Atlantic/Canary",
    "Atlantic/Cape_Verde", "Atlantic/Faeroe",
    "Atlantic/Faroe", "Atlantic/Jan_Mayen", "Atlantic/Madeira",
    "Atlantic/Reykjavik", "Atlantic/South_Georgia", "Atlantic/St_Helena",
    "Atlantic/Stanley", "Australia/ACT", "Australia/Adelaide", "Australia/Brisbane",
    "Australia/Broken_Hill", "Australia/Canberra",
    "Australia/Currie", "Australia/Darwin", "Australia/Eucla", "Australia/Hobart",
    "Australia/LHI", "Australia/Lindeman",
    "Australia/Lord_Howe", "Australia/Melbourne", "Australia/NSW", "Australia/North",
    "Australia/Perth", "Australia/Queensland",
    "Australia/South", "Australia/Sydney", "Australia/Tasmania", "Australia/Victoria",
    "Australia/West", "Australia/Yancowinna",
    "Brazil/Acre", "Brazil/DeNoronha", "Brazil/East", "Brazil/West", "CET", "CST6CDT",
    "Canada/Atlantic", "Canada/Central", "Canada/East-Saskatchewan", "Canada/Eastern",
    "Canada/Mountain", "Canada/Newfoundland",
    "Canada/Pacific", "Canada/Saskatchewan", "Canada/Yukon", "Chile/Continental", "Chile/EasterIsland", "Cuba",
    "EET", "EST", "EST5EDT", "Egypt", "Eire", "Etc/GMT", "Etc/GMT+0", "Etc/GMT+1", "Etc/GMT+10",
    "Etc/GMT+11", "Etc/GMT+12", "Etc/GMT+2", "Etc/GMT+3", "Etc/GMT+4", "Etc/GMT+5", "Etc/GMT+6",
    "Etc/GMT+7", "Etc/GMT+8",
    "Etc/GMT+9", "Etc/GMT-0", "Etc/GMT-1", "Etc/GMT-10", "Etc/GMT-11", "Etc/GMT-12",
    "Etc/GMT-13", "Etc/GMT-14", "Etc/GMT-2",
    "Etc/GMT-3", "Etc/GMT-4", "Etc/GMT-5", "Etc/GMT-6", "Etc/GMT-7", "Etc/GMT-8",
    "Etc/GMT-9", "Etc/GMT0", "Etc/Greenwich",
    "Etc/UCT", "Etc/UTC", "Etc/Universal", "Etc/Zulu", "Europe/Amsterdam", "Europe/Andorra",
    "Europe/Athens", "Europe/Belfast", "Europe/Belgrade", "Europe/Berlin", "Europe/Bratislava", "Europe/Brussels",
    "Europe/Bucharest", "Europe/Budapest", "Europe/Chisinau",
    "Europe/Copenhagen", "Europe/Dublin", "Europe/Gibraltar", "Europe/Guernsey",
    "Europe/Helsinki", "Europe/Isle_of_Man",
    "Europe/Istanbul", "Europe/Jersey", "Europe/Kaliningrad", "Europe/Kiev", "Europe/Lisbon", "Europe/Ljubljana",
    "Europe/London", "Europe/Luxembourg", "Europe/Madrid", "Europe/Malta", "Europe/Mariehamn", "Europe/Minsk",
    "Europe/Monaco", "Europe/Moscow", "Europe/Nicosia", "Europe/Oslo", "Europe/Paris", "Europe/Podgorica",
    "Europe/Prague", "Europe/Riga", "Europe/Rome", "Europe/Samara", "Europe/San_Marino", "Europe/Sarajevo",
    "Europe/Simferopol", "Europe/Skopje", "Europe/Sofia", "Europe/Stockholm", "Europe/Tallinn", "Europe/Tirane",
    "Europe/Tiraspol", "Europe/Uzhgorod", "Europe/Vaduz", "Europe/Vatican", "Europe/Vienna", "Europe/Vilnius",
    "Europe/Volgograd", "Europe/Warsaw", "Europe/Zagreb", "Europe/Zaporozhye", "Europe/Zurich", "GB",
    "GB-Eire", "GMT", "GMT+0", "GMT-0", "GMT0", "Greenwich", "HST", "Hongkong", "Iceland",
    "Indian/Antananarivo", "Indian/Chagos", "Indian/Christmas",
    "Indian/Cocos", "Indian/Comoro", "Indian/Kerguelen", "Indian/Mahe", "Indian/Maldives", "Indian/Mauritius",
    "Indian/Mayotte", "Indian/Reunion", "Iran", "Israel", "Jamaica", "Japan", "Kwajalein", "Libya", "MET",
    "MST", "MST7MDT", "Mexico/BajaNorte", "Mexico/BajaSur", "Mexico/General", "NZ", "NZ-CHAT", "Navajo", "PRC",
    "PST8PDT", "Pacific/Apia", "Pacific/Auckland", "Pacific/Chatham", "Pacific/Chuuk", "Pacific/Easter",
    "Pacific/Efate", "Pacific/Enderbury", "Pacific/Fakaofo", "Pacific/Fiji",
    "Pacific/Funafuti", "Pacific/Galapagos",
    "Pacific/Gambier", "Pacific/Guadalcanal", "Pacific/Guam", "Pacific/Honolulu",
    "Pacific/Johnston", "Pacific/Kiritimati",
    "Pacific/Kosrae", "Pacific/Kwajalein", "Pacific/Majuro", "Pacific/Marquesas",
    "Pacific/Midway", "Pacific/Nauru",
    "Pacific/Niue", "Pacific/Norfolk", "Pacific/Noumea", "Pacific/Pago_Pago",
    "Pacific/Palau", "Pacific/Pitcairn",
    "Pacific/Pohnpei", "Pacific/Ponape", "Pacific/Port_Moresby", "Pacific/Rarotonga",
    "Pacific/Saipan", "Pacific/Samoa",
    "Pacific/Tahiti", "Pacific/Tarawa", "Pacific/Tongatapu", "Pacific/Truk", "Pacific/Wake", "Pacific/Wallis",
    "Pacific/Yap", "Poland", "Portugal", "ROC", "ROK", "Singapore", "Turkey", "UCT", "US/Alaska", "US/Aleutian",
    "US/Arizona", "US/Central", "US/East-Indiana", "US/Eastern", "US/Hawaii", "US/Indiana-Starke",
    "US/Michigan", "US/Mountain", "US/Pacific", "US/Pacific-New", "US/Samoa",
    "UTC", "Universal", "W-SU", "WET", "Zulu"
};

ValueVectorBase* ValueVectorFactory::allocateValueVector(const Drill::FieldMetadata & f, SlicedByteBuf* b){
    common::MinorType type = f.getMinorType();
    common::DataMode mode = f.getDataMode();

    switch (mode) {

        case common::DM_REQUIRED:
            switch (type) {
                case common::TINYINT:
                    return new ValueVectorFixed<int8_t>(b,f.getValueCount());
                case common::SMALLINT:
                    return new ValueVectorFixed<int16_t>(b,f.getValueCount());
                case common::INT:
                    return new ValueVectorFixed<int32_t>(b,f.getValueCount());
                case common::BIGINT:
                    return new ValueVectorFixed<int64_t>(b,f.getValueCount());
                case common::FLOAT4:
                    return new ValueVectorFixed<float>(b,f.getValueCount());
                case common::FLOAT8:
                    return new ValueVectorFixed<double>(b,f.getValueCount());
                    // Decimal digits, width, max precision is defined in
                    // /exec/java-exec/src/main/codegen/data/ValueVectorTypes.tdd
                    // Decimal Design Document: http://bit.ly/drilldecimal
                case common::DECIMAL9:
                    return new ValueVectorDecimal9(b,f.getValueCount(), f.getScale());
                case common::DECIMAL18:
                    return new ValueVectorDecimal18(b,f.getValueCount(), f.getScale());
                case common::DECIMAL28DENSE:
                    return new ValueVectorDecimal28Dense(b,f.getValueCount(), f.getScale());
                case common::DECIMAL38DENSE:
                    return new ValueVectorDecimal38Dense(b,f.getValueCount(), f.getScale());
                case common::DECIMAL28SPARSE:
                    return new ValueVectorDecimal28Sparse(b,f.getValueCount(), f.getScale());
                case common::DECIMAL38SPARSE:
                    return new ValueVectorDecimal38Sparse(b,f.getValueCount(), f.getScale());
                case common::VARDECIMAL:
                    return new ValueVectorVarDecimal(b, f.getValueCount(), f.getScale());
                case common::DATE:
                    return new ValueVectorTyped<DateHolder, int64_t>(b,f.getValueCount());
                case common::TIMESTAMP:
                    return new ValueVectorTyped<DateTimeHolder, int64_t>(b,f.getValueCount());
                case common::TIME:
                    return new ValueVectorTyped<TimeHolder, uint32_t>(b,f.getValueCount());
                case common::TIMESTAMPTZ:
                    return new ValueVectorTypedComposite<DateTimeTZHolder>(b,f.getValueCount());
                case common::INTERVAL:
                    return new ValueVectorTypedComposite<IntervalHolder>(b,f.getValueCount());
                case common::INTERVALDAY:
                    return new ValueVectorTypedComposite<IntervalDayHolder>(b,f.getValueCount());
                case common::INTERVALYEAR:
                    return new ValueVectorTypedComposite<IntervalYearHolder>(b,f.getValueCount());
                case common::BIT:
                    return new ValueVectorBit(b,f.getValueCount());
                case common::VARBINARY:
                    return new ValueVectorVarBinary(b, f.getValueCount());
                case common::VARCHAR:
                    return new ValueVectorVarChar(b, f.getValueCount());
                case common::MONEY:
                default:
                    return new ValueVectorUnimplemented(b, f.getValueCount());
            }
        case common::DM_OPTIONAL:
            switch (type) {
                case common::TINYINT:
                    return new NullableValueVectorFixed<int8_t>(b,f.getValueCount());
                case common::SMALLINT:
                    return new NullableValueVectorFixed<int16_t>(b,f.getValueCount());
                case common::INT:
                    return new NullableValueVectorFixed<int32_t>(b,f.getValueCount());
                case common::BIGINT:
                    return new NullableValueVectorFixed<int64_t>(b,f.getValueCount());
                case common::FLOAT4:
                    return new NullableValueVectorFixed<float>(b,f.getValueCount());
                case common::FLOAT8:
                    return new NullableValueVectorFixed<double>(b,f.getValueCount());
                case common::DECIMAL9:
                    return new NullableValueVectorDecimal9(b,f.getValueCount(), f.getScale());
                case common::DECIMAL18:
                    return new NullableValueVectorDecimal18(b,f.getValueCount(), f.getScale());
                case common::DECIMAL28DENSE:
                    return new NullableValueVectorDecimal28Dense(b,f.getValueCount(), f.getScale());
                case common::DECIMAL38DENSE:
                    return new NullableValueVectorDecimal38Dense(b,f.getValueCount(), f.getScale());
                case common::DECIMAL28SPARSE:
                    return new NullableValueVectorDecimal28Sparse(b,f.getValueCount(), f.getScale());
                case common::DECIMAL38SPARSE:
                    return new NullableValueVectorDecimal38Sparse(b,f.getValueCount(), f.getScale());
                case common::VARDECIMAL:
                    return new NullableValueVectorVarDecimal(b, f.getValueCount(), f.getScale());
                case common::DATE:
                    return new NullableValueVectorTyped<DateHolder,
                           ValueVectorTyped<DateHolder, int64_t> >(b,f.getValueCount());
                case common::TIMESTAMP:
                    return new NullableValueVectorTyped<DateTimeHolder,
                           ValueVectorTyped<DateTimeHolder, int64_t> >(b,f.getValueCount());
                case common::TIME:
                    return new NullableValueVectorTyped<TimeHolder,
                           ValueVectorTyped<TimeHolder, uint32_t> >(b,f.getValueCount());
                case common::TIMESTAMPTZ:
                    return new NullableValueVectorTyped<DateTimeTZHolder,
                           ValueVectorTypedComposite<DateTimeTZHolder> >(b,f.getValueCount());
                case common::INTERVAL:
                    return new NullableValueVectorTyped<IntervalHolder,
                           ValueVectorTypedComposite<IntervalHolder> >(b,f.getValueCount());
                case common::INTERVALDAY:
                    return new NullableValueVectorTyped<IntervalDayHolder,
                           ValueVectorTypedComposite<IntervalDayHolder> >(b,f.getValueCount());
                case common::INTERVALYEAR:
                    return new NullableValueVectorTyped<IntervalYearHolder,
                           ValueVectorTypedComposite<IntervalYearHolder> >(b,f.getValueCount());
                case common::BIT:
                    return new NullableValueVectorTyped<uint8_t,
                           ValueVectorBit >(b,f.getValueCount());
                case common::VARBINARY:
                    //TODO: Varbinary is untested
                    return new NullableValueVectorTyped<VarWidthHolder, ValueVectorVarBinary >(b,f.getValueCount());
                case common::VARCHAR:
                    return new NullableValueVectorTyped<VarWidthHolder, ValueVectorVarChar >(b,f.getValueCount());
                    // not implemented yet
                default:
                    return new ValueVectorUnimplemented(b, f.getValueCount());
            }
        case common::DM_REPEATED:
            switch (type) {
                // not implemented yet
                default:
                    return new ValueVectorUnimplemented(b, f.getValueCount());
            }
    }
    return new ValueVectorUnimplemented(b, f.getValueCount());
}


ret_t FieldBatch::load(){
    const Drill::FieldMetadata& fmd = this->m_fieldMetadata;
    this->m_pValueVector=ValueVectorFactory::allocateValueVector(fmd, this->m_pFieldData);
    return RET_SUCCESS;
}

ret_t FieldBatch::loadNull(size_t nRecords){
    this->m_pValueVector= new ValueVectorNull(this->m_pFieldData, nRecords);
    return RET_SUCCESS;
}
    
RecordBatch::RecordBatch(exec::shared::QueryData* pResult, AllocatedBufferPtr r, ByteBuf_t b)
    :m_fieldDefs(new(std::vector<Drill::FieldMetadata*>)){
        m_pQueryResult=pResult;
        m_pRecordBatchDef=&pResult->def();
        m_numRecords=pResult->row_count();
        m_allocatedBuffer=r;
        m_buffer=b;
        m_numFields=pResult->def().field_size();
        m_bHasSchemaChanged=false;
}


RecordBatch::~RecordBatch(){
    m_buffer=NULL;
    //free memory allocated for FieldBatch objects saved in m_fields;
    for(std::vector<FieldBatch*>::iterator it = m_fields.begin(); it != m_fields.end(); ++it){
        delete *it;
    }
    m_fields.clear();
    for(std::vector<Drill::FieldMetadata*>::iterator it = m_fieldDefs->begin(); it != m_fieldDefs->end(); ++it){
        delete *it;
    }
    m_fieldDefs->clear();
    delete m_pQueryResult;
    delete m_allocatedBuffer;
}

ret_t RecordBatch::build(){
    // For every Field, get the corresponding SlicedByteBuf.
    // Create a Materialized field. Set the Sliced Byted Buf to the correct slice.
    // Set the Field Metadata.
    // Load the vector.(Load creates a valuevector object of the correct type:
    //    Use ValueVectorFactory(type) to create the right type.
    //    Create a Value Vector of the Sliced Byte Buf.
    // Add the field batch to vector
    size_t startOffset=0;
    //TODO: handle schema changes here. Call a client provided callback?
    for(int i=0; i<this->m_numFields; i++){
        const exec::shared::SerializedField f=this->m_pRecordBatchDef->field(i);
        //TODO: free this??
        Drill::FieldMetadata* pFmd = new Drill::FieldMetadata;
        pFmd->set(f);
        size_t len=pFmd->getBufferLength();
        FieldBatch* pField = new FieldBatch(*pFmd, this->m_buffer, startOffset, len) ;
        startOffset+=len;
        // We may get an empty record batch. All the fields will be empty, except for metadata.
        if(len>0){
            pField->load(); // set up the value vectors
        }else if(this->m_numRecords!=pFmd->getValueCount() && pFmd->getValueCount()==0){
            // We may  get an *inconsistent* record batch where the value count in the fields do not match.
            pField->loadNull(this->m_numRecords);
        }
        this->m_fields.push_back(pField);
        this->m_fieldDefs->push_back(pFmd);
    }
    return RET_SUCCESS;
}

void RecordBatch::print(std::ostream& s, size_t num){
    std::string nameList;
    for(std::vector<FieldBatch*>::size_type i = 0; i != this->m_fields.size(); i++) {
        Drill::FieldMetadata fmd=this->getFieldMetadata(i);
        std::string name= fmd.getName();
        nameList+=name;
        nameList+="    ";
    }
    size_t numToPrint=this->m_numRecords;
    if(num>0 && num<numToPrint)numToPrint=num;
    s<<nameList<<std::endl;
    std::string values;
    for(size_t n=0; n<numToPrint; n++){
        values="";
        for(std::vector<FieldBatch*>::size_type i = 0; i != this->m_fields.size(); i++) {
            const ValueVectorBase * v = m_fields[i]->getVector();
            char valueBuf[1024+1];
            memset(valueBuf, 0, sizeof(valueBuf)*sizeof(char));
            if(v->isNull(n)){
                strncpy(valueBuf,"null", (sizeof(valueBuf)-1)*sizeof(char));
            } else{
                v->getValueAt(n, valueBuf, (sizeof(valueBuf)-1)*sizeof(char));
            }
            values+=valueBuf;
            values+="    ";
        }
        s<<values<<std::endl;
    }
}
size_t RecordBatch::getNumFields(){
    return m_pRecordBatchDef->field_size(); 
}

bool RecordBatch::isLastChunk(){
    return false;
}



void DateHolder::load(){
    m_year=1970;
    m_month=1;
    m_day=1;
    const Time64_T  t= m_datetime/1000; // number of seconds since beginning of the Unix Epoch.
    /*
    TL;DR

    The gmttime in standard libray on windows platform cannot represent the date before Unix Epoch.
    http://msdn.microsoft.com/en-us/library/0z9czt0w(v=vs.100).aspx

    """
    Both the 32-bit and 64-bit versions of gmtime, mktime, mkgmtime, and localtime all use one tm structure per thread for the conversion.
    Each call to one of these functions destroys the result of any previous call.
    If timer represents a date before midnight, January 1, 1970, gmtime returns NULL.  There is no error return.

    _gmtime64, which uses the __time64_t structure, enables dates to be expressed up through 23:59:59, December 31, 3000, UTC,
    whereas _gmtime32 only represent dates through 03:14:07 January 19, 2038, UTC.
    Midnight, January 1, 1970, is the lower bound of the date range for both of these functions.

    gmtime is an inline function that evaluates to _gmtime64 and time_t is equivalent to __time64_t unless _USE_32BIT_TIME_T is defined.
    """

    An alternative could be boost date_time libraray.

    ```
    #include <boost/date_time/posix_time/posix_time.hpp>
    using namespace boost::posix_time;
    ptime pt = from_time_t(t);
    struct tm d = to_tm(pt);
    ```

    Howerver, boost date_time library still has year 2038 problem which is still not fixed.
    https://svn.boost.org/trac/boost/ticket/4543
    One reason is that the library converts the 64-bit`time_t t` into `seconds` type to get posix time.
    But boost uses `long` to represent `seconds`, which is 4 bytes on windows platform,
    http://msdn.microsoft.com/en-us/library/s3f49ktz.aspx

    We eventually choose the third-party MIT-licensed library written with ANSI C.
    https://github.com/schwern/y2038
    */
    struct TM d;
    gmtime64_r(&t,&d);
    m_year=1900 + static_cast<int32_t>(d.tm_year);
    m_month=d.tm_mon + 1;
    m_day=d.tm_mday;
}

std::string DateHolder::toString(){
    std::stringstream sstr;
    sstr << m_year << "-" << m_month << "-" << m_day;
    return sstr.str();
};

void TimeHolder::load(){
    m_hr=0;
    m_min=0;
    m_sec=0;
    m_msec=0;

    time_t  t= m_datetime/1000; // number of seconds since beginning of the Unix Epoch.
    struct tm * tm = gmtime(&t);
    m_hr=tm->tm_hour;
    m_min=tm->tm_min;
    m_sec=tm->tm_sec;
    m_msec=m_datetime%1000;
}

std::string TimeHolder::toString(){
    std::stringstream sstr;
    sstr << m_hr <<":" << m_min<<":"<<m_sec<<"."<<m_msec;
    return sstr.str();
};

void DateTimeHolder::load(){
    m_year=1970;
    m_month=1;
    m_day=1;
    m_hr=0;
    m_min=0;
    m_sec=0;
    m_msec=0;

    const Time64_T  t=m_datetime/1000; // number of seconds since beginning of the Unix Epoch.

    struct TM dt;
    gmtime64_r(&t,&dt);

    m_year=1900 + dt.tm_year;
    m_month=dt.tm_mon + 1;
    m_day=dt.tm_mday;
    m_hr=dt.tm_hour;
    m_min=dt.tm_min;
    m_sec=dt.tm_sec;
    m_msec=m_datetime%1000;

}

std::string DateTimeHolder::toString(){
    //TODO: Allow config flag to set delimiter
    std::stringstream sstr;
    sstr << m_year << "-" << m_month << "-" << m_day << " " << m_hr <<":" << m_min<<":"<<m_sec<<"."<<m_msec;
    return sstr.str();
};

void DateTimeTZHolder::load(){
    DateTimeHolder::load();
}

std::string DateTimeTZHolder::toString(){
    std::stringstream sstr;
    sstr << m_year << "-" << m_month << "-" << m_day << " " << m_hr <<":" << m_min<<":"<<m_sec<<"."<<m_msec;
    sstr << "["<<timezoneMap[m_tzIndex]<<"]";
    return sstr.str();
};

std::string IntervalYearHolder::toString(){
    std::stringstream sstr;

    bool isNegative = (m_month < 0);
    int32_t m = (isNegative ? - m_month : m_month);

    int32_t years  = (m / YEARS_TO_MONTHS);
    int32_t months = (m % YEARS_TO_MONTHS);

    if (isNegative) sstr << "-"; // put negative sign here if negative
    sstr << years << "-" << months;
    return sstr.str();
};

// Drill may populate data like 25 hours ("0 25:0:0.0"), we should normalize it to
// 1 day 1 hour "1 1:0:0.0"
std::string IntervalDayHolder::toString(){
    std::stringstream sstr;

    bool isNegative = (m_day < 0) || ( m_day == 0 && m_ms < 0);
    int32_t days = (m_day < 0 ? - m_day : m_day);
    int32_t ms = (m_ms < 0 ? - m_ms : m_ms);

    days += ms / (DAYS_TO_MILLIS);
    int32_t millis = ms % (DAYS_TO_MILLIS);
    int32_t hours  = millis / (HOURS_TO_MILLIS);
    millis = millis % (HOURS_TO_MILLIS);

    int32_t minutes = millis / (MINUTES_TO_MILLIS);
    millis      = millis % (MINUTES_TO_MILLIS);

    int32_t seconds = millis / (SECONDS_TO_MILLIS);
    millis      = millis % (SECONDS_TO_MILLIS);

    assert(hours >=0 && hours <= 23);
    if(isNegative) sstr << "-";
    sstr << days << " " << hours << ":"<<minutes<<":"<<seconds<<"."<<millis;
    return sstr.str();
};

std::string IntervalHolder::toString(){
    std::stringstream sstr;

    bool isNegative = (m_month < 0) || (m_month == 0 && m_day < 0 ) || (m_month == 0 && m_day == 0 && m_ms < 0);
    int32_t m = (m_month < 0 ? - m_month : m_month);
    int32_t days = (m_day < 0 ? - m_day : m_day);
    int32_t ms = (m_ms < 0 ? - m_ms : m_ms);

    int32_t years  = (m / YEARS_TO_MONTHS);
    int32_t months = (m % YEARS_TO_MONTHS);

    days   += ms / (DAYS_TO_MILLIS);
    int32_t millis = ms % (DAYS_TO_MILLIS);
    int32_t hours  = millis / (HOURS_TO_MILLIS);
    millis      = millis % (HOURS_TO_MILLIS);

    int32_t minutes = millis / (MINUTES_TO_MILLIS);
    millis      = millis % (MINUTES_TO_MILLIS);

    int32_t seconds = millis / (SECONDS_TO_MILLIS);
    millis      = millis % (SECONDS_TO_MILLIS);

    assert(hours >=0 && hours <= 23);
    if (isNegative) sstr << "-";
    sstr << years << "-" << months<< "-" << days << " " << hours << ":"<<minutes<<":"<<seconds<<"."<<millis;
    return sstr.str();
};

}// namespace Drill
