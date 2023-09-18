/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.extension.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.*;
import com.qlangtech.tis.extension.impl.MissingDependencyException;
import com.qlangtech.tis.extension.impl.XmlFile;
import com.qlangtech.tis.extension.util.VersionNumber;
import com.qlangtech.tis.install.InstallUtil;
import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import com.qlangtech.tis.plugin.PluginAndCfgsSnapshot;
import com.qlangtech.tis.util.PersistedList;
import com.qlangtech.tis.util.Util;
import com.qlangtech.tis.util.exec.AtmostOneThreadExecutor;
import com.qlangtech.tis.util.exec.DaemonThreadFactory;
import com.qlangtech.tis.util.exec.NamingThreadFactory;
import com.qlangtech.tis.utils.TisMetaProps;
//import edu.umd.cs.findbugs.annotations.CheckForNull;
//import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 17:44
 **/
public class UpdateCenter implements Saveable {
    public static final String PREDEFINED_UPDATE_SITE_ID = "default";
    public static final String KEY_UPDATE_SITE = "/update-site";
    public static final String KEY_DEFAULT_JSON = "default.json";
    private static final String UPDATE_CENTER_URL
            = "http://mirror.qlangtech.com/" + TisMetaProps.getInstance().getVersion() + KEY_UPDATE_SITE + "/";
    public static final String ID_UPLOAD = "_upload";
    /**
     * An {@link ExecutorService} for updating UpdateSites.
     */
    protected static final ExecutorService updateService = Executors.newCachedThreadPool(
            new NamingThreadFactory(new DaemonThreadFactory(), "Update site data downloader"));

    public static void main(String[] args) {
        long total = 688877996;
        long count = 46847443;
        //  (int) (((double) cin.getByteCount() / total) * 100);
        int percentage = (int) (((double) count / total) * 100);
        System.out.println(percentage);


        System.out.println(percentage);
    }

    /**
     * Read timeout when downloading plugins, defaults to 1 minute
     */
    private static final int PLUGIN_DOWNLOAD_READ_TIMEOUT
            = (int) TimeUnit.SECONDS.toMillis(Integer.parseInt(System.getProperty(UpdateCenter.class.getName() + ".pluginDownloadReadTimeoutSeconds", "60")));

