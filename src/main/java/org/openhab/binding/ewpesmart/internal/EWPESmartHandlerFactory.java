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

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EWPESmartHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Pawel Bogut - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.ewpesmart", service = ThingHandlerFactory.class)
public class EWPESmartHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_AIRCON);
    private final Logger logger = LoggerFactory.getLogger(EWPESmartHandlerFactory.class);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        logger.warn("SupportsThingType {}", thingTypeUID);
        logger.warn("SupportsThingType {}", SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID));
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_AIRCON.equals(thingTypeUID)) {
            return new EWPESmartHandler(thing);
        }

        return null;
    }
}
