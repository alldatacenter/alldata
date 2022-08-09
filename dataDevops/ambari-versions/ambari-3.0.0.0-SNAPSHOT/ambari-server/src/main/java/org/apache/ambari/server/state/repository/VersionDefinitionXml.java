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
package org.apache.ambari.server.state.repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.AvailableVersion.Component;
import org.apache.ambari.server.state.repository.StackPackage.UpgradeDependencies;
import org.apache.ambari.server.state.repository.StackPackage.UpgradeDependencyDeserializer;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.RepositoryXml.Os;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.stack.StackReleaseInfo;
import org.apache.ambari.spi.stack.StackReleaseVersion;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class that wraps a repository definition file.
 */
@XmlRootElement(name="repository-version")
@XmlAccessorType(XmlAccessType.FIELD)
public class VersionDefinitionXml {

  private static final Logger LOG = LoggerFactory.getLogger(VersionDefinitionXml.class);

  public static final String SCHEMA_LOCATION = "version_definition.xsd";

  /**
   * Release details.
   */
  @XmlElement(name = "release")
  public Release release;

  /**
   * The manifest of ALL services available in this repository.
   */
  @XmlElementWrapper(name="manifest")
  @XmlElement(name="service")
  List<ManifestService> manifestServices = new ArrayList<>();

  /**
   * For PATCH and SERVICE repositories, this dictates what is available for upgrade
   * from the manifest.
   */
  @XmlElementWrapper(name="available-services")
  @XmlElement(name="service")
  List<AvailableServiceReference> availableServices = new ArrayList<>();

  /**
   * Represents the repository details.  This is reused from stack repo info.
   */
  @XmlElement(name="repository-info")
  public RepositoryXml repositoryInfo;

  /**
   * The xsd location.  Never {@code null}.
   */
  @XmlTransient
  public String xsdLocation;

  @XmlTransient
  private Map<String, AvailableService> m_availableMap;

  @XmlTransient
  private List<ManifestServiceInfo> m_manifest = null;

  @XmlTransient
  private boolean m_stackDefault = false;

  @XmlTransient
  private Map<String, String> m_packageVersions = null;



  /**
   * @param stack the stack info needed to lookup service and component display names
   * @return a collection of AvailableServices used for web service consumption.  This
   * collection is either the subset of the manifest, or the manifest itself if no services
   * are specified as "available".
   */
  public synchronized Collection<AvailableService> getAvailableServices(StackInfo stack) {
    if (null == m_availableMap) {
      Map<String, ManifestService> manifests = buildManifest();
      m_availableMap = new HashMap<>();

      if (availableServices.isEmpty()) {
        // !!! populate available services from the manifest
        for (ManifestService ms : manifests.values()) {
          addToAvailable(ms, stack, Collections.emptySet());
        }
      } else {
        for (AvailableServiceReference ref : availableServices) {
          ManifestService ms = manifests.get(ref.serviceIdReference);

          addToAvailable(ms, stack, ref.components);
        }
      }
    }

    return m_availableMap.values();
  }

  /**
   * Gets the set of services that are included in this XML
   * @return an empty set for STANDARD repositories, or a non-empty set for PATCH type.
   */
  private Set<String> getAvailableServiceNames() {
    if (availableServices.isEmpty()) {
      return Collections.emptySet();
    } else {
      Set<String> serviceNames = new HashSet<>();

      Map<String, ManifestService> manifest = buildManifest();

      for (AvailableServiceReference ref : availableServices) {
        ManifestService ms = manifest.get(ref.serviceIdReference);
        serviceNames.add(ms.serviceName);
      }

      return serviceNames;
    }
  }

  /**
   * Sets if the version definition is a stack default.  This can only be true
   * when parsing "latest-vdf" for a stack.
   */
  public void setStackDefault(boolean stackDefault) {
    m_stackDefault = stackDefault;
  }

  /**
   * Gets if the version definition was built as the default for a stack
   * @return {@code true} if default for a stack
   */
  public boolean isStackDefault() {
    return m_stackDefault;
  }

