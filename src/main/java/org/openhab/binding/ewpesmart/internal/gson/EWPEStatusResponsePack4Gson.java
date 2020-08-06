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
package org.openhab.binding.ewpesmart.internal.gson;

/**
 *
 * The EWPEStatusResponsePack4Gson class is used by Gson to hold values returned from
 * the Air Conditioner during requests for Status Updates to the
 * Air Conditioner.
 *
 * @author John Cunha - Initial contribution
 */
public class EWPEStatusResponsePack4Gson {

    public EWPEStatusResponsePack4Gson(EWPEStatusResponsePack4Gson other) {
        cols = new String[other.cols.length];
        dat = new Integer[other.dat.length];
        System.arraycopy(other.cols, 0, cols, 0, other.cols.length);
        System.arraycopy(other.dat, 0, dat, 0, other.dat.length);
    }

    public String t = null;
    public String mac = null;
    public int r = 0;
    public String[] cols = null;
    public Integer[] dat = null;
}
