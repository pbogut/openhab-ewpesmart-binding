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
package org.openhab.binding.ewpesmart.internal;

import static org.openhab.binding.ewpesmart.internal.EWPESmartBindingConstants.*;

import org.openhab.binding.ewpesmart.internal.device.EWPEDevice;
import org.openhab.binding.ewpesmart.internal.device.EWPEDeviceFinder;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EWPESmartHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Pawel Bogut - Initial contribution
 */
// @NonNullByDefault
public class EWPESmartHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(EWPESmartHandler.class);
    private EWPEDeviceFinder deviceFinder = null;
    private EWPEDevice thisDevice = null;
    private DatagramSocket clientSocket = null;
    private Integer refreshTime = 2;

    private String ipAddress = null;
    private String broadcastAddress = null;

    private EWPESmartConfiguration config;

    public EWPESmartHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Channel update: {}", channelUID.getId());
        try {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            } else if (CHANNEL_POWER.equals(channelUID.getId())) {
                if (command.toString() == "ON") {
                    thisDevice.SetDevicePower(clientSocket, 1);
                } else {
                    thisDevice.SetDevicePower(clientSocket, 0);
                }
                // TODO: handle command
            } else if (CHANNEL_MODE.equals(channelUID.getId())) {
                // TODO: handle command
            } else if (CHANNEL_TURBO.equals(channelUID.getId())) {
                // TODO: handle command
            } else if (CHANNEL_LIGHT.equals(channelUID.getId())) {
                // TODO: handle command
            } else if (CHANNEL_TEMP.equals(channelUID.getId())) {
                // TODO: handle command
            } else if (CHANNEL_SWING_VERTICAL.equals(channelUID.getId())) {
                // TODO: handle command
            } else if (CHANNEL_WIND_SPEED.equals(channelUID.getId())) {
                // TODO: handle command
            } else if (CHANNEL_AIR.equals(channelUID.getId())) {
                // TODO: handle command
            } else if (CHANNEL_DRY.equals(channelUID.getId())) {
                // TODO: handle command
            } else if (CHANNEL_HEALTH.equals(channelUID.getId())) {
                // TODO: handle command
            } else if (CHANNEL_POWER_SAVE.equals(channelUID.getId())) {
                // TODO: handle command
            }

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
            // updateStatus(ThingStatus.ONLINE);
        } catch (Exception e) {
            logger.warn("EWPESmart failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
            // updateStatus(ThingStatus.OFFLINE);
            // e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        logger.debug("EWPESmartHandler for {} is initializing", thing.getUID());

        config = getConfigAs(EWPESmartConfiguration.class);
        logger.debug("EWPESmartHandler config for {} is {}", thing.getUID(), config);

        updateStatus(ThingStatus.UNKNOWN);

        if (!config.isValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
            "Invalid EWPE Smart config. Check configuration.");
        } else {
            // @fixme @todo not working within lambda, not sure why
            // scheduler.execute(() -> {
            //     bindDevice();
            // });
            bindDevice();
        }

        logger.debug("Finished initializing!");
    }


    private void bindDevice() {
        ipAddress = config.getIpAddress();
        refreshTime = config.getRefresh();
        broadcastAddress = config.getBroadcastIp();

        // Now Scan For Airconditioners
        try {
            // First calculate the Broadcast address based on the available interfaces
            InetAddress broadcastIp = InetAddress.getByName(broadcastAddress);

            // Create a new Datagram socket with a specified timeout
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(DATAGRAM_SOCKET_TIMEOUT);

            // Firstly, lets find all Gree Airconditioners on the network
            deviceFinder = new EWPEDeviceFinder(broadcastIp);
            deviceFinder.Scan(clientSocket);
            logger.debug("EWPESmart found {} Devices during scanning", deviceFinder.GetScannedDeviceCount());

            // Now check that this one is amongst the air conditioners that responded.
            thisDevice = deviceFinder.GetDeviceByIPAddress(ipAddress);
            logger.debug("EWPESmart found device {}", thisDevice);
            if (thisDevice != null) {
                // Ok, our device responded
                // Now let's Bind with it
                thisDevice.BindWithDevice(clientSocket);
                if (thisDevice.getIsBound()) {
                    logger.info("EWPESmart AirConditioner Device {} was Succesfully bound", thing.getUID());
                    updateStatus(ThingStatus.ONLINE);

                    // Start the automatic refresh cycles
                    // startAutomaticRefresh();
                    return;
                }
            }
            updateStatus(ThingStatus.ONLINE);
        } catch (UnknownHostException e) {
            logger.debug("EWPESmart failed to scan for airconditioners due to {} ", e.getMessage());
        } catch (IOException e) {
            logger.debug("EWPESmart failed to scan for airconditioners due to {} ", e.getMessage());
        } catch (Exception e) {
            logger.debug("EWPESmart failed to scan for airconditioners due to {} ", e.getMessage());
        }
        updateStatus(ThingStatus.OFFLINE);
    }
}
