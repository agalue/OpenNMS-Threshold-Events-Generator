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
import java.util.Date;

import org.exolab.castor.xml.Marshaller;
import org.opennms.core.xml.CastorUtils;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.config.notifications.Notifications;
import org.opennms.netmgt.config.threshd.ThresholdingConfig;
import org.springframework.core.io.FileSystemResource;

/**
 * The Class NotificationsGenerator.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class NotificationsGenerator {

    /** The UEI processor. */
    private ThresholdEventProcessor eventProcessor;

    /**
     * Instantiates a new notifications generator.
     *
     * @param eventProcessor the event processor
     */
    public NotificationsGenerator(ThresholdEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    /**
     * Generate notifications.
     *
     * @param onmsHome the OpenNMS home directory
     * @param notificationsTemplate the notifications template
     * @throws Exception the exception
     */
    public void generateNotifications(File onmsHome, File notificationsTemplate) throws Exception {
        File thresholdsFile = new File(onmsHome, "etc/thresholds.xml");
        if (!thresholdsFile.exists()) {
            throw new FileNotFoundException(thresholdsFile.getAbsolutePath());
        }
        if (!notificationsTemplate.exists()) {
            throw new FileNotFoundException(notificationsTemplate.getAbsolutePath());
        }

        ThresholdingConfig thresholdsConfig = CastorUtils.unmarshal(ThresholdingConfig.class, new FileSystemResource(thresholdsFile), true);

        Notifications notifications = CastorUtils.unmarshal(Notifications.class, new FileSystemResource(notificationsTemplate), true);
        notifications.getHeader().setCreated(EventConstants.formatToString(new Date()));

        notifications.getNotificationCollection().addAll(eventProcessor.getNotifications(thresholdsConfig.getGroupCollection()));

        File notificationsFile = new File(onmsHome, "etc/notifications.xml");
        System.out.println("Generating " + notificationsFile);
        Marshaller m = new Marshaller(new FileWriter(notificationsFile));
        m.setSuppressNamespaces(false);
        m.marshal(notifications);
    }

}
