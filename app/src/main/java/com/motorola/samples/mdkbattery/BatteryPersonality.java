/**
 * Copyright (c) 2016 Motorola Mobility, LLC.
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.motorola.samples.mdkbattery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Message;
import android.util.Log;

import com.motorola.mod.ModBattery;
import com.motorola.mod.ModDevice;
import com.motorola.mod.ModProtocol;

/**
 * A class to represent ModBattery interface.
 */
public class BatteryPersonality extends Personality {
    private BatteryStat batteryStat;

    /**
     * Handle ACTION_BATTERY_CHANGED intent action
     */
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null != intent && intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                batteryStat.update(intent);
                onBattery();
            }
        }
    };

    /** Constructor */
    public BatteryPersonality(Context context) {
        super(context);
        batteryStat = new BatteryStat();

        /** Register battery intent listener */
        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryReceiver, batteryFilter);
    }

    /** Battery info updated and notify listeners */
    public void onBattery() {
        Message msg = Message.obtain();
        msg.obj = batteryStat;
        msg.what = MSG_MOD_BATTERY;
        notifyListeners(msg);
    }

    /** Mod device attach/detach */
    @Override
    public void onModDevice(ModDevice d) {
        super.onModDevice(d);

        /** Check whether mod device is available and implement BATTERY protocol */
        if (null == modManager || null == modDevice
                || !modDevice.hasDeclaredProtocol(ModProtocol.Protocol.BATTERY)) {
            batteryStat.reset();
        } else {
            queryStatus();
        }
    }

    @Override
    public void onDestroy() {
        context.unregisterReceiver(batteryReceiver);
        batteryStat.reset();

        super.onDestroy();
    }

    /** Query currently battery status */
    public void queryStatus() {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (null != intent) {
            batteryStat.update(intent);
            onBattery();
        }
    }

    /** Battery data and status */
    public class Battery {
        public int level;
        public int icon;
        public int scale;
        public int status;
        public int rechargeStop;
        public int rechargeStart;
        public int plugged;
        public long capFull;
    }

    /** Battery data and status */
    public class BatteryStat {
        public int modEfficiency = ModBattery.BATTERY_USAGE_TYPE_UNKNOWN;
        public int modUsageType = ModBattery.BATTERY_USAGE_TYPE_UNKNOWN;

        /** Phone battery data and status */
        public Battery core = new Battery();

        /** Phone battery data and status */
        public Battery mod = new Battery();

        /** ModBattery interface */
        private ModBattery modBattery;

        /** Get battery change intent to update data and status */
        public void update(Intent intent) {
            /** Update mod charge type each time since it may changed without attach/detach */
            updateMod(intent);

            /** Update core battery status */
            updateCore(intent);
        }

        /**
         * Update mod device battery status and data
         */
        private void updateMod(Intent intent) {
            /** Check attached mod device and check whether the device has the BATTERY protocol */
            if (modManager == null || null == modDevice
                    || !modDevice.hasDeclaredProtocol(ModProtocol.Protocol.BATTERY)) {
                reset();
                return;
            }

            /** Get the ModBattery interface */
            modBattery = modManager.getClassManager(ModBattery.class);
            if (null == modBattery) {
                Log.e(Constants.TAG, "Failed to get ModBattery");
                reset();
                return;
            }

            /**
             * Get the mod battery data.
             * Care IllegalStateException exception in case mod is removed or invalid during query.
             */
            try {
                modUsageType = modBattery.getIntProperty(ModBattery.BATTERY_USAGE_TYPE);
                modEfficiency = modBattery.getIntProperty(ModBattery.BATTERY_EFFICIENCY_MODE);

                mod.rechargeStart = modBattery.getIntProperty(ModBattery.BATTERY_RECHARGE_START_SOC);
                mod.rechargeStop = modBattery.getIntProperty(ModBattery.BATTERY_RECHARGE_STOP_SOC);

                mod.level = modBattery.getBatteryLevel(intent);
                mod.status = modBattery.getBatteryStatus(intent);
                mod.plugged = modBattery.isPlugTypeMod(intent) ? 1 : 0;
                mod.capFull = modBattery.getBatteryCapacity(context);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                reset();
            }
        }

        /** Get the phone battery data */
        private void updateCore(Intent intent) {
            core.level = (int) (100f
                    * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
            core.icon = intent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0);
            core.scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
            core.status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            core.plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        }

        /** Reset when mod battery is not valid */
        private void reset() {
            modBattery = null;
            modEfficiency = ModBattery.BATTERY_USAGE_TYPE_UNKNOWN;
            modUsageType = ModBattery.BATTERY_USAGE_TYPE_UNKNOWN;
            mod.status = BatteryManager.BATTERY_STATUS_UNKNOWN;
        }
    }
}
