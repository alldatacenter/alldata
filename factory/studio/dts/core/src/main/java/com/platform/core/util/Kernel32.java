package com.platform.core.util;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Kernel32
 *
 * @author AllDataDC
 * @version 1.0
 * @since 2023/01/09
 */

public interface Kernel32 extends Library {
    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

    long GetProcessId(Long hProcess);
}
