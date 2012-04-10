/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jline.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import static jline.internal.Preconditions.checkNotNull;

/**
 * Provides access to configuration values.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 2.4
 */
public class Configuration
{
    /**
     * System property which can point to a file or URL containing configuration properties to load.
     *
     * @since 2.7
     */
    public static final String JLINE_CONFIGURATION = "jline.configuration";

    /**
     * Default configuration file name loaded from user's home directory.
     */
    public static final String JLINE_RC = ".jline.rc";

    private static final Properties properties = initProperties();

    private static Properties initProperties() {
        URL url = determineUrl();
        Properties props = new Properties();
        try {
            loadProperties(url, props);
        }
        catch (IOException e) {
            Log.warn("Unable to read configuration from:", url, e);
        }
        return props;
    }

    private static void loadProperties(final URL url, final Properties props) throws IOException {
        Log.debug("Loading properties from:", url);
        InputStream input = url.openStream();
        try {
            props.load(new BufferedInputStream(input));
        }
        finally {
            try {
                input.close();
            }
            catch (IOException e) {
                // ignore
            }
        }

        if (Log.DEBUG) {
            Log.debug("Loaded properties:");
            for (Map.Entry<Object,Object> entry : props.entrySet()) {
                Log.debug("  ", entry.getKey(), "=", entry.getValue());
            }
        }
    }

    private static URL determineUrl() {
        // See if user has customized the configuration location via sysprop
        String tmp = System.getProperty(JLINE_CONFIGURATION);
        if (tmp != null) {
            return Urls.create(tmp);
        }
        else {
            // otherwise use the default
            return Urls.create(new File(getUserHome(), JLINE_RC));
        }
    }

    /**
     * @since 2.7
     */
    public static void reset() {
        Log.debug("Resetting");
        properties.clear();
        properties.putAll(initProperties());
    }

    /**
     * @since 2.7
     */
    public static Properties getProperties() {
        return properties;
    }

    public static String getString(final String name, final String defaultValue) {
        checkNotNull(name);

        String value;

        // Check sysprops first, it always wins
        value = System.getProperty(name);

        if (value == null) {
            // Next try userprops
            value = properties.getProperty(name);

            if (value == null) {
                // else use the default
                value = defaultValue;
            }
        }

        return value;
    }

    public static String getString(final String name) {
        return getString(name, null);
    }

    public static boolean getBoolean(final String name, final boolean defaultValue) {
        String value = getString(name);
        if (value == null) {
            return defaultValue;
        }
        return value.length() == 0
            || value.equalsIgnoreCase("1")
            || value.equalsIgnoreCase("on")
            || value.equalsIgnoreCase("true");
    }

    public static int getInteger(final String name, final int defaultValue) {
        String str = getString(name);
        if (str == null) {
            return defaultValue;
        }
        return Integer.parseInt(str);
    }

    public static long getLong(final String name, final long defaultValue) {
        String str = getString(name);
        if (str == null) {
            return defaultValue;
        }
        return Long.parseLong(str);
    }

    //
    // System property helpers
    //
    
    public static File getUserHome() {
        return new File(System.getProperty("user.home"));
    }

    public static String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }

    //public static String getFileEncoding() {
    //    return System.getProperty("file.encoding");
    //}

    public static String getEncoding() {
        // LC_CTYPE is usually in the form en_US.UTF-8
        String ctype = System.getenv("LC_CTYPE");
        if (ctype != null && ctype.indexOf('.') > 0) {
            return ctype.substring(ctype.indexOf('.') + 1);
        }
        return System.getProperty("input.encoding", Charset.defaultCharset().name());
    }
}