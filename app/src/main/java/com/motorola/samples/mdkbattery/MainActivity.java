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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toolbar;

import com.motorola.mod.ModBattery;
import com.motorola.mod.ModDevice;

import java.text.NumberFormat;

/**
 * A class to represent main activity.
 */
public class MainActivity extends Activity implements View.OnClickListener {
    public static final String MOD_UID = "mod_uid";

    /**
     * Interface for ModManager and ModDevice
     */
    private Personality personality;

    /** Handler for events from mod device */
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Personality.MSG_MOD_DEVICE:
                    /** Mod attach/detach */
                    ModDevice device = personality.getModDevice();
                    onModDevice(device);
                    break;
                case Personality.MSG_MOD_BATTERY:
                    /** Battery data updated */
                    BatteryPersonality.BatteryStat stat = (BatteryPersonality.BatteryStat) msg.obj;
                    onBattery(stat.core, stat.mod, stat.modUsageType, stat.modEfficiency);
                    break;
                default:
                    Log.i(Constants.TAG, "MainActivity - Un-handle mod events: " + msg.what);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);

        TextView textView = (TextView)findViewById(R.id.mod_external_dev_portal);
        if (textView != null) {
            textView.setOnClickListener(this);
        }

        textView = (TextView)findViewById(R.id.mod_source_code);
        if (textView != null) {
            textView.setOnClickListener(this);
        }

        Button button = (Button)findViewById(R.id.status_settings_battery);
        if (button != null) {
            button.setOnClickListener(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        releasePersonality();
    }

    @Override
    public void onPause() {
        super.onPause();

        releasePersonality();
    }

    @Override
    public void onResume() {
        super.onResume();

        initPersonality();
    }

    private void initPersonality() {
        if (null == personality) {
            personality = new BatteryPersonality(this);

            /** Register handler to get event and data update */
            personality.registerListener(handler);
        }
    }

    private void releasePersonality() {
        if (null != personality) {
            personality.onDestroy();
            personality = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            /** Get the UUID from attached mod device */
            String uid = getString(R.string.na);
            if (personality != null
                    && personality.getModDevice() != null
                    && personality.getModDevice().getUniqueId() != null) {
                uid = personality.getModDevice().getUniqueId().toString();
            }
            startActivity(new Intent(this, AboutActivity.class).putExtra(MOD_UID, uid));
            return true;
        }

        if (id == R.id.action_policy) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_PRIVACY_POLICY)));
        }

        return super.onOptionsItemSelected(item);
    }

    /** Button click event from UI */
    @Override
    public void onClick(View v) {
        if (v == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.mod_external_dev_portal:
                /** The Developer Portal link is clicked */
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_DEV_PORTAL)));
                break;
            case R.id.mod_source_code:
                /** The Buy Mods link is clicked */
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_SOURCE_CODE)));
                break;
            case R.id.status_settings_battery:
                /** The Settings -> Battery button is clickec */
                startActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
                break;
            default:
                Log.i(Constants.TAG, "MainActivity - Un-handled button action.");
                break;
        }
    }

    /** Mod device attach/detach */
    public void onModDevice(ModDevice device) {
        /**
         * Get mod device's Product String, which should correspond to
         * the product name or the vendor internal's name.
         */
        TextView tvName = (TextView) findViewById(R.id.mod_name);
        if (null != tvName) {
            if (null != device) {
                tvName.setText(device.getProductString());

                if (device.getVendorId() == Constants.VID_MDK
                        && device.getProductId() == Constants.PID_BATTERY) {
                    tvName.setTextColor(getColor(R.color.mod_match));
                } else {
                    tvName.setTextColor(getColor(R.color.mod_mismatch));
                }
            } else {
                tvName.setText(getString(R.string.na));
                tvName.setTextColor(getColor(R.color.mod_na));
            }
        }

        /**
         * Get mod device's Vendor ID. This is assigned by the Motorola
         * and unique for each vendor.
         */
        TextView tvVid = (TextView) findViewById(R.id.mod_status_vid);
        if (null != tvVid) {
            if (device == null
                    || device.getVendorId() == Constants.INVALID_ID) {
                tvVid.setText(getString(R.string.na));
            } else {
                tvVid.setText(String.format(getString(R.string.mod_pid_vid_format),
                        device.getVendorId()));
            }
        }

        /** Get mod device's Product ID. This is assigned by the vendor */
        TextView tvPid = (TextView) findViewById(R.id.mod_status_pid);
        if (null != tvPid) {
            if (device == null
                    || device.getProductId() == Constants.INVALID_ID) {
                tvPid.setText(getString(R.string.na));
            } else {
                tvPid.setText(String.format(getString(R.string.mod_pid_vid_format),
                        device.getProductId()));
            }
        }

        /** Get mod device's version of the firmware */
        TextView tvFirmware = (TextView) findViewById(R.id.mod_status_firmware);
        if (null != tvFirmware) {
            if (null != device && null != device.getFirmwareVersion()
                    && !device.getFirmwareVersion().isEmpty()) {
                tvFirmware.setText(device.getFirmwareVersion());
            } else {
                tvFirmware.setText(getString(R.string.na));
            }
        }

        /**
         * Get the default Android application associated with the currently attached mod,
         * as read from the mod hardware manifest.
         */
        TextView tvPackage = (TextView) findViewById(R.id.mod_status_package_name);
        if (null != tvPackage) {
            if (device == null
                    || personality.getModManager() == null) {
                tvPackage.setText(getString(R.string.na));
            } else {
                if (personality.getModManager() != null) {
                    String modPackage = personality.getModManager().getDefaultModPackage(device);
                    if (null == modPackage || modPackage.isEmpty()) {
                        modPackage = getString(R.string.name_default);
                    }
                    tvPackage.setText(modPackage);
                }
            }
        }

        /** Reset mod battery status if mod is detached */
        TextView tvStatus = (TextView) findViewById(R.id.mod_battery_status);
        if (null != tvStatus) {
            if (device == null) {
                tvStatus.setText(getString(R.string.na));
            }
        }

        /** Reset mod battery level if mod is detached */
        TextView tvLevel = (TextView) findViewById(R.id.mod_battery_level);
        if (null != tvLevel) {
            if (device == null) {
                tvLevel.setText(getString(R.string.na));
            }
        }

        /** Reset mod battery capacity if mod is detached */
        TextView tvCapacity = (TextView) findViewById(R.id.mod_battery_capacity);
        if (null != tvCapacity) {
            if (device == null) {
                tvCapacity.setText(getString(R.string.na));
                tvCapacity.setVisibility(View.INVISIBLE);
            }
        }
    }

    /** Check whether attached mod is a MDK based on VID/PID */
    private boolean isMDKMod(ModDevice device) {
        if (device == null) {
            // Mod not attached
            return false;
        } else if (device.getVendorId() == Constants.VID_DEVELOPER
                && device.getProductId() == Constants.PID_DEVELOPER) {
            // MDK in developer mode
            return true;
        } else {
            // Check MDK
            return device.getVendorId() == Constants.VID_MDK;
        }
    }

    /** Update UI when get battery data */
    public void onBattery(BatteryPersonality.Battery core, BatteryPersonality.Battery mod,
                          int usage, int efficiency) {
        String statusString = "";
        boolean charging = false;
        boolean absent = false;

        if (mod.status <= BatteryManager.BATTERY_STATUS_UNKNOWN
                || mod.capFull <= 0
                || mod.level < 0
                || usage == ModBattery.BATTERY_USAGE_TYPE_UNKNOWN
                || mod.rechargeStart == Constants.BATTERY_INVALID) {
            // Mod battery invalid, show Absent on UI
            absent = true;
            statusString = getString(R.string.mod_battery_absent);
        } else if (mod.status == BatteryManager.BATTERY_STATUS_CHARGING) {
            // Mod is charging
            statusString = getString(R.string.mod_is_charging);
            charging = true;
        } else if (core.status == BatteryManager.BATTERY_STATUS_CHARGING) {
            // Phone is charging
            if (mod.status == BatteryManager.BATTERY_STATUS_DISCHARGING
                    && mod.level > 0) {
                // Mod is discharging, it is transferring power to phone.
                statusString = getString(R.string.mod_is_charging_core);
                charging = true;
            } else {
                // Phone is charging on other sources than mod
                charging = true;
                statusString = getString(R.string.phone_is_charging);

                switch (core.plugged) {
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        statusString = getString(R.string.charging_on,
                                getString(R.string.phone_is_charging), getString(R.string.ac));
                        break;
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        statusString = getString(R.string.charging_on,
                                getString(R.string.phone_is_charging), getString(R.string.usb));
                        break;
                    case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                        statusString = getString(R.string.charging_on,
                                getString(R.string.phone_is_charging), getString(R.string.wireless));
                        break;
                }
            }
        } else if (mod.level > 0) {
            // Mod battery is not empty
            if ((core.level == 100
                    && mod.status != BatteryManager.BATTERY_STATUS_DISCHARGING
                    && efficiency != ModBattery.BATTERY_EFFICIENCY_OFF)) {
                // Phone battery is full and mod is not discharging
                statusString = getString(R.string.charging_complete);
            } else {
                if (efficiency == ModBattery.BATTERY_EFFICIENCY_OFF) {
                    // Mod always charging phone if efficiency mod is off
                    statusString = getString(R.string.mod_is_charging_core);
                    charging = true;
                } else if (efficiency == ModBattery.BATTERY_EFFICIENCY_ON) {
                    // Mod charging phone when phone battery level under the
                    // BATTERY_RECHARGE_START_SOC threshold if efficiency mod is on
                    statusString = getString(R.string.transfer_paused_level,
                            NumberFormat.getPercentInstance().format(((double) mod.rechargeStart) / 100.0));
                }
            }
        } else {
            // Mod level is 0
            statusString = getString(R.string.mod_battery_empty);
        }

        /** Set status as unknown if none of above scenes  */
        if (statusString.isEmpty()) {
            statusString = String.format(getString(R.string.unknown_status), core.status, mod.status);
        }

        /** Set status string */
        TextView tvStatus = (TextView) findViewById(R.id.status_battery_status);
        if (null != tvStatus) {
            int color = charging ? R.color.status_charging : R.color.uncharging;
            tvStatus.setText(statusString);
            tvStatus.setTextColor(getColor(color));
        }

        /** Set phone battery status */
        tvStatus = (TextView) findViewById(R.id.core_battery_status);
        if (null != tvStatus) {
            tvStatus.setText(getBatteryStatusText(core.status));
        }

        /** Set mod battery status */
        tvStatus = (TextView) findViewById(R.id.mod_battery_status);
        if (null != tvStatus) {
            if (personality.getModDevice() == null
                    || absent) {
                tvStatus.setText(getString(R.string.na));
            } else {
                tvStatus.setText(getBatteryStatusText(mod.status));
            }
        }

        /** Set mod battery usage type and efficiency mode stauts */
        TextView tvType = (TextView) findViewById(R.id.status_mod_battery_type);
        if (null != tvType) {
            if (personality.getModDevice() == null
                    || absent) {
                tvType.setText(getString(R.string.na));
                tvType.setVisibility(View.GONE);
            } else {
                /**
                 * Get mod battery usage type, could be either BATTERY_USAGE_TYPE_UNKNOWN,
                 * BATTERY_USAGE_TYPE_REMOTE, BATTERY_USAGE_TYPE_SUPPLEMENTAL,
                 * or BATTERY_USAGE_TYPE_EMERGENCY.
                 */
                String usageType = getString(R.string.unknown);
                if (usage == ModBattery.BATTERY_USAGE_TYPE_EMERGENCY) {
                    usageType = getString(R.string.battery_usage_type_emergency);
                } else if (usage == ModBattery.BATTERY_USAGE_TYPE_REMOTE) {
                    usageType = getString(R.string.battery_usage_type_remote);
                } else if (usage == ModBattery.BATTERY_USAGE_TYPE_SUPPLEMENTAL) {
                    usageType = getString(R.string.battery_usage_type_supplemental);
                }

                /**
                 * Get mod battery efficiency mode status, represents the state of the charging
                 * policy between the Moto Mod battery and the phone battery. The efficiency mode
                 * is only applicable to supplemental battery type, e.g. battery which declare
                 * the is BATTERY_USAGE_TYPE_SUPPLEMENTAL.
                 */
                String efficiencyType = getString(R.string.off);
                if (efficiency == ModBattery.BATTERY_EFFICIENCY_ON) {
                    efficiencyType = getString(R.string.on);
                }

                /** Set text string to UI widget */
                tvType.setText(String.format(getString(R.string.mod_battery_type),
                        usageType, efficiencyType));
                tvType.setVisibility(View.VISIBLE);
            }
        }

        /** Set phone battery level */
        TextView tvLevel = (TextView) findViewById(R.id.core_battery_level);
        if (null != tvLevel) {
            tvLevel.setText(String.valueOf(core.level) + getString(R.string.percentage));
        }

        /** Set mod battery level */
        tvLevel = (TextView) findViewById(R.id.mod_battery_level);
        if (null != tvLevel) {
            if (personality.getModDevice() == null
                    || absent) {
                tvLevel.setText(getString(R.string.na));
            } else {
                tvLevel.setText(String.valueOf(mod.level) + getString(R.string.percentage));
            }
        }

        /** Set mod battery full capacity */
        TextView tvCapacity = (TextView) findViewById(R.id.mod_battery_capacity);
        if (null != tvCapacity) {
            if (personality.getModDevice() == null
                    || absent) {
                tvCapacity.setText(getString(R.string.na));
                tvCapacity.setVisibility(View.INVISIBLE);
            } else {
                tvCapacity.setText(mod.capFull * mod.level / 100
                        + " / "
                        + mod.capFull + getString(R.string.mah));
                tvCapacity.setVisibility(View.VISIBLE);
            }
        }
    }

    /** Get readable battery status */
    private String getBatteryStatusText(int status) {
        String statusString = getString(R.string.unknown);
        switch (status) {
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                statusString = getString(R.string.unknown);
                break;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusString = getString(R.string.charging);
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusString = getString(R.string.discharging);
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusString = getString(R.string.not_charging);
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusString = getString(R.string.full);
                break;
        }

        return statusString;
    }
}
