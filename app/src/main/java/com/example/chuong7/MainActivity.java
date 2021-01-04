package com.example.chuong7;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.List;

import static android.os.BatteryManager.*;

public class MainActivity extends Activity {
    private static final String TAG = "log7-1";
    private BroadcastReceiver mBatteryChangedReceiver;
    private TextView mTextView; // layout contains TextView to show battery information

    private static String healthCodeToString(int health) {
        switch (health) {
//case BATTERY_HEALTH_COLD: return "Cold"; // API level 11 only
            case BATTERY_HEALTH_DEAD:
                return "Dead";
            case BATTERY_HEALTH_GOOD:
                return "Good";
            case BATTERY_HEALTH_OVERHEAT:
                return "Overheat";
            case BATTERY_HEALTH_OVER_VOLTAGE:
                return "Over voltage";
            case BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "Unspecified failure";
            case BATTERY_HEALTH_UNKNOWN:
            default:
                return "Unknown";
        }
    }

    private static String pluggedCodeToString(int plugged) {
        switch (plugged) {
            case 0:
                return "Battery";
            case BATTERY_PLUGGED_AC:
                return "AC";
            case BATTERY_PLUGGED_USB:
                return "USB";
            default:
                return "Unknown";
        }
    }

    private static String statusCodeToString(int status) {
        switch (status) {
            case BATTERY_STATUS_CHARGING:
                return "Charging";
            case BATTERY_STATUS_DISCHARGING:
                return "Discharging";
            case BATTERY_STATUS_FULL:
                return "Full";
            case BATTERY_STATUS_NOT_CHARGING:
                return "Not charging";
            case BATTERY_STATUS_UNKNOWN:
            default:
                return "Unknown";
        }
    }

    private void showBatteryInfo(Intent intent) {
        if (intent != null) {
            int health = intent.getIntExtra(EXTRA_HEALTH, BATTERY_HEALTH_UNKNOWN);
            String healthString = "Health: " + healthCodeToString(health);
            Log.d(TAG, healthString);
            int level = intent.getIntExtra(EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(EXTRA_SCALE, 100);
            float percentage = (scale != 0) ? (100.f * (level / (float) scale)) : 0.0f;
            String levelString = String.format("Level: %d/%d (%.2f%%)", level, scale, percentage);
            Log.i(TAG, levelString);
            int plugged = intent.getIntExtra(EXTRA_PLUGGED, 0);
            String pluggedString = "Power source: " + pluggedCodeToString(plugged);
            Log.i(TAG, pluggedString);
            boolean present = intent.getBooleanExtra(EXTRA_PRESENT, false);
            String presentString = "Present? " + (present ? "Yes" : "No");
            Log.i(TAG, presentString);
            int status = intent.getIntExtra(EXTRA_STATUS, BATTERY_STATUS_UNKNOWN);
            String statusString = "Status: " + statusCodeToString(status);
            Log.i(TAG, statusString);
            String technology = intent.getStringExtra(EXTRA_TECHNOLOGY);
            String technologyString = "Technology: " + technology;
            Log.i(TAG, technologyString);
            int temperature = intent.getIntExtra(EXTRA_STATUS, Integer.MIN_VALUE);
            String temperatureString = "Temperature: " + temperature;
            Log.i(TAG, temperatureString);
            int voltage = intent.getIntExtra(EXTRA_VOLTAGE, Integer.MIN_VALUE);
            String voltageString = "Voltage: " + voltage;
            Log.i(TAG, voltageString);
            String s = healthString + "\n";
            s += levelString + "\n";
            s += pluggedString + "\n";
            s += presentString + "\n";
            s += statusString + "\n";
            s += technologyString + "\n";
            s += temperatureString + "\n";
            s += voltageString;
            mTextView.setText(s);
            int id = intent.getIntExtra(EXTRA_ICON_SMALL, 0);
            setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, id);
        } else {
            String s = "No battery information";
            Log.i(TAG, s);
            mTextView.setText(s);
            setFeatureDrawable(Window.FEATURE_LEFT_ICON, null);
        }
    }

