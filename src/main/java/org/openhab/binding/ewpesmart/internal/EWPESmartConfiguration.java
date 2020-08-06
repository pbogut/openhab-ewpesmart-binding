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

import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link EWPESmartConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Pawel Bogut - Initial contribution
 */
public class EWPESmartConfiguration {

    /**
     * Sample configuration parameter. Replace with your own.
     */
    public @Nullable String broadcastIp; //EWPESmartBindingConstants.CONFIG_BROADCAST_IP
    public @Nullable String ipAddress; //EWPESmartBindingConstants.CONFIG_IP_ADDRESS
    public Integer refresh; //EWPESmartBindingConstants.CONFIG_REFRESH

    public String getIpAddress() {
        return ipAddress;
    }

    public String getBroadcastIp() {
        return broadcastIp;
    }

    public Integer getRefresh() {
        return refresh;
    }

    public boolean isValid() {
        try {
            if (ipAddress.isEmpty()) {
                return false;
            }
            if (broadcastIp.isEmpty()) {
                return false;
            }
            if (refresh.intValue() <= 0) {
                throw new IllegalArgumentException("Refresh time must be positive number!");
            }
            return true;
        } catch (Exception err) {
            return false;
        }
    }
}
