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

package datart.security.util;


import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class SecurityUtils {

    public static final int VERIFY_CODE_TIMEOUT_MIN = 10;

    private static final int RANDOM_CODE_LEN = 8;

    private static final char[] SEEDS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    public static String randomPassword(Integer length) {
        IntStream intStream = new Random().ints(0, SEEDS.length);
        return intStream.limit(length).mapToObj(i -> SEEDS[i]).map(String::valueOf).collect(Collectors.joining());
    }

    public static String randomPassword() {
        return randomPassword(RANDOM_CODE_LEN);
    }

}
