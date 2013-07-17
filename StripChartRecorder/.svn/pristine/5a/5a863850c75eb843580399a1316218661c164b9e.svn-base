package bishwas.gcdc.stripchartrecorder.helpers;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Responsible for storing Shared Preferences
 * Has methods to store and retrieve Preference
 * 
 * @author Bishwas Gautam
 * 
 *  Copyright (C) 2011 Gulf Coast Data Concepts Licensed under the GNU Lesser
 *  General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
@SuppressWarnings("serial")
public class ConfigManager implements Serializable {

	public static final int BUILD_OVER_11 = 1;
	public static final int BUILD_LESS_THAN_11 = 0;

	public static final String KEY_CHANNEL_X = "channel_x";
	public static final String KEY_CHANNEL_Y = "channel_y";
	public static final String KEY_CHANNEL_Z = "channel_z";
	public static final String KEY_CHANNEL_RMS = "channel_rms";
	private static final String KEY_VERTICAL_AXIS_AUTOSCALE = "vertical_axis_autoscale";
	public final String DEFAULT_INITIALIZED = "default_initialized";

	public final String CONFIG_INFO = "configInfoPref";
	public final String BATTERY_STATUS = "battery_status";
	public final String GAIN = "gain";
	public final String CHARGE_STATUS = "charge_status";
	public final String DEADBAND = "dead_band";
	public final String REBOOT_ON_DISCONNECT = "reboot_on_disconnect";
	public final String TEMPERATURE_STATUS = "temperature_status";
	public final String STATUS_INFO = "deviceStatusPref";
	public final String AVAILABLE_SAMPLE_RATES = "sampleRatePref";
	public final String CURRENT_SAMPLE_RATE = "current_sample_rate";
	public final String DEVICE_NAME = "device_info";
	public final String DEVICE_ADDRESS = "device_address";
	public final String CONNECTION_ATTEMPTS = "connection_attempts";
	public final String LIST_PREF_VALUE = "sample_rates_list_pref";
	public final String IS_BUILD_OVER11 = "android_version";

	private static SharedPreferences myPref;
	private static SharedPreferences.Editor myPrefEditor;

	private static ConfigManager instance = null;

	private ConfigManager(Context context) {

		ConfigManager.myPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		ConfigManager.myPrefEditor = myPref.edit();
	}

	/**
	 * @param context: The application context
	 * @return singleton instance of ConfigManager
	 * Will get called by StripChartRecorder only
	 */
	public static ConfigManager getConfigManager(Context context) {
		if (instance == null)
			instance = new ConfigManager(context);
		return instance;
	}

	/**
	 * @return singleton instance of ConfigManager
	 *
	 */
	public static ConfigManager getConfigManager() {

		return instance;
	}

	/**
	 * @return true if Android build is greater than or over 3.2(honeycomb)
	 */
	public boolean is_BUILD_OVER_11() {
		int result=myPref.getInt(IS_BUILD_OVER11, -1);
		if (result==-1)
		return (android.os.Build.VERSION.SDK_INT>= 11);
		else
		return (result==BUILD_OVER_11);
	}

	
	public void set_is_BUILD_OVER_11(int value) {
		if (value == BUILD_OVER_11)

			myPrefEditor.putInt(IS_BUILD_OVER11, value);
	}

	public String getCurrentListValue() {
		return myPref.getString(LIST_PREF_VALUE, "0");
	}

	public boolean isPreferencesInitialized() {
		return myPref.getBoolean(DEFAULT_INITIALIZED, false);
	}

	public void setDefaultInitialized() {
		myPrefEditor.putBoolean(DEFAULT_INITIALIZED, true);
		commit();
	}

	public String getConfigInfo() {

		return "Temperature : " + getTemperatureStatus() + " degree Celsius"
				+ "\nGain : " + getGain();

	}

	public String getStatusInfo() {
		String rod = isRebootOnDisconnect() ? "Yes" : "No";
		return "Battery : " + getBatteryStatus() + " (mv)" + "\nDeadband : "
				+ getDeadband() + "\nReboot On Disconnect: " + rod;
	}

	public void setGain(String value) {
		String[] parts = value.replace(";", "").split(",");
		Log.d("ConfigManager", "From gain:: " + value);
		myPrefEditor.putString(GAIN, parts[1]);
		commit();

	}

	public String getGain() {
		return myPref.getString(GAIN, "N/A");
	}

	public void setBatteryStatus(String status) {

		String batteryVal = status.replaceAll(" ", "");
		myPrefEditor.putInt(BATTERY_STATUS, Integer.valueOf(batteryVal));
		commit();

	}

	public int getBatteryStatus() {

		return myPref.getInt(BATTERY_STATUS, 0);
	}

	public void setTemperatureStatus(String status) {
		String[] parts = status.split(",");

		myPrefEditor.putFloat(TEMPERATURE_STATUS,
				Float.valueOf(parts[1].replaceAll(" ", "")));
		commit();
		setBatteryStatus(parts[4]);
	}

	public float getTemperatureStatus() {

		return myPref.getFloat(TEMPERATURE_STATUS, (float) 0.0);
	}

