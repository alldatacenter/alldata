from dataclasses import dataclass
from typing import Tuple

from soda.sampler.sample_context import SampleContext
from soda.sampler.sample_ref import SampleRef
from soda.sampler.soda_cloud_sampler import SodaCloudSampler


@dataclass
class Sample:
    rows: Tuple[Tuple]
    sample_ref: SampleRef


class MockSampler(SodaCloudSampler):
    def __init__(self):
        self.samples = []

    def store_sample(self, sample_context: SampleContext) -> SampleRef:
        sample_ref = super().store_sample(sample_context)
        self.samples.append(Sample(rows=sample_context.sample.get_rows(), sample_ref=sample_ref))
        return sample_ref
