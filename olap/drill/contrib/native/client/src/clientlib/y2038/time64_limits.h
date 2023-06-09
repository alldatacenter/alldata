/*

Copyright (c) 2007-2010  Michael G Schwern

This software originally derived from Paul Sheer's pivotal_gmtime_r.c.

The MIT License:

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

*/


/* 
   Maximum and minimum inputs your system's respective time functions
   can correctly handle.  time64.h will use your system functions if
   the input falls inside these ranges and corresponding USE_SYSTEM_*
   constant is defined.
*/

#ifndef TIME64_LIMITS_H
#define TIME64_LIMITS_H

/* Max/min for localtime() */
#define SYSTEM_LOCALTIME_MAX     2147483647
#define SYSTEM_LOCALTIME_MIN    -2147483647-1

/* Max/min for gmtime() */
#define SYSTEM_GMTIME_MAX        2147483647
#define SYSTEM_GMTIME_MIN       -2147483647-1

/* Max/min for mktime() */
static const struct tm SYSTEM_MKTIME_MAX = {
    7,
    14,
    19,
    18,
    0,
    138,
    1,
    17,
    0
#ifdef HAS_TM_TM_GMTOFF
    ,-28800
#endif
#ifdef HAS_TM_TM_ZONE
    ,"PST"
#endif
};

static const struct tm SYSTEM_MKTIME_MIN = {
    52,
    45,
    12,
    13,
    11,
    1,
    5,
    346,
    0
#ifdef HAS_TM_TM_GMTOFF
    ,-28800
#endif
#ifdef HAS_TM_TM_ZONE
    ,"PST"
#endif
};

/* Max/min for timegm() */
#ifdef HAS_TIMEGM
static const struct tm SYSTEM_TIMEGM_MAX = {
    7,
    14,
    3,
    19,
    0,
    138,
    2,
    18,
    0
    #ifdef HAS_TM_TM_GMTOFF
        ,0
    #endif
    #ifdef HAS_TM_TM_ZONE
        ,"UTC"
    #endif
};

static const struct tm SYSTEM_TIMEGM_MIN = {
    52,
    45,
    20,
    13,
    11,
    1,
    5,
    346,
    0
    #ifdef HAS_TM_TM_GMTOFF
        ,0
    #endif
    #ifdef HAS_TM_TM_ZONE
        ,"UTC"
    #endif
};
#endif /* HAS_TIMEGM */

#endif /* TIME64_LIMITS_H */
