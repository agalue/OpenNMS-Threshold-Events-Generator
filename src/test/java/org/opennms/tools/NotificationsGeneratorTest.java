/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2013 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2013 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.tools;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opennms.core.test.MockLogAppender;

/**
 * The Class NotificationsGeneratorTest.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class NotificationsGeneratorTest {

    /**
     * Set up the test.
     *
     * @throws Exception the exception
     */
    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging();
        File configDir = new File("target/opennms-home/etc");
        if (configDir.exists()) {
            FileUtils.deleteDirectory(configDir);
        }
        configDir.mkdirs();
        FileUtils.copyFile(new File("src/test/resources/opennms-home/etc/thresholds.xml"), new File(configDir, "thresholds.xml"));
    }

    /**
     * Tears down the test.
     *
     * @throws Exception the exception
     */
    @After
    public void tearDown() throws Exception {
        MockLogAppender.assertNoWarningsOrGreater();
    }

    /**
     * Test generator.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGenerator() throws Exception {
        File onmsHome = new File("target/opennms-home");
        Assert.assertTrue(onmsHome.exists());
        NotificationsGenerator generator = new NotificationsGenerator(new ThresholdEventProcessor());
        File notificationsTemplate = new File("src/test/resources/notifications.xml");
        generator.generateNotifications(onmsHome, notificationsTemplate);
        File target = new File("target/opennms-home/etc/notifications.xml");

        // FIXME Validate notification content

        Assert.assertTrue(target.exists());
    }

}
