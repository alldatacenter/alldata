from feathr import Feature
from feathr import ExpressionTransformation
from feathr import DerivedFeature
from feathr import BOOLEAN, FLOAT, FLOAT_VECTOR, ValueType
from feathr import TypedKey
import pytest

def assert_config_equals(one, another):
    assert one.translate(str.maketrans('', '', ' \n\t\r')) == another.translate(str.maketrans('', '', ' \n\t\r'))

def test_single_key_derived_feature_to_config():
    """Single key derived feature config generation should work"""
    user_key = TypedKey(full_name="mockdata.user", key_column="user_id", key_column_type=ValueType.INT32, description="An user identifier")
    user_embedding = Feature(name="user_embedding", feature_type=FLOAT_VECTOR, key=user_key)

    # A derived feature
    derived_feature = DerivedFeature(name="user_embemdding_derived",
                                        feature_type=FLOAT,
                                        key=user_key,
                                        input_features=user_embedding,
                                        transform="if_else(user_embedding, user_embedding, [])")

    derived_feature_config = """
    user_embemdding_derived: {
        key: [user_id]
        inputs: {
            user_embedding: {key: [user_id], feature: user_embedding}
        }
        definition.sqlExpr: "if_else(user_embedding, user_embedding, [])"
        type: {
            type: TENSOR
            tensorCategory: DENSE
            dimensionType: []
            valType: FLOAT
        }
    }"""
    assert_config_equals(derived_feature.to_feature_config(), derived_feature_config)

def test_multikey_derived_feature_to_config():
    """Multikey derived feature config generation should work"""
    user_key = TypedKey(full_name="mockdata.user", key_column="user_id", key_column_type=ValueType.INT32, description="An user identifier")
    item_key = TypedKey(full_name="mockdata.item", key_column="item_id", key_column_type=ValueType.INT32, description="An item identifier")

    user_embedding = Feature(name="user_embedding", feature_type=FLOAT_VECTOR, key=user_key)
    item_embedding = Feature(name="item_embedding", feature_type=FLOAT_VECTOR, key=item_key)

    # A derived feature
    user_item_similarity = DerivedFeature(name="user_item_similarity",
                                        feature_type=FLOAT,
                                        key=[user_key, item_key],
                                        input_features=[user_embedding, item_embedding],
                                        transform="similarity(user_embedding, item_embedding)")

    derived_feature_config = """
    user_item_similarity: {
        key: [user_id, item_id]
        inputs: {
            user_embedding: {key: [user_id], feature: user_embedding}
            item_embedding: {key: [item_id], feature: item_embedding}
        }
        definition.sqlExpr: "similarity(user_embedding,item_embedding)"
        type: {
            type: TENSOR
            tensorCategory: DENSE
            dimensionType: []
            valType: FLOAT
        }
    }"""
    assert_config_equals(user_item_similarity.to_feature_config(), derived_feature_config)


def test_derived_feature_to_config_with_alias():
    # More complicated use case, viewer viewee aliasged user key
    # References the same key feature with different alias
    user_key = TypedKey(full_name="mockdata.user", key_column="user_id", key_column_type=ValueType.INT32, description="An user identifier")
    user_embedding = Feature(name="user_embedding", key=user_key, feature_type=FLOAT_VECTOR)
    viewer_viewee_distance = DerivedFeature(name="viewer_viewee_distance",
                        key=[user_key.as_key("viewer"), user_key.as_key("viewee")],
                        feature_type=FLOAT,
                        input_features=[user_embedding.with_key("viewer").as_feature("viewer_embedding"),
                                        user_embedding.with_key("viewee").as_feature("viewee_embedding")],
                        transform="distance(viewer_embedding, viewee_embedding)")

    expected_feature_config = """
    viewer_viewee_distance: {
        key: [viewer, viewee]
        inputs: {
            viewer_embedding: {key: [viewer], feature: user_embedding}
            viewee_embedding: {key: [viewee], feature: user_embedding}
        }
        definition.sqlExpr: "distance(viewer_embedding, viewee_embedding)"
        type: {
            type: TENSOR
            tensorCategory: DENSE
            dimensionType: []
            valType: FLOAT
        }
    }"""
    assert_config_equals(viewer_viewee_distance.to_feature_config(), expected_feature_config)


