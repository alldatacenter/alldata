/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.swagger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;

import org.apache.ambari.annotations.SwaggerOverwriteNestedAPI;
import org.apache.ambari.annotations.SwaggerPreferredParent;
import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import com.github.kongchen.swagger.docgen.reader.JaxrsReader;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.swagger.annotations.Api;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.properties.StringProperty;

/**
 * Customized {@link com.github.kongchen.swagger.docgen.reader.ClassSwaggerReader} implementation to
 * treat nested API's.
 */
public class AmbariSwaggerReader extends JaxrsReader {

  /**
   * Logger instance.
   */
  protected final static Logger logger = LoggerFactory.getLogger(AmbariSwaggerReader.class);

  public AmbariSwaggerReader(Swagger swagger, Log LOG) {
    super(swagger, LOG);
  }

  private final Map<Class<?>, NestedApiRecord> nestedAPIs = Maps.newHashMap();

  @Override
  public Swagger getSwagger() {
    if (null == this.swagger) {
      this.swagger = new Swagger();
    }
    return this.swagger;
  }

  /**
   * Original method is overwritten so that to gather information about top level api - nested api relations
   */
  @Override
  public Swagger read(Set<Class<?>> classes) {
    // scan for and register nested API classes
    logger.debug("Looking for nested API's");
    for (Class<?> cls: classes) {
      logger.debug("Examining API {}", cls.getSimpleName());
      for (Method method: cls.getMethods()) {
        Path methodPath = AnnotationUtils.findAnnotation(method, Path.class);
        if (null != methodPath) {
          Class<?> returnType = method.getReturnType();
          Api nestedApi = AnnotationUtils.findAnnotation(returnType, Api.class);
          Path nestedApiPath = AnnotationUtils.findAnnotation(returnType, Path.class);
          logger.debug("Examining API method {}#{}, path={}, returnType={}", cls.getSimpleName(), method.getName(),
              nestedApiPath != null ? nestedApiPath.value() : null, returnType.getSimpleName());
          if (null != nestedApi) {
            if (null != nestedApiPath) {
              logger.info("This class exists both as top level and nested API: {}, treating it as top level API",
                  returnType.getName());
            }
            else {
              boolean skipAdd = false;
              Class<?> preferredParentClass = cls;
              String parentApiPath;
              String methodPathAsString = methodPath.value();

              // API is a nested API of multiple top level APIs
              if (nestedAPIs.containsKey(returnType)) {
                SwaggerPreferredParent preferredParentAnnotation = AnnotationUtils.findAnnotation(returnType,
                        SwaggerPreferredParent.class);
                if (null != preferredParentAnnotation) {
                  preferredParentClass = preferredParentAnnotation.preferredParent();
                  if (nestedAPIs.get(returnType).parentApi.getName().equals(preferredParentClass.getName())) {
                    skipAdd = true;
                  } else {
                    logger.info("Setting top level API of {} to {} based on @SwaggerPreferredParent " +
                            "annotation", returnType, preferredParentClass.getSimpleName());
                    try {
                      method = preferredParentClass.getMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException exc) {
                      skipAdd = true;
                      logger.error("{} class defined as parent API is invalid due to method mismatch! Ignoring " +
                              "API {}", preferredParentClass, returnType);
                    }
                  }
                } else {
                  logger.warn("{} is a nested API of multiple top level API's. Ignoring top level API {}",
                          returnType, cls);
                  skipAdd = true;
                }
              }

              if (skipAdd) {
                continue;
              } else {
                nestedAPIs.remove(returnType);
              }

              // API parent customization by @SwaggerOverwriteNestedAPI
              SwaggerOverwriteNestedAPI swaggerOverwriteNestedAPI = AnnotationUtils.findAnnotation(returnType,
                      SwaggerOverwriteNestedAPI.class);
              if (null != swaggerOverwriteNestedAPI) {
                preferredParentClass = swaggerOverwriteNestedAPI.parentApi();
                parentApiPath = swaggerOverwriteNestedAPI.parentApiPath();
                methodPathAsString = swaggerOverwriteNestedAPI.parentMethodPath();
              } else {
                parentApiPath = validateParentApiPath(preferredParentClass);
              }

              logger.info("Registering nested API: {}", returnType);
              NestedApiRecord nar = new NestedApiRecord(returnType, preferredParentClass, parentApiPath, method,
                methodPathAsString);
              nestedAPIs.put(returnType, nar);
            }
          }
        }
      }
    }
    logger.info("Found {} nested API's", nestedAPIs.size());
    // With all information gathered, call superclass implementation
    return super.read(classes);
  }

