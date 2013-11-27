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
import org.opennms.core.xml.CastorUtils;
import org.opennms.netmgt.xml.eventconf.Event;
import org.opennms.netmgt.xml.eventconf.Events;
import org.springframework.core.io.FileSystemResource;

/**
 * The Class ThresholdEventsGeneratorTest.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class EventsGeneratorTest {

    /**
     * Set up the test.
     *
     * @throws Exception the exception
     */
    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging();
        File onmsHome = new File("target/opennms-home");
        if (onmsHome.exists()) {
            FileUtils.deleteDirectory(onmsHome);
        }
        onmsHome.mkdirs();
        System.setProperty("opennms.home", onmsHome.getAbsolutePath());

        File events = new File(onmsHome, "etc/events");
        events.mkdirs();
        FileUtils.copyFile(new File("src/test/resources/opennms-home/etc/thresholds.xml"), new File(onmsHome, "etc/thresholds.xml"));
        FileUtils.copyFile(new File("src/test/resources/opennms-home/etc/snmp-graph.properties"), new File(onmsHome, "etc/snmp-graph.properties"));
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
        File onmsHome = new File(System.getProperty("opennms.home"));
        Assert.assertTrue(onmsHome.exists());
        EventsGenerator generator = new EventsGenerator(new ThresholdEventProcessor());
        generator.generateThresholdEvents(onmsHome);
        File target = new File("target/opennms-home/etc/events/Thresholds-Categorized.events.xml");
        Assert.assertTrue(target.exists());

        Events events = CastorUtils.unmarshal(Events.class, new FileSystemResource(target), true);

        // Problem that has a possible resolution
        Event e = findEvent(events, "uei.opennms.org/threshold/windows/cpu/high/major/exceeded");
        Assert.assertNotNull(e);
        System.err.println(e.getEventLabel());
        System.err.println(e.getLogmsg().getContent());
        Assert.assertEquals("Major", e.getSeverity());
        Assert.assertEquals(1, e.getAlarmData().getAlarmType().intValue());
        Assert.assertTrue(e.getLogmsg().getContent().matches(".*results.htm.*with.*on instance.*"));

        // Problem resolution
        e = findEvent(events, "uei.opennms.org/threshold/windows/cpu/high/major/rearmed");
        Assert.assertNotNull(e);
        System.err.println(e.getEventLabel());
        System.err.println(e.getLogmsg().getContent());
        Assert.assertEquals("Normal", e.getSeverity());
        Assert.assertEquals(2, e.getAlarmData().getAlarmType().intValue());
        Assert.assertTrue(e.getLogmsg().getContent().matches(".*results.htm.*with.*on instance.*"));

        // Problem without possible resolution
        e = findEvent(events, "uei.opennms.org/threshold/apc/ups/vac-in/warning/ac/exceeded");
        Assert.assertNotNull(e);
        System.err.println(e.getEventLabel());
        System.err.println(e.getLogmsg().getContent());
        Assert.assertEquals("Warning", e.getSeverity());
        Assert.assertEquals(3, e.getAlarmData().getAlarmType().intValue());
        Assert.assertTrue(e.getLogmsg().getContent().contains("changed from"));
    }

    /**
     * Find event.
     *
     * @param events the events
     * @param uei the UEI
     * @return the event
     */
    private Event findEvent(Events events, String uei) {
        for (Event e : events.getEventCollection()) {
            if (e.getUei().equals(uei))
                return e;
        }
        return null;
    }

}
