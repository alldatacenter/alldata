# coding: utf-8

#pylint: disable=method-hidden

import json
from decimal import Decimal
from datetime import date, datetime, timedelta


class JSONEncoder2(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, datetime):
            return obj.strftime("%Y-%m-%d %H:%M:%S")
        elif isinstance(obj, date):
            return obj.strftime("%Y-%m-%d")
        elif isinstance(obj, Decimal):
            return float(obj)
        elif isinstance(obj, timedelta):
            if len(str(obj).split(':', 1)[0]) <=1:
                return '0' + str(obj)
            else:
                return str(obj)
        return json.JSONEncoder.default(self, obj)


def jsondumps(obj):
    ret = json.dumps(obj, ensure_ascii=False, cls=JSONEncoder2)
    return ret


def jsonloads(s):
    # ret = json.loads(s)
    ret = json.JSONDecoder(strict=False).decode(s)
    return ret
