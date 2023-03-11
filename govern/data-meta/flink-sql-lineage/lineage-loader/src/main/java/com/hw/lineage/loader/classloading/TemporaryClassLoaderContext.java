package com.hw.lineage.loader.classloading;

/**
 * Sets a context class loader in a "try-with-resources" pattern.
 *
 * <pre>{@code
 * try (TemporaryClassLoaderContext ignored = TemporaryClassLoaderContext.of(classloader)) {
 *     // code that needs the context class loader
 * }
 * }</pre>
 *
 * <p>This is conceptually the same as the code below.
 *
 * <pre>{@code
 * ClassLoader original = Thread.currentThread().getContextClassLoader();
 * Thread.currentThread().setContextClassLoader(classloader);
 * try {
 *     // code that needs the context class loader
 * } finally {
 *     Thread.currentThread().setContextClassLoader(original);
 * }
 * }</pre>
 *
 * @description: TemporaryClassLoaderContext
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class TemporaryClassLoaderContext implements AutoCloseable {

    /**
     * Sets the context class loader to the given ClassLoader and returns a resource that sets it
     * back to the current context ClassLoader when the resource is closed.
     *
     * <pre>{@code
     * try (TemporaryClassLoaderContext ignored = TemporaryClassLoaderContext.of(classloader)) {
     *     // code that needs the context class loader
     * }
     * }</pre>
     */
    public static TemporaryClassLoaderContext of(ClassLoader cl) {
        final Thread t = Thread.currentThread();
        final ClassLoader original = t.getContextClassLoader();

        t.setContextClassLoader(cl);

        return new TemporaryClassLoaderContext(t, original);
    }

    private final Thread thread;

    private final ClassLoader originalContextClassLoader;

    private TemporaryClassLoaderContext(Thread thread, ClassLoader originalContextClassLoader) {
        this.thread = thread;
        this.originalContextClassLoader = originalContextClassLoader;
    }

    @Override
    public void close() {
        thread.setContextClassLoader(originalContextClassLoader);
    }
}
