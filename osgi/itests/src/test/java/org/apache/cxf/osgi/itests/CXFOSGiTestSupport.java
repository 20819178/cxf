/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.osgi.itests;



import java.io.File;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.container.internal.JavaVersionUtil;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.options.extra.VMOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

/**
 *
 */
public class CXFOSGiTestSupport {

    @Inject
    protected BundleContext bundleContext;

    protected MavenUrlReference cxfUrl;
    protected MavenUrlReference karafUrl;
    protected MavenUrlReference amqUrl;
    protected MavenUrlReference springLegacyUrl;

    private static String getKarafVersion() {
        return MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf-minimal");
    }

    /**
     * Create an {@link org.ops4j.pax.exam.Option} for using a .
     *
     * @return
     */
    protected Option cxfBaseConfig() {
        karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf-minimal").version(getKarafVersion())
                .type("tar.gz");
        cxfUrl = maven().groupId("org.apache.cxf.karaf").artifactId("apache-cxf").versionAsInProject()
                .type("xml").classifier("features");
        amqUrl = maven().groupId("org.apache.activemq")
                .artifactId("activemq-karaf").type("xml").classifier("features").versionAsInProject();
        springLegacyUrl = maven().groupId("org.apache.karaf.features").artifactId("spring-legacy")
                .version(getKarafVersion()).type("xml").classifier("features");
        String localRepo = System.getProperty("localRepository");
        Object urp = System.getProperty("cxf.useRandomFirstPort");
        final File loggingCfg;
        try {
            loggingCfg = new File(getClass().getResource("/etc/org.ops4j.pax.logging.cfg").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if (JavaVersionUtil.getMajorVersion() >= 9) {
            return composite(karafDistributionConfiguration()
                             .frameworkUrl(karafUrl)
                             .karafVersion(getKarafVersion())
                             .name("Apache Karaf")
                             .useDeployFolder(false)
                             .unpackDirectory(new File("target/paxexam/")),
                         //DO NOT COMMIT WITH THIS LINE ENABLED!!!
                         //KarafDistributionOption.keepRuntimeFolder(),
                         //debugConfiguration(), // nor this
                         systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                         systemProperty("java.awt.headless").value("true"),
                         replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", loggingCfg),
                         when(localRepo != null)
                             .useOptions(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                                                                  "org.ops4j.pax.url.mvn.localRepository",
                                                                  localRepo)),
                             new VMOption("--add-reads=java.xml=java.logging"),
                             new VMOption("--add-exports=java.base/"
                                 + "org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
                             new VMOption("--patch-module"),
                             new VMOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-" 
                             + System.getProperty("karaf.version", "4.2.2") + ".jar"),
                             new VMOption("--patch-module"),
                             new VMOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-" 
                             + System.getProperty("karaf.version", "4.2.2") + ".jar"),
                             new VMOption("--add-opens"),
                             new VMOption("java.base/java.security=ALL-UNNAMED"),
                             new VMOption("--add-opens"),
                             new VMOption("java.base/java.net=ALL-UNNAMED"),
                             new VMOption("--add-opens"),
                             new VMOption("java.base/java.lang=ALL-UNNAMED"),
                             new VMOption("--add-opens"),
                             new VMOption("java.base/java.util=ALL-UNNAMED"),
                             new VMOption("--add-opens"),
                             new VMOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
                             new VMOption("--add-opens"),
                             new VMOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
                             new VMOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
                             new VMOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
                             new VMOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
                             new VMOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
                             new VMOption("-classpath"),
                             new VMOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*"),
                         when(urp != null).useOptions(systemProperty("cxf.useRandomFirstPort").value("true")));
        } else {
            return composite(karafDistributionConfiguration()
                             .frameworkUrl(karafUrl)
                             .karafVersion(getKarafVersion())
                             .name("Apache Karaf")
                             .useDeployFolder(false)
                             .unpackDirectory(new File("target/paxexam/")),
                         //DO NOT COMMIT WITH THIS LINE ENABLED!!!
                         //KarafDistributionOption.keepRuntimeFolder(),
                         //debugConfiguration(), // nor this
                         systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                         systemProperty("java.awt.headless").value("true"),
                         replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", loggingCfg),
                         when(localRepo != null)
                             .useOptions(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                                                                  "org.ops4j.pax.url.mvn.localRepository",
                                                                  localRepo)),
                         when(urp != null).useOptions(systemProperty("cxf.useRandomFirstPort").value("true")));
        }
    }

    protected static Option testUtils() {
        return mavenBundle().groupId("org.apache.cxf").artifactId("cxf-testutils").versionAsInProject();
    }

    protected void assertBundleStarted(String name) {
        Bundle bundle = findBundleByName(name);
        assertNotNull("Bundle " + name + " should be installed", bundle);
        assertEquals("Bundle " + name + " should be started", Bundle.ACTIVE, bundle.getState());
    }

    protected Bundle findBundleByName(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return bundle;
            }
        }
        return null;
    }

    protected void assertBlueprintNamespacePublished(String namespace, long timeout)
            throws InvalidSyntaxException, InterruptedException {
        AbstractServerActivator.awaitService(bundleContext,
                String.format("(&(objectClass=org.apache.aries.blueprint.NamespaceHandler)"
                        + "(osgi.service.blueprint.namespace=%s))", namespace),
                timeout);
    }

}
