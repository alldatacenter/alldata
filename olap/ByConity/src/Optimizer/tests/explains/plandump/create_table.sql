CREATE TABLE max_avg_parition_by  (
    id Nullable(Int), department Nullable(String),
    onboard_date Nullable(String),
    age Nullable(Int)
)
ENGINE = MergeTree()ORDER BY id ;
CREATE TABLE people (
    id Nullable(Int),
    department Nullable(String),
    onboard_date Nullable(String),
    age Nullable(Int)
)
ENGINE = MergeTree() ORDER BY id;