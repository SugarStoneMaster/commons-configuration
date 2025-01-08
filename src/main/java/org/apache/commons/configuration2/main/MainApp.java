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

package org.apache.commons.configuration2.main;

import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.MergeCombiner;

import java.io.File;

public class MainApp {

    public static void main(String[] args) {
        try {
            // 1. Load Configuration from a .properties file
            String configFilePath = System.getenv().getOrDefault("CONFIG_FILE_PATH", "/app/config.properties");
            File configFile = new File(configFilePath);

            if (!configFile.exists()) {
                System.err.println("Configuration file not found: " + configFilePath);
                System.exit(1);
            }

            System.out.println("Loading configuration from properties file...");
            FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                    new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                            .configure(new Parameters()
                                    .properties()
                                    .setFile(configFile));

            PropertiesConfiguration config = builder.getConfiguration();
            System.out.println("App Name: " + config.getString("app.name", "UnknownApp"));
            System.out.println("App Version: " + config.getString("app.version", "0.0"));

            // 2. Demonstrate Interpolation
            demonstrateInterpolation(config);

            // 3. Work with List Properties
            demonstrateListProperties(config);

            // 4. Demonstrate Hierarchical Configuration
            HierarchicalConfiguration<ImmutableNode> hierarchicalConfig = demonstrateHierarchicalConfiguration();

            // 5. Combined Configuration
            demonstrateCombinedConfiguration(config, hierarchicalConfig);

            // 6. Save Updated Configuration
            System.out.println("\nUpdating and saving the configuration...");
            config.setProperty("app.name", "UpdatedApp");
            builder.save();
            System.out.println("Configuration updated and saved to: " + configFilePath);

        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    private static void demonstrateInterpolation(PropertiesConfiguration config) {
        System.out.println("\nInterpolating dynamic variables...");
        System.out.println("API URL: " + config.getString("api.url", "http://default-url.com"));
    }

    private static void demonstrateListProperties(PropertiesConfiguration config) {
        System.out.println("\nWorking with list properties...");
        config.addProperty("supportedLocales", "en_US");
        config.addProperty("supportedLocales", "fr_FR");
        config.addProperty("supportedLocales", "es_ES");

        for (Object locale : config.getList("supportedLocales")) {
            System.out.println("Supported Locale: " + locale);
        }
    }

    private static HierarchicalConfiguration<ImmutableNode> demonstrateHierarchicalConfiguration() {
        System.out.println("\nDemonstrating hierarchical configuration...");
        HierarchicalConfiguration<ImmutableNode> hierarchicalConfig = new BaseHierarchicalConfiguration();
        hierarchicalConfig.addProperty("database.host", "localhost");
        hierarchicalConfig.addProperty("database.port", 3306);
        hierarchicalConfig.addProperty("database.credentials.username", "root");
        hierarchicalConfig.addProperty("database.credentials.password", "password");

        System.out.println("Database Host: " + hierarchicalConfig.getString("database.host"));
        System.out.println("Database Port: " + hierarchicalConfig.getInt("database.port"));
        System.out.println("Database Username: " + hierarchicalConfig.getString("database.credentials.username"));
        return hierarchicalConfig;
    }

    private static void demonstrateCombinedConfiguration(Configuration config, HierarchicalConfiguration<ImmutableNode> hierarchicalConfig) {
        System.out.println("\nCreating a combined configuration...");
        CombinedConfiguration combinedConfig = new CombinedConfiguration(new MergeCombiner());
        combinedConfig.addConfiguration(config, "PropertiesConfig");
        combinedConfig.addConfiguration(hierarchicalConfig, "HierarchicalConfig");

        System.out.println("Combined App Name: " + combinedConfig.getString("app.name"));
    }
}
