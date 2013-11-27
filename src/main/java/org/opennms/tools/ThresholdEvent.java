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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.jexl2.ExpressionImpl;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.lang.StringUtils;
import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.config.threshd.Basethresholddef;
import org.opennms.netmgt.config.threshd.Expression;
import org.opennms.netmgt.config.threshd.Threshold;
import org.opennms.netmgt.model.PrefabGraph;
import org.opennms.netmgt.xml.eventconf.AlarmData;
import org.opennms.netmgt.xml.eventconf.Event;
import org.opennms.netmgt.xml.eventconf.Logmsg;

/**
 * The Class ThresholdEvent.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class ThresholdEvent implements Comparable<ThresholdEvent> {

    /** The Constant BASE_UEI. */
    public static final String BASE_UEI = "uei.opennms.org/threshold";

    /** The Constant INSTANCE_INFO. */
    public static final String INSTANCE_INFO = "instance <b>%parm[label]%</b>";

    /** The base UEI. */
    private String baseUei;

    /** The graph link. */
    private String graphLink = "graph/results.htm?resourceId=%parm[resourceId]%&reports=all";

    /** The threshold id. */
    private String thresholdId;

    /** The threshold name. */
    private String thresholdName;

    /** The threshold parameter. */
    private String thresholdParam;

    /** The threshold expression. */
    private String thresholdExpression;

    /** The threshold description. */
    private String thresholdDescription;

    /** The threshold instance information format. */
    private String instanceInfo;

    /** The exceeded flag (true if the event will be related with a threshold exceeded, false if it is related with a threshold rearmed. */
    private boolean exceeded = false;

    /** The event object. */
    private Event event = new Event();

    /** The use computed threshold expression. */
    private boolean useComputedThresholdExpression = false;

    /** The metric list. */
    private List<String> metricList = new ArrayList<String>();

    /** The metric templates. */
    private List<PrefabGraph> metricTemplates;

    /**
     * Instantiates a new threshold event.
     *
     * @param def the base threshold definition
     * @param baseUei the base UEI
     * @param isExceeded the is exceeded
     * @param useComputedThresholdExpression the use computed threshold expression
     * @param instanceInfo the instance information
     * @param metricTemplates the metric templates
     */
    public ThresholdEvent(Basethresholddef def, String baseUei, boolean isExceeded, boolean useComputedThresholdExpression, String instanceInfo, List<PrefabGraph> metricTemplates) {
        this.baseUei = baseUei;
        this.metricTemplates = metricTemplates;
        this.useComputedThresholdExpression = useComputedThresholdExpression;
        if (!def.getDsType().equals("node")) { // Instance doesn't apply to node resources
            this.instanceInfo = instanceInfo;
        }
        updateMetrics(def);
        updateBaseParams(def, isExceeded);
    }

    /**
     * Update metrics.
     *
     * @param def the base threshold definition
     */
    private void updateMetrics(Basethresholddef def) {
        metricList.clear();
        if (def instanceof Threshold) {
            String dsName = ((Threshold)def).getDsName();
            thresholdExpression = dsName;
            metricList.add(dsName);
        } else {
            thresholdExpression = ((Expression)def).getExpression();
            JexlEngine expressionParser = new JexlEngine();
            try {
                ExpressionImpl e = (ExpressionImpl) expressionParser.createExpression(thresholdExpression);
                for (List<String> list : e.getVariables()) {
                    if (list.get(0).equalsIgnoreCase("math")) {
                        continue;
                    }
                    if (list.get(0).equalsIgnoreCase("datasources")) {
                        metricList.add(list.get(1).intern());
                    } else {
                        metricList.add(list.get(0).intern());
                    }
                }
            } catch (Exception e) {
                LogUtils.errorf(this, "Can't parse expression %s.", thresholdExpression);
            }
        }
    }

    /**
     * Update base parameters.
     *
     * @param def the base threshold definition
     * @param isExceeded the is exceeded
     */
    private void updateBaseParams(Basethresholddef def, boolean isExceeded) {
        exceeded = isExceeded;

        if (exceeded) {
            event.setUei(def.getTriggeredUEI());
        } else {
            event.setUei(def.getRearmedUEI());
        }
        if (def.getDescription() != null) {
            thresholdDescription = def.getDescription();
        }
        thresholdId = def.getType();

        updateThresholdName();
        updateThresholdParam();
        updateGraphLink();

        updateEventSeverity();
        updateEventLabel();
        updateEventLogMessage();
        updateEventDescription();
        updateEventAlarmData();
    }

    /**
     * Update threshold name.
     *
     * @throws IllegalArgumentException the illegal argument exception
     */
    private void updateThresholdName() throws IllegalArgumentException {
        if (thresholdId == null) {
            throw new IllegalArgumentException("threshold-type cannot be null");
        } else if (thresholdId.equals("high")) {
            thresholdName = "High";
        } else if (thresholdId.equals("low")) {
            thresholdName = "Low";
        } else if (thresholdId.equals("absoluteChange")) {
            thresholdName = "Absolute Change";
        } else if (thresholdId.equals("relativeChange")) {
            thresholdName = "Relative Change";
        } else if (thresholdId.equals("rearmingAbsoluteChange")) {
            thresholdName = "Rearming Absolute Change";
        } else {
            throw new IllegalArgumentException("Invalid threshold-type " + thresholdId);
        }
    }

    /**
     * Update threshold parameter.
     */
    private void updateThresholdParam() {
        if (thresholdId.equals("absoluteChange")) {
            thresholdParam = "%parm[changeThreshold]%";
        } else if (thresholdId.equals("relativeChange")) {
            thresholdParam = "%parm[multiplier]%";
        } else {
            thresholdParam = "%parm[threshold]%";
        }
    }

    /**
     * Update graph link.
     */
    private void updateGraphLink() {
        if (metricTemplates != null) {
            List<String> templates = new ArrayList<String>();
            for (PrefabGraph graph : metricTemplates) {
                boolean found = false;
                for (String c : graph.getColumns()) {
                    if (metricList.contains(c)) {
                        found = true;
                        continue;
                    }
                }
                if (found) {
                    templates.add(graph.getName());
                }
            }
            if (!templates.isEmpty()) {
                graphLink = graphLink.replaceAll("all", StringUtils.join(templates, "&reports="));
            }
        }
    }

    /**
     * Update event severity.
     */
    private void updateEventSeverity() {
        String eventSeverity = "Normal";
        String eventUei = event.getUei().toLowerCase();
        if (exceeded) {
            eventSeverity = "Warning";
            if (eventUei.contains("minor")) {
                eventSeverity = "Minor";
            } else if (eventUei.contains("major")) {
                eventSeverity = "Major";
            } else if (eventUei.contains("critical")) {
                eventSeverity = "Critical";
            }
        }
        event.setSeverity(eventSeverity);
    }

    /**
     * Update event label.
     */
    private void updateEventLabel() {
        StringBuffer sb = new StringBuffer();
        sb.append("User-defined custom ");
        sb.append(thresholdName.toLowerCase());
        sb.append(" threshold ");
        sb.append(getThresholdEventType());
        sb.append(" event for ");
        sb.append(getThresholdExpression().toLowerCase());
        if (exceeded) {
            sb.append(" [").append(event.getSeverity()).append("]");
        }
        event.setEventLabel(sb.toString());
    }

    /**
     * Update event log message.
     */
    private void updateEventLogMessage() {
        StringBuffer sb = new StringBuffer();
        sb.append("<b><a href='").append(graphLink).append("'>").append(getThresholdExpression()).append("</b></a> ");
        sb.append(thresholdName).append(" threshold ");
        sb.append("<b>").append(thresholdParam).append("</b> ").append(getThresholdEventType());
        if (thresholdId.equals("absoluteChange") || thresholdId.equals("relativeChange")) {
            sb.append(" changed from %parm[previousValue]% to ");
        } else {
            sb.append(" with ");
        }
        sb.append("<font color=").append(exceeded ? "#cc0000" : "#4e9a06").append("><b>%parm[value]%</b></font>");
        if (instanceInfo != null && !instanceInfo.trim().equals("")) {
            sb.append(", on ").append(instanceInfo);
        }
        sb.append(", for metric %parm[ds]%, on node %nodelabel%.");
        event.setLogmsg(new Logmsg());
        event.getLogmsg().setDest("logndisplay");
        event.getLogmsg().setContent(sb.toString());
    } 

    /**
     * Update event description.
     */
    private void updateEventDescription() {
        StringBuffer sb = new StringBuffer();
        sb.append("<p>");
        sb.append(thresholdName).append(" threshold ").append(getThresholdEventType());
        sb.append(" for %service% datasource %parm[ds]% on interface %interface% for node %nodelabel% (nodeId %nodeid%).");
        sb.append("</p>");
        if (thresholdDescription != null) {
            sb.append("<p><b>Description:</b> ").append(thresholdDescription).append("</p>");
        }
        sb.append("<br>\n");
        sb.append("        <table style='width:50%; white-space: nowrap;'>\n");
        sb.append("        <tr><td><b>").append("Data Source").append("</b></td><td>").append("%parm[ds]%").append("</td></tr>\n");
        sb.append("        <tr><td><b>").append("Resource Label").append("</b></td><td>").append("%parm[label]%").append("</td></tr>\n");
        sb.append("        <tr><td><b>").append("Resource Instance").append("</b></td><td>").append("%parm[instance]%").append("</td></tr>\n");
        sb.append("        <tr><td><b>").append("Resource ID").append("</b></td><td>").append("%parm[resourceId]%").append("</td></tr>\n");
        sb.append("        <tr><td><b>").append("Current Metric Value").append("</b></td><td>").append("%parm[value]%").append("</td></tr>\n");
        if (thresholdId.equals("absoluteChange")) {
            sb.append("        <tr><td><b>").append("Change Threshold").append("</b></td><td>").append("%parm[changeThreshold]%").append("</td></tr>\n");
            sb.append("        <tr><td><b>").append("Previous Value").append("</b></td><td>").append("%parm[previousValue]%").append("</td></tr>\n");            
            sb.append("        <tr><td><b>").append("Trigger Value").append("</b></td><td>").append("%parm[trigger]%").append("</td></tr>\n");
        } else if (thresholdId.equals("relativeChange")) {
            sb.append("        <tr><td><b>").append("Multiplier (Threshold Value)").append("</b></td><td>").append("%parm[multiplier]%").append("</td></tr>\n");
            sb.append("        <tr><td><b>").append("Previous Value").append("</b></td><td>").append("%parm[previousValue]%").append("</td></tr>\n");            
        } else if (thresholdId.equals("rearmingAbsoluteChange")) {
            sb.append("        <tr><td><b>").append("Threshold Value").append("</b></td><td>").append("%parm[threshold]%").append("</td></tr>\n");
            sb.append("        <tr><td><b>").append("Previous Value").append("</b></td><td>").append("%parm[previousValue]%").append("</td></tr>\n");            
            sb.append("        <tr><td><b>").append("Trigger Value").append("</b></td><td>").append("%parm[trigger]%").append("</td></tr>\n");
        } else {
            sb.append("        <tr><td><b>").append("Threshold Value").append("</b></td><td>").append("%parm[threshold]%").append("</td></tr>\n");
            sb.append("        <tr><td><b>").append("Rearm Value").append("</b></td><td>").append("%parm[rearm]%").append("</td></tr>\n");            
            sb.append("        <tr><td><b>").append("Trigger Value").append("</b></td><td>").append("%parm[trigger]%").append("</td></tr>\n");
        }
        sb.append("        </table>\n");
        sb.append("        </br><p>All parameters: %parm[all]%</p>");
        event.setDescr(sb.toString());
    }

    /**
     * Update event alarm data.
     */
    private void updateEventAlarmData() {
        AlarmData data = new AlarmData();
        String baseKey = ":%dpname%:%nodeid%:%interface%:%parm[ds]%:" + thresholdParam + ":%parm[trigger]%:%parm[rearm]%:%parm[label]%";
        if (exceeded) {
            data.setAutoClean(false);
            if (thresholdId.equals("absoluteChange") || thresholdId.equals("relativeChange")) {
                data.setAlarmType(3);
            } else {
                data.setAlarmType(1);
            }
            data.setReductionKey("%uei%" + baseKey);
        } else {
            data.setAutoClean(false);
            data.setAlarmType(2);
            data.setReductionKey("%uei%" + baseKey);
            String exceeded = event.getUei().replaceFirst("rearmed", "exceeded");
            data.setClearKey(exceeded + baseKey);
        }
        if (data.getReductionKey() != null) {
            event.setAlarmData(data);
        }
    }

    /**
     * Checks if is exceeded.
     *
     * @return true, if is exceeded. false, if it is rearmed
     */
    protected boolean isExceeded() {
        return exceeded;
    }

    /**
     * Gets the event severity.
     *
     * @return the event severity
     */
    protected String getEventSeverity() {
        return event.getSeverity();
    }

    /**
     * Gets the threshold id.
     *
     * @return the threshold id
     */
    protected String getThresholdId() {
        return thresholdId;
    }

    /**
     * Gets the threshold type.
     *
     * @return the threshold type
     */
    protected String getThresholdEventType() {
        return exceeded ? "exceeded" : "rearmed";
    }

    /**
     * Gets the metric type.
     *
     * @return the metric type
     */
    protected String getThresholdExpression() {
        if (useComputedThresholdExpression) {
            List<String> elements = new ArrayList<String>();
            String[] sections = event.getUei().replace(baseUei + '/', "").split("\\/");
            for (String section : sections) {
                if (!section.toLowerCase().matches("(?i)(high|low|ac|rc|absolutechange|relativechange|exceed.+|rearm.+|" + event.getSeverity() + ")")) {
                    elements.add(section);
                }
            }
            return StringUtils.join(elements, "-").toUpperCase();
        }
        return thresholdExpression;
    }

    /**
     * Gets the base UEI.
     *
     * @return the base UEI
     */
    public String getBaseUei() {
        return baseUei;
    }

    /**
     * Gets the UEI.
     *
     * @return the UEI
     */
    public String getEventUei() {
        return event.getUei();
    }

    /**
     * Gets the event.
     *
     * @return the event
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Compare to.
     *
     * @param e the threshold event
     * @return the int
     */
    public int compareTo(ThresholdEvent e) {
        return getEventUei().compareTo(e.getEventUei());
    }

}
