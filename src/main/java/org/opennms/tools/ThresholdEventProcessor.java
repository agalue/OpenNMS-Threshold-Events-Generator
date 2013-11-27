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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.config.notifications.Notification;
import org.opennms.netmgt.config.threshd.Basethresholddef;
import org.opennms.netmgt.config.threshd.Expression;
import org.opennms.netmgt.config.threshd.Group;
import org.opennms.netmgt.config.threshd.Threshold;
import org.opennms.netmgt.model.PrefabGraph;
import org.opennms.netmgt.xml.eventconf.Events;
import org.opennms.netmgt.xml.eventconf.Event;

/**
 * The Class ThresholdEventProcessor.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class ThresholdEventProcessor {

    /** The Constant BASE_URI. */
    public static final String BASE_URI = "uei.opennms.org/threshold";

    /** The configuration properties. */
    private Properties config = new Properties();

    /** The instances map. */
    private Map<String,String> instancesMap = new HashMap<String,String>();

    /** The destination paths map. */
    private Map<String,String> destinationsMap = new HashMap<String,String>();

    /**
     * Instantiates a new UEI processor.
     */
    public ThresholdEventProcessor() {
        try {
            config.load(getClass().getResourceAsStream("/config.properties"));
            parseConfig();
        } catch (IOException e) {
            LogUtils.errorf(this, "Can't load config.properties");
        }
    }

    /**
     * Instantiates a new UEI processor.
     *
     * @param config the configuration
     */
    public ThresholdEventProcessor(Properties config) {
        this.config = config;
        parseConfig();
    }

    /**
     * Parses the configuration.
     */
    private void parseConfig() {
        for (Object o : config.keySet()) {
            String key = (String) o;
            if (key.startsWith("instance")) {
                String ueiSubstring = key.replace("instance[", "").replace("]", "");
                instancesMap.put(ueiSubstring, config.getProperty(key));
            }
            if (key.startsWith("destinationPath")) {
                String ueiSubstring = key.replace("destinationPath[", "").replace("]", "");
                destinationsMap.put(ueiSubstring, config.getProperty(key));
            }
        }
    }

    /**
     * Gets the Events object.
     *
     * @param groups the groups
     * @param metrics the metrics
     * @return the Events object
     */
    public Events getEvents(List<Group> groups, List<PrefabGraph> metrics) {
        Set<ThresholdEvent> list = getThresholdEvents(groups, metrics);
        Events events = new Events();
        for (ThresholdEvent e : list) {
            events.getEventCollection().add(e.getEvent());
        }
        Collections.sort(events.getEventCollection(), new Comparator<Event>() {
            public int compare(Event e0, Event e1) {
                return e0.getUei().compareTo(e1.getUei());
            }
        });
        return events;
    }

    /**
     * Gets the notifications.
     *
     * @param groups the groups
     * @return the notifications
     */
    public List<Notification> getNotifications(List<Group> groups) {
        List<Notification> notifications = new ArrayList<Notification>();
        for (ThresholdEvent e : getThresholdEvents(groups, null)) {
            ThresholdNotification n = new ThresholdNotification(e, getDestinationPath(e.getEventUei()));
            notifications.add(n.getNotification());
        }
        Collections.sort(notifications, new Comparator<Notification>() {
            public int compare(Notification n1, Notification n2) {
                return n1.getName().compareTo(n2.getName());
            }
        });
        return notifications;
    }

    /**
     * Gets the threshold events.
     *
     * @param groups the groups
     * @param metrics the metrics
     * @return the threshold events
     */
    private Set<ThresholdEvent> getThresholdEvents(List<Group> groups, List<PrefabGraph> metrics) {
        Set<ThresholdEvent> events = new TreeSet<ThresholdEvent>();
        boolean useComputedExpression = Boolean.parseBoolean(config.getProperty("useComputedExpression", "true"));
        String baseUei = config.getProperty("baseUei", ThresholdEvent.BASE_UEI);
        for (Group g : groups) {
            for (Threshold t : g.getThresholdCollection()) {
                if (t.getTriggeredUEI() == null || t.getTriggeredUEI().trim().equals("")) {
                    generateTriggeredUei(t, baseUei, t.getDsName());
                    LogUtils.warnf(this, "There is no TriggeredUEI for threshold %s using threshold %s on group %s; using %s", t.getType(), t.getDsName(), g.getName(), t.getTriggeredUEI());
                }
                events.add(new ThresholdEvent(t, baseUei, true, useComputedExpression, getInstanceInfo(t.getDsType()), metrics));
                if (shouldAddRearm(t, g.getName())) {
                    events.add(new ThresholdEvent(t, baseUei, false, useComputedExpression, getInstanceInfo(t.getDsType()), metrics));
                }
            }
            for (Expression ex : g.getExpressionCollection()) {
                if (ex.getTriggeredUEI() == null || ex.getTriggeredUEI().trim().equals("")) {
                    generateTriggeredUei(ex, baseUei, g.getName());
                    LogUtils.warnf(this, "There is no TriggeredUEI for threshold %s using expression '%s' on group %s; using %s", ex.getType(), ex.getExpression(), g.getName(), ex.getTriggeredUEI());
                }
                events.add(new ThresholdEvent(ex, baseUei, true, useComputedExpression, getInstanceInfo(ex.getDsType()), metrics));
                if (shouldAddRearm(ex, g.getName())) {
                    events.add(new ThresholdEvent(ex, baseUei, false, useComputedExpression, getInstanceInfo(ex.getDsType()), metrics));
                }
            }
        }
        return events;
    }

    /**
     * Checks if a rearm event must be added or not.
     *
     * @param def the base threshold definition
     * @param groupName the group name
     * @return true, if successful
     */
    private boolean shouldAddRearm(Basethresholddef def, String groupName) {
        boolean addRearm = true;
        if (def.getRearmedUEI() == null || def.getRearmedUEI().trim().equals("")) {
            if (def.getType().matches("(absolute|relative)Change")) {
                addRearm = false;
            } else {
                generateRearmedUei(def);
                String expression = def instanceof Threshold ? ((Threshold)def).getDsName() : ((Expression)def).getExpression();
                LogUtils.warnf(this, "There is no RearmedUEI for threshold %s using expression '%s' on group %s; using %s", def.getType(), expression, groupName, def.getRearmedUEI());
            }
        }
        return addRearm;
    }

    /**
     * Generate rearmed UEI.
     *
     * @param def the base threshold definition
     * @throws IllegalArgumentException the illegal argument exception
     */
    private void generateRearmedUei(Basethresholddef def) throws IllegalArgumentException {
        if (def.getTriggeredUEI() == null) {
            throw new IllegalArgumentException("TriggeredUEI cannot be null");
        }
        def.setRearmedUEI(def.getTriggeredUEI().replaceFirst("exceed", "rearm"));
    }

    /**
     * Generate triggered UEI.
     *
     * @param def the base threshold definition
     * @param baseUei the base UEI
     * @param metric the metric
     */
    private void generateTriggeredUei(Basethresholddef def, String baseUei, String metric) {
        def.setTriggeredUEI(baseUei + "/" + def.getType() + "/" + def.getDsType() + "/" + metric + "/exceeded");
    }

    /**
     * Gets the instance info.
     *
     * @param uei the Event UEI
     * @return the instance info
     */
    protected String getInstanceInfo(String uei) {
        for (Entry<String,String> entry : instancesMap.entrySet()) {
            if (uei.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return ThresholdEvent.INSTANCE_INFO;
    }

    /**
     * Gets the destination path.
     *
     * @param uei the Event UEI
     * @return the destination path
     */
    protected String getDestinationPath(String uei) {
        for (Entry<String,String> entry : destinationsMap.entrySet()) {
            Pattern p = Pattern.compile(entry.getKey());
            Matcher m = p.matcher(uei);
            if (m.matches()) {
                return entry.getValue();
            }
        }
        return "Email-Admin";
    }

}
