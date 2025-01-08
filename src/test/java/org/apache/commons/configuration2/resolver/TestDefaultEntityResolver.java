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

package org.apache.commons.configuration2.resolver;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates tests for missing branches in the DefaultEntityResolver's resolveEntity(...) method.
 */
public class TestDefaultEntityResolver {

    private DefaultEntityResolver resolver;

    @BeforeEach
    public void setUp() {
        resolver = new DefaultEntityResolver() {
            // If your real class 'DefaultEntityResolver' is not final, you can override getRegisteredEntities().
            // Otherwise, you might set them up some other way.
            private final Map<String, URL> entityMap = new HashMap<>();


            @Override
            public Map<String, URL> getRegisteredEntities() {
                return entityMap;
            }
        };
    }

    /**
     * 1) If 'publicId' is not found in the map => entityURL == null => returns null.
     *    This covers the 'default' return path.
     */
    @Test
    public void testResolveEntityNoMapping() throws SAXException {
        // publicId not in map => entityURL is null => returns null
        String nonExistentPublicId = "someRandomPublicId";
        InputSource source = resolver.resolveEntity(nonExistentPublicId, "someSystemId");
        assertNull(source, "When entityURL is null, method should return null.");
    }

    /**
     * 2) Successfully found a mapping => entityURL != null => attempts to open a connection
     *    We simulate a real or mock URL that can return an InputStream without error => success branch.
     */
    @Test
    public void testResolveEntitySuccess() throws Exception {
        // Insert a known mapping for 'myPublicId' -> a fake in-memory URL
        Map<String, URL> entityMap = resolver.getRegisteredEntities();
        entityMap.put("myPublicId", new URL("file:///fake/path/to/entity.xml"));

        // Now we also need to override how openConnection() and getInputStream() work on that URL
        // We can do that by creating a custom URLStreamHandler or a specialized approach.
        // Alternatively, if the code goes through 'openConnection()' on a file URL,
        // you might rely on the file not existing. Usually, that would cause an error, so let's provide a special handler:

        URLStreamHandler testHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return new URLConnection(u) {
                    @Override
                    public void connect() throws IOException {
                        // no-op
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        // Return a dummy input
                        return new ByteArrayInputStream("test data".getBytes());
                    }
                };
            }
        };
        // Create a synthetic URL that uses our 'testHandler'
        URL syntheticUrl = new URL("testproto", "localhost", 0, "/test", testHandler);
        entityMap.put("myPublicId", syntheticUrl);

        // Now let's call
        InputSource source = resolver.resolveEntity("myPublicId", "someSystemId");
        assertNotNull(source, "Expect a valid InputSource when entityURL is not null.");
        assertEquals(syntheticUrl.toExternalForm(), source.getSystemId());
        // We can also check the content if we read from source.getByteStream().
    }

    /**
     * 3) Trigger the IOException in the try block => leads to throw new SAXException(e).
     *    We do this by returning a URL that always fails on openConnection() or getInputStream().
     */
    @Test
    public void testResolveEntityIOException() {
        // Insert a mapping => entityURL != null
        Map<String, URL> entityMap = resolver.getRegisteredEntities();
        // We'll create a special URL that throws on openConnection()
        URLStreamHandler throwingHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                throw new IOException("Simulated I/O error opening connection");
            }
        };
        try {
            URL failingUrl = new URL("failproto", "localhost", 0, "/fail", throwingHandler);
            entityMap.put("failPublicId", failingUrl);

            // When we call resolveEntity => tries to openConnection => IOException => new SAXException(e)
            assertThrows(SAXException.class, () -> {
                resolver.resolveEntity("failPublicId", "someSystemId");
            }, "Should throw SAXException if an IOException occurs reading the input stream.");
        } catch (MalformedURLException e) {
            fail("Unexpected MalformedURLException in test setup: " + e.getMessage());
        }
    }

    /**
     * 1) Test that registerEntityId(...) throws IllegalArgumentException when publicId is null.
     */
    @Test
    public void testRegisterEntityIdNullPublicId() throws MalformedURLException {
        URL dummyUrl = new URL("file:///dummy");

        // Attempting to register with a null publicId => should throw IAE
        assertThrows(IllegalArgumentException.class, () -> {
            resolver.registerEntityId(null, dummyUrl);
        }, "Expected an IllegalArgumentException when publicId is null.");
    }

    /**
     * 2) Test that registerEntityId(...) stores the mapping when publicId is not null.
     *    We verify that the entry is actually placed in getRegisteredEntities().
     */
    @Test
    public void testRegisterEntityIdSuccess() throws MalformedURLException {
        String publicId = "myTestPublicId";
        URL entityUrl = new URL("file:///validPath");

        resolver.registerEntityId(publicId, entityUrl);

        // Assuming getRegisteredEntities() is accessible or there's another way to verify the mapping
        URL storedUrl = resolver.getRegisteredEntities().get(publicId);
        assertNotNull(storedUrl, "Registered URL should not be null in the map.");
        assertEquals(entityUrl, storedUrl, "The stored URL should match the one we passed to registerEntityId.");
    }
}