  private String validateParentApiPath(Class<?> cls) {
    Path apiPath = AnnotationUtils.findAnnotation(cls, Path.class);
    if (null == apiPath) {
      logger.warn("Parent api {} also seems to be a nested API. The current version does not support " +
              "multi-level nesting.", cls.getSimpleName());
      return "";
    }
    else {
      return apiPath.value();
    }
  }

  /**
   * Original method is overwritten to handle nested api's properly
   */
  @Override
  protected Swagger read(Class<?> cls, String parentPath,
                         String parentMethod,
                         boolean readHidden,
                         String[] parentConsumes,
                         String[] parentProduces,
                         Map<String, Tag> parentTags,
                         List<Parameter> parentParameters) {
    NestedApiRecord nestedApiRecord = nestedAPIs.get(cls);
    if (null != nestedApiRecord) {
      logger.info("Processing nested API: {}", nestedApiRecord);
      List<Parameter> pathParameters = new ArrayList<>();
      SwaggerOverwriteNestedAPI swaggerOverwriteNestedAPI = AnnotationUtils.findAnnotation(nestedApiRecord.nestedApi,
        SwaggerOverwriteNestedAPI.class);
      if (null != swaggerOverwriteNestedAPI) {
        logger.info("Will use path params from @SwaggerOverwriteNestedAPI: {}", (Object[]) swaggerOverwriteNestedAPI
          .pathParameters());
        for (String param : swaggerOverwriteNestedAPI.pathParameters()) {
          PathParameter pathParam = new PathParameter();
          pathParam.setName(param);
          pathParam.setType(StringProperty.TYPE);
          pathParameters.add(pathParam);
        }
      } else {
        // Get the path parameters of the parent API method. All methods of the nested API class should include these
        // parameters.
        Operation operation = parseMethod(nestedApiRecord.parentMethod);
        pathParameters = ImmutableList.copyOf(
          Collections2.filter(operation.getParameters(), Predicates.instanceOf(PathParameter.class)));
        logger.info("Will copy path params from parent method: {}",
          Lists.transform(pathParameters, new ParameterToName()));
      }
      return super.read(cls,
          joinPaths(nestedApiRecord.parentApiPath, nestedApiRecord.parentMethodPath, parentPath),
          parentMethod, readHidden,
          parentConsumes, parentProduces, parentTags, pathParameters);
    }
    else {
      logger.info("Processing top level API: {}", cls.getSimpleName());
      return super.read(cls, parentPath, parentMethod, readHidden, parentConsumes, parentProduces, parentTags, parentParameters);
    }
  }

  /**
   * Joins path elements properly with slashes avoiding duplicate slashes.
   *
   * @param firstPath the first path element
   * @param paths optionally other path elements
   * @return the joined path
   */
  static String joinPaths(String firstPath, String... paths) {
    StringBuilder joined = new StringBuilder(firstPath);
    for(String path: paths) {
      if (path.isEmpty()) { /* NOP */ }
      else if (joined.length() == 0) {
        joined.append(path);
      }
      else if (joined.charAt(joined.length() - 1) == '/') {
        if (path.startsWith("/")) {
          joined.append(path.substring(1, path.length()));
        }
        else {
          joined.append(path);
        }
      }
      else {
        if (path.startsWith("/")) {
          joined.append(path);
        }
        else {
          joined.append('/').append(path);
        }

      }
    }
    return joined.toString();
  }
}

class ParameterToName implements Function<Parameter, String> {
  @Override
  public String apply(Parameter input) {
    return input.getName();
  }
}

class NestedApiRecord {
  final Class<?> nestedApi;
  final Class<?> parentApi;
  final String parentApiPath;
  final Method parentMethod;
  final String parentMethodPath;

  public NestedApiRecord(Class<?> nestedApi, Class<?> parentApi, String parentApiPath, Method parentMethod, String parentMethodPath) {
    this.nestedApi = nestedApi;
    this.parentApi = parentApi;
    this.parentApiPath = parentApiPath;
    this.parentMethod = parentMethod;
    this.parentMethodPath = parentMethodPath;
  }

  @Override
  public String toString() {
    return "NestedApiRecord {" +
        "nestedApi=" + nestedApi +
        ", parentApi=" + parentApi +
        ", parentApiPath='" + parentApiPath + '\'' +
        ", parentMethod=" + parentMethod +
        ", parentMethodPath='" + parentMethodPath + '\'' +
        '}';
  }
}