    /**
     * {@linkplain 'UpdateSite#getId()' ID} of the default update site.
     *
     * @since 1.483; configurable via system property since 2.4
     */
    public static final String ID_DEFAULT = System.getProperty(UpdateCenter.class.getName() + ".defaultUpdateSiteId", PREDEFINED_UPDATE_SITE_ID);
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCenter.class);
    /**
     * {@link UpdateSite}s from which we've already installed a plugin at least once.
     * This is used to skip network tests.
     */
    private final Set<UpdateSite> sourcesUsed = new HashSet<>();
    /**
     * List of {@link UpdateSite}s to be used.
     */
    private final PersistedList<UpdateSite> sites = new PersistedList<>(this);
    /**
     * @see '#isSiteDataReady'
     */
    private transient volatile boolean siteDataLoading;
    /**
     * Update center configuration data
     */
    private UpdateCenterConfiguration config;

    /**
     * List of created {@link UpdateCenterJob}s. Access needs to be synchronized.
     */
    private final Vector<UpdateCenterJob> jobs = new Vector<>();

    /**
     * {@link ExecutorService} that performs installation.
     *
     * @since 1.501
     */
    private final ExecutorService installerService = new AtmostOneThreadExecutor(
            new NamingThreadFactory(new DaemonThreadFactory(), "Update center installer thread"));
    private boolean requiresRestart;

    static {

//        String ucOverride = System.getProperty(UpdateCenter.class.getName() + ".updateCenterUrl");
//        if (ucOverride != null) {
//            LOGGER.info("Using a custom update center defined by the system property: {}", ucOverride);
//            UPDATE_CENTER_URL = ucOverride;
//        } else {
        // UPDATE_CENTER_URL = ;
        //}
    }


    public UpdateCenter() {
        this.configure(new UpdateCenterConfiguration());
    }

    public PersistedList<UpdateSite> getSites() {
        return this.sites;
    }

    /**
     * Creates an update center.
     *
     * @param config Requested configuration. May be {@code null} if defaults should be used
     * @return Created Update center. {@link UpdateCenter} by default, but may be overridden
     * @since 2.4
     */
    public static UpdateCenter createUpdateCenter( UpdateCenterConfiguration config) {

        return new UpdateCenter();
    }

    public List<UpdateCenterJob> getJobs() {
        synchronized (jobs) {
            return new ArrayList<>(jobs);
        }
    }

    /**
     * Configures update center to get plugins/updates from alternate servers,
     * and optionally using alternate strategies for downloading, installing
     * and upgrading.
     *
     * @param config Configuration data
     * @see 'UpdateCenterConfiguration'
     */
    public void configure(UpdateCenterConfiguration config) {
        if (config != null) {
            this.config = config;
        }
    }

    public List<UpdateSite> getSiteList() {
        return this.sites.toList();
    }


    public synchronized Future<UpdateCenterJob> addJob(UpdateCenterJob job) {
        if (job.site != null) {
            addConnectionCheckJob(job.site);
        }
        return job.submit();
    }

    private ConnectionCheckJob getConnectionCheckJob(UpdateSite site) {
        synchronized (jobs) {
            for (UpdateCenterJob job : jobs) {
                if (job instanceof ConnectionCheckJob && job.site != null && job.site.getId().equals(site.getId())) {
                    return (ConnectionCheckJob) job;
                }
            }
        }
        return null;
    }


    /**
     * Called to persist the currently installing plugin states. This allows
     * us to support install resume if Jenkins is restarted while plugins are
     * being installed.
     */
    public synchronized void persistInstallStatus() {
        List<UpdateCenterJob> jobs = getJobs();

        boolean activeInstalls = false;
        for (UpdateCenterJob job : jobs) {
            if (job instanceof InstallationJob) {
                InstallationJob installationJob = (InstallationJob) job;
                if (!installationJob.status.isSuccess()) {
                    activeInstalls = true;
                }
            }
        }

        if (activeInstalls) {
            InstallUtil.persistInstallStatus(jobs); // save this info
        } else {
            InstallUtil.clearInstallStatus(); // clear this info
        }
    }


    private ConnectionCheckJob addConnectionCheckJob(UpdateSite site) {
        // Create a connection check job if the site was not already in the sourcesUsed set i.e. the first
        // job (in the jobs list) relating to a site must be the connection check job.
        if (sourcesUsed.add(site)) {
            ConnectionCheckJob connectionCheckJob = newConnectionCheckJob(site);
            connectionCheckJob.submit();
            return connectionCheckJob;
        } else {
            // Find the existing connection check job for that site and return it.
            ConnectionCheckJob connectionCheckJob = getConnectionCheckJob(site);
            if (connectionCheckJob != null) {
                return connectionCheckJob;
            } else {
                throw new IllegalStateException("Illegal addition of an UpdateCenter job without calling UpdateCenter.addJob. " +
                        "No ConnectionCheckJob found for the site.");
            }
        }
    }

    ConnectionCheckJob newConnectionCheckJob(UpdateSite site) {
        return new ConnectionCheckJob(site);
    }

    public List<UpdateSite.Plugin> getAvailables() {
        Map<String, UpdateSite.Plugin> pluginMap = new LinkedHashMap<>();
        for (UpdateSite site : sites) {
            for (UpdateSite.Plugin plugin : site.getAvailables()) {
                final UpdateSite.Plugin existing = pluginMap.get(plugin.name);
                if (existing == null) {
                    pluginMap.put(plugin.name, plugin);
                } else if (!existing.version.equals(plugin.version)) {
                    // allow secondary update centers to publish different versions
                    // TODO refactor to consolidate multiple versions of the same plugin within the one row
                    final String altKey = plugin.name + ":" + plugin.version;
                    if (!pluginMap.containsKey(altKey)) {
                        pluginMap.put(altKey, plugin);
                    }
                }
            }
        }

        return new ArrayList<>(pluginMap.values());
    }

    /**
     * Returns latest install/upgrade job for the given plugin.
     *
     * @return InstallationJob or null if not found
     */
    public InstallationJob getJob(UpdateSite.Plugin plugin) {
        List<UpdateCenterJob> jobList = getJobs();
        Collections.reverse(jobList);
        for (UpdateCenterJob job : jobList)
            if (job instanceof InstallationJob) {
                InstallationJob ij = (InstallationJob) job;
                if (ij.plugin.name.equals(plugin.name) && ij.plugin.sourceId.equals(plugin.sourceId))
                    return ij;
            }
        return null;
    }

    /**
     * Loads the data from the disk into this object.
     */
    public synchronized void load() throws IOException {
        XmlFile file = getConfigFile();
        if (file.exists()) {
            try {
                sites.replaceBy(((PersistedList) file.unmarshal(sites)).toList());
            } catch (IOException e) {
                LOGGER.warn("Failed to load " + file, e);
            }
            boolean defaultSiteExists = false;
            for (UpdateSite site : sites) {
                // replace the legacy site with the new site
                if (site.isLegacyDefault()) {
                    sites.remove(site);
                } else if (ID_DEFAULT.equals(site.getId())) {
                    defaultSiteExists = true;
                }
            }
            if (!defaultSiteExists) {
                sites.add(createDefaultUpdateSite());
            }
        } else {
            if (sites.isEmpty()) {
                // If there aren't already any UpdateSources, add the default one.
                // to maintain compatibility with existing UpdateCenterConfiguration, create the default one as specified by UpdateCenterConfiguration
                sites.add(createDefaultUpdateSite());
            }
        }
        siteDataLoading = false;
    }

    /**
     * Ensure that all UpdateSites are up to date, without requiring a user to
     * browse to the instance.
     *
     * @return a list of {@link FormValidation} for each updated Update Site
     * @throws ExecutionException
     * @throws InterruptedException
     * @since 1.501
     */
    public List<FormValidation> updateAllSites() throws InterruptedException, ExecutionException {
        List<Future<FormValidation>> futures = new ArrayList<>();
        for (UpdateSite site : getSites()) {
            Future<FormValidation> future = site.updateDirectly();
            if (future != null) {
                futures.add(future);
            }
        }

        List<FormValidation> results = new ArrayList<>();
        for (Future<FormValidation> f : futures) {
            results.add(f.get());
        }
        return results;
    }

    protected UpdateSite createDefaultUpdateSite() {
        UpdateSite dftUpdateSite = new UpdateSite(PREDEFINED_UPDATE_SITE_ID, config.getUpdateCenterUrl() + KEY_DEFAULT_JSON);
//        if (!dftUpdateSite.existLocal()) {
//
//        }
        return dftUpdateSite;
    }

    @Override
    public synchronized void save() {
//        if(BulkChange.contains(this))   return;
//        try {
//            getConfigFile().write(sites);
//            SaveableListener.fireOnChange(this, getConfigFile());
//        } catch (IOException e) {
//            LOGGER.log(Level.WARNING, "Failed to save "+getConfigFile(),e);
//        }
    }


    public UpdateSite.Plugin getPlugin(String artifactId, VersionNumber minVersion) {
        if (minVersion == null) {
            return getPlugin(artifactId);
        }
        for (UpdateSite s : sites) {
            UpdateSite.Plugin p = s.getPlugin(artifactId);
            if (checkMinVersion(p, minVersion)) {
                return p;
            }
        }
        return null;
    }

    private boolean checkMinVersion(UpdateSite.Plugin p, VersionNumber minVersion) {
        return p != null
                && (minVersion == null || !minVersion.isNewerThan(new VersionNumber(p.version)));
    }

    /**
     * Gets the plugin with the given name from the first {@link UpdateSite} to contain it.
     *
     * @return Discovered {@link 'Plugin'}. {@code null} if it cannot be found
     */
    public UpdateSite.Plugin getPlugin(String artifactId) {
        for (UpdateSite s : sites) {
            UpdateSite.Plugin p = s.getPlugin(artifactId);
            if (p != null) return p;
        }
        return null;
    }

    private XmlFile getConfigFile() {

        return Descriptor.getConfigFile(UpdateCenter.class.getName());

//        return new XmlFile(XSTREAM,new File(TIS.get().root,
//                UpdateCenter.class.getName()+".xml"));
    }

    /**
     * Strategy object for controlling the update center's behaviors.
     *
     * <p>
     * Until 1.333, this extension point used to control the configuration of
     * where to get updates (hence the name of this class), but with the introduction
     * of multiple update center sites capability, that functionality is achieved by
     * simply installing another {@link UpdateSite}.
     *
     * <p>
     * See {@link UpdateSite} for how to manipulate them programmatically.
     *
     * @since 1.266
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static class UpdateCenterConfiguration {
        /**
         * Creates default update center configuration - uses settings for global update center.
         */
        public UpdateCenterConfiguration() {
        }

        /**
         * Check network connectivity by trying to establish a connection to
         * the host in connectionCheckUrl.
         *
         * @param job                The connection checker that is invoking this strategy.
         * @param connectionCheckUrl A string containing the URL of a domain
         *                           that is assumed to be always available.
         * @throws IOException if a connection can't be established
         */
        public void checkConnection(ConnectionCheckJob job, String connectionCheckUrl) throws IOException {
            testConnection(new URL(connectionCheckUrl));
        }

        /**
         * Check connection to update center server.
         *
         * @param job             The connection checker that is invoking this strategy.
         * @param updateCenterUrl A sting containing the URL of the update center host.
         * @throws IOException if a connection to the update center server can't be established.
         */
        public void checkUpdateCenter(ConnectionCheckJob job, String updateCenterUrl) throws IOException {
            testConnection(toUpdateCenterCheckUrl(updateCenterUrl));
        }

        /**
         * Converts an update center URL into the URL to use for checking its connectivity.
         *
         * @param updateCenterUrl the URL to convert.
         * @return the converted URL.
         * @throws MalformedURLException if the supplied URL is malformed.
         */
        static URL toUpdateCenterCheckUrl(String updateCenterUrl) throws MalformedURLException {
            URL url;
            if (updateCenterUrl.startsWith("http://") || updateCenterUrl.startsWith("https://")) {
                url = new URL(updateCenterUrl + (updateCenterUrl.indexOf('?') == -1 ? "?uctest" : "&uctest"));
            } else {
                url = new URL(updateCenterUrl);
            }
            return url;
        }

        /**
         * Validate the URL of the resource before downloading it.
         *
         * @param job The download job that is invoking this strategy. This job is
         *            responsible for managing the status of the download and installation.
         * @param src The location of the resource on the network
         * @throws IOException if the validation fails
         */
        public void preValidate(DownloadJob job, URL src) throws IOException {
        }

        /**
         * Validate the resource after it has been downloaded, before it is
         * installed. The default implementation does nothing.
         *
         * @param job The download job that is invoking this strategy. This job is
         *            responsible for managing the status of the download and installation.
         * @param src The location of the downloaded resource.
         * @throws IOException if the validation fails.
         */
        public void postValidate(DownloadJob job, File src) throws IOException {
        }

        private static MessageDigest getDigest(String algorithm) {
            try {
                return MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.info("Failed to instantiate message digest algorithm, may only have weak or no verification of downloaded file", algorithm);
            }
            return null;
        }

        /**
         * Download a plugin or core upgrade in preparation for installing it
         * into its final location. Implementations will normally download the
         * resource into a temporary location and hand off a reference to this
         * location to the install or upgrade strategy to move into the final location.
         *
         * @param job The download job that is invoking this strategy. This job is
         *            responsible for managing the status of the download and installation.
         * @param src The URL to the resource to be downloaded.
         * @return A File object that describes the downloaded resource.
         * @throws IOException if there were problems downloading the resource.
         * @see DownloadJob
         */
        public File download(DownloadJob job, URL src) throws IOException {

            // Java spec says SHA-1 and SHA-256 exist, and SHA-512 might not, so one try/catch block should be fine
            final MessageDigest sha1 = getDigest("SHA-1");
            final MessageDigest sha256 = getDigest("SHA-256");
            final MessageDigest sha512 = getDigest("SHA-512");

            File dst = job.getDestination();
            File tmp = new File(dst.getPath() + ".tmp");
            // URLConnection con = null;
            //  try {
            long total = job.getSize();

            if (total < 1) {
                // don't know exactly how this happens, but report like
                // http://www.ashlux.com/wordpress/2009/08/14/hudson-and-the-sonar-plugin-fail-maveninstallation-nosuchmethoderror/
                // indicates that this kind of inconsistency can happen. So let's be defensive
                throw new IllegalStateException("Inconsistent file length: expected " + total);
            }

            HttpUtils.get(src, new ConfigFileContext.StreamProcess<Void>() {
                @Override
                public Void p(HttpURLConnection con, InputStream stream) throws IOException {

                    // con.getContentLength();
                    byte[] buf = new byte[8192];
                    int len;


                    LOGGER.info("Downloading " + job.getName());
                    try {
                        Thread t = Thread.currentThread();
                        String oldName = t.getName();
                        t.setName(oldName + ": " + src);
                        int percentage;
                        try (OutputStream _out = Files.newOutputStream(tmp.toPath());
                             OutputStream out =
                                     sha1 != null ? new DigestOutputStream(
                                             sha256 != null ? new DigestOutputStream(
                                                     sha512 != null ? new DigestOutputStream(_out, sha512) : _out, sha256) : _out, sha1) : _out;
                             //InputStream in = con.getInputStream();
                             CountingInputStream cin = new CountingInputStream(stream)) {
                            while ((len = cin.read(buf)) >= 0) {
                                out.write(buf, 0, len);

                                //
                                if (job.status == null || job.status.used.get()) {
                                    percentage = (int) (((double) cin.getByteCount() / total) * 100);
                                    //  System.out.println("---------cin.getCount():" + cin.getByteCount() + ",total:" + total + ",percentage:" + percentage);
                                    job.status = job.new Installing(percentage, cin.getByteCount());
                                }

                            }
                        } catch (IOException | InvalidPathException e) {
                            throw new IOException("Failed to load " + src + " to " + tmp, e);
                        } finally {
                            t.setName(oldName);
                        }
                        //  return total;
                    } catch (IOException e) {
                        // assist troubleshooting in case of e.g. "too many redirects" by printing actual URL
                        String extraMessage = "";
                        if (con != null && con.getURL() != null && !src.toString().equals(con.getURL().toString())) {
                            // Two URLs are considered equal if different hosts resolve to same IP. Prefer to log in case of string inequality,
                            // because who knows how the server responds to different host name in the request header?
                            // Also, since it involved name resolution, it'd be an expensive operation.
                            extraMessage = " (redirected to: " + con.getURL() + ")";
                        }
                        throw new RuntimeException("Failed to download from " + src + extraMessage, e);
                    }
                    return null;
                }

                @Override
                public Void p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                    throw new UnsupportedOperationException();
                }
            });

