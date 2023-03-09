from typing import Optional
from jinja2 import Template
from loguru import logger
from feathr.definition.feathrconfig import HoconConvertible

class ConflictsAutoCorrection():
    """Conflicts auto-correction handler settings. 
       Used in feature join when some conflicts exist 
       between feature names and observation dataset columns.
    
    Attributes:
        rename_features: rename feature names when solving conflicts.
                         Default by 'False' which means to rename observation dataset columns.
        suffix: customized suffix to be applied to conflicts names. Default by "1"
                eg. The conflicted column name 'field' will become 'field_1' if suffix is "1"
    """
    def __init__(self, rename_features: bool = False, suffix: str = "1") -> None:
        self.rename_features = rename_features
        self.suffix = suffix
    
    def to_feature_config(self) -> str:
        tm = Template("""
            {% if auto_correction.rename_features %}
            renameFeatures: True
            {% else %}
            renameFeatures: False
            {% endif %}
            suffix: {{auto_correction.suffix}}  
        """)
        return tm.render(auto_correction=self)

class ObservationSettings(HoconConvertible):
    """Time settings of the observation data. Used in feature join.

    Attributes:
        observation_path: path to the observation dataset, i.e. input dataset to get with features
        event_timestamp_column (Optional[str]): The timestamp field of your record. As sliding window aggregation feature assume each record in the source data should have a timestamp column.
        timestamp_format (Optional[str], optional): The format of the timestamp field. Defaults to "epoch". Possible values are:
            - `epoch` (seconds since epoch), for example `1647737463`
            - `epoch_millis` (milliseconds since epoch), for example `1647737517761`
            - Any date formats supported by [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html).
        file_format: format of the dataset file. Default as "csv"
        is_file_path: if the 'observation_path' is a path of file (instead of a directory). Default as 'True'
        conflicts_auto_correction: settings about auto-correct feature names conflicts. 
                                   Default as None which means do not enable it.                                  
    """
    def __init__(self,
                 observation_path: str,
                 event_timestamp_column: Optional[str] = None,
                 timestamp_format: str = "epoch",
                 conflicts_auto_correction: ConflictsAutoCorrection = None,
                 file_format: str = "csv",
                 is_file_path: bool = True) -> None:
        self.event_timestamp_column = event_timestamp_column
        self.timestamp_format = timestamp_format
        self.observation_path = observation_path
        if observation_path.startswith("http"):
            logger.warning("Your observation_path {} starts with http, which is not supported. Consider using paths starting with wasb[s]/abfs[s]/s3.", observation_path)
        self.file_format = file_format
        self.is_file_path = is_file_path
        self.conflicts_auto_correction = conflicts_auto_correction
        
    def to_feature_config(self) -> str:
        tm = Template("""
                {% if setting.event_timestamp_column is not none %}
                settings: {
                    joinTimeSettings: {
                        timestampColumn: {
                            def: "{{setting.event_timestamp_column}}"
                            format: "{{setting.timestamp_format}}"
                        }
                    }
                    {% if setting.conflicts_auto_correction is not none %}
                    conflictsAutoCorrectionSettings: {
                        {{setting.conflicts_auto_correction.to_feature_config()}}
                    }
                    {% endif %}
                }
                {% endif %}
                observationPath: "{{setting.observation_path}}"
            """)
        return tm.render(setting=self)

