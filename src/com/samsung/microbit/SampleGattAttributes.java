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

package com.samsung.microbit;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String DEVICE_FIRMWARE_UPDATE = "00001530-1212-efde-1523-785feabcd123";
    public static String DFU_CONTROL_POINT="00001531-1212-efde-1523-785feabcd123";
    public static String DFU_PACKET="00001532-1212-efde-1523-785feabcd123";

    static {
        // Sample Services.
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
    	attributes.put(DEVICE_FIRMWARE_UPDATE, "Device Firmware Update Service");

        // Sample Characteristics.
        attributes.put(DFU_CONTROL_POINT, "DFU Control Point");
        attributes.put(DFU_PACKET, "DFU Packet");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