  /**
   * Gets the list of stack services, applying information from the version
   * definition. This will include both services which advertise a version and
   * those which do not.
   * 
   * @param stack
   *          the stack for which to get the information
   * @return the list of {@code ManifestServiceInfo} instances for each service
   *         in the stack
   */
  public synchronized List<ManifestServiceInfo> getStackServices(StackInfo stack) {

    if (null != m_manifest) {
      return m_manifest;
    }

    Map<String, Set<String>> manifestVersions = new HashMap<>();

    for (ManifestService manifest : manifestServices) {
      String name = manifest.serviceName;

      if (!manifestVersions.containsKey(name)) {
        manifestVersions.put(manifest.serviceName, new TreeSet<>());
      }

      manifestVersions.get(manifest.serviceName).add(manifest.version);
    }

    m_manifest = new ArrayList<>();

    for (ServiceInfo si : stack.getServices()) {
      Set<String> versions = manifestVersions.containsKey(si.getName()) ?
          manifestVersions.get(si.getName()) : Collections.singleton(
              null == si.getVersion() ? "" : si.getVersion());

      m_manifest.add(new ManifestServiceInfo(si.getName(), si.getDisplayName(),
          si.getComment(), versions));
    }

    return m_manifest;
  }

  /**
   * Gets the package version for an OS family
   * @param osFamily  the os family
   * @return the package version, or {@code null} if not found
   */
  public String getPackageVersion(String osFamily) {
    if (null == m_packageVersions) {
      m_packageVersions = new HashMap<>();

      for (Os os : repositoryInfo.getOses()) {
        m_packageVersions.put(os.getFamily(), os.getPackageVersion());
      }
    }

    return m_packageVersions.get(osFamily);
  }

  /**
   * Returns the XML representation of this instance.
   */
  public String toXml() throws Exception {

    JAXBContext ctx = JAXBContext.newInstance(VersionDefinitionXml.class);
    Marshaller marshaller = ctx.createMarshaller();
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    InputStream xsdStream = VersionDefinitionXml.class.getClassLoader().getResourceAsStream(xsdLocation);

    if (null == xsdStream) {
      throw new Exception(String.format("Could not load XSD identified by '%s'", xsdLocation));
    }

    try {
      Schema schema = factory.newSchema(new StreamSource(xsdStream));
      marshaller.setSchema(schema);
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.setProperty("jaxb.noNamespaceSchemaLocation", xsdLocation);

      StringWriter w = new StringWriter();
      marshaller.marshal(this, w);

      return w.toString();
    } finally {
      IOUtils.closeQuietly(xsdStream);
    }
  }

  /**
   * Gets a summary for cluster given the version information in this version.
   * @param cluster the cluster by which to iterate services
   * @return a summary instance
   * @throws AmbariException
   */
  public ClusterVersionSummary getClusterSummary(Cluster cluster, AmbariMetaInfo metaInfo)
      throws AmbariException {

    Map<String, ManifestService> manifests = buildManifestByService();
    Set<String> available = getAvailableServiceNames();

    available = available.isEmpty() ? manifests.keySet() : available;

    Map<String, ServiceVersionSummary> summaries = new HashMap<>();
    for (String serviceName : available) {
      Service service = cluster.getServices().get(serviceName);
      if (null == service) {
        // !!! service is not installed
        continue;
      }

      ServiceVersionSummary summary = new ServiceVersionSummary();
      summaries.put(service.getName(), summary);

      StackId stackId = service.getDesiredRepositoryVersion().getStackId();
      StackInfo stack = metaInfo.getStack(stackId);
      StackReleaseVersion stackReleaseVersion = stack.getReleaseVersion();

      StackReleaseInfo serviceVersion = stackReleaseVersion.parse(
          service.getDesiredRepositoryVersion().getVersion());

      // !!! currently only one version is supported (unique service names)
      ManifestService manifest = manifests.get(serviceName);

      final StackReleaseInfo versionToCompare;
      final String summaryReleaseVersion;

      if (StringUtils.isEmpty(manifest.releaseVersion)) {
        versionToCompare = release.getReleaseInfo();
        summaryReleaseVersion = release.version;
      } else {
        versionToCompare = stackReleaseVersion.parse(manifest.releaseVersion);
        summaryReleaseVersion = manifest.releaseVersion;
      }

      summary.setVersions(manifest.version, summaryReleaseVersion);

      if (RepositoryType.STANDARD == release.repositoryType) {
        summary.setUpgrade(true);
      } else {
        // !!! installed service already meets the release version, then nothing to upgrade
        // !!! TODO should this be using the release compatible-with field?
        Comparator<StackReleaseInfo> comparator = stackReleaseVersion.getComparator();
        if (comparator.compare(versionToCompare, serviceVersion) > 0) {
          summary.setUpgrade(true);
        }
      }
    }

    return new ClusterVersionSummary(summaries);
  }

