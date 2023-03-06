class ChangeOverTimeCfg:
    def __init__(self):
        self.last_measurements: Optional[int] = None
        self.last_aggregation: Optional[str] = None
        self.same_day_last_week: bool = False
        self.same_day_last_month: bool = False
        self.percent: bool = False

    def to_jsonnable(self):
        jsonnable = {}
        if self.last_measurements:
            jsonnable["last_measurements"] = self.last_measurements
        if self.last_aggregation:
            jsonnable["last_aggregation"] = self.last_aggregation
        if self.same_day_last_week:
            jsonnable["same_day_last_week"] = self.same_day_last_week
        if self.same_day_last_month:
            jsonnable["same_day_last_month"] = self.same_day_last_month
        if self.percent:
            jsonnable["percent"] = self.percent
        return jsonnable
