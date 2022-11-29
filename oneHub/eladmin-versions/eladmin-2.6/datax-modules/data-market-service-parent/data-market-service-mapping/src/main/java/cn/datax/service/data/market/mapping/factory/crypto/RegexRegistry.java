package cn.datax.service.data.market.mapping.factory.crypto;

import cn.datax.service.data.market.api.enums.RegexCrypto;

import java.util.EnumMap;
import java.util.Map;

public class RegexRegistry {

    private final Map<RegexCrypto, Crypto> regex_enum_map = new EnumMap<>(RegexCrypto.class);

    public RegexRegistry() {
        regex_enum_map.put(RegexCrypto.CHINESE_NAME, new CHINESENAMECrypto());
        regex_enum_map.put(RegexCrypto.ID_CARD, new IDCARDCrypto());
        regex_enum_map.put(RegexCrypto.FIXED_PHONE, new FIXEDPHONECrypto());
        regex_enum_map.put(RegexCrypto.MOBILE_PHONE, new MOBILEPHONECrypto());
        regex_enum_map.put(RegexCrypto.ADDRESS, new ADDRESSCrypto());
        regex_enum_map.put(RegexCrypto.EMAIL, new EMAILCrypto());
        regex_enum_map.put(RegexCrypto.BANK_CARD, new BANKCARDCrypto());
        regex_enum_map.put(RegexCrypto.CNAPS_CODE, new CNAPSCODECrypto());
    }

    public Crypto getRegex(RegexCrypto crypto) {
        return regex_enum_map.get(crypto);
    }
}
