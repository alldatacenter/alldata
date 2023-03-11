package com.hw.lineage.loader.plugin;

import com.hw.lineage.loader.PluginTestBase;
import com.hw.lineage.loader.service.OtherTestService;
import com.hw.lineage.loader.service.TestService;
import com.hw.lineage.loader.service.impl.plugina.TestServiceA;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;


/**
 * @description: PluginLoaderTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class PluginLoaderTest extends PluginTestBase {

    @Test
    public void testLoadPlugin() throws Exception {
        URL classpathA = createPluginJarURLFromString(PLUGIN_A_JAR);
        String[] parentPatterns = {TestService.class.getName(), OtherTestService.class.getName()};

        PluginDescriptor pluginDescriptorA =
                new PluginDescriptor(PLUGIN_A, new URL[]{classpathA}, parentPatterns);
        URLClassLoader pluginClassLoaderA =
                PluginLoader.createPluginClassLoader(pluginDescriptorA, PARENT_CLASS_LOADER, new String[0]);

        Assert.assertNotEquals(PARENT_CLASS_LOADER, pluginClassLoaderA);
        final PluginLoader pluginLoaderA = new PluginLoader(PLUGIN_A, pluginClassLoaderA);

        Iterator<TestService> testServiceIteratorA = pluginLoaderA.load(TestService.class);
        Assert.assertTrue(testServiceIteratorA.hasNext());
        TestService testServiceA = testServiceIteratorA.next();
        Assert.assertEquals("A-hello-Dynamic-A-hello", testServiceA.say("hello"));

        Assert.assertFalse(testServiceIteratorA.hasNext());
        Assert.assertEquals(TestServiceA.class.getCanonicalName(), testServiceA.getClass().getCanonicalName());

        // The plugin must return the same class loader as the one used to load it.
        Assert.assertEquals(pluginClassLoaderA, testServiceA.getClassLoader());
        Assert.assertEquals(pluginClassLoaderA, testServiceA.getClass().getClassLoader());

        /**
         *  Looks strange, but we want to ensure that those classes are not instance of each other
         *  because they were loaded by different classloader instances because the plugin loader uses
         *  child-before-parent order.
         */
        Assert.assertFalse(testServiceA instanceof TestServiceA);

        // In the following we check for isolation of classes between different plugin loaders.
        PluginLoader secondPluginLoaderA =
                PluginLoader.create(pluginDescriptorA, PARENT_CLASS_LOADER, new String[0]);

        TestService secondTestServiceA = secondPluginLoaderA.load(TestService.class).next();
        Assert.assertEquals("A-hello-Dynamic-A-hello", secondTestServiceA.say("hello"));

        // Again, this looks strange, but we expect classes with the same name, that are not equal.
        Assert.assertEquals(
                testServiceA.getClass().getCanonicalName(),
                secondTestServiceA.getClass().getCanonicalName());
        Assert.assertNotEquals(testServiceA.getClass(), secondTestServiceA.getClass());
    }

    @Test
    public void testClose() throws MalformedURLException {
        URL classpathA = createPluginJarURLFromString(PLUGIN_A_JAR);
        String[] parentPatterns = {TestService.class.getName()};
        PluginDescriptor pluginDescriptorA =
                new PluginDescriptor(PLUGIN_A, new URL[]{classpathA}, parentPatterns);

        URLClassLoader pluginClassLoaderA =
                PluginLoader.createPluginClassLoader(
                        pluginDescriptorA, PARENT_CLASS_LOADER, new String[0]);

        PluginLoader pluginLoaderA = new PluginLoader(PLUGIN_A, pluginClassLoaderA);
        pluginLoaderA.close();

        Assert.assertThrows(
                ClassNotFoundException.class,
                () -> pluginClassLoaderA.loadClass(junit.framework.Test.class.getName()));
    }
}