  /**
   * Gets information about services which cannot be upgraded because the
   * repository does not contain required dependencies. This method will look at
   * the {@code cluster-env/stack_packages} structure to see if there are any
   * dependencies listed in the {@code upgrade-dependencies} key.
   *
   * @param cluster
   *          the cluster (not {@code null}).
   * @param metaInfo
   *          the metainfo instance
   * @return a mapping of service name to its missing service dependencies, or
   *         an empty map if there are none (never {@code null}).
   * @throws AmbariException
   */
  public Set<String> getMissingDependencies(Cluster cluster, AmbariMetaInfo metaInfo)
      throws AmbariException {
    Set<String> missingDependencies = Sets.newTreeSet();

    String stackPackagesJson = cluster.getClusterProperty(
        ConfigHelper.CLUSTER_ENV_STACK_PACKAGES_PROPERTY, null);

    // quick exit
    if (StringUtils.isEmpty(stackPackagesJson)) {
      return missingDependencies;
    }

    // create a GSON builder which can deserialize the stack_packages.json
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(UpgradeDependencies.class, new UpgradeDependencyDeserializer());
    Gson gson = gsonBuilder.create();
    Type type = new TypeToken<Map<String, StackPackage>>(){}.getType();

    // a mapping of stack name to stack-select/conf-select/upgade-deps
    final Map<String, StackPackage> stackPackages;

    try {
      stackPackages = gson.fromJson(stackPackagesJson, type);
    } catch( Exception exception ) {
      LOG.warn("Unable to deserialize the stack packages JSON, assuming no service dependencies",
          exception);

      return missingDependencies;
    }

    StackId stackId = new StackId(release.stackId);
    StackPackage stackPackage = stackPackages.get(stackId.getStackName());
    if (null == stackPackage || null == stackPackage.upgradeDependencies) {
      return missingDependencies;
    }

    Map<String, List<String>> dependencies = stackPackage.upgradeDependencies.dependencies;

    if (null == dependencies || dependencies.isEmpty()) {
      return missingDependencies;
    }

    // the installed services in the cluster
    Map<String,Service> installedServices = cluster.getServices();

    ClusterVersionSummary clusterVersionSummary = getClusterSummary(cluster, metaInfo);
    Set<String> servicesInUpgrade = clusterVersionSummary.getAvailableServiceNames();
    Set<String> servicesInRepository = getAvailableServiceNames();

    for (String serviceInUpgrade : servicesInUpgrade) {
      List<String> servicesRequired = dependencies.get(serviceInUpgrade);
      if (null == servicesRequired) {
        continue;
      }

      for (String serviceRequired : servicesRequired) {
        if (!servicesInRepository.contains(serviceRequired) && installedServices.containsKey(serviceRequired)) {
          missingDependencies.add(serviceRequired);
        }
      }
    }

    // now that we have built the list of missing dependencies based solely on
    // the services participating in the upgrade, recursively see if any of
    // those services have dependencies as well
    missingDependencies = getRecursiveDependencies(missingDependencies, dependencies,
        servicesInUpgrade, installedServices.keySet());

    return missingDependencies;
  }

