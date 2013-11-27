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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.config.threshd.Expression;
import org.opennms.netmgt.config.threshd.Threshold;
import org.opennms.netmgt.dao.support.PropertiesGraphDao;
import org.opennms.netmgt.xml.eventconf.Event;
import org.springframework.core.io.FileSystemResource;

/**
 * The Class ThresholdEventTest.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class ThresholdEventTest {

    /** The graph dao. */
    private PropertiesGraphDao graphDao;

    /**
     * Set up the test.
     *
     * @throws Exception the exception
     */
    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging();
        graphDao = new PropertiesGraphDao();
        File graphTemplatesFile = new File("src/test/resources/opennms-home/etc/snmp-graph.properties");
        graphDao.loadProperties("performance", new FileSystemResource(graphTemplatesFile));
        Assert.assertFalse(graphDao.getAllPrefabGraphs().isEmpty());
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
     * Event single threshold.
     *
     * @throws Exception the exception
     */
    @Test
    public void eventSingleThreshold() throws Exception {
        Threshold t = new Threshold();
        t.setType("high");
        t.setTrigger(1);
        t.setValue(10.0);
        t.setRearm(5.0);
        t.setDsType("if");
        t.setDsLabel("ifName");
        t.setDsName("ifInOctets");
        t.setDescription("Test ifInOctets");
        t.setTriggeredUEI("uei.opennms.org/threshold/ifInOctets/major/exceeded");
        t.setRearmedUEI("uei.opennms.org/threshold/ifInOctets/major/rearmed");
        ThresholdEvent te = new ThresholdEvent(t, ThresholdEvent.BASE_UEI, true, true, ThresholdEvent.INSTANCE_INFO, graphDao.getAllPrefabGraphs());
        Event e = te.getEvent();
        System.out.println(JaxbUtils.marshal(e));

        Assert.assertEquals("Major", e.getSeverity());
        Assert.assertEquals("User-defined custom high threshold exceeded event for ifinoctets [Major]", e.getEventLabel());
        Assert.assertTrue(e.getLogmsg().getContent().contains("resourceId=%parm[resourceId]%&reports=mib2.traffic-inout&reports=mib2.bits"));
    }

    /**
     * Event expression threshold.
     *
     * @throws Exception the exception
     */
    @Test
    public void eventExpressionThreshold() throws Exception {
        Expression t = new Expression();
        t.setType("high");
        t.setTrigger(1);
        t.setValue(90.0);
        t.setRearm(85.0);
        t.setDsLabel("ifName");
        t.setDsType("if");
        t.setExpression("((ifInOctets + ifOutOctets) * 8 / ifSpeed) * 100");
        t.setDescription("Test Utilization");
        t.setTriggeredUEI("uei.opennms.org/threshold/interface/utilization/exceeded");
        t.setRearmedUEI("uei.opennms.org/threshold/interface/utilization/rearmed");
        ThresholdEvent te = new ThresholdEvent(t, ThresholdEvent.BASE_UEI, true, true, ThresholdEvent.INSTANCE_INFO, graphDao.getAllPrefabGraphs());
        Event e = te.getEvent();
        System.out.println(JaxbUtils.marshal(e));

        Assert.assertEquals("Warning", e.getSeverity());
        Assert.assertEquals("User-defined custom high threshold exceeded event for interface-utilization [Warning]", e.getEventLabel());
        Assert.assertTrue(e.getLogmsg().getContent().contains("resourceId=%parm[resourceId]%&reports=mib2.traffic-inout&reports=mib2.bits"));
    }

}
