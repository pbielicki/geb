/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package geb.spock

import geb.Browser
import geb.Configuration
import geb.ConfigurationLoader
import groovy.transform.builder.Builder

import java.util.function.Function
import java.util.function.Supplier

class GebTestManager {

    private final static Supplier<String> NULL_STRING_SUPPLIER = { null }

    private final Supplier<String> configurationEnvironmentNameSupplier
    private final Supplier<String> configurationScriptResourcePathSupplier
    private final Supplier<Configuration> configurationSupplier
    private final Supplier<Browser> browserSupplier
    private final Function<String, String> reportLabelCreator

    private Browser browser

    @Builder
    GebTestManager(
            Supplier<String> configurationEnvironmentNameSupplier,
            Supplier<String> configurationScriptResourcePathSupplier, Supplier<Configuration> configurationSupplier,
            Supplier<Browser> browserSupplier, Function<String, String> reportLabelCreator
    ) {
        this.configurationEnvironmentNameSupplier = configurationEnvironmentNameSupplier ?: NULL_STRING_SUPPLIER
        this.configurationScriptResourcePathSupplier = configurationScriptResourcePathSupplier ?: NULL_STRING_SUPPLIER
        this.configurationSupplier = configurationSupplier ?: defaultConfigurationSupplier
        this.browserSupplier = browserSupplier ?: defaultBrowserSupplier
        this.reportLabelCreator = reportLabelCreator
    }

    String getConfigurationEnvironmentName() {
        configurationEnvironmentNameSupplier.get()
    }

    String getConfigurationScriptResourcePath() {
        configurationScriptResourcePathSupplier.get()
    }

    Configuration getConfiguration() {
        configurationSupplier.get()
    }

    Browser getBrowser() {
        if (browser == null) {
            browser = browserSupplier.get()
        }
        browser
    }

    void resetBrowser() {
        def config = browser?.config
        if (config?.autoClearCookies) {
            browser.clearCookiesQuietly()
        }
        if (config?.autoClearWebStorage) {
            browser.clearWebStorage()
        }
        browser = null
    }

    void reportFailure() {
        if (browser) {
            report "failure"
        }
    }

    void reportEnd() {
        if (browser && !browser.config.reportOnTestFailureOnly) {
            report "end"
        }
    }

    void report(String label) {
        browser.report(createReportLabel(label))
    }

    private String createReportLabel(String label = "") {
        if (!reportLabelCreator) {
            throw new IllegalStateException("A reportLabelCreator has not been supplied but report label creation " +
                    "has been requested. To be able to use ${getClass().simpleName} for reporting purposes you need " +
                    "to supply a reportLabelCreator function at creation time.")
        }

        reportLabelCreator.apply(label)
    }

    private Supplier<Configuration> getDefaultConfigurationSupplier() {
        { ->
            def classLoader = new GroovyClassLoader(getClass().classLoader)
            def configLoader = new ConfigurationLoader(configurationEnvironmentName, System.properties, classLoader)

            configLoader.getConf(configurationScriptResourcePath)
        }
    }

    private Supplier<Browser> getDefaultBrowserSupplier() {
        { -> new Browser(configuration) }
    }
}
