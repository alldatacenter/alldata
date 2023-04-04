/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;

public class DateUtils {

    private static final String[] FMT = {"y", "M", "d", "H", "m", "s", "S"};

    public static String inferDateFormat(String src) {
        int fmtIdx = 0;
        boolean findMatch = false;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < src.length(); i++) {
            char chr = src.charAt(i);
            if (Character.isDigit(chr)) {
                findMatch = true;
                stringBuilder.append(FMT[fmtIdx]);
            } else {
                if (findMatch) {
                    fmtIdx++;
                    findMatch = false;
                }
                stringBuilder.append(chr);
            }
            if (fmtIdx == FMT.length - 1 && i < src.length() - 1) {
                stringBuilder.append(src.substring(i + 1));
                break;
            }
        }
        return stringBuilder.toString();
    }

    public static boolean isDateFormat(String format) {
        return StringUtils.isNotBlank(format) && "yyyy-MM-dd".equalsIgnoreCase(format.trim());
    }

    public static boolean isDateTimeFormat(String format) {
        return StringUtils.isNotBlank(format) && "yyyy-MM-dd HH:mm:ss".equalsIgnoreCase(format.trim());
    }

    public static String withTimeString(String name) {
        return name + DateFormatUtils.format(new Date(), DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS"));
    }

}
