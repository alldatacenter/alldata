"""Handles user feedback consumption."""
# extract the regularity
# generate a "fake" custom regressor which:
#   # captures the delta between the predicted and actual value,
#   # reproduces is forward at captured regularity with some smoothing/decay around the point

from datetime import date
from typing import Any, Dict

import pandas as pd
from soda.common.logs import Logs

FEEDBACK_REASONS = {
    "expectedDailySeasonality": {
        "internal_remap": "daily_seasonality",
        "frequency_unit": "D",
        "frequency_value": 1,
    },
    "expectedWeeklySeasonality": {
        "internal_remap": "weekly_seasonality",
        "frequency_unit": "W",
        "frequency_value": 1,
    },
    "expectedMonthlySeasonality": {
        "internal_remap": "monthly_seasonality",
        "frequency_unit": "M",
        "frequency_value": 1,
    },
    "expectedYearlySeasonality": {
        "internal_remap": "yearly_seasonality",
        "frequency_unit": "Y",
        "frequency_value": 1,
    },
}


WEEK_ANCHORED_OFFSETS = {
    "Monday": "MON",
    "Tuesday": "TUE",
    "Wednesday": "WED",
    "Thursday": "THU",
    "Friday": "FRI",
    "Saturday": "SAT",
    "Sunday": "SUN",
}


class FeedbackProcessor:
    """Processes user feedback."""

    def __init__(self, params: Dict[str, Any], df_historic: pd.DataFrame, logs: Logs):
        """Constructor for FeedbackProcessor."""
        self._logs = logs
        self._params = params
        self.has_feedback = self.check_feedback(df_historic)
        self.df_feedback_processed: pd.DataFrame = self.process_feedback(df_historic)
        self._has_misclassification = False
        self.has_exegonenous_regressor = False

    def run(self):
        self.flag_misclassification()
        self.derive_exogenous_regressor()

    @staticmethod
    def check_feedback(df_historic: pd.DataFrame) -> bool:
        df = df_historic.copy()
        if "feedback" in df.columns:
            return not df["feedback"].isnull().all()
        return False

    def process_feedback(self, df_historic: pd.DataFrame) -> pd.DataFrame:
        df = df_historic.copy()
        if self.has_feedback:
            df["feedback"] = df["feedback"].fillna(pd.NA)
            df["feedback"] = df["feedback"].apply(lambda x: {} if pd.isnull(x) else x)
            feedback_array = df["feedback"].values
            df_flattened = pd.json_normalize(feedback_array)  # type: ignore
            df_joined = pd.merge(df, df_flattened, left_index=True, right_index=True)
            return df_joined
        return df

    def flag_misclassification(self):
        # TODO: when we deprecate the legacy field we might want to flip the entire direction of is_misclassification
        df_feedback_processed_cols = self.df_feedback_processed.columns
        if self.has_feedback:
            if "isCorrectlyClassified" in df_feedback_processed_cols:
                self.df_feedback_processed["isCorrectlyClassified"] = self.df_feedback_processed[
                    "isCorrectlyClassified"
                ].fillna(True)
            else:
                self.df_feedback_processed["isCorrectlyClassified"] = True

            self.df_feedback_processed["is_misclassification"] = ~self.df_feedback_processed["isCorrectlyClassified"]

            # Replace NAN in "reason" with some string that won't match later so that we dump it
            # and it does not upset the rest of the flow
            if "reason" in df_feedback_processed_cols:
                self.df_feedback_processed["reason"] = self.df_feedback_processed["reason"].fillna("Invalid reason")
            else:
                self.df_feedback_processed["reason"] = "Invalid reason"

    def derive_exogenous_regressor(self):
        if self.has_feedback:
            feedback_ref_mapping = pd.DataFrame.from_dict(FEEDBACK_REASONS, orient="index").reset_index()

            self.df_feedback_processed["predicted_to_real_delta"] = (
                self.df_feedback_processed["y"] - self.df_feedback_processed["anomaly_predicted_value"]
            )
            df_regressor_ref = self.df_feedback_processed.loc[
                self.df_feedback_processed["is_misclassification"] == True  # noqa: E712
            ]
            self._logs.debug(f"Processing {len(df_regressor_ref)} user feedbacks")
            df_regressor_ref = df_regressor_ref.merge(
                feedback_ref_mapping, how="left", left_on="reason", right_on="index"
            )

            # grab the day of the week
            df_regressor_ref["day_of_week"] = df_regressor_ref["ds"].dt.day_name()
            df_regressor_ref = df_regressor_ref.replace({"day_of_week": WEEK_ANCHORED_OFFSETS})

            # for each row in the ref table
            # build a date_range with start as misclass and end as today using frequency unit + DAY (if "W").
            offsets = pd.DataFrame()
            df_regressor_ref["ds"] = df_regressor_ref["ds"].dt.tz_localize(None)
            if not df_regressor_ref.empty:
                for _, misclassification in df_regressor_ref.iterrows():
                    _offsets = pd.date_range(
                        misclassification["ds"],
                        date.today(),
                        freq=f'W-{misclassification["day_of_week"]}',
                        normalize=True,
                    )
                    _offsets = _offsets.to_frame(index=False, name="exg_reg_date")
                    _offsets["delta"] = misclassification["predicted_to_real_delta"]
                    _offsets["misclassification_start"] = misclassification["ds"]
                    _offsets["chosen_reason"] = misclassification["reason"]
                    _offsets["normalised_date"] = pd.to_datetime(misclassification["ds"]).normalize()

                    # concat and join to main table.
                    offsets = offsets.append(_offsets)
                self.offsets = offsets
                # Consider only weekly feedback # TODO: enable the other ones later.
                self.offsets = self.offsets.loc[self.offsets["chosen_reason"] == "expectedWeeklySeasonality"]

                # join the offsets to the main table on offset date
                self.df_feedback_processed["normalised_date_left"] = self.df_feedback_processed["ds"].dt.normalize()
                self.df_feedback_processed = self.df_feedback_processed.merge(
                    self.offsets,
                    how="left",
                    left_on="normalised_date_left",
                    right_on="exg_reg_date",
                )

                # drop columns we do not want anymore
                feedback_processor_params = self._params["feedback_processor_params"]["output_columns"]
                self.df_feedback_processed = self.df_feedback_processed[
                    self.df_feedback_processed.columns[
                        self.df_feedback_processed.columns.isin(list(feedback_processor_params.keys()))
                    ]
                ]
                # rename columns
                self.df_feedback_processed = self.df_feedback_processed.rename(columns=feedback_processor_params)
                self.has_exegonenous_regressor = True

                # fill nas with 0s? # TODO: We might want to revisit this if 0s mess the non
                # feedbacked data points because the model tries to learn too much from it
                self.df_feedback_processed.loc[
                    self.df_feedback_processed["external_regressor"].isnull(), "external_regressor"
                ] = 0
