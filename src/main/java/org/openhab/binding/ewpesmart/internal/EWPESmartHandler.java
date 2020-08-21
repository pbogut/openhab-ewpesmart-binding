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
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EWPESmartHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Pawel Bogut - Initial contribution
 */
@NonNullByDefault
public class EWPESmartHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(EWPESmartHandler.class);
    private @Nullable EWPEDeviceFinder deviceFinder = null;
    private @Nullable EWPEDevice thisDevice = null;
    private @Nullable DatagramSocket clientSocket = null;
    private Integer refreshTime = 2;
    private boolean isRefreshing = false;
    private @Nullable ScheduledFuture<?> refreshTask;

    private String ipAddress = "";
    private String broadcastAddress = "";
    private long lastRefreshTime = 0;

    private @Nullable EWPESmartConfiguration config;

    public EWPESmartHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Channel update: {}", channelUID.getId());
        // due to timeouts that happens often lets try to update few times
        int tryNo = 1;
        while(true) {
            try {
                doHandleCommand(channelUID, command);
                break;
            } catch (SocketTimeoutException e) {
                logger.debug("EWPESmart: failed to send command to airconditioners due to Timeout, try no. {}", tryNo);
                if (tryNo >= SEND_MESSAGE_TRIES) {
                    logger.warn("EWPESmart failed to update channel {} due to connection timeout after {} tries", channelUID.getId(), e.getMessage(), tryNo);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Could not control device due to multiple connection timeouts.");
                    break; // just give up
                }
                tryNo++;
            } catch (Exception e) {
                logger.warn("EWPESmart failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                // updateStatus(ThingStatus.OFFLINE);
                // e.printStackTrace();
                break;
            }
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
            scheduler.execute(() -> {
                int tryNo = 1;
                while(true) {
                    try {
                        bindDevice();
                        break;
                    } catch (SocketTimeoutException e) {
                        logger.debug("EWPESmart: failed to scan for airconditioners due to Timeout, try no. {}", tryNo);
                        if (tryNo >= BIND_DEVICE_TRIES) {
                            logger.warn("EWPESmart failed to bind device thing.getUID() due to connection timeout after {} tries", e.getMessage(), tryNo);
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                    "Could not bind device due to multiple connection timeouts.");
                            break; // just give up
                        }
                        tryNo++;
                    }
                }
            });
        }

        logger.debug("Finished initializing!");
    }

    private void bindDevice() throws SocketTimeoutException {
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
                    startAutomaticRefresh();
                    return;
                }
            }
            updateStatus(ThingStatus.ONLINE);
        } catch (SocketTimeoutException e) {
            // bubble up so we can retry
            throw e;
        } catch (UnknownHostException e) {
            logger.debug("EWPESmart failed to scan for airconditioners due to {} ({})", e.getMessage(), e.getClass());
        } catch (IOException e) {
            logger.debug("EWPESmart failed to scan for airconditioners due to {} ({})", e.getMessage(), e.getClass());
        } catch (Exception e) {
            logger.debug("EWPESmart failed to scan for airconditioners due to {} ({})", e.getMessage(), e.getClass());
        }
        updateStatus(ThingStatus.OFFLINE);
        // updateStatus(ThingStatus.ONLINE);
    }

    private void startAutomaticRefresh() {
        Runnable refresher = new Runnable() {
            @Override
            public void run() {
                if (thisDevice == null || !thisDevice.getIsBound()) {
                    return;
                }

                try {
                    logger.debug("EWPESmart executing automatic update of values");
                    // safeguard for multiple REFRESH commands
                    if (isMinimumRefreshTimeExceeded() && !isRefreshing) {
                        isRefreshing = true;
                        logger.debug("Fetching status values from device.");
                        // Get the current status from the Airconditioner
                        thisDevice.getDeviceStatus(clientSocket);
                    } else {
                        logger.debug(
                                "Skipped fetching status values from device because minimum refresh time not reached");
                    }

                    // Update All Channels
                    List<Channel> channels = getThing().getChannels();
                    for (Channel channel : channels) {
                        publishChannelIfLinked(channel.getUID());
                    }

                } catch (Exception e) {
                    logger.debug("EWPESmart failed during automatic update of airconditioner values due to {} ({}) ", e.getMessage(), e.getClass());
                } finally {
                    isRefreshing = false;
                }

                logger.debug("EWPESmart refresh");
            }
        };

        refreshTask = scheduler.scheduleWithFixedDelay(refresher, 0, refreshTime.intValue(), TimeUnit.SECONDS);
        logger.debug("Start EWPESmart automatic refresh with {} second intervals", refreshTime.intValue());
    }

    private boolean isMinimumRefreshTimeExceeded() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRefresh = currentTime - lastRefreshTime;
        if (timeSinceLastRefresh < MINIMUM_REFRESH_TIME) {
            return false;
        }
        lastRefreshTime = currentTime;
        return true;
    }

    private void doHandleCommand(ChannelUID channelUID, Command command) throws Exception {
        if (command instanceof RefreshType) {
            // TODO: handle data refresh
            logger.debug("EWPESmart refresh {}", channelUID.getId());
        } else if (CHANNEL_POWER.equals(channelUID.getId())) {
            if (command.toString() == "ON") {
                thisDevice.SetDevicePower(clientSocket, 1);
            } else {
                thisDevice.SetDevicePower(clientSocket, 0);
            }
        } else if (CHANNEL_MODE.equals(channelUID.getId())) {
            int val = ((DecimalType) command).intValue();
            thisDevice.SetDeviceMode(clientSocket, val);
        } else if (CHANNEL_TURBO.equals(channelUID.getId())) {
            if (command.toString() == "ON") {
                thisDevice.SetDeviceTurbo(clientSocket, 1);
            } else {
                thisDevice.SetDeviceTurbo(clientSocket, 0);
            }
        } else if (CHANNEL_LIGHT.equals(channelUID.getId())) {
            if (command.toString() == "ON") {
                thisDevice.SetDeviceLight(clientSocket, 1);
            } else {
                thisDevice.SetDeviceLight(clientSocket, 0);
            }
        } else if (CHANNEL_TEMP.equals(channelUID.getId())) {
            int val = ((DecimalType) command).intValue();
            thisDevice.SetDeviceTempSet(clientSocket, val);
        } else if (CHANNEL_SWING_VERTICAL.equals(channelUID.getId())) {
            int val = ((DecimalType) command).intValue();
            thisDevice.SetDeviceSwingVertical(clientSocket, val);
        } else if (CHANNEL_WIND_SPEED.equals(channelUID.getId())) {
            int val = ((DecimalType) command).intValue();
            thisDevice.SetDeviceWindspeed(clientSocket, val);
        } else if (CHANNEL_AIR.equals(channelUID.getId())) {
            if (command.toString() == "ON") {
                thisDevice.SetDeviceAir(clientSocket, 1);
            } else {
                thisDevice.SetDeviceAir(clientSocket, 0);
            }
        } else if (CHANNEL_DRY.equals(channelUID.getId())) {
            if (command.toString() == "ON") {
                thisDevice.SetDeviceDry(clientSocket, 1);
            } else {
                thisDevice.SetDeviceDry(clientSocket, 0);
            }
        } else if (CHANNEL_HEALTH.equals(channelUID.getId())) {
            if (command.toString() == "ON") {
                thisDevice.SetDeviceHealth(clientSocket, 1);
            } else {
                thisDevice.SetDeviceHealth(clientSocket, 0);
            }
        } else if (CHANNEL_POWER_SAVE.equals(channelUID.getId())) {
            if (command.toString() == "ON") {
                thisDevice.SetDevicePwrSaving(clientSocket, 1);
            } else {
                thisDevice.SetDevicePwrSaving(clientSocket, 0);
            }
        }
    }

    private void publishChannelIfLinked(ChannelUID channelUID) {
        String channelID = channelUID.getId();
        boolean statusChanged = false;
        // if (channelID != null && isLinked(channelID)) {
        if (thisDevice != null && thisDevice.getIsBound() && isLinked(channelID)) {
            State state = null;
            Integer stateValue = null;
            if (CHANNEL_POWER.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("Pow")) {
                    logger.trace("Pow value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("Pow");
                    if (stateValue.intValue() != 1) {
                        state = OnOffType.OFF;
                    } else {
                        state = OnOffType.ON;
                    }
                }
            } else if (CHANNEL_MODE.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("Mod")) {
                    logger.trace("Mod value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("Mod");
                    state = new DecimalType(stateValue);
                }
            } else if (CHANNEL_TURBO.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("Tur")) {
                    logger.trace("Mod value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("Tur");
                    if (stateValue.intValue() != 1) {
                        state = OnOffType.OFF;
                    } else {
                        state = OnOffType.ON;
                    }
                }
            } else if (CHANNEL_LIGHT.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("Lig")) {
                    logger.trace("Lig value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("Lig");
                    if (stateValue.intValue() != 1) {
                        state = OnOffType.OFF;
                    } else {
                        state = OnOffType.ON;
                    }
                }
            } else if (CHANNEL_TEMP.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("SetTem")) {
                    logger.trace("SetTem value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("SetTem");
                    state = new DecimalType(stateValue);
                }
            } else if (CHANNEL_TEMP_SENSOR.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("TemSen")) {
                    logger.trace("TemSen value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("TemSen");
                    state = new DecimalType(stateValue);
                }
            } else if (CHANNEL_SWING_VERTICAL.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("SwUpDn")) {
                    logger.trace("SwUpDn value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("SwUpDn");
                    state = new DecimalType(stateValue);
                }
            } else if (CHANNEL_WIND_SPEED.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("WdSpd")) {
                    logger.trace("WdSpd value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("WdSpd");
                    state = new DecimalType(stateValue);
                }
            } else if (CHANNEL_AIR.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("Air")) {
                    logger.trace("Air value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("Air");
                    if (stateValue.intValue() != 1) {
                        state = OnOffType.OFF;
                    } else {
                        state = OnOffType.ON;
                    }
                }
            } else if (CHANNEL_DRY.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("Blo")) {
                    logger.trace("Blo value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("Blo");
                    if (stateValue.intValue() != 1) {
                        state = OnOffType.OFF;
                    } else {
                        state = OnOffType.ON;
                    }
                }
            } else if (CHANNEL_HEALTH.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("Health")) {
                    logger.trace("Health value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("Health");
                    if (stateValue.intValue() != 1) {
                        state = OnOffType.OFF;
                    } else {
                        state = OnOffType.ON;
                    }
                }
            } else if (CHANNEL_POWER_SAVE.equals(channelID)) {
                if (thisDevice.HasStatusValChanged("SvSt")) {
                    logger.trace("SvSt value has changed!");
                    statusChanged = true;
                    stateValue = thisDevice.GetIntStatusVal("SvSt");
                    if (stateValue.intValue() != 1) {
                        state = OnOffType.OFF;
                    } else {
                        state = OnOffType.ON;
                    }
                }
            }
            if (state != null && statusChanged == true) {
                logger.debug("Updating channel state for ChannelID {} : {}", channelID, state);
                updateState(channelID, state);
            }
        }
    }

    /**
     * Shutdown thing, make sure background jobs are canceled
     */
    @SuppressWarnings("null")
    @Override
    public void dispose() {
        updateStatus(ThingStatus.OFFLINE);
        logger.debug("EWPESmart Shutdown thing {}", thing.getUID());
        try {
            if (refreshTask != null) {
                refreshTask.cancel(true);
                refreshTask = null;
            }
            logger.debug("EWPESmart refreshTask stopped for thing {}", thing.getUID());
        } catch (Exception e) {
            logger.debug("EWPESmart Exception on dispose(): {} ({})", e.getMessage(), e.getClass());
        } finally {
            super.dispose();
        }
    }
}
