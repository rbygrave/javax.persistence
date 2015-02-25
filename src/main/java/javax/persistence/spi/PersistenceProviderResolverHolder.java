/*
 * Copyright (c) 2008, 2009, 2011 Oracle, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.  The Eclipse Public License is available
 * at http://www.eclipse.org/legal/epl-v10.html and the Eclipse Distribution License
 * is available at http://www.eclipse.org/org/documents/edl-v10.php.
 */
package javax.persistence.spi;

import javax.persistence.PersistenceException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the global PersistenceProviderResolver instance. If no PersistenceProviderResolver is set by the
 * environment, the default PersistenceProviderResolver is used. Implementations must be thread-safe.
 */
public class PersistenceProviderResolverHolder
{
    private static final PersistenceProviderResolver DEFAULT_RESOLVER = new PersistenceProviderResolverPerClassLoader();

    private static volatile PersistenceProviderResolver RESOLVER;

    /**
     * Returns the current persistence provider resolver
     * @return persistence provider resolver in use
     */
    public static PersistenceProviderResolver getPersistenceProviderResolver()
    {
        return RESOLVER == null ? DEFAULT_RESOLVER : RESOLVER;
    }

    /**
     * Defines the persistence provider resolver used.
     * @param resolver PersistenceProviderResolver to be used.
     */
    public static void setPersistenceProviderResolver(PersistenceProviderResolver resolver)
    {
        RESOLVER = resolver;
    }

    /**
     * Cache PersistenceProviderResolver per classloader and use the current classloader as a key. Use
     * CachingPersistenceProviderResolver for each PersistenceProviderResolver instance.
     */
    private static class PersistenceProviderResolverPerClassLoader implements PersistenceProviderResolver
    {
        // FIXME use a ConcurrentHashMap with weak entry
        private final WeakHashMap<ClassLoader, PersistenceProviderResolver> resolvers = new WeakHashMap<ClassLoader, PersistenceProviderResolver>();

        private volatile short barrier = 1;

        /**
         * {@inheritDoc}
         */
        public List<PersistenceProvider> getPersistenceProviders()
        {
            ClassLoader cl = getContextualClassLoader();
            if (barrier == 1)
            {
            } // read barrier syncs state with other threads
            PersistenceProviderResolver currentResolver = resolvers.get(cl);
            if (currentResolver == null)
            {
                currentResolver = new CachingPersistenceProviderResolver(cl);
                resolvers.put(cl, currentResolver);
                barrier = 1;
            }
            return currentResolver.getPersistenceProviders();
        }

        /**
         * {@inheritDoc}
         */
        public void clearCachedProviders()
        {
            // todo : should we clear all providers from all resolvers here?
            ClassLoader cl = getContextualClassLoader();
            if (barrier == 1)
            {
            } // read barrier syncs state with other threads

            PersistenceProviderResolver currentResolver = resolvers.get(cl);
            if (currentResolver != null)
            {
                currentResolver.clearCachedProviders();
            }
        }

        private static ClassLoader getContextualClassLoader()
        {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null)
            {
                cl = PersistenceProviderResolverPerClassLoader.class.getClassLoader();
            }
            return cl;
        }

        /**
         * Resolve the list of Persistence providers for a given classloader and cache the results. Avoids to
         * keep any reference from this class to the classloader being passed to the constructor.
         */
        private static class CachingPersistenceProviderResolver implements PersistenceProviderResolver
        {
            // this assumes that the class loader keeps the list of classes loaded
            private final List<WeakReference<Class<? extends PersistenceProvider>>> resolverClasses = new ArrayList<WeakReference<Class<? extends PersistenceProvider>>>();

            public CachingPersistenceProviderResolver(ClassLoader cl)
            {
                loadResolverClasses(cl);
            }

            private void loadResolverClasses(ClassLoader cl)
            {
                synchronized (resolverClasses)
                {
                    try
                    {
                        Enumeration<URL> resources = cl.getResources("META-INF/services/" + PersistenceProvider.class.getName());
                        Set<String> names = new HashSet<String>();
                        while (resources.hasMoreElements())
                        {
                            URL url = resources.nextElement();
                            InputStream is = url.openStream();
                            try
                            {
                                names.addAll(providerNamesFromReader(new BufferedReader(new InputStreamReader(is))));
                            }
                            finally
                            {
                                is.close();
                            }
                        }
                        for (String s : names)
                        {
                            @SuppressWarnings("unchecked")
                            Class<? extends PersistenceProvider> providerClass = (Class<? extends PersistenceProvider>) cl.loadClass(s);
                            WeakReference<Class<? extends PersistenceProvider>> reference = new WeakReference<Class<? extends PersistenceProvider>>(providerClass);
                            resolverClasses.add(reference);
                        }
                    }
                    catch (IOException e)
                    {
                        throw new PersistenceException(e);
                    }
                    catch (ClassNotFoundException e)
                    {
                        throw new PersistenceException(e);
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public List<PersistenceProvider> getPersistenceProviders()
            {
                synchronized (resolverClasses)
                {
                    List<PersistenceProvider> providers = new ArrayList<PersistenceProvider>(resolverClasses.size());
                    try
                    {
                        for (WeakReference<Class<? extends PersistenceProvider>> providerClass : resolverClasses)
                        {
                            providers.add(providerClass.get().newInstance());
                        }
                    }
                    catch (InstantiationException e)
                    {
                        throw new PersistenceException(e);
                    }
                    catch (IllegalAccessException e)
                    {
                        throw new PersistenceException(e);
                    }
                    return providers;
                }
            }

            /**
             * {@inheritDoc}
             */
            public synchronized void clearCachedProviders()
            {
                synchronized (resolverClasses)
                {
                    resolverClasses.clear();
                    loadResolverClasses(PersistenceProviderResolverPerClassLoader.getContextualClassLoader());
                }
            }

            private static final Pattern nonCommentPattern = Pattern.compile("^([^#]+)");

            private static Set<String> providerNamesFromReader(BufferedReader reader) throws IOException
            {
                Set<String> names = new HashSet<String>();
                String line;
                while ((line = reader.readLine()) != null)
                {
                    line = line.trim();
                    Matcher m = nonCommentPattern.matcher(line);
                    if (m.find())
                    {
                        names.add(m.group().trim());
                    }
                }
                return names;
            }
        }
    }
}
