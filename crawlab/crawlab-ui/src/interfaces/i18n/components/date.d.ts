interface LComponentsDate {
  dateRangePicker: {
    options: {
      today: string;
      yesterday: string;
      pastNMinutes: string;
      pastNHours: string;
      pastNDays: string;
      pastNWeeks: string;
      pastNMonths: string;
      custom: string;
    };
  };
  units: {
    second: string;
    minute: string;
    hour: string;
    day: string;
    week: string;
    month: string;
    quarter: string;
    year: string;
  };
}
