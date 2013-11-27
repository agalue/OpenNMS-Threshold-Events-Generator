OpenNMS-Threshold-Events-Generator
==================================

A tool for generating a nice threshold events definitions (and notifications) based on the current threshold settings.

To compile the tool, use Maven:

```
mvn install
```

This will generate a JAR with dependencies, in order to be able to execute the tool easily, for example:

```
java -jar threshold-events-generator-0.1-jar-with-dependencies.jar
```

The tool generates two files based on the content of thresholds.xml, specially if the threshol definitions contains expects to use custom events (triggeredUEI and rearmedUEI):

* Thresholds-Categorized.events.xml
* notifications.xml [optional]

OpenNMS usually put the custom events for thresholds on etc/events/programmatic.events.xml. In case you have your custom events there please remove them, and remember to add a reference into eventconf.xml to Thresholds-Categorized.events.xml.

Here is some recommendation about how to build the UEI for custom threshold events. This is a guidance and it is not strictly required but highly recommended:

```
uei.opennms.org/threshold/{serverType}/{metricType|purpose}/{thresholdType}/{severity}/{thresholdCondition}
```

where:

* _serverType_ (optional): windows, unix, linux, cisco, juniper, firewall, router, etc.
* _metricType|purpose_: description of the threshold, for example: network/bandwidth/in, disk/utilization
* _thresholdType_: high, low, ac, rc
* _thresholdCondition_: exceeded, rearmed

The tool reads a pre-defined configuration file called config.properties where all the customizations used for instances of generic resources and other customizable fields are defined. It is possible to pass an external file if you want to change some of the customizations or add new entries.

## Features

* The Log Message contains a link to the resource graphs displaying only the graphs affected by the datasources involved on the threshold definition.

* The Description contains a tabular view of all the important threshold parameters customized per threshold type.

* The Alarm Data is generated automatically depending on the threshold type.

## Configuration [Optional]

1. Default prefix for Event UEI

Syntax:

```
baseUei = uei.opennms.org/threshold
```

It is important to be consistent while defining custom events, and all of them should share the same prefix.

2. Use computed expressions

Syntax:

```
useComputedExpression = true|false
```

When building the event description or log message content, the default operation is to compute the "purpose" or "name" of the event from the UEI (which of course requires that all the UEI are formatted like suggested: uei.opennms.org/threshold/{serverType}/{metricType|purpose}/{thresholdType}/{severity}/{thresholdCondition}, and basically the serverType and metricType or purpose are going to be used for this).

If the value is set to false, the name of the datasource (for single thresholds) or the expression formula (for computed thresholds) is going to be used.
 
3. Destination Path

Syntax:

```
destinationPath[XXX] = YYY
```

* XXX, is a regular expression applied to the UEIs.
* YYY, is the name of the destinationPath that will be assigned to the notifications based on the expression XXX

It is possible to define several entries with this syntax.

For example:

```
destinationPath[.*] = Email-Admin
```

4. Resource Instances

Syntax:

```
instance[XXX] = YYY
```

* XXX, is a section of the UEI to be used as substring.
* YYY, is the text associated with the instance

It is possible to define several entries with this syntax.

For example:

```
instance[/network/bandwidth] = interface <b>%parm[ifLabel]%</b> (ifIndex <b>%parm[ifIndex]%</b>)  
instance[/windows/cpu/] = instance <b>%parm[label]%</b>
```

_Default Configuration_

```
baseUei = uei.opennms.org/threshold
useComputedExpression = true

destinationPath[.*] = Email-Admin

instance[if] = interface <b>%parm[ifLabel]%</b> (ifIndex <b>%parm[ifIndex]%</b>)
instance[hrStorageIndex] = disk <b>%parm[label]%</b>
instance[dskIndex] = disk <b>%parm[label]%</b>
```

# Requirements

This tool is based on OpenNMS 1.12.2, and it is _NOT_ going to work on earlier versions.

The tool requires access thresholds.xml from the OpenNMS installation, so it is recommended to execute the tool from the OpenNMS server directly. If the threshold defnitions doesn't have custom UEIs, the tool is going to suggest UEIs and it is going to generate the definitions.

# Sample Event

```
<event>
    <uei>uei.opennms.org/threshold/network/bandwidth/in/high/minor/exceeded</uei>
    <event-label>User-defined custom high threshold exceeded event for network-bandwidth-in [Minor]</event-label>
    <descr>&lt;p&gt;High threshold exceeded for %service% datasource %parm[ds]% on interface %interface% for node %nodelabel% (nodeId %nodeid%).&lt;/p&gt;&lt;br&gt;
        &lt;table style='width:50%; white-space: nowrap;'&gt;
        &lt;tr&gt;&lt;td&gt;&lt;b&gt;Data Source&lt;/b&gt;&lt;/td&gt;&lt;td&gt;%parm[ds]%&lt;/td&gt;&lt;/tr&gt;
        &lt;tr&gt;&lt;td&gt;&lt;b&gt;Resource Label&lt;/b&gt;&lt;/td&gt;&lt;td&gt;%parm[label]%&lt;/td&gt;&lt;/tr&gt;
        &lt;tr&gt;&lt;td&gt;&lt;b&gt;Resource Instance&lt;/b&gt;&lt;/td&gt;&lt;td&gt;%parm[instance]%&lt;/td&gt;&lt;/tr&gt;
        &lt;tr&gt;&lt;td&gt;&lt;b&gt;Current Metric Value&lt;/b&gt;&lt;/td&gt;&lt;td&gt;%parm[value]%&lt;/td&gt;&lt;/tr&gt;
        &lt;tr&gt;&lt;td&gt;&lt;b&gt;Threshold Value&lt;/b&gt;&lt;/td&gt;&lt;td&gt;%parm[threshold]%&lt;/td&gt;&lt;/tr&gt;
        &lt;tr&gt;&lt;td&gt;&lt;b&gt;Rearm Value&lt;/b&gt;&lt;/td&gt;&lt;td&gt;%parm[rearm]%&lt;/td&gt;&lt;/tr&gt;
        &lt;tr&gt;&lt;td&gt;&lt;b&gt;Trigger Value&lt;/b&gt;&lt;/td&gt;&lt;td&gt;%parm[trigger]%&lt;/td&gt;&lt;/tr&gt;
        &lt;/table&gt;
        &lt;/br&gt;&lt;p&gt;All parameters: %parm[all]%&lt;/p&gt;</descr>
    <logmsg dest="logndisplay">&lt;b&gt;&lt;a href='graph/results.htm?resourceId=%parm[resourceId]%&amp;reports=all'&gt;NETWORK-BANDWIDTH-IN&lt;/b&gt;&lt;/a&gt; HIGH threshold &lt;b&gt;%parm[threshold]%&lt;/b&gt; exceeded with &lt;font color=#cc0000&gt;&lt;b&gt;%parm[value]%&lt;/b&gt;&lt;/font&gt;, on interface &lt;b&gt;%parm[ifLabel]%&lt;/b&gt; (ifIndex &lt;b&gt;%parm[ifIndex]%&lt;/b&gt;), for metric %parm[ds]%, on node %nodelabel%.</logmsg>
    <severity>Minor</severity>
    <alarm-data reduction-key="%uei%:%dpname%:%nodeid%:%interface%:%parm[ds]%:%parm[threshold]%:%parm[trigger]%:%parm[rearm]%:%parm[label]%" alarm-type="1" auto-clean="false"/>
</event>
```


