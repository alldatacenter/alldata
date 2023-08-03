/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 package org.apache.ranger.service.filter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.apache.ranger.common.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import com.sun.jersey.api.container.filter.LoggingFilter;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

public class RangerRESTAPIFilter extends LoggingFilter {
	Logger logger = LoggerFactory.getLogger(RangerRESTAPIFilter.class);
	static volatile boolean initDone = false;

	boolean logStdOut = true;
	HashMap<String, String> regexPathMap = new HashMap<String, String>();
	HashMap<String, Pattern> regexPatternMap = new HashMap<String, Pattern>();
	List<String> regexList = new ArrayList<String>();
	List<String> loggedRestPathErrors = new ArrayList<String>();

	void init() {
		if (initDone) {
			return;
		}
		synchronized (RangerRESTAPIFilter.class) {
			if (initDone) {
				return;
			}

			logStdOut = PropertiesUtil.getBooleanProperty(
					"xa.restapi.log.enabled", false);

			// Build hash map
			try {
				loadPathPatterns();
			} catch (Throwable t) {
				logger.error(
						"Error parsing REST classes for PATH patterns. Error ignored, but should be fixed immediately",
						t);
			}
			initDone = true;
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.sun.jersey.spi.container.ContainerRequestFilter#filter(com.sun.jersey
	 * .spi.container.ContainerRequest)
	 */
	@Override
	public ContainerRequest filter(ContainerRequest request) {
		if (!initDone) {
			init();
		}
		if (logStdOut) {
			String path = request.getRequestUri().getPath();

			if ((request.getMediaType() == null || !"multipart".equals(request.getMediaType()
					.getType()))
					&& !path.endsWith("/service/general/logs")) {
				try {
					request = super.filter(request);
				} catch (Throwable t) {
					logger.error("Error FILTER logging. path=" + path, t);
				}
			}
		}

		return request;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.sun.jersey.spi.container.ContainerResponseFilter#filter(com.sun.jersey
	 * .spi.container.ContainerRequest,
	 * com.sun.jersey.spi.container.ContainerResponse)
	 */
	@Override
	public ContainerResponse filter(ContainerRequest request,
			ContainerResponse response) {
		if (logStdOut) {
			// If it is image, then don't call super
			if (response.getMediaType() == null) {
				logger.info("DELETE ME: Response= mediaType is null");
			}
			if (response.getMediaType() == null
					|| !"image".equals(response.getMediaType().getType())) {

				response = super.filter(request, response);
			}
		}

		return response;
	}

	private void loadPathPatterns() throws ClassNotFoundException {
		String pkg = "org.apache.ranger.service";
		// List<Class> cList = findClasses(new File(dir), pkg);
		@SuppressWarnings("rawtypes")
		List<Class> cList = findClasses(pkg);
		for (@SuppressWarnings("rawtypes")
		Class klass : cList) {
			Annotation[] annotations = klass.getAnnotations();
			for (Annotation annotation : annotations) {
				if (!(annotation instanceof Path)) {
					continue;
				}
				Path path = (Path) annotation;
				if (path.value().startsWith("crud")) {
					continue;
				}
				// logger.info("path=" + path.value());
				// Loop over the class methods
				for (Method m : klass.getMethods()) {
					Annotation[] methodAnnotations = m.getAnnotations();
					String httpMethod = null;
					String servicePath = null;
					for (Annotation methodAnnotation : methodAnnotations) {
						if (methodAnnotation instanceof GET) {
							httpMethod = "GET";
						} else if (methodAnnotation instanceof PUT) {
							httpMethod = "PUT";
						} else if (methodAnnotation instanceof POST) {
							httpMethod = "POST";
						} else if (methodAnnotation instanceof DELETE) {
							httpMethod = "DELETE";
						} else if (methodAnnotation instanceof Path) {
							servicePath = ((Path) methodAnnotation)
									.value();
						}
					}

					if (httpMethod == null) {
						continue;
					}

					String fullPath = path.value();
					String regEx = httpMethod + ":" + path.value();
					if (servicePath != null) {
						if (!servicePath.startsWith("/")) {
							servicePath = "/" + servicePath;
						}
						UriTemplate ut = new UriTemplate(servicePath);
						regEx = httpMethod + ":" + path.value()
								+ ut.getPattern().getRegex();
						fullPath += servicePath;
					}
					Pattern regexPattern = Pattern.compile(regEx);

					if (regexPatternMap.containsKey(regEx)) {
						logger.warn("Duplicate regex=" + regEx + ", fullPath="
								+ fullPath);
					}
					regexList.add(regEx);
					regexPathMap.put(regEx, fullPath);
					regexPatternMap.put(regEx, regexPattern);

					logger.info("path=" + path.value() + ", servicePath="
							+ servicePath + ", fullPath=" + fullPath
							+ ", regEx=" + regEx);
				}
			}
		}
		// ReOrder list
		int i = 0;
		for (i = 0; i < 10; i++) {
			boolean foundMatches = false;
			List<String> tmpList = new ArrayList<String>();
			for (int x = 0; x < regexList.size(); x++) {
				boolean foundMatch = false;
				String rX = regexList.get(x);
				for (int y = 0; y < x; y++) {
					String rY = regexList.get(y);
					Matcher matcher = regexPatternMap.get(rY).matcher(rX);
					if (matcher.matches()) {
						foundMatch = true;
						foundMatches = true;
						// logger.info("rX " + rX + " matched with rY=" + rY
						// + ". Moving rX to the top. Loop count=" + i);
						break;
					}
				}
				if (foundMatch) {
					tmpList.add(0, rX);
				} else {
					tmpList.add(rX);
				}
			}
			regexList = tmpList;
			if (!foundMatches) {
				logger.info("Done rearranging. loopCount=" + i);
				break;
			}
		}
		if (i == 10) {
			logger.warn("Couldn't rearrange even after " + i + " loops");
		}

		logger.info("Loaded " + regexList.size() + " API methods.");
		// for (String regEx : regexList) {
		// logger.info("regEx=" + regEx);
		// }
	}

	@SuppressWarnings("rawtypes")
	private List<Class> findClasses(String packageName)
			throws ClassNotFoundException {
		List<Class> classes = new ArrayList<Class>();

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				true);

		// scanner.addIncludeFilter(new
		// AnnotationTypeFilter(<TYPE_YOUR_ANNOTATION_HERE>.class));

		for (BeanDefinition bd : scanner.findCandidateComponents(packageName)) {
			classes.add(Class.forName(bd.getBeanClassName()));
		}

		return classes;
	}

}
