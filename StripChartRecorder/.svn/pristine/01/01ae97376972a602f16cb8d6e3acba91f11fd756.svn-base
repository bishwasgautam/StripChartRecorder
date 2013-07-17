package bishwas.gcdc.stripchartrecorder.core;




import java.util.Set;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;
import bishwas.gcdc.stripchartrecorder.helpers.ConfigManager;



/**
 * Loads and manages Preference Fragments from file
 * Related to Control Panel Preference Fragments
 * Will run on Android 3.2 and after
 * 
 * @author Bishwas Gautam
 * 
 
 */
public class PrefFragment extends PreferenceFragment {

	ConfigManager configManager;
	Preference deviceInfoPref,configInfoPref,device_status,sample_rates;
	private ListPreference lp;
	private EditTextPreference device_name;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		configManager=ConfigManager.getConfigManager();
		initializeUI();


	}
	/**
	 * Loads header files according to the header name (what user clicks)
	 */
	private void initializeUI() {
		String prefFileName=getArguments().getString("resource");
		if(prefFileName.equals("device_info")){
			addPreferencesFromResource(R.xml.device_info);
			deviceInfoPref=findPreference("deviceInfoPref");
			//Show device name and address
			deviceInfoPref.setSummary("Name : "+configManager.getDefaultDeviceName() + "\n" +
					"Address : "+configManager.getDefaultDeviceMacAddress());
			
			device_name=(EditTextPreference)findPreference(getString(R.string.key_device_name));
			if(device_name!=null)
			device_name.setText(configManager.getDefaultDeviceName());
			
		}
		else if(prefFileName.equals("config_info")){
			addPreferencesFromResource(R.xml.config_info);
			configInfoPref=findPreference("configInfoPref");
			//Display configuration info
			configInfoPref.setSummary(configManager.getConfigInfo());
		}
		else if(prefFileName.equals("device_status")){
			addPreferencesFromResource(R.xml.device_status);
			device_status=findPreference("deviceStatusPref");
			//Display status info
			device_status.setSummary(configManager.getStatusInfo());
		}
		else if(prefFileName.equals("sample_rates")){
			addPreferencesFromResource(R.xml.sample_rates);
			lp=(ListPreference)findPreference("sample_rates_list_pref");
			sample_rates=findPreference("sampleRatePref");
			
			Set<String> availableRates=configManager.getAvailableSampleRates();
			String sampleRates=(availableRates==null)? "Not available":availableRates.toString();
			sample_rates.setSummary(sampleRates.toString());


			
			if(availableRates!=null){
			String[] array = availableRates.toArray(new String[0]);
			lp.setEntries(array);
			lp.setEntryValues(array);}else{
				
				lp.setEntries(new String[]{"12","25","50","100","200","400","800"});
				lp.setEntryValues(new String[]{"12","25","50","100","200","400","800"});
			}
			
			lp.setDefaultValue(configManager.getCurrentSampleRate());
			lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Log.d("PrefFragment","New val = "+newValue.toString());
					lp.setSummary(newValue.toString());
					return true;
				}
			});
			
			int currentSampleRate=configManager.getCurrentSampleRate();
			lp.setDefaultValue(currentSampleRate);
			if(currentSampleRate==0)
				lp.setSummary("Not Available");
			else lp.setSummary(String.valueOf(currentSampleRate));
		}
		

	}
	
	
}