  /**
   * Gets all dependencies required to perform an upgrade, considering that the
   * original set's depenencies may have dependencies of their own.
   *
   * @param missingDependencies
   *          the set of missing dependencies so far.
   * @param dependencies
   *          the master list of dependency associations
   * @param servicesInUpgrade
   *          the services which the VDF indicates are going to be in the
   *          upgrade *
   * @param installedServices
   *          the services installed in the cluster
   * @return a new set including any dependencies of those which were already
   *         found
   */
  Set<String> getRecursiveDependencies(Set<String> missingDependencies,
      Map<String, List<String>> dependencies, Set<String> servicesInUpgrade,
      Set<String> installedServices) {
    Set<String> results = Sets.newHashSet();
    results.addAll(missingDependencies);

    for (String missingDependency : missingDependencies) {
      if (dependencies.containsKey(missingDependency)) {
        List<String> subDependencies = dependencies.get(missingDependency);
        for (String subDependency : subDependencies) {
          if (!missingDependencies.contains(subDependency)
              && installedServices.contains(subDependency)
              && !servicesInUpgrade.contains(subDependency)) {
            results.add(subDependency);
            results.addAll(getRecursiveDependencies(results, dependencies, servicesInUpgrade,
                installedServices));
          }
        }
      }
    }

    return results;
  }

  /**
   * Structures the manifest by service name.
   * <p/>
   * !!! WARNING. This is currently based on the assumption that there is one and only
   * one version for a service in the VDF.  This may have to change in the future.
   * </p>
   * @return
   */
  private Map<String, ManifestService> buildManifestByService() {
    Map<String, ManifestService> manifests = new HashMap<>();

    for (ManifestService manifest : manifestServices) {
      if (!manifests.containsKey(manifest.serviceName)) {
        manifests.put(manifest.serviceName, manifest);
      }
    }

    return manifests;
  }

  /**
   * Helper method to use a {@link ManifestService} to generate the available services structure
   * @param manifestService          the ManifestService instance
   * @param stack       the stack object
   * @param components  the set of components for the service
   */
  private void addToAvailable(ManifestService manifestService, StackInfo stack, Set<String> components) {
    ServiceInfo service = stack.getService(manifestService.serviceName);

    if (!m_availableMap.containsKey(manifestService.serviceName)) {
      String display = (null == service) ? manifestService.serviceName: service.getDisplayName();

      AvailableService available = new AvailableService(manifestService.serviceName, display);
      m_availableMap.put(manifestService.serviceName, available);
    }

    AvailableService as = m_availableMap.get(manifestService.serviceName);
    as.getVersions().add(new AvailableVersion(manifestService.version,
        manifestService.versionId,
        manifestService.releaseVersion,
        buildComponents(service, components)));
  }


  /**
   * @return the list of manifest services to a map for easier access.
   */
  private Map<String, ManifestService> buildManifest() {
    Map<String, ManifestService> normalized = new HashMap<>();

    for (ManifestService ms : manifestServices) {
      normalized.put(ms.serviceId, ms);
    }
    return normalized;
  }

  /**
   * @param service the service containing components
   * @param components the set of components in the service
   * @return the set of components name/display name pairs
   */
  private Set<Component> buildComponents(ServiceInfo service, Set<String> components) {
    Set<Component> set = new HashSet<>();

    for (String component : components) {
      ComponentInfo ci = service.getComponentByName(component);
      String display = (null == ci) ? component : ci.getDisplayName();
      set.add(new Component(component, display));
    }

    return set;
  }

  /**
   * Parses a URL for a definition XML file into the object graph.
   * @param   url the URL to load.  Can be a file URL reference also.
   * @return  the definition
   */
  public static VersionDefinitionXml load(URL url) throws Exception {

    InputStream stream = null;
    try {
      stream = url.openStream();
      return load(stream);
    } finally {
      IOUtils.closeQuietly(stream);
    }
  }

  /**
   * Parses an xml string.  Used when the xml is in the database.
   * @param   xml the xml string
   * @return  the definition
   */
  public static VersionDefinitionXml load(String xml) throws Exception {
    return load(new ByteArrayInputStream(xml.getBytes("UTF-8")));
  }

