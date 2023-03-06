import logging
import math
from typing import List

import numpy as np
import pandas as pd
from pydantic import BaseModel

from soda.scientific.distribution.utils import RefDataCfg

BINS_AND_WEIGHTS_REF_LOG = (
    "To learn more about the computation of bins and weights refer https://go.soda.io/bins-weights"
)


class DRO(BaseModel):
    weights: List
    bins: List

    class Config:
        arbitrary_types_allowed = True


def normalize(data: np.ndarray) -> np.ndarray:
    return data / np.sum(data)


class DROGenerator:
    def __init__(self, cfg: RefDataCfg, data: list) -> None:
        self.distribution_type = cfg.distribution_type
        self.data = data
        self.maximum_allowed_bin_size = 1e6

    @staticmethod
    def _compute_n_bins(data: np.ndarray) -> int:
        _range = (data.min(), data.max())

        if _range[0] == _range[1]:  # It means that there is a single distinct value in array
            n_bins = 1
            return n_bins

        first_edge, last_edge = np.lib.histograms._get_outer_edges(data, _range)

        bin_width = np.lib.histograms._hist_bin_auto(data, _range)

        n_bins = min(np.ceil(np.lib.histograms._unsigned_subtract(last_edge, first_edge) / bin_width), data.size)
        return int(n_bins)

    @staticmethod
    def _remove_outliers_with_iqr(data: np.ndarray) -> np.ndarray:
        # Remove outliers
        q1, q3 = np.percentile(data, [25, 75])
        IQR = q3 - q1
        lower_range = q1 - (1.5 * IQR)
        upper_range = q3 + (1.5 * IQR)
        filtered_data = data[np.where((data >= lower_range) & (data <= upper_range))]
        # TODO: insert doc link that explains IQR
        logging.warning(
            f"""Generating the distribution reference object using automatic bin size detection would cause a
memory error. This is generally caused by the presence of outliers in the dataset which leads to very high number
of bins.

We filtered values above {lower_range} and below {upper_range} using IQR

{BINS_AND_WEIGHTS_REF_LOG}
"""
        )
        return filtered_data

    def generate_continuous_dro(self) -> DRO:
        data = np.array(self.data, dtype=float)
        if (np.isnan(data)).any():
            data_len = data.shape[0]
            none_count = np.count_nonzero(np.isnan(data))
            data = data[~np.isnan(data)]
            logging.warning(
                f"""{none_count} out of {data_len} rows
has None values! To estimate the weights and bins, the null values
has been ignored!
            """
            )

        n_bins = self._compute_n_bins(data)

        # First check whether we have n_bins having lower than data size
        if n_bins < min(self.maximum_allowed_bin_size, data.size):
            weights, bins = np.histogram(data, bins="auto", density=False)

        # If we have too much n_bins, then we apply several methods
        else:
            # First remove the outliers from data and recompute number of bins
            outlier_filtered_data = self._remove_outliers_with_iqr(data)
            n_bins = self._compute_n_bins(outlier_filtered_data)

            # If n_bins is lower than data size then run auto mode again
            if n_bins < min(self.maximum_allowed_bin_size, outlier_filtered_data.size):
                weights, bins = np.histogram(outlier_filtered_data, bins="auto", density=False)

            # If not then take the sqrt of data size and make it as our new n_bins
            else:
                n_sqrt_bins = int(np.ceil(math.sqrt(outlier_filtered_data.size)))
                if n_sqrt_bins < self.maximum_allowed_bin_size:
                    logging.warning(
                        f"""Filtering out outliers did not solve the memory error. As a last resort, we will
take the square root of the data size to set the number of bins.

{BINS_AND_WEIGHTS_REF_LOG}
"""
                    )
                    weights, bins = np.histogram(outlier_filtered_data, bins=n_sqrt_bins, density=False)
                else:
                    logging.warning(
                        f"""We set n_bins={self.maximum_allowed_bin_size} as maximum since
automatically computed {n_bins} is higher than maximum allowed bin size: {self.maximum_allowed_bin_size}

{BINS_AND_WEIGHTS_REF_LOG}
"""
                    )
                    weights, bins = np.histogram(
                        outlier_filtered_data, bins=self.maximum_allowed_bin_size, density=False
                    )

        # Prepend 0 since weights and bins do not have the same size
        weights = np.insert(weights, 0, 0)
        weights = normalize(weights)
        return DRO(weights=weights.tolist(), bins=bins.tolist())

    def generate_categorical_dro(self) -> DRO:
        data = pd.Series(self.data)
        value_counts = data.value_counts()

        labels = value_counts.index.to_numpy()
        weights = value_counts.to_numpy()
        weights = normalize(weights)
        return DRO(weights=weights.tolist(), bins=labels.tolist())

    def generate(self) -> DRO:
        if self.distribution_type == "continuous":
            return self.generate_continuous_dro()
        else:
            return self.generate_categorical_dro()
