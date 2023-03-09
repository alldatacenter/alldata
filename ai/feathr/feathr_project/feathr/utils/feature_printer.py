from pprint import pprint
from typing import Union, List

from feathr.definition.anchor import FeatureAnchor
from feathr.definition.query_feature_list import FeatureQuery
from feathr.definition.materialization_settings import MaterializationSettings

class FeaturePrinter:
    """The class for pretty-printing features"""
    
    @staticmethod
    def pretty_print_anchors(anchor_list: List[FeatureAnchor]) -> None:
        """Pretty print features

        Args:
            feature_list: FeatureAnchor
        """

        if all(isinstance(anchor, FeatureAnchor) for anchor in anchor_list):
            for anchor in anchor_list:
                pprint("%s is the achor of %s" % \
                        (anchor.name, [feature.name for feature in anchor.features]))
        else:
            raise TypeError("anchor_list must be FeatureAnchor or List[FeatureAnchor]")

    @staticmethod
    def pretty_print_feature_query(feature_query: FeatureQuery) -> None:
        """Pretty print feature query

        Args:
            feature_query: feature query
        """

        if isinstance(feature_query, FeatureQuery):
            print("Features in feature_query: %s" % feature_query.feature_list)
        else:
            raise TypeError("feature_query must be FeatureQuery")

    @staticmethod
    def pretty_print_materialize_features(settings: MaterializationSettings) -> None:
        """Pretty print feature query

        Args:
            feature_query: feature query
        """
        if isinstance(settings, MaterializationSettings):
            print("Materialization features in settings: %s" % settings.feature_names)
        else:
            raise TypeError("settings must be MaterializationSettings")