def test_multi_key_derived_feature_to_config_with_alias():
    # References the same relation feature key alias with different alias
    # Note that in this case, it is possible that distance(a, b) != distance(b,a)
    user_key = TypedKey(full_name="mockdata.user", key_column="user_id", key_column_type=ValueType.INT32, description="An user identifier")
    user_embedding = Feature(name="user_embedding", key=user_key, feature_type=FLOAT_VECTOR)

    viewer_viewee_distance = DerivedFeature(name="viewer_viewee_distance",
                        key=[user_key.as_key("viewer"), user_key.as_key("viewee")],
                        feature_type=FLOAT,
                        input_features=[user_embedding.with_key("viewer").as_feature("viewer_embedding"),
                                        user_embedding.with_key("viewee").as_feature("viewee_embedding")],
                        transform="distance(viewer_embedding, viewee_embedding)")

    viewee_viewer_combined = DerivedFeature(name = "viewee_viewer_combined_distance",
                                        key=[user_key.as_key("viewer"), user_key.as_key("viewee")],
                                        feature_type=FLOAT,
                                        input_features=[viewer_viewee_distance.with_key(["viewer", "viewee"])
                                                                            .as_feature("viewer_viewee_distance"),
                                                        viewer_viewee_distance.with_key(["viewee", "viewer"])
                                                                            .as_feature("viewee_viewer_distance"),],
                                        transform=ExpressionTransformation("viewer_viewee_distance + viewee_viewer_distance"))
    # Note that unlike key features, a relation feature does not need a feature anchor.

    expected_feature_config = """
    viewee_viewer_combined_distance: {
        key: [viewer, viewee]
        inputs: {
            viewer_viewee_distance: {key: [viewer, viewee], feature: viewer_viewee_distance}
            viewee_viewer_distance: {key: [viewee, viewer], feature: viewer_viewee_distance}
        }
        definition.sqlExpr: "viewer_viewee_distance + viewee_viewer_distance"
        type: {
            type: TENSOR
            tensorCategory: DENSE
            dimensionType: []
            valType: FLOAT
        }
    }"""
    assert_config_equals(viewee_viewer_combined.to_feature_config(), expected_feature_config)

def test_derived_feature_on_multikey_anchored_feature_to_config():
    """Multikey derived feature config generation should work"""
    user_key = TypedKey(full_name="mockdata.user", key_column="user_id", key_column_type=ValueType.INT32, description="First part of an user identifier")
    user_key2 = TypedKey(full_name="mockdata.user2", key_column="user_id2", key_column_type=ValueType.INT32, description="Second part of an user identifier")

    user_embedding = Feature(name="user_embedding", feature_type=FLOAT_VECTOR, key=[user_key, user_key2])

    # A derived feature
    user_item_derived = DerivedFeature(name="user_item_similarity",
                                        feature_type=FLOAT,
                                        key=[user_key.as_key("viewer"), user_key2.as_key("viewee")],
                                        input_features=user_embedding.with_key(["viewer", "viewee"]),
                                        transform="if_else(user_embedding, user_embedding, [])")

    derived_feature_config = """
    user_item_similarity: {
        key: [viewer, viewee]
        inputs: {
            user_embedding: {key: [viewer, viewee], feature: user_embedding}
        }
        definition.sqlExpr: "if_else(user_embedding, user_embedding, [])"
        type: {
            type: TENSOR
            tensorCategory: DENSE
            dimensionType: []
            valType: FLOAT
        }
    }"""
    assert_config_equals(user_item_derived.to_feature_config(), derived_feature_config)



def test_multi_key_derived_feature_to_config_with_wrong_alias():
    # References the same relation feature key alias with wrong alias
    # Should throw exception
    user_key = TypedKey(full_name="mockdata.user", key_column="user_id", key_column_type=ValueType.INT32, description="An user identifier")
    user_embedding = Feature(name="user_embedding", key=user_key, feature_type=FLOAT_VECTOR)

    with pytest.raises(AssertionError):
        viewer_viewee_distance = DerivedFeature(name="viewer_viewee_distance",
                                    key=[user_key.as_key("non_exist_alias"), user_key.as_key("viewee")],
                                    feature_type=FLOAT,
                                    input_features=[user_embedding.with_key("viewer").as_feature("viewer_embedding"),
                                                    user_embedding.with_key("viewee").as_feature("viewee_embedding")],
                                    transform="distance(viewer_embedding, viewee_embedding)")