	public void setDeadband(String status) {

		myPrefEditor.putInt(DEADBAND,
				Integer.valueOf(status.split(":")[1].replaceAll(" ", "")));
		commit();
	}

	public int getDeadband() {

		return myPref.getInt(DEADBAND, 0);
	}

	public boolean isRebootOnDisconnect() {
		boolean rod = myPref.getInt(REBOOT_ON_DISCONNECT, -1) == 1 ? true
				: false;
		return rod;

	}

	public void setRebootOnDisconnect(String val) {
		int rod = val.split(":")[1].contains("no") ? 0 : 1;
		myPrefEditor.putInt(REBOOT_ON_DISCONNECT, rod);
		commit();

	}

	public void setCharge(String status) {
		String[] parts = status.split(":");
		String chargeVal = parts[2].replaceAll(" ", "").replace("(mA)", "");

		myPrefEditor.putInt(CHARGE_STATUS, Integer.valueOf(chargeVal));
		commit();
	}

	public int getCharge() {

		return myPref.getInt(CHARGE_STATUS, 0);
	}

	public int getCurrentSampleRate() {
		return myPref.getInt(CURRENT_SAMPLE_RATE, 0);
	}

	public Set<String> getAvailableSampleRates() {
		if (!this.is_BUILD_OVER_11()) {
			String returned = myPref.getString(AVAILABLE_SAMPLE_RATES, "DNE");
			String[] rates = null;
			if (!returned.equals("DNE"))
				rates = returned.split(",");
			if (rates == null)
				return null;
			return new HashSet<String>(Arrays.asList(rates));
		}
		return myPref.getStringSet(AVAILABLE_SAMPLE_RATES, null);
	}

	private void setAvailableSampleRatesForLesserVersions(String line) {

		myPrefEditor.putString(AVAILABLE_SAMPLE_RATES, line.split(":")[1]);
		commit();
	}

	public void setAvailableSampleRates(String availableRates) {

		if (!this.is_BUILD_OVER_11()) {
			setAvailableSampleRatesForLesserVersions(availableRates);
			return;
		}

		Set<String> FinalRatesSet = stringToSetForAvailableRates(availableRates);
		myPrefEditor.putStringSet(AVAILABLE_SAMPLE_RATES, FinalRatesSet);
		commit();

	}

	public Set<String> stringToSetForAvailableRates(String availableRates) {

		if (availableRates == null) {
			Set<String> notAvailable = new HashSet<String>();
			notAvailable.add("N/A");
			Log.d("ConfigManager", "Not available");
			return notAvailable;

		} else {

			String[] splitFirstLine = availableRates.split(":");

			String[] rates = splitFirstLine[1].split(",");

			Set<String> finalSet = new HashSet<String>(Arrays.asList(rates));

			Log.d("ConfigManager", "Final Set" + finalSet.toString());

			return finalSet;
		}

	}

	public void setCurrentSampleRate(String value) {
		int val;
		if(value.contains(":"))val=Integer.parseInt(value.split(":")[1].replaceAll(" ", ""));
		else val=Integer.parseInt(value);
		myPrefEditor.putInt(CURRENT_SAMPLE_RATE,
				val);
		commit();
	}

	private void commit() {
		myPrefEditor.commit();

	}

	public String getDefaultDeviceMacAddress() {
		return myPref.getString(DEVICE_ADDRESS, "DNE");

	}

	public void setDefaultDeviceMacAddress(String defaultDeviceMacAddress) {
		myPrefEditor.putString(DEVICE_ADDRESS, defaultDeviceMacAddress);
		commit();
	}

	public String getDefaultDeviceName() {
		return myPref.getString(DEVICE_NAME, "DNE");
	}

	public void setDefaultDeviceName(String defaultDeviceName) {
		myPrefEditor.putString(DEVICE_NAME, defaultDeviceName);
		commit();
	}

	public void setChannelChecked(String keyChannel, boolean isChecked) {
		myPrefEditor.putBoolean(keyChannel, isChecked);
		commit();

	}

	public boolean isChannelChecked(String keyChannel) {
		return myPref.getBoolean(keyChannel, false);
	}

	public boolean isVerticalAxisAutoScaled() {
		return myPref.getBoolean(KEY_VERTICAL_AXIS_AUTOSCALE, false);
	}

	public void setVerticalAxisAutoScaled(boolean autoScaled) {
		myPrefEditor.putBoolean(KEY_VERTICAL_AXIS_AUTOSCALE, autoScaled);
		commit();
	}

/*	public void setScreenWidth(int width) {
		myPrefEditor.putInt(SCREEN_WIDTH, width);
		commit();
		
	}
	
	public int getScreenWidth(){
		return myPref.getInt(SCREEN_WIDTH, 0);
	}

	public void setScreenHeight(int height) {
		myPrefEditor.putInt(SCREEN_HEIGHT, height);
		commit();
		
	}
	
	public int getScreenHeight(){
		return myPref.getInt(SCREEN_HEIGHT, 0);
	}*/

}