    private void showBatteryInfo() {
        Intent intent = registerReceiver(null, new
                IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        showBatteryInfo(intent);
    }

    private void createBatteryReceiver() {
        mBatteryChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showBatteryInfo(intent);
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.battery);
        byte[] test = new byte[0];
        showBatteryInfo();
        showNetworkInfoToast();
        transferData(test);
        requestLocationUpdates();
        requestPassiveLocationUpdates();
        showLocationProvidersPowerRequirement();
        getMyLocationProvider();
//        getLastKnownLocation();
        registerWithAccelerometer();
//        setupAlarm(true);
//        setupInexactAlarm(true);
//        runInWakeLock();

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBatteryChangedReceiver);
        enableBatteryReceiver(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBatteryChangedReceiver == null) {
            createBatteryReceiver();
        }
        registerReceiver(mBatteryChangedReceiver, new
                IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        enableBatteryReceiver(true);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        unregisterReceiver(mBatteryChangedReceiver);
        mBatteryChangedReceiver = null;
    }

    private void enableBatteryReceiver(boolean enabled) {
        PackageManager pm = getPackageManager();
        ComponentName receiverName = new ComponentName(this, BatteryReceiver.class);
        int newState;
        if (enabled) {
            newState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } else {
            newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        }
        pm.setComponentEnabledSetting(receiverName, newState,
                PackageManager.DONT_KILL_APP);
    }

    //Networking
    //7-6
    private void showNetworkInfoToast() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
// to show only the active connection
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null) {
            Toast.makeText(this, "Active: " + info.toString(),
                    Toast.LENGTH_LONG).show();
        }
