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
package org.openhab.binding.ewpesmart.internal.device;

import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.openhab.binding.ewpesmart.internal.encryption.Crypto;
import org.openhab.binding.ewpesmart.internal.gson.EWPEScanReponsePack4Gson;
import org.openhab.binding.ewpesmart.internal.gson.EWPEScanRequest4Gson;
import org.openhab.binding.ewpesmart.internal.gson.EWPEScanResponse4Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

/**
 * The EWPEDeviceFinder provides functionality for searching for
 * EWPE Airconditioners on the network and keeping a list of
 * found devices.
 *
 * @author John Cunha - Initial contribution
 */

public class EWPEDeviceFinder {

    protected InetAddress mIPAddress = null;
    protected HashMap<String, EWPEDevice> mDevicesHashMap = new HashMap<>();

    public EWPEDeviceFinder(InetAddress broadcastAddress) throws UnknownHostException {
        // mIPAddress = InetAddress.getByName("192.168.1.255");
        mIPAddress = broadcastAddress;
    }

    public void Scan(DatagramSocket clientSocket) throws IOException, Exception {
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        Logger logger = LoggerFactory.getLogger(EWPEDeviceFinder.class);

        // Send the Scan message
        // EWPEProtocolUtils protocolUtils = new EWPEProtocolUtils();
        // sendData = protocolUtils.CreateScanRequest();
        EWPEScanRequest4Gson scanGson = new EWPEScanRequest4Gson();
        scanGson.t = "scan";

        GsonBuilder gsonBuilder = new GsonBuilder();
        // gsonBuilder.setLenient();
        Gson gson = gsonBuilder.create();
        String scanReq = gson.toJson(scanGson);
        sendData = scanReq.getBytes();

        logger.trace("EWPEair Binding Sending scan packet to {}", mIPAddress);

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, mIPAddress, 7000);
        clientSocket.send(sendPacket);

        // Loop for respnses from devices until we get a timeout.
        boolean timeoutRecieved = false;
        while (!timeoutRecieved) {
            // Receive a response
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                clientSocket.receive(receivePacket);
                InetAddress remoteAddress = receivePacket.getAddress();
                int remotePort = receivePacket.getPort();

                // Read the response
                String modifiedSentence = new String(receivePacket.getData());
                StringReader stringReader = new StringReader(modifiedSentence);
                EWPEScanResponse4Gson scanResponseGson = gson.fromJson(new JsonReader(stringReader),
                        EWPEScanResponse4Gson.class);

                // If there was no pack, ignore the response
                if (scanResponseGson.pack == null) {
                    continue;
                }

                scanResponseGson.decryptedPack = Crypto.decryptPack(Crypto.GetAESGeneralKeyByteArray(),
                        scanResponseGson.pack);
                String decryptedMsg = Crypto.decryptPack(Crypto.GetAESGeneralKeyByteArray(),
                        scanResponseGson.pack);

                // If something was wrong with the decryption, ignore the response
                if (decryptedMsg == null) {
                    continue;
                }
                logger.debug("EWPESmart Binding Response received from address {}", remoteAddress);
                logger.debug("EWPESmart Binding Response : {}", decryptedMsg);

                // Create the JSON to hold the response values
                stringReader = new StringReader(decryptedMsg);
                scanResponseGson.packJson = gson.fromJson(new JsonReader(stringReader), EWPEScanReponsePack4Gson.class);

                // Now make sure the device is reported as a EWPE device
                if (scanResponseGson.packJson.brand.equals("gree")) {
                    // Create a new EWPEDevice
                    EWPEDevice newDevice = new EWPEDevice();
                    newDevice.setAddress(remoteAddress);
                    newDevice.setPort(remotePort);
                    newDevice.setScanResponseGson(scanResponseGson);

                    AddDevice(newDevice);
                }
            } catch (SocketTimeoutException e) {
                // We've received a timeout so lets quit searching for devices
                timeoutRecieved = true;
            }
        }
    }

    public void AddDevice(EWPEDevice newDevice) {
        mDevicesHashMap.put(newDevice.getId(), newDevice);
    }

    public EWPEDevice GetDevice(String id) {
        return mDevicesHashMap.get(id);
    }

    public HashMap<String, EWPEDevice> GetDevices() {
        return mDevicesHashMap;
    }

    public EWPEDevice GetDeviceByIPAddress(String ipAddress) {
        EWPEDevice returnDevice = null;

        Set<String> keySet = mDevicesHashMap.keySet();
        Iterator<String> iter = keySet.iterator();
        while (returnDevice == null && iter.hasNext()) {
            Object thiskey = iter.next();
            EWPEDevice currDevice = mDevicesHashMap.get(thiskey);
            if (currDevice != null && currDevice.getAddress().getHostAddress().equals(ipAddress)) {
                returnDevice = currDevice;
            }
        }

        return returnDevice;
    }

    public Integer GetScannedDeviceCount() {
        return new Integer(mDevicesHashMap.size());
    }
}