//                con = connect(job, src);
//                Objects.requireNonNull(con, "con can not be null");
//                //JENKINS-34174 - set timeout for downloads, may hang indefinitely
//                // particularly noticeable during 2.0 install when downloading
//                // many plugins
//                con.setReadTimeout(PLUGIN_DOWNLOAD_READ_TIMEOUT);


            if (sha1 != null) {
                byte[] digest = sha1.digest();
                job.computedSHA1 = Base64.getEncoder().encodeToString(digest);
            }
            if (sha256 != null) {
                byte[] digest = sha256.digest();
                job.computedSHA256 = Base64.getEncoder().encodeToString(digest);
            }
            if (sha512 != null) {
                byte[] digest = sha512.digest();
                job.computedSHA512 = Base64.getEncoder().encodeToString(digest);
            }
            return tmp;
//            } catch (IOException e) {
//                // assist troubleshooting in case of e.g. "too many redirects" by printing actual URL
//                String extraMessage = "";
//                if (con != null && con.getURL() != null && !src.toString().equals(con.getURL().toString())) {
//                    // Two URLs are considered equal if different hosts resolve to same IP. Prefer to log in case of string inequality,
//                    // because who knows how the server responds to different host name in the request header?
//                    // Also, since it involved name resolution, it'd be an expensive operation.
//                    extraMessage = " (redirected to: " + con.getURL() + ")";
//                }
//                throw new IOException("Failed to download from " + src + extraMessage, e);
//            }
        }

