from abc import ABC, abstractmethod

from soda.sampler.sample_context import SampleContext
from soda.sampler.sample_ref import SampleRef

DEFAULT_FAILED_ROWS_SAMPLE_LIMIT = 100


class Sampler(ABC):
    @abstractmethod
    def store_sample(self, sample_context: SampleContext) -> SampleRef:
        pass
