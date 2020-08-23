/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.ewpesmart.internal.discovery;

import static org.openhab.binding.ewpesmart.internal.EWPESmartBindingConstants.*;

import org.openhab.binding.ewpesmart.internal.device.EWPEDevice;
import org.openhab.binding.ewpesmart.internal.device.EWPEDeviceFinder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Iterator;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link EWPESmartDiscovery} is responsible for discovering supported
 * things.
 *
 * @author Pawel Bogut - Initial contribution
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.ewpesmart")
public class EWPESmartDiscovery extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(EWPESmartDiscovery.class);

    private static final int SEARCH_TIME = 10;

    private @Nullable EWPEDeviceFinder deviceFinder = null;
    private @Nullable DatagramSocket clientSocket = null;

    @Activate
    public EWPESmartDiscovery () {
        super(SUPPORTED_THING_TYPES_UIDS, SEARCH_TIME, true);
        try {
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(DATAGRAM_SOCKET_TIMEOUT);
        } catch (Exception e ) {
            logger.error("EWPESmart: Could not create clientSocket {}", e.getMessage());
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    public void startScan() {
        search();
    }

    private void search() {
        try {
            // Create a new Datagram socket with a specified timeout
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();

            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                logger.trace("EWPESmart: Get {} network interface", ni.getDisplayName());

                List<InterfaceAddress> list = ni.getInterfaceAddresses();
                Iterator<InterfaceAddress> it = list.iterator();

                while (it.hasNext()) {
                    InterfaceAddress ia = it.next();
                    if (ia.getBroadcast() == null) {
                        continue;
                    }
                    logger.trace("EWPESmart: Scan for devices on {} broadcast address", ia.getBroadcast());
                    searchNetwork(ia.getBroadcast());
                }
            }
        } catch (Exception e) {
            logger.error("EWPESmart: Error while scanning for devices {}", e.getMessage());
        }
    }

    private void searchNetwork(InetAddress broadcastIp) throws Exception {
        // Firstly, lets find all Gree Airconditioners on the network
        deviceFinder = new EWPEDeviceFinder(broadcastIp);
        deviceFinder.Scan(clientSocket);

        for (HashMap.Entry<String, EWPEDevice> e : deviceFinder.GetDevices().entrySet()) {
            EWPEDevice device = e.getValue();
            DiscoveryResult discoveryResult = buildDiscoveryResult(device);

            logger.trace("EWPESmart: Discovered thing {}", getDeviceName(device));
            thingDiscovered(discoveryResult);
        }
    }

    private DiscoveryResult buildDiscoveryResult(EWPEDevice device) {
        Map<String, Object> properties = new HashMap<>();

        properties.put("broadcastIp", (device.getBroadcast() + "").replace("/", ""));
        properties.put("ipAddress", (device.getAddress() + "").replace("/", ""));
        properties.put("refresh", 2);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder
            .create(getThingUID(device))
            .withThingType(THING_TYPE_AIRCON)
            .withProperties(properties)
            .withRepresentationProperty("ipAddress")
            .withLabel(getDeviceName(device))
            .build();

        return discoveryResult;
    }

    private String getDeviceName(EWPEDevice device) {
        return ("EWPESmart AirCon " + device.getName()
             + " - " + device.getAddress()).replace("/", "");
    }

    private ThingUID getThingUID(EWPEDevice device) {
        return new ThingUID("ewpesmart:EWPEAirCon:" + device.getId());
    }
}
