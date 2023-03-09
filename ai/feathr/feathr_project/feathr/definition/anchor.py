from typing import List, Optional, Dict
from feathr.definition.feature import Feature
from feathr.definition.source import Source
from feathr.definition.typed_key import DUMMY_KEY
from jinja2 import Template
from feathr.definition.source import INPUT_CONTEXT
from feathr.definition.feathrconfig import HoconConvertible


class FeatureAnchor(HoconConvertible):
    """
        A feature anchor defines a set of features on top of a data source, a.k.a. a set of features anchored to a source.

        The feature producer writes multiple anchors for a feature, exposing the same feature name for the feature
        consumer to reference it.

        Attributes:
            name: Unique name of the anchor.
            source: data source that the features are anchored to. Should be either of `INPUT_CONTEXT` or `feathr.source.Source`
            features: list of features defined within this anchor.
            registry_tags: A dict of (str, str) that you can pass to feature registry for better organization.
                           For example, you can use {"deprecated": "true"} to indicate this anchor is deprecated, etc.
    """
    def __init__(self,
                name: str,
                source: Source,
                features: List[Feature],
                registry_tags: Optional[Dict[str, str]] = None,
                **kwargs):
        self.name = name
        self.features = features
        self.source = source
        self.registry_tags=registry_tags
        # Add a hidden option to skip validation, Anchor could be half-constructed during the loading from registry
        if not kwargs.get("__no_validate", False) :
            self.validate_features()

    def validate_features(self):
        """Validate that anchor is non-empty and all its features share the same key"""
        # assert len(self.features) > 0
        if self.source != INPUT_CONTEXT:
            for feature in self.features:
                if feature.key == [DUMMY_KEY]:
                    raise RuntimeError(f"For anchors of non-INPUT_CONTEXT source, key of feature {feature.name} "
                                       f"should be explicitly specified and not left blank.")
        
    def to_feature_config(self) -> str:
        tm = Template("""
            {{anchor_name}}: {
                source: {{source.name}}
                key.sqlExpr: [{{key_list}}]
                features: {
                    {% for feature in features %}
                        {{feature.to_feature_config()}}
                    {% endfor %}
                }
            }
        """)
        key_list = ','.join((key for key in self.features[0].key_alias) if len(self.features)!=0 else [])
        return tm.render(anchor_name = self.name,
                        key_list = key_list,
                        features = self.features,
                        source = self.source)

    def __str__(self):
        return self.to_feature_config()
