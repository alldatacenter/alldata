from abc import ABC, abstractmethod
from typing import Tuple

from soda.sampler.sample_schema import SampleSchema


class Sample(ABC):
    @abstractmethod
    def get_rows(self) -> Tuple[Tuple]:
        pass

    @abstractmethod
    def get_schema(self) -> SampleSchema:
        pass
