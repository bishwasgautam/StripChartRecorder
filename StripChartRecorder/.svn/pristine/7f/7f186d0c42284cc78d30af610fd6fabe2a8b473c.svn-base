package bishwas.gcdc.stripchartrecorder.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import bishwas.gcdc.stripchartrecorder.helpers.AlertDialogHelper;
import bishwas.gcdc.stripchartrecorder.helpers.BluetoothConnectionFactory;
import bishwas.gcdc.stripchartrecorder.helpers.ConfigManager;

/**
 * Handles storage, retrieval of application preferences and applies changes made to device as well, if applicable.
 * Loads preference headers  (preference fragments ) for API > 11 else loads preference screens.
 * @author Bishwas Gautam
 * 
 *  Copyright (C) 2011 Gulf Coast Data Concepts Licensed under the GNU Lesser
 *  General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 * 
 */
public class ControlPanel extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	protected Method mLoadHeaders = null;
	protected Method mHasHeaders = null;
	
	
	private BluetoothConnectionFactory connectionFactory;
	private ConfigManager configManager;
	private SharedPreferences prefs;

	private final String TAG = "Control Panel"; //$NON-NLS-1$
	Preference deviceInfoPref, configInfoPref, device_status, sample_rates;
	private ListPreference lp;
	private EditTextPreference device_name;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Control Panel :: OnCreate()"); //$NON-NLS-1$

		// onBuildHeaders() will be called during super.onCreate()
		try {
			mLoadHeaders = getClass().getMethod(
					"loadHeadersFromResource", int.class, List.class); //$NON-NLS-1$
			mHasHeaders = getClass().getMethod("hasHeaders"); //$NON-NLS-1$
		} catch (NoSuchMethodException e) {
		}

		super.onCreate(savedInstanceState);

		// Retrieve the instance of ConfigManager passed by MainScreen as Extra
		// on intent
		Intent whoCalled = getIntent();
		try {
			connectionFactory = (BluetoothConnectionFactory) whoCalled
					.getSerializableExtra(StripChartRecorder.CONNECTION_FACTORY);
		} catch (Exception e) {
			e.printStackTrace();

		}
		configManager = ConfigManager.getConfigManager();

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);

		if (!configManager.is_BUILD_OVER_11()) {
			addPreferencesFromResource(R.xml.device_info);
			addPreferencesFromResource(R.xml.device_status);
			addPreferencesFromResource(R.xml.sample_rates);
			addPreferencesFromResource(R.xml.config_info);
			updateUIaccordingToPrefValues();

		}

		String apiAfterHoneycomb = configManager.is_BUILD_OVER_11() ? "Yes" : "No"; //$NON-NLS-1$ //$NON-NLS-2$
		Log.d(TAG, " Buit after HoneyComb :: " + apiAfterHoneycomb); //$NON-NLS-1$

	}

	@SuppressWarnings("deprecation")
	private void updateUIaccordingToPrefValues() {

		deviceInfoPref = findPreference(Messages
				.getString("ControlPanel.key_device_info_pref")); //$NON-NLS-1$
		deviceInfoPref
				.setSummary("Name : " + configManager.getDefaultDeviceName() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
						"Address : "
						+ configManager.getDefaultDeviceMacAddress()); //$NON-NLS-1$
		device_name = (EditTextPreference) findPreference(getString(R.string.key_device_name));
		device_name.setText(configManager.getDefaultDeviceName());

		configInfoPref = findPreference(Messages
				.getString("ControlPanel.key_config_info_pref")); //$NON-NLS-1$
		configInfoPref.setSummary(configManager.getConfigInfo());

		device_status = findPreference(Messages
				.getString("ControlPanel.key_device_status_pref")); //$NON-NLS-1$
		device_status.setSummary(configManager.getStatusInfo());

		manageListPreference();

	}

	@SuppressWarnings("deprecation")
	private void manageListPreference() {

		sample_rates = findPreference(Messages
				.getString("ControlPanel.key_sample_rate_pref")); //$NON-NLS-1$

		lp = (ListPreference) findPreference("sample_rates_list_pref");

		Set<String> availableRates = configManager.getAvailableSampleRates();

		String sampleRates = (availableRates == null) ? "Not available"
				: availableRates.toString();

		sample_rates.setSummary(sampleRates.toString());

		if (availableRates != null) {
			String[] array = availableRates.toArray(new String[0]);
			lp.setEntries(array);
			lp.setEntryValues(array);
		} else {

			lp.setEntries(new String[] { "12", "25", "50", "100", "200", "400",
					"800" });
			lp.setEntryValues(new String[] { "12", "25", "50", "100", "200",
					"400", "800" });
		}

		lp.setDefaultValue(configManager.getCurrentSampleRate());
		lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				Log.d("PrefFragment", "New val = " + newValue.toString());
				lp.setSummary(newValue.toString());
				return true;
			}
		});

		int currentSampleRate = configManager.getCurrentSampleRate();
		lp.setDefaultValue(currentSampleRate);
		if (currentSampleRate == 0)
			lp.setSummary("Not Available");
		else
			lp.setSummary(String.valueOf(currentSampleRate));

	}

	@Override
	public void onBuildHeaders(List<Header> aTarget) {
		try {
			mLoadHeaders.invoke(this, new Object[] { R.xml.pref_headers,
					aTarget });
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_preferences, menu);
		return true;
	}

	private void changeSampleRate(final int selectedValue) {
		Log.d(TAG, "Changing Sample Rate"); //$NON-NLS-1$
		int currentValue = configManager.getCurrentSampleRate();
		Set<String> availableRates = configManager.getAvailableSampleRates();
		String[] array = availableRates.toArray(new String[0]);
		int[] vals = new int[array.length];
		for (int i = 0; i < array.length; i++) {
			vals[i] = Integer.valueOf(array[i]);
		}
		Arrays.sort(vals);

		final boolean isSelectedValueGreater = (selectedValue > currentValue);
		Arrays.sort(vals);
		final int difference = Math.abs(Arrays
				.binarySearch(vals, selectedValue)
				- Arrays.binarySearch(vals, currentValue));
		Log.d(TAG, "difference :: " + difference); //$NON-NLS-1$
		if (connectionFactory != null && connectionFactory.isRead_trigger()) {
			AlertDialogHelper
					.showDialog(
							this,
							Messages.getString("ControlPanel.msg_alert_dialog_confirm"), "OK", //$NON-NLS-1$ //$NON-NLS-2$
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									sendSampleChangeQuery(difference,
											isSelectedValueGreater);
									configManager.setCurrentSampleRate(String
											.valueOf(selectedValue));
								}

								//
							}, null, null, null, null, null);
			return;
		}

		sendSampleChangeQuery(difference, isSelectedValueGreater);
		configManager.setCurrentSampleRate(String.valueOf(selectedValue));

	}

	private void sendSampleChangeQuery(int difference,
			boolean isSelectedValueGreater) {
		if (connectionFactory == null || difference > 5)
			return;
		for (int i = 0; i < difference; i++) {
			if (isSelectedValueGreater)
				connectionFactory.doubleSampleRate();
			else
				connectionFactory.halveSampleRate();

		}

	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d(TAG, "Preference Changed"); //$NON-NLS-1$

		String listPrefKey = Messages
				.getString("ControlPanel.key_sample_rates_key_pref"); //$NON-NLS-1$
		if (key.equals(listPrefKey)) {

			int selectedValue = Integer.valueOf(configManager
					.getCurrentListValue());
			Log.d(TAG, "Selected :: " + selectedValue); //$NON-NLS-1$
			if (selectedValue > 0 && connectionFactory!=null && connectionFactory.getState()==BluetoothConnectionFactory.STATE_CONNECTED)
				changeSampleRate(selectedValue);

		}

		else if (key.equals(getString(R.string.key_device_name))) {
			try {
				device_name = (EditTextPreference)this.findPreference(getString(R.string.key_device_name));
				device_name.setSummary(device_name.getText()
						+ "\nClick to rename");
			} catch (NullPointerException e) {
				e.printStackTrace();
			}

		}

	}

}
