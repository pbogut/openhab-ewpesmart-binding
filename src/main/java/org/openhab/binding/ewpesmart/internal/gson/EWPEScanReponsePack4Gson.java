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
 * The EWPEScanReponsePack4Gson class is used by Gson to hold values returned by
 * the Air Conditioner during Scan Requests to the Air Conditioner.
 *
 * @author John Cunha - Initial contribution
 */
public class EWPEScanReponsePack4Gson {

    public String t = null;
    public String cid = null;
    public String bc = null;
    public String brand = null;
    public String catalog = null;
    public String mac = null;
    public String mid = null;
    public String model = null;
    public String name = null;
    public String series = null;
    public String vender = null;
    public String ver = null;
    public int lock = 0;
}
