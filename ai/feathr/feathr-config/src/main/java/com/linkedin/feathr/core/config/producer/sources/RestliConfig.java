package com.linkedin.feathr.core.config.producer.sources;

import com.google.common.base.Preconditions;
import com.linkedin.data.schema.PathSpec;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;


/**
 * Represents the Rest.Li source config
 */
public final class RestliConfig extends SourceConfig {
  public static final String RESOURCE_NAME = "restResourceName";

  /**
   * @deprecated As of beta, the field name is a typo and will be removed
   */
  @Deprecated
  public static final String RESOUCE_NAME = "restResouceName";
  // Note: typo but still being supported. Ought to be removed.

  public static final String KEY_EXPR = "keyExpr";

  /**
   * @deprecated As of beta, this field is deprecated in favor of KEY_EXPR(keyExpr)
   */
  @Deprecated
  public static final String ENTITY_TYPE = "restEntityType";    // Note: this field is deprecated in favor of 'keyExpr'

  public static final String REQ_PARAMS = "restReqParams";
  public static final String PATH_SPEC = "pathSpec";
  public static final String FINDER = "finder";

  // Keys used in REQ_PARAMS
  public static final String JSON = "json";
  public static final String JSON_ARRAY = "jsonArray";
  public static final String JSON_ARRAY_ARRAY = "array";
  public static final String MVEL_KEY = "mvel";
  public static final String FILE = "file";

  private final String _resourceName;
  private final Optional<String> _keyExpr;
  private final Optional<Map<String, Object>> _reqParams;
  private final Optional<PathSpec> _pathSpec;
  private final Optional<String> _finder;

  /**
   * Constructor with keyExpr only
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param resourceName Name of the Rest.Li resource
   * @param keyExpr Key expression
   * @param reqParams request parameters specified as a Map
   * @param pathSpec PathSpec
   */
  public RestliConfig(@Nonnull String sourceName, @Nonnull String resourceName, @Nonnull String keyExpr,
      Map<String, Object> reqParams, PathSpec pathSpec) {
    this(sourceName, resourceName, keyExpr, reqParams, pathSpec, null);
  }

  /**
   * Construct a finder based {@link RestliConfig} for non-association resources where there is no association key required
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param resourceName Name of the Rest.Li resource
   * @param reqParams request parameters specified as a Map
   * @param pathSpec PathSpec
   * @param finder the finder method name of the resource.
   */
  public RestliConfig(@Nonnull String sourceName, @Nonnull String resourceName, Map<String, Object> reqParams,
      PathSpec pathSpec, @Nonnull String finder) {
    this(sourceName, resourceName, null, reqParams, pathSpec, finder);
  }

  /**
   * Constructor for creating a new instance of {@link RestliConfig} with both keyExpr
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param keyExpr Key expression for the resource.
   * @param resourceName Name of the Rest.Li resource
   * @param reqParams request parameters specified as a Map
   * @param pathSpec PathSpec
   * @param finder the finder method name of the resource.
   */
  public RestliConfig(String sourceName, String resourceName, String keyExpr, Map<String, Object> reqParams, PathSpec pathSpec, String finder) {
    super(sourceName);
    Preconditions.checkArgument(keyExpr != null || finder != null, "Either keyExpr or finder must be present for a RestLi source");
    _resourceName = resourceName;
    _keyExpr = Optional.ofNullable(keyExpr);
    _reqParams = Optional.ofNullable(reqParams);
    _pathSpec = Optional.ofNullable(pathSpec);
    _finder = Optional.ofNullable(finder);
  }

  public String getResourceName() {
    return _resourceName;
  }

  /**
   * @deprecated this might return null, please use {@link #getOptionalKeyExpr()} instead
   */
  @Deprecated
  public String getKeyExpr() {
    return _keyExpr.orElse(null);
  }

  public Optional<String> getOptionalKeyExpr() {
    return _keyExpr;
  }

  public Optional<Map<String, Object>> getReqParams() {
    return _reqParams;
  }

  public Optional<PathSpec> getPathSpec() {
    return _pathSpec;
  }

  public Optional<String> getFinder() {
    return _finder;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.RESTLI;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    RestliConfig that = (RestliConfig) o;
    return Objects.equals(_resourceName, that._resourceName) && Objects.equals(_keyExpr, that._keyExpr)
        && Objects.equals(_reqParams, that._reqParams) && Objects.equals(_pathSpec, that._pathSpec) && Objects.equals(
        _finder, that._finder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _resourceName, _keyExpr, _reqParams, _pathSpec, _finder);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RestliConfig{");
    sb.append("_resourceName='").append(_resourceName).append('\'');
    sb.append(", _keyExpr=").append(_keyExpr);
    sb.append(", _reqParams=").append(_reqParams);
    sb.append(", _pathSpec=").append(_pathSpec);
    sb.append(", _finder=").append(_finder);
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
