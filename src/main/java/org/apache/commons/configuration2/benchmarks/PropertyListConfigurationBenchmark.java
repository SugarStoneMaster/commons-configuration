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

package org.apache.commons.configuration2.benchmarks;
import org.apache.commons.configuration2.plist.PropertyListConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.openjdk.jmh.annotations.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class PropertyListConfigurationBenchmark {

    /**
     * Name of the .plist resource to load.
     */
    @Param({
            "sampleSmall.plist",
            "sampleMedium.plist",
            "sampleLarge.plist"
    })
    private String plistResourceName;

    /**
     * The configuration object loaded once per trial from the specified resource.
     * This is our "base config" that read-only benchmarks can directly use.
     */
    private PropertyListConfiguration baseConfig;

    @Setup(Level.Trial)
    public void setup() throws ConfigurationException, IOException {
        // Load the chosen .plist resource once
        baseConfig = new PropertyListConfiguration();
        try (InputStream is = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(plistResourceName),
                "Resource not found: " + plistResourceName);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            baseConfig.read(reader);
        }
    }

    /**
     * Benchmark: retrieve a property from the base config.
     * We pick a key that definitely exists in each .plist.
     * If sampleLarge.plist is huge, this tests the overhead of config lookups.
     */
    @Benchmark
    public String benchmarkGetProperty() {
        // Access some known key that appears in all .plist variants
        return baseConfig.getString("key1"); // or any property guaranteed to exist
    }

    /**
     * Benchmark: write the configuration to an in-memory buffer.
     * Larger .plist data will cost more to serialize.
     */
    @Benchmark
    public void benchmarkWriteConfiguration() throws ConfigurationException, IOException {
        try (Writer writer = new StringWriter()) {
            baseConfig.write(writer);
        }
    }
}