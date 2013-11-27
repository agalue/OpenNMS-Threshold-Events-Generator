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

import org.opennms.netmgt.config.notifications.Notification;

/**
 * The Class ThresholdNotification.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class ThresholdNotification {

    /** The notification. */
    private Notification notification;
    
    /** The event. */
    private ThresholdEvent event;

    /**
     * Instantiates a new threshold notification.
     *
     * @param event the threshold event object
     * @param destinationPath the destination path
     */
    public ThresholdNotification(ThresholdEvent event, String destinationPath) {
        this.event = event;
        notification = new Notification();
        notification.setStatus("on");
        notification.setWriteable("yes");
        notification.setUei(event.getEventUei());
        notification.setRule("IPADDR != '0.0.0.0'");
        notification.setDestinationPath(destinationPath);
        notification.setTextMessage("%logmsg%");
        updateName();
        updateSubject();
        updateNumericMessage();
    }

    /**
     * Updates the name.
     */
    private void updateName() {
        StringBuffer sb = new StringBuffer();
        sb.append("TH-");
        sb.append(event.getThresholdExpression()).append(" ");
        sb.append(event.getThresholdId()).append(" ");
        sb.append(event.getEventSeverity().toUpperCase());
        sb.append(" notification");
        sb.append(event.isExceeded() ? "" : " Rearmed");
        notification.setName(sb.toString());
    }

    /**
     * Updates the subject.
     */
    private void updateSubject() {
        StringBuffer sb = new StringBuffer();
        sb.append("[TH]");
        sb.append("[").append(event.getThresholdId()).append("]");
        sb.append(" #%noticeid%: %nodelabel% - ");
        sb.append(event.getThresholdExpression());
        sb.append(" %parm[ds]% ");
        sb.append(event.getThresholdEventType()).append(".");
        notification.setSubject(sb.toString());
    }

    /**
     * Updates the numeric message.
     *
     */
    private void updateNumericMessage() {
        StringBuffer sb = new StringBuffer();
        sb.append("[").append(event.getThresholdId()).append("]");
        sb.append(" - (%parm[ds]% %parm[threshold]%/%parm[value]%) ");
        sb.append(event.getThresholdEventType()).append(".");
        notification.setNumericMessage(sb.toString());
    }

    /**
     * Gets the notification.
     *
     * @return the notification
     */
    public Notification getNotification() {
        return notification;
    }
}
