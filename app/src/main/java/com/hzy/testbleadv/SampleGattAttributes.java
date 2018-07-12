/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hzy.testbleadv;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String HEAR_RATE_SERVICE ="0000180d-0000-1000-8000-00805f9b34fb";

    public static String WRITE_SERVICE    = "0000f200-0000-1000-8000-00805f9b34fb";

    //public static String WIFI_PASSWORD_SERVICE = "0000f100-0000-1000-8000-aabbccddeeff";
    //public static String WIFI_PASSWORD_SERVICE_REVERT = "ffeeddcc-bbaa-0080-0010-000000f10000";
    //public static String WIFI_PASSWORD_CHARACTERISTIC = "0000f101-0000-1000-8000-aabbccddeeff";
   // public static String WIFI_SSID =  "0000f202-0000-1000-8000-aabbccddeeff";
    public static String USER_DEFINE_SERVICE = "0000f100-0000-1000-8000-00805f9b34fb";
    public static String READ_CHARACTERISTIC = "0000f101-0000-1000-8000-00805f9b34fb";
    public static String RED_DESCRIPTOR =  "0000f202-0000-1000-8000-00805f9b34fb";

    public static String WRITE_CHARACTERISTIC = "0000f328-0000-1000-8000-00805f9b34fb";
    public static String NOTIFY_CHARCTERISTIC = "0000f428-0000-1000-8000-00805f9b34fb";


    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put(USER_DEFINE_SERVICE, "WiFi Password Service");


        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put(READ_CHARACTERISTIC,"WiFi password");
        attributes.put(WRITE_CHARACTERISTIC,"Write Characteristic");
        attributes.put(NOTIFY_CHARCTERISTIC,"Notify Characteristic");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