//        /**
//         * Connects to the given URL for downloading the binary. Useful for tweaking
//         * how the connection gets established.
//         */
//        protected URLConnection connect(DownloadJob job, URL src) throws IOException {
//            //return ProxyConfiguration.open(src);
//            return src.openConnection();
//        }

        /**
         * Called after a plugin has been downloaded to move it into its final
         * location. The default implementation is a file rename.
         *
         * @param job The install job that is invoking this strategy.
         * @param src The temporary location of the plugin.
         * @param dst The final destination to install the plugin to.
         * @throws IOException if there are problems installing the resource.
         */
        public void install(DownloadJob job, File src, File dst) throws IOException {
            job.replace(dst, src);
        }

        /**
         * Called after an upgrade has been downloaded to move it into its final
         * location. The default implementation is a file rename.
         *
         * @param job The upgrade job that is invoking this strategy.
         * @param src The temporary location of the upgrade.
         * @param dst The final destination to install the upgrade to.
         * @throws IOException if there are problems installing the resource.
         */
        public void upgrade(DownloadJob job, File src, File dst) throws IOException {
            job.replace(dst, src);
        }

        /**
         * Returns an "always up" server for Internet connectivity testing.
         *
         * @deprecated as of 1.333
         * With the introduction of multiple update center capability, this information
         * is now a part of the {@code update-center.json} file. See
         * {@code http://jenkins-ci.org/update-center.json} as an example.
         */
        @Deprecated
        public String getConnectionCheckUrl() {
            return "http://www.baidu.com";
        }

        /**
         * Returns the URL of the server that hosts the update-center.json
         * file.
         *
         * @return Absolute URL that ends with '/'.
         * @deprecated as of 1.333
         * With the introduction of multiple update center capability, this information
         * is now moved to {@link UpdateSite}.
         */
        @Deprecated
        public String getUpdateCenterUrl() {
            return UPDATE_CENTER_URL;
        }

        /**
         * Returns the URL of the server that hosts plugins and core updates.
         *
         * @deprecated as of 1.333
         * {@code update-center.json} is now signed, so we don't have to further make sure that
         * we aren't downloading from anywhere unsecure.
         */
        @Deprecated
        public String getPluginRepositoryBaseUrl() {
            return "http://jenkins-ci.org/";
        }


        private void testConnection(URL url) throws IOException {

            HttpUtils.get(url, new ConfigFileContext.StreamProcess<Void>() {
                @Override
                public void error(int status, InputStream errstream, IOException e) throws Exception {
                    // super.error(status, errstream, e);
                    throw new HttpRetryException("Invalid response code (" + status + ") from URL: " + url, status);
                }

                @Override
                public Void p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                    //try (InputStream is = connection.getInputStream()) {
                    try {
                        IOUtils.copy(stream, NullOutputStream.NULL_OUTPUT_STREAM);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    //}
                    return null;
                }
            });

//            try {
//
//
//
//                URLConnection connection = ProxyConfiguration.open(url);
//
//                if (connection instanceof HttpURLConnection) {
//                    int responseCode = ((HttpURLConnection) connection).getResponseCode();
//                    if (HttpURLConnection.HTTP_OK != responseCode) {
//                        throw new HttpRetryException("Invalid response code (" + responseCode + ") from URL: " + url, responseCode);
//                    }
//                } else {
//                    try (InputStream is = connection.getInputStream()) {
//                        IOUtils.copy(is, NullOutputStream.NULL_OUTPUT_STREAM);
//                    }
//                }
//            } catch (SSLHandshakeException e) {
//                if (e.getMessage().contains("PKIX path building failed"))
//                    // fix up this crappy error message from JDK
//                    throw new IOException("Failed to validate the SSL certificate of " + url, e);
//            }
        }
    }

    /**
     * Things that {@link 'UpdateCenter#installerService'} executes.
     * <p>
     * This object will have the {@code row.jelly} which renders the job on UI.
     */
    public abstract class UpdateCenterJob implements Runnable {
        /**
         * Unique ID that identifies this job.
         *
         * @see 'UpdateCenter#getJob(int)'
         */
        public final int id = iota.incrementAndGet();

        /**
         * Which {@link UpdateSite} does this belong to?
         */
        @JSONField(serialize = false)
        public final UpdateSite site;

        /**
         * Simple correlation ID that can be used to associated a batch of jobs e.g. the
         * installation of a set of plugins.
         */
        private UUID correlationId = null;

        /**
         * If this job fails, set to the error.
         */
        protected Throwable error;

        protected UpdateCenterJob(UpdateSite site) {
            this.site = site;
        }

//        public Api getApi() {
//            return new Api(this);
//        }

        public UUID getCorrelationId() {
            return correlationId;
        }

        public void setCorrelationId(UUID correlationId) {
            if (this.correlationId != null) {
                throw new IllegalStateException("Illegal call to set the 'correlationId'. Already set.");
            }
            this.correlationId = correlationId;
        }

        /**
         * @deprecated as of 1.326
         * Use {@link #submit()} instead.
         */
        @Deprecated
        public void schedule() {
            submit();
        }


        public String getType() {
            return getClass().getSimpleName();
        }

        /**
         * Schedules this job for an execution
         *
         * @return {@link Future} to keeps track of the status of the execution.
         */
        public Future<UpdateCenterJob> submit() {
            LOGGER.info("Scheduling " + this + " to installerService");
            // TODO: seems like this access to jobs should be synchronized, no?
            // It might get synch'd accidentally via the addJob method, but that wouldn't be good.
            jobs.add(this);
            return installerService.submit(this, this);
        }

        public String getErrorMessage() {
            return error != null ? error.getMessage() : null;
        }

        public Throwable getError() {
            return error;
        }
    }


    /*package*/ interface WithComputedChecksums {
        String getComputedSHA1();

        String getComputedSHA256();

        String getComputedSHA512();
    }

    /**
     * Base class for a job that downloads a file from the Jenkins project.
     */
    public abstract class DownloadJob extends UpdateCenterJob implements WithComputedChecksums {
        /**
         * Immutable object representing the current state of this job.
         */
        public volatile InstallationStatus status = new Pending();

        /**
         * Where to download the file from.
         */
        protected abstract URL getURL() throws MalformedURLException;

        /**
         * Where to download the file to.
         */
        protected abstract File getDestination();

        /**
         * Code name used for logging.
         */
        public abstract String getName();

        /**
         * 下载内容体积
         *
         * @return
         */
        public abstract long getSize();

        /**
         * Display name used for the GUI.
         *
         * @since 2.189
         */
        public String getDisplayName() {
            return getName();
        }

        /**
         * Called when the whole thing went successfully.
         */
        protected abstract void onSuccess();

        /**
         * During download, an attempt is made to compute the SHA-1 checksum of the file.
         * This is the base64 encoded SHA-1 checksum.
         *
         * @since 1.641
         */
        @Override
        public String getComputedSHA1() {
            return computedSHA1;
        }

        private String computedSHA1;

        /**
         * Base64 encoded SHA-256 checksum of the downloaded file, if it could be computed.
         *
         * @since 2.130
         */
        @Override
        public String getComputedSHA256() {
            return computedSHA256;
        }

        private String computedSHA256;

        /**
         * Base64 encoded SHA-512 checksum of the downloaded file, if it could be computed.
         *
         * @since 2.130
         */
        @Override
        public String getComputedSHA512() {
            return computedSHA512;
        }

        private String computedSHA512;

        // private Authentication authentication;

//        /**
//         * Get the user that initiated this job
//         */
//        public Authentication getUser() {
//            return this.authentication;
//        }

        protected DownloadJob(UpdateSite site) {
            super(site);
        }

        @Override
        public void run() {
            try {
                LOGGER.info("Starting the installation of " + getName());

                _run();

                LOGGER.info("Installation successful: " + getName());
                status = new Success();
                onSuccess();
            } catch (InstallationStatus e) {
                status = e;
                if (status.isSuccess()) onSuccess();
                requiresRestart |= status.requiresRestart();
            } catch (MissingDependencyException e) {
                LOGGER.error("Failed to install {}: {}", getName(), e.getMessage());
                status = new Failure(e);
                error = e;
            } catch (Throwable e) {
                LOGGER.error("Failed to install " + getName(), e);
                status = new Failure(e);
                error = e;
            }
        }

        protected void _run() throws IOException, InstallationStatus {
            URL src = getURL();

            config.preValidate(this, src);

            File dst = getDestination();
            File tmp = config.download(this, src);

            config.postValidate(this, tmp);
            config.install(this, tmp, dst);
        }

        /**
         * Called when the download is completed to overwrite
         * the old file with the new file.
         */
        protected void replace(File dst, File src) throws IOException {
            File bak = Util.changeExtension(dst, ".bak");
            bak.delete();
            dst.renameTo(bak);
            dst.delete(); // any failure up to here is no big deal
            if (!src.renameTo(dst)) {
                throw new IOException("Failed to rename " + src + " to " + dst);
            }
        }

        /**
         * Indicates the status or the result of a plugin installation.
         * <p>
         * Instances of this class is immutable.
         */

        public abstract class InstallationStatus extends Throwable {
            public final int id = iota.incrementAndGet();

            private final AtomicBoolean used = new AtomicBoolean(false);

            public void setUsed() {
                used.set(true);
            }

            public boolean isSuccess() {
                return false;
            }

            public final String getType() {
                return getClass().getSimpleName();
            }

            @JSONField(serialize = false)
            @Override
            public StackTraceElement[] getStackTrace() {
                return super.getStackTrace();
            }

            /**
             * Indicates that a restart is needed to complete the tasks.
             */
            public boolean requiresRestart() {
                return false;
            }
        }

        /**
         * Indicates that the installation of a plugin failed.
         */
        public class Failure extends InstallationStatus {
            public final Throwable problem;

            public Failure(Throwable problem) {
                this.problem = problem;
            }

            public String getProblemStackTrace() {
                return ExceptionUtils.getRootCauseMessage(problem);
            }
        }

        /**
         * Indicates that the installation was successful but a restart is needed.
         */
        public class SuccessButRequiresRestart extends Success {
            private final String message;

            public SuccessButRequiresRestart(String message) {
                this.message = message;
            }

            @Override
            public String getMessage() {
                return this.message;
            }

            @Override
            public boolean requiresRestart() {
                return true;
            }
        }

        /**
         * Indicates that the plugin was successfully installed.
         */
        public class Success extends InstallationStatus {
            @Override
            public boolean isSuccess() {
                return true;
            }
        }

        /**
         * Indicates that the plugin was successfully installed.
         */
        public class Skipped extends InstallationStatus {
            @Override
            public boolean isSuccess() {
                return true;
            }
        }

        /**
         * Indicates that the plugin is waiting for its turn for installation.
         */
        public class Pending extends InstallationStatus {
        }

        /**
         * Installation of a plugin is in progress.
         */
        public class Installing extends InstallationStatus {
            /**
             * % completed download, or -1 if the percentage is not known.
             */
            public final int percentage;
            private final long downloadSize;

            public Installing(int percentage, long downloadSize) {
                this.percentage = percentage;
                this.downloadSize = downloadSize;
            }

            public String getDownload() {
                return FileUtils.byteCountToDisplaySize(downloadSize);
            }
        }
    }

    public class InstallationJob extends DownloadJob {
        /**
         * What plugin are we trying to install?
         */
        @JSONField(serialize = false)
        public final UpdateSite.Plugin plugin;
        private final IPluginCoord coord;

        protected final PluginManager pm = TIS.get().getPluginManager();

        /**
         * True to load the plugin into this Jenkins, false to wait until restart.
         */
        protected final boolean dynamicLoad;

        List<PluginWrapper> batch;

        @Override
        public final long getSize() {
            try {
                return coord.getSize();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @deprecated as of 1.442
         */
        public InstallationJob(UpdateSite.Plugin plugin, IPluginCoord coord, UpdateSite site) {
            this(plugin, coord, site, false);
        }

        public InstallationJob(UpdateSite.Plugin plugin, IPluginCoord coord, UpdateSite site, boolean dynamicLoad) {
            super(site);
            this.plugin = plugin;
            this.dynamicLoad = dynamicLoad;
            this.coord = coord;
        }

        public boolean isContainClassifier() {
            return plugin.isMultiClassifier();
        }

        public String getClassifier() {
            return this.coord.getGav();
        }

        @Override
        protected URL getURL() throws MalformedURLException {
            return coord.getDownloadUrl(); //new URL(plugin.url);
        }

        @Override
        protected File getDestination() {
            File baseDir = pm.rootDir;
            Optional<PluginClassifier> classifier = coord.getClassifier();
            String tpiName = plugin.name + PluginManager.PACAKGE_TPI_EXTENSION;
            if (classifier.isPresent()) {
                tpiName = classifier.get().getTPIPluginName(plugin.name, PluginManager.PACAKGE_TPI_EXTENSION);
            }
            return new File(baseDir, tpiName);
        }

//        private File getLegacyDestination() {
//            File baseDir = pm.rootDir;
//            return new File(baseDir, plugin.name + ".hpi");
//        }

        @Override
        public String getName() {
            return plugin.name;
        }

        @Override
        public String getDisplayName() {
            return plugin.getDisplayName();
        }

        @Override
        public void _run() throws IOException, InstallationStatus {
            if (wasInstalled()) {
                // Do this first so we can avoid duplicate downloads, too
                // check to see if the plugin is already installed at the same version and skip it
                LOGGER.info("Skipping duplicate install of: " + plugin.getDisplayName() + "@" + plugin.version);
                return;
            }
            try {
                super._run();

                // if this is a bundled plugin, make sure it won't get overwritten
//                PluginWrapper pw = plugin.getInstalled();
//                if (pw != null && pw.isBundled()) {
//                    try (ACLContext ctx = ACL.as2(ACL.SYSTEM2)) {
//                        pw.doPin();
//                    }
//                }

                if (dynamicLoad) {
                    try {
                        pm.dynamicLoad(getDestination(), false
                                , new PluginAndCfgsSnapshot.PluginWrapperList(batch));
                    } catch (RestartRequiredException e) {
                        throw new SuccessButRequiresRestart(e.getMessage());
                    } catch (Exception e) {
                        throw new IOException("Failed to dynamically deploy this plugin", e);
                    }
                } else {
                    throw new SuccessButRequiresRestart("_UpdateCenter_DownloadButNotActivated");
                }
            } finally {
                synchronized (this) {
                    // There may be other threads waiting on completion
                    LOGGER.info("Install complete for: " + plugin.getDisplayName() + "@" + plugin.version);
                    // some status other than Installing or Downloading needs to be set here
                    // {@link #isAlreadyInstalling()}, it will be overwritten by {@link DownloadJob#run()}
                    status = new Skipped();
                    notifyAll();
                }
            }
        }

        /**
         * Indicates there is another installation job for this plugin
         *
         * @since 2.1
         */
        protected boolean wasInstalled() {
            synchronized (UpdateCenter.this) {
                for (UpdateCenterJob job : getJobs()) {
                    if (job == this) {
                        // oldest entries first, if we reach this instance,
                        // we need it to continue installing
                        return false;
                    }
                    if (job instanceof InstallationJob) {
                        InstallationJob ij = (InstallationJob) job;
                        if (ij.plugin.equals(plugin) && ij.plugin.version.equals(plugin.version)) {
                            // wait until other install is completed
                            synchronized (ij) {
                                if (ij.status instanceof Installing || ij.status instanceof Pending) {
                                    try {
                                        LOGGER.info("Waiting for other plugin install of: " + plugin.getDisplayName() + "@" + plugin.version);
                                        ij.wait();
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                // Must check for success, otherwise may have failed installation
                                if (ij.status instanceof Success) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            }
        }

        @Override
        protected void onSuccess() {
            pm.pluginUploaded = true;
        }

        @Override
        public String toString() {
            return super.toString() + "[plugin=" + plugin.title + "]";
        }

        /**
         * Called when the download is completed to overwrite
         * the old file with the new file.
         */
        @Override
        protected void replace(File dst, File src) throws IOException {
            if (site == null || !site.getId().equals(ID_UPLOAD)) {
                verifyChecksums(this, plugin, src);
            }

            File bak = Util.changeExtension(dst, ".bak");
            bak.delete();

//            final File legacy = getLegacyDestination();
//            if (legacy.exists()) {
//                if (!legacy.renameTo(bak)) {
//                    legacy.delete();
//                }
//            }
            if (dst.exists()) {
                if (!dst.renameTo(bak)) {
                    dst.delete();
                }
            }

            if (!src.renameTo(dst)) {
                throw new IOException("Failed to rename " + src + " to " + dst);
            }
        }

        void setBatch(List<PluginWrapper> batch) {
            this.batch = batch;
        }

    }


    public final class CompleteBatchJob extends UpdateCenterJob {

        private final List<PluginWrapper> batch;
        private final long start;

        public volatile CompleteBatchJobStatus status = new Pending();

        public CompleteBatchJob(List<PluginWrapper> batch, long start, UUID correlationId) {
            super(getCoreSource());
            this.batch = batch;
            this.start = start;
            setCorrelationId(correlationId);
        }

        @Override
        public void run() {
            LOGGER.info("Completing installing of plugin batch…");
            status = new Running();
            try {
                TIS.get().getPluginManager().start(new PluginAndCfgsSnapshot.PluginWrapperList(batch));
                status = new Success();
            } catch (Exception x) {
                status = new Failure(x);
                LOGGER.warn("Failed to start some plugins", x);
            }
            LOGGER.info("Completed installation of {} plugins in {}ms", batch.size(), ((System.currentTimeMillis() - start)));
        }


        public abstract class CompleteBatchJobStatus {
            public final int id = iota.incrementAndGet();
        }

        public class Pending extends CompleteBatchJobStatus {
        }

        public class Running extends CompleteBatchJobStatus {
        }

        public class Success extends CompleteBatchJobStatus {
        }

        public class Failure extends CompleteBatchJobStatus {
            Failure(Throwable problemStackTrace) {
                this.problemStackTrace = problemStackTrace;
            }

            public final Throwable problemStackTrace;
        }

    }

    /**
     * Enables a required plugin, provides feedback in the update center
     */
    public class EnableJob extends UpdateCenter.InstallationJob {
        public EnableJob(UpdateSite site, UpdateSite.Plugin plugin, IPluginCoord coord, boolean dynamicLoad) {
            super(plugin, coord, site, dynamicLoad);
        }

        public UpdateSite.Plugin getPlugin() {
            return plugin;
        }

        @Override
        public void run() {
            try {
                PluginWrapper installed = plugin.getInstalled();
                synchronized (installed) {
                    if (!installed.isEnabled()) {
                        try {
                            installed.enable();
                        } catch (IOException e) {
                            LOGGER.error("Failed to enable " + plugin.getDisplayName(), e);
                            error = e;
                            status = new Failure(e);
                        }

                        if (dynamicLoad) {
                            try {
                                // remove the existing, disabled inactive plugin to force a new one to load
                                pm.dynamicLoad(getDestination(), true, null);
                            } catch (Exception e) {
                                LOGGER.error("Failed to dynamically load " + plugin.getDisplayName(), e);
                                error = e;
                                requiresRestart = true;
                                status = new Failure(e);
                            }
                        } else {
                            requiresRestart = true;
                        }
                    }
                }
            } catch (Throwable e) {
                LOGGER.error("An unexpected error occurred while attempting to enable " + plugin.getDisplayName(), e);
                error = e;
                requiresRestart = true;
                status = new Failure(e);
            }
            if (status instanceof Pending) {
                status = new Success();
            }
        }
    }

    public UpdateSite getCoreSource() {
        for (UpdateSite s : sites) {
            UpdateSite.Data data = s.getData();
            if (data != null && data.core != null) {
                return s;
            }
        }
        return null;
    }

    /**
     * A no-op, e.g. this plugin is already installed
     */
    public class NoOpJob extends EnableJob {
        public NoOpJob(UpdateSite site, UpdateSite.Plugin plugin, IPluginCoord coord) {
            super(site, plugin, coord, false);
        }

        @Override
        public void run() {
            // do nothing
            status = new Success();
        }
    }

    /**
     * Implements the checksum verification logic with fallback to weaker algorithm for {@link DownloadJob}.
     *
     * @param job   The job downloading the file to check
     * @param entry The metadata entry for the file to check
     * @param file  The downloaded file
     * @throws IOException thrown when one of the checks failed, or no checksum could be computed.
     */
    /* package */
    static void verifyChecksums(WithComputedChecksums job, UpdateSite.Entry entry, File file) throws IOException {
//        VerificationResult result512 = verifyChecksums(entry.getSha512(), job.getComputedSHA512(), false);
//        switch (result512) {
//            case PASS:
//                // this has passed so no reason to check the weaker checksums
//                return;
//            case FAIL:
//                throwVerificationFailure(entry.getSha512(), job.getComputedSHA512(), file, "SHA-512");
//                break;
//            case NOT_COMPUTED:
//                LOGGER.log(WARNING, "Attempt to verify a downloaded file (" + file.getName() + ") using SHA-512 failed since it could not be computed. Falling back to weaker algorithms. Update your JRE.");
//                break;
//            case NOT_PROVIDED:
//                break;
//            default:
//                throw new IllegalStateException("Unexpected value: " + result512);
//        }
//
//        VerificationResult result256 = verifyChecksums(entry.getSha256(), job.getComputedSHA256(), false);
//        switch (result256) {
//            case PASS:
//                return;
//            case FAIL:
//                throwVerificationFailure(entry.getSha256(), job.getComputedSHA256(), file, "SHA-256");
//                break;
//            case NOT_COMPUTED:
//            case NOT_PROVIDED:
//                break;
//            default:
//                throw new IllegalStateException("Unexpected value: " + result256);
//        }
//
//        if (result512 == VerificationResult.NOT_PROVIDED && result256 == VerificationResult.NOT_PROVIDED) {
//            LOGGER.log(INFO, "Attempt to verify a downloaded file (" + file.getName() + ") using SHA-512 or SHA-256 failed since your configured update site does not provide either of those checksums. Falling back to SHA-1.");
//        }
//
//        VerificationResult result1 = verifyChecksums(entry.getSha1(), job.getComputedSHA1(), true);
//        switch (result1) {
//            case PASS:
//                return;
//            case FAIL:
//                throwVerificationFailure(entry.getSha1(), job.getComputedSHA1(), file, "SHA-1");
//                break;
//            case NOT_COMPUTED:
//                throw new IOException("Failed to compute SHA-1 of downloaded file, refusing installation");
//            case NOT_PROVIDED:
//                throw new IOException("Unable to confirm integrity of downloaded file, refusing installation");
//        }
    }

    /**
     * Simple connection status enum.
     */
    enum ConnectionStatus {
        /**
         * Connection status has not started yet.
         */
        PRECHECK,
        /**
         * Connection status check has been skipped.
         * As example, it may happen if there is no connection check URL defined for the site.
         *
         * @since 2.4
         */
        SKIPPED,
        /**
         * Connection status is being checked at this time.
         */
        CHECKING,
        /**
         * Connection status was not checked.
         */
        UNCHECKED,
        /**
         * Connection is ok.
         */
        OK,
        /**
         * Connection status check failed.
         */
        FAILED;

        static final String INTERNET = "internet";
        static final String UPDATE_SITE = "updatesite";
    }

    /**
     * Tests the internet connectivity.
     */
    public final class ConnectionCheckJob extends UpdateCenterJob {
        private final Vector<String> statuses = new Vector<>();

        final Map<String, ConnectionStatus> connectionStates = new ConcurrentHashMap<>();

        public ConnectionCheckJob(UpdateSite site) {
            super(site);
            connectionStates.put(ConnectionStatus.INTERNET, ConnectionStatus.PRECHECK);
            connectionStates.put(ConnectionStatus.UPDATE_SITE, ConnectionStatus.PRECHECK);
        }

        @Override
        public void run() {
            connectionStates.put(ConnectionStatus.INTERNET, ConnectionStatus.UNCHECKED);
            connectionStates.put(ConnectionStatus.UPDATE_SITE, ConnectionStatus.UNCHECKED);
            if (site == null || ID_UPLOAD.equals(site.getId())) {
                return;
            }
            LOGGER.info("Doing a connectivity check");
            Future<?> internetCheck = null;
            try {
                final String connectionCheckUrl = site.getConnectionCheckUrl();
                if (connectionCheckUrl != null) {
                    connectionStates.put(ConnectionStatus.INTERNET, ConnectionStatus.CHECKING);
                    statuses.add("CheckingInternet");
                    // Run the internet check in parallel
                    internetCheck = updateService.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                config.checkConnection(ConnectionCheckJob.this, connectionCheckUrl);
                            } catch (Exception e) {
                                if (e.getMessage().contains("Connection timed out")) {
                                    // Google can't be down, so this is probably a proxy issue
                                    connectionStates.put(ConnectionStatus.INTERNET, ConnectionStatus.FAILED);
                                    // Messages.UpdateCenter_Status_ConnectionFailed(Functions.xmlEscape(connectionCheckUrl))
                                    statuses.add("ConnectionFailed:" + connectionCheckUrl);
                                    return;
                                }
                            }
                            connectionStates.put(ConnectionStatus.INTERNET, ConnectionStatus.OK);
                        }
                    });
                } else {
                    LOGGER.info("Update site ''{}'' does not declare the connection check URL. "
                            + "Skipping the network availability check.", site.getId());
                    connectionStates.put(ConnectionStatus.INTERNET, ConnectionStatus.SKIPPED);
                }

                connectionStates.put(ConnectionStatus.UPDATE_SITE, ConnectionStatus.CHECKING);
                statuses.add("CheckingJavaNet");

                config.checkUpdateCenter(this, site.getUrl());

                connectionStates.put(ConnectionStatus.UPDATE_SITE, ConnectionStatus.OK);
                statuses.add("Status_Success");
            } catch (UnknownHostException e) {
                connectionStates.put(ConnectionStatus.UPDATE_SITE, ConnectionStatus.FAILED);
                statuses.add("UnknownHostException:" + e.getMessage());
                addStatus(e);
                error = e;
            } catch (Exception e) {
                connectionStates.put(ConnectionStatus.UPDATE_SITE, ConnectionStatus.FAILED);
                addStatus(e);
                error = e;
            }

            if (internetCheck != null) {
                try {
                    // Wait for internet check to complete
                    internetCheck.get();
                } catch (Exception e) {
                    LOGGER.info("Error completing internet connectivity check: " + e.getMessage(), e);
                }
            }
        }

        private void addStatus(Throwable e) {
            statuses.add(ExceptionUtils.getRootCauseMessage(e));
            //  statuses.add("<pre>" + Functions.xmlEscape(Functions.printThrowable(e)) + "</pre>");
        }

        public String[] getStatuses() {
            synchronized (statuses) {
                return statuses.toArray(new String[statuses.size()]);
            }
        }


    }

    /**
     * Sequence number generator.
     */
    private static final AtomicInteger iota = new AtomicInteger();
//    public static final XStream2 XSTREAM = new XStream2(XmlFile.DEFAULT_DRIVER);
//
//    static {
//        XSTREAM.alias("site",UpdateSite.class);
//       // XSTREAM.alias("sites",PersistedList.class);
//    }
}
