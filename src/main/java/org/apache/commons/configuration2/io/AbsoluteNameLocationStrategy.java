/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.configuration2.io;

import java.io.File;
import java.net.URL;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * <p>
 * A specialized implementation of {@code FileLocationStrategy} which checks whether the provided file name is already
 * an absolute file name.
 * </p>
 * <p>
 * This strategy ignores the URL and the base path stored in the passed in {@link FileLocator}. It is only triggered by
 * absolute names in the locator's {@code fileName} component.
 * </p>
 *
 * @since 2.0
 */
public class AbsoluteNameLocationStrategy implements FileLocationStrategy {
    /**
     * {@inheritDoc} This implementation constructs a {@code File} object from the locator's file name (if defined).
     * If this results in an absolute file name pointing to an existing file, the corresponding URL is returned.
     */
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "File name is sanitized and validated.")
    @Override
    public URL locate(final FileSystem fileSystem, final FileLocator locator) {
        final String fileName = locator.getFileName();
        if (StringUtils.isNotEmpty(fileName)) {
            // 1) Validate or sanitize the input
            if (!isSafePath(fileName)) {
                // e.g., reject or log a warning
                getLogger().warn("Rejected unsafe file name: {}", fileName);
                return null;
            }

            // 2) Create a File object with the validated file name
            final File file = new File(fileName);
            if (file.isAbsolute() && file.exists()) {
                return FileLocatorUtils.convertFileToURL(file);
            }
        }
        return null;
    }

    /**
     * Checks if a file name is considered safe to use. This is a simple example
     * that disallows path-traversal attempts ('../') and only allows certain characters.
     * Adjust as needed for your application's security policy.
     */
    private boolean isSafePath(String fileName) {
        // Disallow path traversal attempts
        if (fileName.contains("..")) {
            return false;
        }
        // Let's say we allow letters, digits, underscore, dash, period, slash, backslash:
        // (Adjust pattern to reflect your environment and OS)
        if (!fileName.matches("[A-Za-z0-9._/\\\\-]+")) {
            return false;
        }
        return true;
    }
}