// to show all connections
//        NetworkInfo[] array = cm.getAllNetworkInfo();
//        if (array != null) {
//            String s = "All: ";
//            for (NetworkInfo i : array) {
//                s += i.toString() + "\n";
//            }
//            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
//        }
    }

    //7-7
    private void transferData(byte[] array) {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean backgroundDataSetting = cm.getBackgroundDataSetting();
        if (backgroundDataSetting) {
// transfer data
            Log.i("log7-7", "transfer");
        } else {
// honor setting and do not transfer data
            Log.i("log7-7", "do not transfer");
        }
    }

    private void requestLocationUpdates() {
        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getAllProviders();
        Log.d("log7-8", providers.toString());
        if (providers != null && !providers.isEmpty()) {
            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.i("log7-8", location.toString());
                }

                @Override
                public void onProviderDisabled(String provider) {
                    Log.i("log7-8", provider + " location provider disabled");
                }

                @Override
                public void onProviderEnabled(String provider) {
                    Log.i("log7-8", provider + " location provider enabled");
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    Log.i("log7-8", provider + " location provider status changed to " +
                            status);
                }
            };
            for (String name : providers) {
                Log.i("log7-8", "Requesting location updates on " + name);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                lm.requestLocationUpdates(name, 0, 0, listener);
//                lm.requestLocationUpdates(name, DateUtils.HOUR_IN_MILLIS * 1, 100, listener);
            }
        }
    }

    private void disableLocationListener(LocationListener listener) {
        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        lm.removeUpdates(listener);
    }

    private void requestPassiveLocationUpdates() {
        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i("log7-11", "[PASSIVE] " + location.toString());
// let's say you only care about GPS location updates
                if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
// if you care about accuracy, make sure you call hasAccuracy()
// (same comment for altitude and bearing)
                    if (location.hasAccuracy() && (location.getAccuracy() < 10.0f)) {
// do something here
                    }
                }
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.i("log7-11", "[PASSIVE] " + provider + " location provider disabled");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.i("log7-11", "[PASSIVE] " + provider + " location provider enabled");
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i("log7-11", "[PASSIVE] " + provider + " location provider status changed to " + status);
            }
        };
        Log.i("log7-11", "Requesting passive location updates");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, DateUtils.SECOND_IN_MILLIS * 30, 100, listener);
    }

    private static String powerRequirementCodeToString(int powerRequirement) {
        switch (powerRequirement) {
            case Criteria.POWER_LOW:
                return "Low";
            case Criteria.POWER_MEDIUM:
                return "Medium";
            case Criteria.POWER_HIGH:
                return "High";
            default:
                return String.format("Unknown (%d)", powerRequirement);
        }
    }

    private void showLocationProvidersPowerRequirement() {
        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getAllProviders();
        if (providers != null) {
            for (String name : providers) {
                LocationProvider provider = lm.getProvider(name);
                if (provider != null) {
                    int powerRequirement = provider.getPowerRequirement();
                    Log.i("7-12", name + " location provider power requirement: " +
                            powerRequirementCodeToString(powerRequirement));
                }
            }
        }
    }

    private LocationProvider getMyLocationProvider() {
        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        LocationProvider provider = null;
// define your criteria here
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setAltitudeRequired(true);
        criteria.setBearingAccuracy(Criteria.NO_REQUIREMENT); // API level 9
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true); // most likely you want the user to be able to set that
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_LOW); // API level 9
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setSpeedAccuracy(Criteria.ACCURACY_MEDIUM); // API level 9
        criteria.setSpeedRequired(false);
        criteria.setVerticalAccuracy(Criteria.NO_REQUIREMENT); // API level 9
        List<String> names = lm.getProviders(criteria, false); // perfect matches only
        if ((names != null) && !names.isEmpty()) {
            for (String name : names) {
                provider = lm.getProvider(name);
                Log.d("7-13", "[getMyLocationProvider] " + provider.getName() + " " +
                        provider);
            }
            provider = lm.getProvider(names.get(0));
        } else {
            Log.d("7-13", "Could not find perfect match for location provider");
            String name = lm.getBestProvider(criteria, false); // not necessarily perfect match
            if (name != null) {
                provider = lm.getProvider(name);
                Log.d("7-13", "[getMyLocationProvider] " + provider.getName() + " " +
                        provider);
            }
        }
        return provider;
    }

    private Location getLastKnownLocation() {
        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        List<String> names = lm.getAllProviders();
        Location location = null;
        if (names != null) {
            for (String name : names) {
                if (!LocationManager.PASSIVE_PROVIDER.equals(name)) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    }
                    Location l = lm.getLastKnownLocation(name);
                    if ((l != null) && (location == null || l.getTime() >
                            location.getTime())) {
                        location = l;
                    }
                }
            }
        }
        return location;
    }
    //Goldfish 3-axis  Accelerometer su dung de xac dinh huong chuyen dong cua vat the bang cach do gia toc 3 truc X/Y/Z
    private void registerWithAccelerometer() {
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (sensors != null && ! sensors.isEmpty()) {
            SensorEventListener listener = new SensorEventListener() {
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    Log.i("7-15", "Accuracy changed to " + accuracy);
                }
                @Override
                public void onSensorChanged(SensorEvent event) {
                    Log.i("7-15", String.format("x:%.2f y:%.2f z:%.2f ",
                            event.values[0], event.values[1], event.values[2]));
                }
            };
            Sensor sensor = sensors.get(0);
            Log.d("7-15", "Using sensor " + sensor.getName() + " from " +
                    sensor.getVendor());
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void setupAlarm(boolean cancel) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MyService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        if (cancel) {
            am.cancel(pendingIntent);
        } else {
            long interval = DateUtils.HOUR_IN_MILLIS * 1;
            long firstInterval = DateUtils.MINUTE_IN_MILLIS * 30;
            am.setRepeating(AlarmManager.RTC_WAKEUP, firstInterval, interval,
                    pendingIntent);
// use am.set(...) to schedule a non-repeating alarm
        }
    }

    private void setupInexactAlarm(boolean cancel) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MyService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        if (cancel) {
            am.cancel(pendingIntent);
        } else {
            long interval = AlarmManager.INTERVAL_HOUR;
            long firstInterval = DateUtils.MINUTE_IN_MILLIS * 30;
            am.setInexactRepeating(AlarmManager.RTC, firstInterval, interval,
                    pendingIntent);
        }
    }

    private void runInWakeLock(Runnable runnable, int flags) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(flags, "My WakeLock");
        wl.acquire();
        runnable.run();
        wl.release();
    }



}