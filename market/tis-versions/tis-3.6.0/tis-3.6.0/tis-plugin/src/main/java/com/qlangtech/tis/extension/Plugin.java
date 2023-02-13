/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.extension;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.impl.XmlFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class Plugin implements Saveable {

    @Deprecated
    protected Plugin() {
    }

    /**
     * Set by the {@link PluginManager}, before the {@link #start()} method is called.
     * This points to the {@link PluginWrapper} that wraps
     * this {@link Plugin} object.
     */
    /*package*/
    transient PluginWrapper wrapper;

    /**
     * Gets the paired {@link PluginWrapper}.
     *
     * @since 1.426
     */
    public PluginWrapper getWrapper() {
        return wrapper;
    }

    public void start() throws Exception {
    }

    /**
     * Called after {@link #start()} is called for all the plugins.
     *
     * @throws Exception any exception thrown by the plugin during the initialization will disable plugin.
     */
    public void postInitialize() throws Exception {
    }

    /**
     * Called to orderly shut down
     *
     * <p>
     * This is a good opportunity to clean up resources that plugin started.
     * This method will not be invoked if the {@link #start()} failed abnormally.
     *
     * @throws Exception if any exception is thrown, it is simply recorded and shut-down of other
     *                   plugins continue. This is primarily just a convenience feature, so that
     *                   each plugin author doesn't have to worry about catching an exception and
     *                   recording it.
     * @since 1.42
     */
    public void stop() throws Exception {
    }

    /**
     * Handles the submission for the system configuration.
     *
     * <p>
     * If this class defines <tt>config.jelly</tt> view, be sure to
     * override this method and persists the submitted values accordingly.
     *
     * <p>
     * The following is a sample <tt>config.jelly</tt> that you can start yours with:
     * <pre><xmp>
     * <j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define" xmlns:l="/lib/layout" xmlns:t="/lib/hudson" xmlns:f="/lib/form">
     *   <f:section title="Locale">
     *     <f:entry title="${%Default Language}" help="/plugin/locale/help/default-language.html">
     *       <f:textbox name="systemLocale" value="${it.systemLocale}" />
     *     </f:entry>
     *   </f:section>
     * </j:jelly>
     * </xmp></pre>
     *
     * <p>
     * This allows you to access data as {@code formData.getString("systemLocale")}
     *
     * <p>
     * If you are using this method, you'll likely be interested in
     * using {@link #save()} and {@link #load()}.
     * @since 1.305
     */
    // public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, FormException {
    // configure(formData);
    // }
    /**
     * This method serves static resources in the plugin under <tt>hudson/plugin/SHORTNAME</tt>.
     */
    // public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    // String path = req.getRestOfPath();
    //
    // if (path.startsWith("/META-INF/") || path.startsWith("/WEB-INF/")) {
    // throw HttpResponses.notFound();
    // }
    //
    // if(path.length()==0)
    // path = "/";
    //
    // // Stapler routes requests like the "/static/.../foo/bar/zot" to be treated like "/foo/bar/zot"
    // // and this is used to serve long expiration header, by using Jenkins.VERSION_HASH as "..."
    // // to create unique URLs. Recognize that and set a long expiration header.
    // String requestPath = req.getRequestURI().substring(req.getContextPath().length());
    // boolean staticLink = requestPath.startsWith("/static/");
    //
    // long expires = staticLink ? TimeUnit2.DAYS.toMillis(365) : -1;
    //
    // // use serveLocalizedFile to support automatic locale selection
    // try {
    // rsp.serveLocalizedFile(req, wrapper.baseResourceURL.toURI().resolve(new URI(null, '.' + path, null)).toURL(), expires);
    // } catch (URISyntaxException x) {
    // throw new IOException(x);
    // }
    // }
    //
    // Convenience methods for those plugins that persist configuration
    //

    /**
     * Loads serializable fields of this instance from the persisted storage.
     *
     * <p>
     * If there was no previously persisted state, this method is no-op.
     *
     * @since 1.245
     */
    protected void load() throws IOException {
        XmlFile xml = getConfigXml();
        if (xml.exists()) {
            xml.unmarshal(this);
        }
    }

    /**
     * Saves serializable fields of this instance to the persisted storage.
     *
     * @since 1.245
     */
    public void save() throws IOException {
        // if(BulkChange.contains(this))   return;
        XmlFile config = getConfigXml();
        config.write(this, Collections.emptySet());
        // SaveableListener.fireOnChange(this, config);
    }

    protected XmlFile getConfigXml() {

        String relativePath = wrapper.getShortName() + ".xml";
        return new XmlFile(new File(TIS.pluginCfgRoot, relativePath));
    }

    /**
     * Dummy instance of {@link Plugin} to be used when a plugin didn't
     * supply one on its own.
     *
     * @since 1.321
     */
    public static final class DummyImpl extends Plugin {
    }
}
