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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link EWPESmartBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Pawel Bogut - Initial contribution
 */
@NonNullByDefault
public class EWPESmartBindingConstants {

    private static final String BINDING_ID = "ewpesmart";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_AIRCON = new ThingTypeUID(BINDING_ID, "EWPEAirCon");

    // List of all Config options
    public static final String CONFIG_BROADCAST_IP = "broadcastIp";
    public static final String CONFIG_IP_ADDRESS = "ipAddress";
    public static final String CONFIG_REFRESH = "refresh";

    // List of all Channel ids
    public static final String POWER_CHANNEL = "power";
    public static final String MODE_CHANNEL = "mode";
    public static final String TURBO_CHANNEL = "turbo";
    public static final String LIGHT_CHANNEL = "light";
    public static final String TEMP_CHANNEL = "temp";
    public static final String SWINGV_CHANNEL = "swingVertical";
    public static final String WINDSPEED_CHANNEL = "windSpeed";
    public static final String AIR_CHANNEL = "air";
    public static final String DRY_CHANNEL = "dry";
    public static final String HEALTH_CHANNEL = "health";
    public static final String PWRSAV_CHANNEL = "powerSave";
}
