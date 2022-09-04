package com.alibaba.sreworks.warehouse.common.constant;


import java.util.regex.Pattern;

public class DomainConstant {

    public static final String DOAMIN_ABBREVIATION_REGEX = "^[a-z][a-z0-9_]{0,62}[a-z0-9]$";

    public static final Pattern DOAMIN_ABBREVIATION_PATTERN = Pattern.compile(DOAMIN_ABBREVIATION_REGEX);
}
