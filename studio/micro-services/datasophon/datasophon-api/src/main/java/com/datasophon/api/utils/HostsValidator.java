package com.datasophon.api.utils;

import com.datasophon.api.annotation.Hosts;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.HostUtils;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;


public class HostsValidator implements ConstraintValidator<Hosts, String> {

    private static final Pattern ALPHABET_AND_NUMBER = Pattern.compile("[a-zA_Z0-9,]+");
    private static final Pattern BASIC = Pattern.compile("[a-zA-Z0-9_.\\[\\-\\],]+");
    private static final Pattern ALPHABET = Pattern.compile(".*[a-zA-Z].*+");
    private static final Pattern IP = Pattern.compile("[0-9.\\[\\]\\-]+");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        //1.只有字母和数字
        if (ALPHABET_AND_NUMBER.matcher(value).matches()) {
            return true;
        }
        //2.不能有特殊字符
        if (!BASIC.matcher(value).matches()) {
            return false;
        }
        //3.字母和[-]任一字符不能同时出现
        boolean flagOne = ALPHABET.matcher(value).matches();
        boolean flagTwo=value.contains(Constants.CENTER_BRACKET_LEFT);
        boolean flagThree=value.contains(Constants.CENTER_BRACKET_RIGHT);
        boolean flagFour=value.contains(Constants.HYPHEN);
        boolean flagFive=flagTwo|| flagThree || flagFour;
        //if (ALPHABET.matcher(value).matches() && (value.contains(Constants.CENTER_BRACKET_LEFT) || value.contains(Constants.CENTER_BRACKET_RIGHT) || value.contains(Constants.HYPHEN))) {
        if (flagOne && flagFive) {
            return false;
        }
        //4. [ - ] 三个字符顺序不能颠倒
        int left = value.indexOf("[");
        int mid = value.indexOf("-");
        int right = value.indexOf("]");
        if (left > mid || left > right || mid > right) {
            return false;
        }
        //5.
        String[] items = value.split(",");
        for (String item : items) {
            //如果是标准ip
            if (HostUtils.checkIP(item)) {
                continue;
            }
            //*[*-*]* 类型的ip
            if (IP.matcher(item).matches()) {
                String[] ipItems = item.split("\\.");
                int splitLen = ipItems.length;
                //每个ip地址 .splitLength == 4
                if (splitLen != 4) {
                    return false;
                }
                for (int i = 0; i < splitLen; i++) {
                    String curNumStr = ipItems[i];
                    //粗筛，这里直接不判断这种
                    if (curNumStr.contains("[") || curNumStr.contains("-") || curNumStr.contains("]")) {
                        continue;
                    }
                    if (ALPHABET.matcher(curNumStr).matches()) {
                        return false;
                    }
                    int curLen = ipItems[i].length();
                    //不是第一个不能以0开头 || 以 0 开头却大于0
                    boolean conditionOne=i != 0 && ipItems[i].startsWith("0");
                    boolean conditionTwo=curLen > 1 && ipItems[i].startsWith("0");
                    //if ((i != 0 && ipItems[i].startsWith("0")) || (curLen > 1 && ipItems[i].startsWith("0"))) {
                    if (conditionOne|| conditionTwo) {
                        return false;
                    }
                    int splitInt = Integer.parseInt(ipItems[i]);
                    //1 < 位数 <= 3 , 0 < 值 < 255
                    if (curLen < 1 || curLen > 3 || splitInt > 255 || splitInt < 0) {
                        return false;
                    }
                }
            }
            //否则按主机名处理
            HostUtils.checkHostname(item);
        }
        return true;
    }

}
