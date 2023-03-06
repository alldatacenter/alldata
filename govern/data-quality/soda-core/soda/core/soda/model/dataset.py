import dataclasses


@dataclasses.dataclass(frozen=True)
class Dataset:
    """Represents a dataset."""

    name: str
    schema: str
    database: str
