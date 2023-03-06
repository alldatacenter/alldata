#  Copyright 2020 Soda
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#   http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import datetime
import json
from datetime import timezone
from decimal import Decimal
from enum import Enum

from soda.common.undefined_instance import Undefined


class JsonHelper:
    @staticmethod
    def to_json(o):
        return json.dumps(JsonHelper.to_jsonnable(o))

    @staticmethod
    def to_json_pretty(o):
        return json.dumps(JsonHelper.to_jsonnable(o), indent=2)

    @staticmethod
    def to_jsonnable(o):
        if o is None or isinstance(o, str) or isinstance(o, int) or isinstance(o, float) or isinstance(o, bool):
            return o
        if isinstance(o, dict):
            for key, value in o.items():
                update = False
                if not isinstance(key, str):
                    del o[key]
                    key = str(key)
                    update = True

                jsonnable_value = JsonHelper.to_jsonnable(value)
                if value is not jsonnable_value:
                    value = jsonnable_value
                    update = True
                if update:
                    o[key] = value
            return o
        if isinstance(o, tuple):
            return JsonHelper.to_jsonnable(list(o))
        if isinstance(o, list):
            for i in range(len(o)):
                element = o[i]
                jsonnable_element = JsonHelper.to_jsonnable(element)
                if element is not jsonnable_element:
                    o[i] = jsonnable_element
            return o
        if isinstance(o, Decimal):
            return float(o)
        if isinstance(o, datetime.datetime):
            return o.astimezone(timezone.utc).isoformat(timespec="seconds")
        if isinstance(o, datetime.date):
            return o.strftime("%Y-%m-%d")
        if isinstance(o, datetime.time):
            return o.strftime("%H:%M:%S")
        if isinstance(o, datetime.timedelta):
            return str(o)
        if isinstance(o, Enum):
            return o.value
        if isinstance(o, Undefined):
            return None
        if isinstance(o, Exception):
            return str(o)
        raise RuntimeError(f"Do not know how to jsonize {o} ({type(o)})")

    @classmethod
    def filter_null_values(cls, o):
        if isinstance(o, dict):
            return {k: cls.filter_null_values(v) for k, v in o.items() if v is not None}
        if isinstance(o, list) or isinstance(o, tuple):
            return [cls.filter_null_values(e) for e in o if e is not None]
        return o
