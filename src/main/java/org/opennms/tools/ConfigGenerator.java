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
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * The Class ConfigGenerator.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class ConfigGenerator {

    /** The Constant COMMAND_HELP. */
    private static final String COMMAND_HELP =
            "Generate notifications.xml and " + EventsGenerator.EVENTS_FILENAME + " based on the thresholds defined on thresholds.xml\n\n" +
                    "Syntax: java -jar " + getJarName() + " \\\n" +
                    "        --opennms-home <OpenNMS Home Directory> \\\n" +
                    "        [--config <Configuration Properties file>] \\\n" +
                    "        [--notifications <Template for notifications.xml>] \\\n" +
                    "\n" +
                    "    -d|--opennms-home    OpenNMS Home Directory (example: /opt/opennms or /usr/share/opennms)\n" +
                    "    -c|--config          Configuration Properties file\n" +
                    "                         (if not passed, default settings will be used)\n" +
                    "    -n|--notifications   Template for notifications.xml, used as the base content.\n" +
                    "                         (if not passed, notifications.xml won't be generated)\n" +
                    "\n" +
                    "Warnings:\n" +
                    " - This tool requires at least OpenNMS 1.12.2\n" +
                    " - Do not add any custom events on " + EventsGenerator.EVENTS_FILENAME + " they will be overriden\n";

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        Options opts = new Options();
        opts.addOption("d", "opennms-home", true, "OpenNMS Home Directory (example: /opt/opennms or /usr/share/opennms)");
        opts.addOption("n", "notifications", true, "Template with basic content of notifications.xml");
        opts.addOption("c", "config", true, "Configuration File (optional)");

        File onmsHomeDir = null;
        File configFile = null;
        File notificationsFile = null;

        GnuParser parser = new GnuParser();
        try {
            CommandLine cmd = parser.parse(opts, args);
            if (cmd.hasOption('d')) {
                onmsHomeDir = new File(cmd.getOptionValue('d'));
                if (!onmsHomeDir.exists()) {
                    printHelp("OpenNMS Home Directory does not exist.");
                    System.exit(1);
                }
            } else {
                printHelp("You must specify an OpenNMS Home Directory");
                System.exit(1);
            }
            if (cmd.hasOption('c')) {
                configFile = new File(cmd.getOptionValue('c'));
                if (!configFile.exists()) {
                    printHelp("Configuration properties file does not exist.");
                    System.exit(1);
                }
            }
            if (cmd.hasOption('n')) {
                notificationsFile = new File(cmd.getOptionValue('n'));
                if (!notificationsFile.exists()) {
                    printHelp("Template for notifications.xml does not exist.");
                    System.exit(1);
                }
            }
        } catch (ParseException e) {
            printHelp("Failed to parse command line options");
            System.exit(1);
        }

        ThresholdEventProcessor eventProcessor = null;
        if (configFile == null) {
            eventProcessor = new ThresholdEventProcessor();
        } else {
            Properties config = new Properties();
            try {
                config.load(new FileInputStream(configFile));
            } catch (Exception e) {
                printHelp("Can't parse configuration file, " + e.getMessage());
                System.exit(1);
            }
            eventProcessor = new ThresholdEventProcessor(config);
        }

        try {
            System.setProperty("opennms.home", onmsHomeDir.getAbsolutePath());
            EventsGenerator eventsGen = new EventsGenerator(eventProcessor);
            eventsGen.generateThresholdEvents(onmsHomeDir);
            if (notificationsFile != null) {
                NotificationsGenerator notifGen = new NotificationsGenerator(eventProcessor);
                notifGen.generateNotifications(onmsHomeDir, notificationsFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the help.
     *
     * @param msg the message
     */
    public static void printHelp(String msg) {
        System.out.println("Error: " + msg + "\n\n");
        System.out.println(COMMAND_HELP);
    }

    /**
     * Gets the JAR name.
     *
     * @return the JAR name
     */
    private static String getJarName() {
        try {
            File f = new File(ConfigGenerator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.getName();
        } catch (URISyntaxException e) {
            return "[jar-name]";
        }
    }
}
