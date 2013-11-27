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
import java.io.FileNotFoundException;
import java.io.FileWriter;

import org.opennms.core.xml.CastorUtils;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.config.threshd.ThresholdingConfig;
import org.opennms.netmgt.dao.support.PropertiesGraphDao;
import org.opennms.netmgt.xml.eventconf.Events;
import org.springframework.core.io.FileSystemResource;

/**
 * The Class EventsGenerator.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class EventsGenerator {

    /** The Constant EVENTS_FILENAME. */
    public static final String EVENTS_FILENAME = "Thresholds-Categorized.events.xml";

    /** The UEI processor. */
    private ThresholdEventProcessor eventProcessor;

    /**
     * Instantiates a new threshold events generator.
     *
     * @param eventProcessor the threshold event processor
     */
    public EventsGenerator(ThresholdEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    /**
     * Generate threshold events.
     *
     * @param onmsHome the OpenNMS home directory
     * @throws Exception the exception
     */
    public void generateThresholdEvents(File onmsHome) throws Exception {
        File thresholdsFile = new File(onmsHome, "etc/thresholds.xml");
        if (!thresholdsFile.exists()) {
            throw new FileNotFoundException(thresholdsFile.getAbsolutePath());
        }
        ThresholdingConfig thresholdsConfig = CastorUtils.unmarshal(ThresholdingConfig.class, new FileSystemResource(thresholdsFile), true);

        File graphTemplatesFile = new File(onmsHome, "etc/snmp-graph.properties");
        if (!graphTemplatesFile.exists()) {
            throw new FileNotFoundException(graphTemplatesFile.getAbsolutePath());
        }
        PropertiesGraphDao graphDao = new PropertiesGraphDao();
        graphDao.loadProperties("performance", new FileSystemResource(graphTemplatesFile));

        Events events = eventProcessor.getEvents(thresholdsConfig.getGroupCollection(), graphDao.getAllPrefabGraphs());
        File eventFile = new File(onmsHome, "etc/events/" + EVENTS_FILENAME);
        System.out.println("Generating " + eventFile);
        JaxbUtils.marshal(events, new FileWriter(eventFile));
    }

}