  /**
   * Parses a stream into an object graph
   * @param stream  the stream
   * @return the definition
   * @throws Exception
   */
  private static VersionDefinitionXml load(InputStream stream) throws Exception {

    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    XMLStreamReader xmlReader = xmlFactory.createXMLStreamReader(stream);

    xmlReader.nextTag();
    String xsdName = xmlReader.getAttributeValue(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "noNamespaceSchemaLocation");

    if (null == xsdName) {
      throw new IllegalArgumentException("Provided XML does not have a Schema defined using 'noNamespaceSchemaLocation'");
    }

    InputStream xsdStream = VersionDefinitionXml.class.getClassLoader().getResourceAsStream(xsdName);

    if (null == xsdStream) {
      throw new Exception(String.format("Could not load XSD identified by '%s'", xsdName));
    }

    JAXBContext ctx = JAXBContext.newInstance(VersionDefinitionXml.class);
    Unmarshaller unmarshaller = ctx.createUnmarshaller();

    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = factory.newSchema(new StreamSource(xsdStream));
    unmarshaller.setSchema(schema);

    try {
      VersionDefinitionXml xml = (VersionDefinitionXml) unmarshaller.unmarshal(xmlReader);
      xml.xsdLocation = xsdName;
      return xml;
    } finally {
      IOUtils.closeQuietly(xsdStream);
    }
  }

  /**
   * Builds a Version Definition that is the default for the stack.
   * <p>
   * This will use all of the services defined on the stack, excluding those
   * which do not advertise a version. If a service doesn't advertise a version,
   * we cannot include it in a generated VDF.
   *
   * @return the version definition
   */
  public static VersionDefinitionXml build(StackInfo stackInfo) {

    VersionDefinitionXml xml = new VersionDefinitionXml();
    xml.m_stackDefault = true;
    xml.release = new Release();
    xml.repositoryInfo = new RepositoryXml();
    xml.xsdLocation = SCHEMA_LOCATION;

    StackId stackId = new StackId(stackInfo.getName(), stackInfo.getVersion());

    xml.release.repositoryType = RepositoryType.STANDARD;
    xml.release.stackId = stackId.toString();
    xml.release.version = stackInfo.getVersion();
    xml.release.releaseNotes = "NONE";
    xml.release.display = stackId.toString();

    for (ServiceInfo si : stackInfo.getServices()) {
      // do NOT build a manifest entry for services on the stack which cannot be
      // a part of an upgrade - this is to prevent services like KERBEROS from
      // preventing an upgrade if it's in Maintenance Mode
      if (!si.isVersionAdvertised()) {
        continue;
      }

      ManifestService ms = new ManifestService();
      ms.serviceName = si.getName();
      ms.version = StringUtils.trimToEmpty(si.getVersion());
      ms.serviceId = ms.serviceName + "-" + ms.version.replace(".", "");
      xml.manifestServices.add(ms);
    }

    if (null != stackInfo.getRepositoryXml()) {
      xml.repositoryInfo.getOses().addAll(stackInfo.getRepositoryXml().getOses());
    }

    try {
      xml.toXml();
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }

    return xml;
  }

  /**
   * Used to facilitate merging when multiple version definitions are provided.  Ambari
   * represents them as a unified entity.  Since there is no knowledge of which one is
   * "correct" - the first one is used for the release meta-info.
   */
  public static class Merger {
    private VersionDefinitionXml m_xml = new VersionDefinitionXml();
    private boolean m_seeded = false;

    public Merger() {
      m_xml.release = new Release();
      m_xml.repositoryInfo = new RepositoryXml();
    }

    /**
     * Adds definition to this one.
     * @param version the version the definition represents
     * @param xml the definition object
     */
    public void add(String version, VersionDefinitionXml xml) {
      if (!m_seeded) {
        m_xml.xsdLocation = xml.xsdLocation;

        StackId stackId = new StackId(xml.release.stackId);

        m_xml.release.build = null; // could be combining builds, so this is invalid
        m_xml.release.compatibleWith = xml.release.compatibleWith;
        m_xml.release.display = stackId.getStackName() + "-" + xml.release.version;
        m_xml.release.repositoryType = RepositoryType.STANDARD; // assumption since merging only done for new installs
        m_xml.release.releaseNotes = xml.release.releaseNotes;
        m_xml.release.stackId = xml.release.stackId;
        m_xml.release.version = version;
        m_xml.manifestServices.addAll(xml.manifestServices);

        m_seeded = true;
      }

      m_xml.repositoryInfo.getOses().addAll(xml.repositoryInfo.getOses());
    }

    /**
     * @return the merged definition file
     */
    public VersionDefinitionXml merge() {
      return m_seeded ? m_xml : null;
    }
  }

}
