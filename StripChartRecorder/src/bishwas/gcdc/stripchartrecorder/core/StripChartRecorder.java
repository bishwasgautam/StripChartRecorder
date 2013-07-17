package bishwas.gcdc.stripchartrecorder.core;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ZoomControls;
import bishwas.gcdc.stripchartrecorder.graph.GraphAssistant;
import bishwas.gcdc.stripchartrecorder.helpers.AlertDialogHelper;
import bishwas.gcdc.stripchartrecorder.helpers.BluetoothConnectionFactory;
import bishwas.gcdc.stripchartrecorder.helpers.ConfigManager;
import bishwas.gcdc.stripchartrecorder.helpers.DemoDataSimulator;

import com.jjoe64.graphview.LineGraphView;

/**
 * StripChartRecorder is the main activity of the application, which defines the
 * overall layout, tabs, graphView and their real binding. Establishes all the
 * connections and co-ordinates between the UI and data. Also takes care of
 * starting/ stoping Demo mode and so on..
 * 
 * @author Bishwas Gautam
 * 
 *         Copyright (C) 2011 Gulf Coast Data Concepts Licensed under the GNU
 *         Lesser General Public License (LGPL)
 *         http://www.gnu.org/licenses/lgpl.html
 */

@SuppressWarnings("deprecation")
public class StripChartRecorder extends TabActivity {

	private static final boolean D = true;
	private boolean lookedForDevice = false;

	/**
	 * Message types sent from the BluetoothConnectionFactory Handler
	 */
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_CONNECTION_ERROR = 3;
	public static final int MESSAGE_DEVICE_INFO = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_AVAILABLE_SAMPLE_RATES = 6;
	public static final int MESSAGE_STATUS_INFO = 7;
	public static final int MESSAGE_CONFIGURATION_INFO = 8;
	public static final int MESSAGE_NEW_DEVICE = 9;
	public static final int MESSAGE_DISCOVERY_FINISHED = 10;
	public static final int MESSAGE_DEMO_RUN_FINISH = 11;
	public static final int MESSAGE_NEW_STRIPCHART = 12;

	/**
	 * Intent request codes
	 */
	private static final int REQUEST_CONNECT_DEVICE = 1;

	private static final int REQUEST_ENABLE_BT = 2;

	/**
	 * Debugging
	 */
	private static final String TAG = "MainScreen"; //$NON-NLS-1$

	// Key names received from the BluetoothConnectionFactory Handler
	public static final String DEVICE_NAME = "device_name"; //$NON-NLS-1$
	public static final String DEVICE_ADDRESS = "device_address"; //$NON-NLS-1$
	public static final String TOAST = "toast"; //$NON-NLS-1$
	public static final String CONNECTION_ERROR_TYPE = "none"; //$NON-NLS-1$

	// Intent Extra Keys
	public static final String CONNECTION_FACTORY = "connection_factory"; //$NON-NLS-1$
	public static final String CONFIG_MANAGER = "config_manager"; //$NON-NLS-1$

	/**
	 * Name of the connected device
	 */
	private String myConnectedDeviceName = null;
	private String myConnectedDeviceAddress = null;

	/**
	 * Shows a dialog on screen
	 */
	private ProgressDialog progressDialog = null;

	/**
	 * UI elements
	 */
	private Button btn_ppr, btn_stop, btn_zoom_reset, btn_clear;

	private TextView txtView_connected_to, txtView_receiving_msg;
	private CheckBox channel_x, channel_y, channel_z, channel_rms;
	private ZoomControls zoom_controls;
	private ToggleButton tb_autoscale;

	private MenuItem mMenuItemConnect, menu_startDemo;

	public ArrayList<BluetoothDevice> newDevices = new ArrayList<BluetoothDevice>();

	/**
	 * Local bluetooth adapter
	 */
	private BluetoothAdapter myBluetoothAdapter = null;

	/**
	 * Handles most of the graph related tasks
	 */
	private GraphAssistant graphAssistant, t;
	/**
	 * 
	 * This class takes care of extracting demo values from a text file and
	 * sending those values to handler
	 */
	private DemoDataSimulator demo;

	/**
	 * The main graph view element
	 */
	private LineGraphView stripChart;
	/**
	 * Parent view for stripChart
	 */
	private LinearLayout graphContainer;
	/**
	 * handles Preferences, has set and get methods
	 */

	LayoutInflater mylayoutInflater;

	private ConfigManager configManager;
	/**
	 * Handles messages from other threads
	 */
	private MessageHandler myHandler;

	/**
	 * Takes care of handling graph according to check on the button
	 */
	private CompoundButton.OnCheckedChangeListener myDynamicCheckChangeListener;
	/**
	 * Listener for clickables
	 */
	private OnClickListener myClickListener;

	/**
	 * myConnectionFactory : Handles all the bluetooth related tasks including
	 * connecting/ disconnecting/ reading from and writing to bluetooth device
	 * Also pushes commands to the RN42 device to change sample rates
	 * 
	 */
	private BluetoothConnectionFactory myConnectionFactory = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "++ON CREATE ++ "); //$NON-NLS-1$
		// Debug.startMethodTracing("stats");
		// requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);

		if (myHandler == null)
			myHandler = new MessageHandler(this);

		if (configManager == null)
			configManager = ConfigManager.getConfigManager(this);

		if (graphAssistant == null)
			graphAssistant = new GraphAssistant(this, getResources().getText(
					R.string.chart_name).toString());

		// Setup the user interface
		setupUI();

		bindGraphViewToViewContainer();

		// Get the local bluetooth adapter
		if (myBluetoothAdapter == null)
			myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If no bluetooth adapter is found (null), Bluetooth is unsupported
		if (myBluetoothAdapter == null) {

			AlertDialogHelper.showDialog(StripChartRecorder.this,
					"No bluetooth was detected", "View Demo",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							dialog.dismiss();

							startDemo();
							showViews();

						}
					}, "Exit", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							finish();

						}
					}, "Get Device", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							Uri uri = Uri
									.parse("http://yhst-21371435622982.stores.yahoo.net/data-acquisition.html");
							Intent intent = new Intent(Intent.ACTION_VIEW, uri);
							startActivity(intent);
						}
					}, null);

		}

	}

	/**
	 * Binds the graph view to graph Container (linear layout)
	 */
	private void bindGraphViewToViewContainer() {
		stripChart = graphAssistant.getStripChart();
		if (graphContainer == null)
			graphContainer = (LinearLayout) findViewById(R.id.graphViewLayout);

		graphContainer.addView(stripChart);

	}

	/**
	 * Sets up environment for demo mode and starts it
	 * 
	 */
	protected void startDemo() {
		if (myConnectionFactory != null)
			myConnectionFactory = null;

		demo = new DemoDataSimulator(myHandler);
		demo.getFileData(this);
		if (!configManager.isPreferencesInitialized())
			checkAllChannels();
		refreshChannels();
		if (!graphAssistant.hasStarted()) {

			graphAssistant.start();
		}
		demo.start();
		showViews();

	}

	@Override
	public void onStart() {
		super.onStart();
		if (!getTabHost().getCurrentTabTag().equals("main"))
			return;
		if (demo != null && demo.isRunning())
			return;
		if (D)
			Log.e(TAG, "++ON START ++ "); //$NON-NLS-1$

		// If bluetooth is not turned on, request that it be turned on.

		if (!myBluetoothAdapter.isEnabled()) {

			// Ask if the user wants demo mode
			/*---Write code here--


			 */

			enableBT();

		}
		// Initialize myConnectionFactory

		if (myConnectionFactory == null)
			myConnectionFactory = new BluetoothConnectionFactory(this,
					myHandler);

		if (configManager == null)
			configManager = ConfigManager.getConfigManager(this);

		hideProgressDialog();
		AlertDialogHelper.hideDialog();

		if (myConnectionFactory != null
				&& myConnectionFactory.getState() != BluetoothConnectionFactory.STATE_CONNECTED) {
			if (configManager.isPreferencesInitialized()) {

				String defaultdeviceAddress = configManager
						.getDefaultDeviceMacAddress();
				// Debugging
				Log.d(TAG,
						"Default device :: " + defaultdeviceAddress.toString()); //$NON-NLS-1$

				if (defaultdeviceAddress.equals("DNE"))findDevicesInRange(); //$NON-NLS-1$
				else
					connectDevice(defaultdeviceAddress);

			} else {
				Log.d(TAG, "++++++FIRST RUN+++++++"); //$NON-NLS-1$
				showProgressDialog("App first run. Please wait.... "); //$NON-NLS-1$
				findDevicesInRange();
			}
		}
	}

	/* This method filters and handles all intent callbacks */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode); //$NON-NLS-1$
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object

				// Attempt to connect to the device

				txtView_connected_to.setText(R.string.connecting_to);

				txtView_connected_to.setVisibility(View.VISIBLE);

				String deviceAddress = configManager
						.getDefaultDeviceMacAddress();
				if (deviceAddress.equals("DNE") || !deviceAddress.equals(address)) { //$NON-NLS-1$
					configManager.setDefaultDeviceMacAddress(address);
				}

				connectDevice(address);

			}

			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {

				Toast.makeText(this, R.string.bt_enabled, Toast.LENGTH_SHORT)
						.show();
				// Bluetooth is now enabled. Connect to default device or let
				// user choose

			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled"); //$NON-NLS-1$
				Toast.makeText(this, R.string.bt_not_enabled,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "++ON RESUME"); //$NON-NLS-1$

		try {
			graphAssistant._resume();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public synchronized void onPause() {
		super.onPause();

		if (D)
			Log.e(TAG, "--ON PAUSE--"); //$NON-NLS-1$

		try {
			graphAssistant._pause();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (D)
			Log.e(TAG, "--ON DESTROY--"); //$NON-NLS-1$

		BluetoothConnectionFactory.userDisconnected = true;

		try {
			unregisterReceiver(myReceiver);
		} catch (Exception e) {
			Log.e(TAG, "**onDestroy** Cannot unregister receiver : myReceiver");
			e.printStackTrace();
		}

		if (myConnectionFactory != null) {
			myConnectionFactory.stop();
			myConnectionFactory = null;
		}

		finish();
	}

	/**
	 * Handler to handle messages from child activities
	 */
	static class MessageHandler extends Handler {
		private final WeakReference<StripChartRecorder> outerClass;

		MessageHandler(StripChartRecorder ref) {
			outerClass = new WeakReference<StripChartRecorder>(ref);
		}

		@Override
		public void handleMessage(Message msg) {
			StripChartRecorder recorder = outerClass.get();
			if (recorder != null) {

				recorder.messageProcessPort(msg);

			}

		}

	}

	/**
	 * Extracts x,y,z values from incoming string and sends to current
	 * GraphAssistant thread
	 * 
	 * @param readMessage
	 *            Incoming String containing x,y,z values separated by comma(,)
	 */
	private void feedValuestoGraph(String readMessage) {
		int i;

		if (readMessage == null)
			return;

		String[] values = readMessage.split(","); //$NON-NLS-1$
		double timeStamp = Double.valueOf(values[0]);

		synchronized (this) {
			// if(!graphAssistant.isAlive())return;
			t = this.graphAssistant;

		}
		for (i = 1; i < values.length; i++) {
			double value = Double.valueOf(values[i]);
			t.newValue(i - 1, timeStamp, value);

		}

		t.redraw();

		graphContainer.invalidate();

		t = null;

	}

	/**
	 * Processes messages from the handler
	 * 
	 * @param incoming
	 *            : Incoming message to process
	 */
	public void messageProcessPort(Message incoming) {
		String readMessage;
		switch (incoming.what) {
		case MESSAGE_STATE_CHANGE:
			if (D)
				Log.i(TAG, "MESSAGE STATE CHANGED :  " + incoming.arg1); //$NON-NLS-1$
			switch (incoming.arg1) {
			case BluetoothConnectionFactory.STATE_CONNECTED:

				// Dismiss the dialog if it is showing
				hideProgressDialog();

				Toast.makeText(getApplicationContext(),
						"Connection established", Toast.LENGTH_SHORT).show(); //$NON-NLS-1$

				if (!configManager.isPreferencesInitialized())

					initializeOnFirstRun();
				else
					initializePreferences();

				showViews();
				// Start streaming
				myConnectionFactory.startStreaming();

				if (mMenuItemConnect != null) {
					mMenuItemConnect
							.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
					mMenuItemConnect.setTitle(R.string.disconnect);
				}

				if (menu_startDemo != null)
					menu_startDemo.setEnabled(false);

				lookedForDevice = false;
				BluetoothConnectionFactory.userDisconnected = false;
				try {

					unregisterReceiver(myReceiver);

				} catch (Exception e) {
					Log.d(TAG, "Cannot unregisterReceiver :: "); //$NON-NLS-1$
					e.printStackTrace();
				}

				if (graphAssistant != null && !graphAssistant.hasStarted())
					graphAssistant.start();

				break;
			case BluetoothConnectionFactory.STATE_CONNECTING:
				// Show progress bar
				break;
			case BluetoothConnectionFactory.STATE_NONE:
				if (mMenuItemConnect != null) {
					mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
					mMenuItemConnect.setTitle(R.string.connect);
				}
				if (demo == null && menu_startDemo != null) {
					menu_startDemo.setEnabled(true);
				}
				break;
			}

		case MESSAGE_READ:
			try {
				readMessage = (String) incoming.obj;
				// Change the label or text to readMessage
			} catch (NullPointerException e) {

				Log.e(TAG, e.toString());
				e.printStackTrace();
				break;
			}
			if (readMessage != null) {

				txtView_receiving_msg.setText("Receiving : " + readMessage); //$NON-NLS-1$

				feedValuestoGraph(readMessage);
			}

			break;

		case MESSAGE_CONNECTION_ERROR:
			int errorType = incoming.getData().getInt(CONNECTION_ERROR_TYPE);

			if (errorType == BluetoothConnectionFactory.CONNECTION_FAILED) {

				// If already not looked for devices in range
				if (!lookedForDevice)
					findDevicesInRange();
				else
				// Cannot connect, display failed status
				{

					hideProgressDialog();

					AlertDialogHelper.showDialog(
							StripChartRecorder.this,
							"Unable to connect to device : "
									+ configManager.getDefaultDeviceName(),
							"View Demo", new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {

									dialog.dismiss();

									startDemo();

								}
							}, null, null, "Retry",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									connectDevice(configManager
											.getDefaultDeviceMacAddress());

								}
							}, null);

				}

			}
			// For errorType :::CONNECTION_LOST, connection is
			// re-established in BluetoothConnectionManager

			break;

		case MESSAGE_DEVICE_INFO:

			// save the connected device's name for future use
			myConnectedDeviceName = incoming.getData().getString(DEVICE_NAME);
			myConnectedDeviceAddress = incoming.getData().getString(
					DEVICE_ADDRESS);

			// Save the device info

			configManager.setDefaultDeviceName(myConnectedDeviceName);
			configManager.setDefaultDeviceMacAddress(myConnectedDeviceAddress);

			break;
		case MESSAGE_TOAST:
			Toast.makeText(getApplicationContext(),
					incoming.getData().getString(TOAST), Toast.LENGTH_SHORT)
					.show();
			break;

		case MESSAGE_AVAILABLE_SAMPLE_RATES:
			// readBuffer= (byte[])msg.obj;

			try {
				readMessage = (String) incoming.obj;

			} catch (NullPointerException e) {

				Log.e(TAG, "Error parsing available sample rates");
				// If error occured, keep reading until rates are fetched

				myConnectionFactory.readSampleRates();

				e.printStackTrace();
				break;
			}
			// Save retrieved sample Rates
			Log.d("sample", "Available Sample Rate :: " + readMessage); //$NON-NLS-1$ //$NON-NLS-2$
			if (readMessage.contains("SR")) //$NON-NLS-1$
				configManager.setAvailableSampleRates(readMessage);
			else if (readMessage.contains("SENS")); //$NON-NLS-1$
			else if (readMessage.contains("DEV")); //$NON-NLS-1$

			break;
		case MESSAGE_STATUS_INFO:

			try {
				readMessage = (String) incoming.obj;
			} catch (NullPointerException e) {

				Log.e(TAG, e.toString());
				// If error occured, keep reading until rates are fetched

				myConnectionFactory.getStatusInfo();

				e.printStackTrace();
				break;
			}

			// Save retrieved sample Rates
			Log.d("sample", "STATUS_INFO " + readMessage); //$NON-NLS-1$ //$NON-NLS-2$

			if (readMessage.contains("Bat"))configManager.setCharge(readMessage); //$NON-NLS-1$

			else if (readMessage.contains("SR"))configManager.setCurrentSampleRate(readMessage); //$NON-NLS-1$
			else if (readMessage.contains("DB"))configManager.setDeadband(readMessage); //$NON-NLS-1$
			else if (readMessage.contains("ROD"))configManager.setRebootOnDisconnect(readMessage); //$NON-NLS-1$

			break;
		case MESSAGE_CONFIGURATION_INFO:

			try {
				readMessage = (String) incoming.obj;

			} catch (NullPointerException e) {

				Log.e(TAG, e.toString());
				// If error occured, keep reading until rates are fetched

				myConnectionFactory.getConfigurationInfo();

				e.printStackTrace();
				break;
			}
			// Save retrieved sample Rates
			Log.d("sample", "Config Info :: " + readMessage); //$NON-NLS-1$ //$NON-NLS-2$
			if (readMessage.contains("Temperature")) //$NON-NLS-1$
				configManager.setTemperatureStatus(readMessage);
			else if (readMessage.contains("Gain, low")) //$NON-NLS-1$
				configManager.setGain(readMessage);
			break;
		case MESSAGE_DEMO_RUN_FINISH:
			demo = null;
			if (menu_startDemo != null)
				menu_startDemo.setEnabled(true);
			break;
		case MESSAGE_NEW_STRIPCHART:
			try {
				graphContainer.removeAllViews();
			} catch (Exception e) {
				Log.e(TAG, "Error removing views from parent \n");
				e.printStackTrace();

			}
			bindGraphViewToViewContainer();
			graphContainer.invalidate();
			break;

		}
	}

	/**
	 * Handles BT intents Looks for matching RN42 BT devices
	 */
	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.d(TAG, "Found device: " + device.getName()); //$NON-NLS-1$
				try {
					if (device.getName().substring(0, 4).equals("RN42")) { //$NON-NLS-1$
						Log.d(TAG,
								"Matching device in range:: " + device.getAddress()); //$NON-NLS-1$
						adapter.add(device.getName()
								+ "\n" + device.getAddress()); //$NON-NLS-1$
					}

				} catch (NullPointerException e) {

				}
				// When discovery is finished, change the Activity title
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				Log.d(TAG, "Discovery Finished"); //$NON-NLS-1$
				Log.d(TAG,
						"Devices found = " + StripChartRecorder.this.newDevices.size()); //$NON-NLS-1$
				if (adapter.getCount() > 0)
					showMatchingDevicesForConnect();
				else {
					// Notify user that no device was found in range
					hideProgressDialog();
					if (configManager.getDefaultDeviceMacAddress()
							.equals("DNE")) {
						AlertDialogHelper
								.showDialog(
										StripChartRecorder.this,
										"No device was found in range. You need a RN42 device for application to run",
										"View Demo",
										new DialogInterface.OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {

												dialog.dismiss();

												startDemo();
												showViews();

											}
										}, null, null, "Get Device",
										new DialogInterface.OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {

												Uri uri = Uri
														.parse("http://yhst-21371435622982.stores.yahoo.net/data-acquisition.html");
												Intent intent = new Intent(
														Intent.ACTION_VIEW, uri);
												startActivity(intent);
											}
										}, null);
					} else
						AlertDialogHelper.showDialog(StripChartRecorder.this,
								"No device was found in range", "View Demo",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {

										dialog.dismiss();

										startDemo();
										showViews();

									}
								}, null, null, null, null, null);

				}
			}
		}
	};

	private ArrayAdapter<String> adapter;

	/**
	 * Show a choice dialog with list of matching RN42 devices
	 * 
	 */
	private void showMatchingDevicesForConnect() {

		mylayoutInflater = LayoutInflater.from(this);
		View content = mylayoutInflater.inflate(R.layout.dialog_layout, null);
		ListView lv = (ListView) content.findViewById(R.id.list);
		lv.setAdapter(adapter);
		lv.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> aView, View v, int arg2,
					long arg3) {
				String info = ((TextView) v).getText().toString();
				String address = info.substring(info.length() - 17);
				AlertDialogHelper.hideDialog();
				connectDevice(address);

			}
		});

		hideProgressDialog();
		AlertDialogHelper
				.showDialog(
						this,
						"Select a RN42 device to connect to", null, null, null, null, myConnectedDeviceAddress, null, content); //$NON-NLS-1$

	}

	/**
	 * Dismisses progress dialog if showing
	 */
	private void hideProgressDialog() {
		if (progressDialog == null)
			return;

		if (progressDialog.isShowing())
			progressDialog.dismiss();

	}

	/**
	 * Fetches device status and stores them in Preferences
	 */
	public void initializePreferences() {
		Log.d(TAG, " Initializing Preferences ...."); //$NON-NLS-1$

		showProgressDialog("Please bear with me, now initializing preferences.."); //$NON-NLS-1$

		myConnectionFactory.getStatusInfo();
		android.os.SystemClock.sleep(100);
		myConnectionFactory.getConfigurationInfo();
		android.os.SystemClock.sleep(100);
		BluetoothConnectionFactory.resetCommandReference();
		BluetoothConnectionFactory.start_reading_preferences = false;

		hideProgressDialog();
	}

	private void refreshChannels() {
		channel_x.setChecked(configManager
				.isChannelChecked(ConfigManager.KEY_CHANNEL_X));
		channel_y.setChecked(configManager
				.isChannelChecked(ConfigManager.KEY_CHANNEL_Y));
		channel_z.setChecked(configManager
				.isChannelChecked(ConfigManager.KEY_CHANNEL_Z));
		channel_rms.setChecked(configManager
				.isChannelChecked(ConfigManager.KEY_CHANNEL_RMS));

	}

	/**
	 * Sets Prefs corresponding to channels to true
	 */
	private void checkAllChannels() {
		// Set all to true (Default)
		configManager.setChannelChecked(ConfigManager.KEY_CHANNEL_X, true);
		configManager.setChannelChecked(ConfigManager.KEY_CHANNEL_Y, true);
		configManager.setChannelChecked(ConfigManager.KEY_CHANNEL_Z, true);
		configManager.setChannelChecked(ConfigManager.KEY_CHANNEL_RMS, true);

	}

	/**
	 * Run once at the start Fetches and stores available sample rates from
	 * device
	 */
	public void initializeOnFirstRun() {
		Log.d(TAG, "****First Run*****");
		getAndStoreDeviceBuildInfo();

		myConnectionFactory.readSampleRates();
		android.os.SystemClock.sleep(100);
		checkAllChannels();
		configManager.setDefaultInitialized();

		initializePreferences();

	}

	/**
	 * Check and stores device build
	 */
	public void getAndStoreDeviceBuildInfo() {
		int SDK_INT = android.os.Build.VERSION.SDK_INT;

		int value = (SDK_INT >= 11) ? ConfigManager.BUILD_OVER_11
				: ConfigManager.BUILD_LESS_THAN_11;
		configManager.set_is_BUILD_OVER_11(value);
	}

	/**
	 * Look for RN42 device in range
	 */
	private void findDevicesInRange() {
		if (adapter == null)
			adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_single_choice);

		if (!myBluetoothAdapter.isEnabled()) {
			enableBT();
		}

		showProgressDialog("Looking for RN42 device in range"); //$NON-NLS-1$

		lookedForDevice = true;

		// Start Discovery

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

		// Register for broadcasts when discovery has finished
		this.registerReceiver(myReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(myReceiver, filter);

		if (myBluetoothAdapter.isDiscovering())
			myBluetoothAdapter.cancelDiscovery();
		myBluetoothAdapter.startDiscovery();

	}

	/**
	 * Takes care of initialization and configurations of all the buttons, text
	 * views and tab
	 */
	private void setupUI() {

		// Setup the tabs
		SetupTab();
		// Setup check buttons and graph
		setupGraphControl_UI_Elements();
		// Setup buttons and TVs
		setupButtonsAndTextViews();
		// Setup zoom controls
		setupZoomControls();
		// Refresh channel checks
		refreshChannels();
	}

	/**
	 * Assign click listeners to zoomControls and zoom Reset button
	 */
	private void setupZoomControls() {
		this.zoom_controls = (ZoomControls) findViewById(R.id.zoomControls);

		zoom_controls.setOnZoomInClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// Lower max and min vals
				if (configManager.isVerticalAxisAutoScaled())
					return;
				if (!graphAssistant.zoom(true))
					zoom_controls.setIsZoomInEnabled(false);
				else {
					zoom_controls.setIsZoomInEnabled(true);
					zoom_controls.setIsZoomOutEnabled(true);
				}
			}
		});

		zoom_controls.setOnZoomOutClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (configManager.isVerticalAxisAutoScaled())
					return;
				// Increase max and min vals
				if (!graphAssistant.zoom(false))
					zoom_controls.setIsZoomOutEnabled(false);
				else {
					zoom_controls.setIsZoomInEnabled(true);
					zoom_controls.setIsZoomOutEnabled(true);
				}

			}
		});

	}

	/**
	 * Assigns buttons, textViews from resources and binds OnClick Listeners if
	 * required
	 */
	private void setupButtonsAndTextViews() {
		/* Initialize the text views */

		// Shows connected device info
		txtView_connected_to = (TextView) findViewById(R.id.connected_to_textview);

		// Shows streaming message from the connected device
		txtView_receiving_msg = (TextView) findViewById(R.id.stream_message_textview);

		myClickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.btn_ppr:
					String label = (String) btn_ppr.getText();
					if (label.equals("Play")) {
						if (graphAssistant != null
								&& graphAssistant.hasStarted()) {

							if (t == null) {
								synchronized (this) {
									t = graphAssistant;

								}
								t.setHandler(myHandler);
								t.clear();
								t = null;
							}
						}

						if (myConnectionFactory == null) {
							startDemo();
							showViews();
						}

						else if (demo == null
								&& myConnectionFactory != null
								&& myConnectionFactory.getState() == BluetoothConnectionFactory.STATE_CONNECTED)
							myConnectionFactory.startStreaming();

						btn_ppr.setText(R.string.pause);
						btn_stop.setEnabled(true);
						btn_clear.setEnabled(true);
						if (t == null) {
							synchronized (this) {
								t = graphAssistant;
							}

						}
						t._resume();
						t = null;

					} else if (label.equals("Pause")) {
						if (t == null) {
							synchronized (this) {
								t = graphAssistant;
							}

						}
						t._pause();
						t = null;
						btn_ppr.setText(R.string.resume);

					} else if (label.equals("Resume")) {
						if (t == null) {
							synchronized (this) {
								t = graphAssistant;
							}

						}
						t._resume();
						t = null;
						btn_ppr.setText(R.string.pause);
					} else
						;
					break;
				case R.id.btn_stop:

					if (demo != null) {
						demo._stop();
						demo = null;
					}

					else if (demo == null
							&& myConnectionFactory != null
							&& myConnectionFactory.getState() == BluetoothConnectionFactory.STATE_CONNECTED)
						myConnectionFactory.stopStreaming();
					btn_stop.setEnabled(false);
					btn_ppr.setEnabled(true);
					btn_ppr.setText(R.string.play);
					break;
				case R.id.btn_clear_graph:

					if (demo != null) {
						demo._stop();
						demo = null;

					} else if (myConnectionFactory != null
							&& myConnectionFactory.getState() == BluetoothConnectionFactory.STATE_CONNECTED)
						myConnectionFactory.stopStreaming();

					if (t == null) {
						synchronized (this) {
							t = graphAssistant;
						}

					}
					t.setHandler(myHandler);
					t.clear();
					t = null;

					btn_ppr.setEnabled(true);
					btn_stop.setEnabled(false);
					btn_ppr.setText(R.string.play);
					btn_clear.setEnabled(false);

					break;
				case R.id.btn_zoom_reset:
					if (t == null) {
						synchronized (this) {
							t = graphAssistant;
						}
					}
					t.zoomReset();

					t = null;
					break;

				}

			}
		};

		btn_ppr = (Button) findViewById(R.id.btn_ppr);
		btn_ppr.setOnClickListener(myClickListener);

		btn_stop = (Button) findViewById(R.id.btn_stop);
		btn_stop.setOnClickListener(myClickListener);

		btn_clear = (Button) findViewById(R.id.btn_clear_graph);
		btn_clear.setOnClickListener(myClickListener);

		btn_zoom_reset = (Button) findViewById(R.id.btn_zoom_reset);
		btn_zoom_reset.setOnClickListener(myClickListener);

	}

	/**
	 * Initializes myDynamicCheckChangeListener Binds channel check box and
	 * autoScale toggle with graph
	 */
	private void setupGraphControl_UI_Elements() {
		if (myDynamicCheckChangeListener == null)
			myDynamicCheckChangeListener = new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {

					switch (buttonView.getId()) {
					case (R.id.cb_x):
						configManager.setChannelChecked(
								ConfigManager.KEY_CHANNEL_X, isChecked);
						if (!isChecked)
							graphAssistant
									.removeSeriesFromStripChart(GraphAssistant.INDEX_OF_X);
						else
							graphAssistant
									.addSeriesToStripChart(GraphAssistant.INDEX_OF_X);
						break;
					case (R.id.cb_y):
						configManager.setChannelChecked(
								ConfigManager.KEY_CHANNEL_Y, isChecked);
						if (!isChecked)
							graphAssistant
									.removeSeriesFromStripChart(GraphAssistant.INDEX_OF_Y);
						else
							graphAssistant
									.addSeriesToStripChart(GraphAssistant.INDEX_OF_Y);
						break;
					case (R.id.cb_z):
						configManager.setChannelChecked(
								ConfigManager.KEY_CHANNEL_Z, isChecked);
						if (!isChecked)
							graphAssistant
									.removeSeriesFromStripChart(GraphAssistant.INDEX_OF_Z);
						else
							graphAssistant
									.addSeriesToStripChart(GraphAssistant.INDEX_OF_Z);
						break;
					case (R.id.cb_rms):
						configManager.setChannelChecked(
								ConfigManager.KEY_CHANNEL_RMS, isChecked);
						if (!isChecked)
							graphAssistant
									.removeSeriesFromStripChart(GraphAssistant.INDEX_OF_RMS);
						else
							graphAssistant
									.addSeriesToStripChart(GraphAssistant.INDEX_OF_RMS);
						break;
					case (R.id.tb_autoscale):
						configManager.setVerticalAxisAutoScaled(isChecked);
						graphAssistant.setYAxisAutoScale(isChecked);
						break;
					default:
						Log.d(TAG, "Something must have gone wrong, ID: <"
								+ buttonView.getId() + ">");
						break;
					}
				}
			};

		channel_x = (CheckBox) findViewById(R.id.cb_x);
		channel_y = (CheckBox) findViewById(R.id.cb_y);
		channel_z = (CheckBox) findViewById(R.id.cb_z);
		channel_rms = (CheckBox) findViewById(R.id.cb_rms);

		channel_x.setOnCheckedChangeListener(this.myDynamicCheckChangeListener);
		channel_y.setOnCheckedChangeListener(this.myDynamicCheckChangeListener);
		channel_z.setOnCheckedChangeListener(this.myDynamicCheckChangeListener);
		channel_rms
				.setOnCheckedChangeListener(this.myDynamicCheckChangeListener);

		tb_autoscale = (ToggleButton) findViewById(R.id.tb_autoscale);
		tb_autoscale.setOnCheckedChangeListener(myDynamicCheckChangeListener);

		// Reset channel checks (from last save)
		restoreChannelCheckStat();

	}

	/**
	 * Resets checks for CheckBox elements from last saved status in Preferences
	 */
	public void restoreChannelCheckStat() {
		channel_x.setChecked(configManager
				.isChannelChecked(ConfigManager.KEY_CHANNEL_X));
		channel_y.setChecked(configManager
				.isChannelChecked(ConfigManager.KEY_CHANNEL_Y));
		channel_z.setChecked(configManager
				.isChannelChecked(ConfigManager.KEY_CHANNEL_Z));
		channel_rms.setChecked(configManager
				.isChannelChecked(ConfigManager.KEY_CHANNEL_RMS));
	}

	/**
	 * Shows BT device list for connect
	 */
	protected void startManualConnectIntent() {
		Intent showDevices = new Intent(StripChartRecorder.this,
				DeviceListActivity.class);
		startActivityForResult(showDevices, REQUEST_CONNECT_DEVICE);

	}

	/**
	 * Triggers an intent for user's permission to turn the bluetooth on.
	 */
	private void enableBT() {

		// Trigger an intent to turn the bluetooth on, requires user's approval
		Intent turnOnBluetoothIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(turnOnBluetoothIntent, REQUEST_ENABLE_BT);
		// Intent a=new Intent(BluetoothDevice.class.)

	}

	/**
	 * Changes visibility of textView and button elements pertinent to
	 * connection establishment
	 */
	private void showViews() {
		txtView_connected_to.setVisibility(View.VISIBLE);
		txtView_receiving_msg.setVisibility(View.VISIBLE);
		txtView_connected_to.setText(R.string.connected_to);
		if (myConnectedDeviceName != null)
			txtView_connected_to.append(" " + myConnectedDeviceName.toString());
		else
			txtView_connected_to.append(" " + "My Demo Device");//$NON-NLS-1$
		btn_ppr.setEnabled(true);
		btn_stop.setEnabled(true);
		btn_ppr.setText("Pause");
	}

	/**
	 * SetupTab :: Configure and setup the tab UI
	 */
	private void SetupTab() {
		TabHost host = getTabHost();

		TabHost.TabSpec homeTabSpecs = host.newTabSpec("main"); //$NON-NLS-1$
		homeTabSpecs.setIndicator("Home"); //$NON-NLS-1$
		homeTabSpecs.setContent(R.id.main_relative_layout);

		TabHost.TabSpec controlPanelTabSpecs = host.newTabSpec("control_panel"); //$NON-NLS-1$
		Intent in1 = new Intent(this, ControlPanel.class);
		if (myConnectionFactory != null)
			in1.putExtra(CONNECTION_FACTORY, this.myConnectionFactory);
		// in1.putExtra(CONFIG_MANAGER, this.configManager);
		controlPanelTabSpecs.setIndicator("Control Panel"); //$NON-NLS-1$
		controlPanelTabSpecs.setContent(in1);

		TabHost.TabSpec aboutTabSpecs = host.newTabSpec("about"); //$NON-NLS-1$
		Intent in2 = new Intent(this, About.class);

		aboutTabSpecs.setIndicator("About"); //$NON-NLS-1$
		aboutTabSpecs.setContent(in2);
		host.addTab(homeTabSpecs);
		host.addTab(controlPanelTabSpecs);
		host.addTab(aboutTabSpecs);

	}

	/**
	 * Connects to the device specified by address
	 * 
	 * @param deviceMacAddress
	 *            :bluetooth device's mac address
	 * 
	 */
	private void connectDevice(String deviceMacAddress) {

		if (myConnectionFactory == null)
			myConnectionFactory = new BluetoothConnectionFactory(this,
					myHandler);

		if (deviceMacAddress.equals("DNE")) { //$NON-NLS-1$
			Toast.makeText(
					getBaseContext(),
					"Cannot find device on file, please connect manually", Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
			return;
		}

		BluetoothDevice device = myBluetoothAdapter
				.getRemoteDevice(deviceMacAddress);
		String deviceName = device.getName();
		showProgressDialog("Connecting to device : " + deviceName); //$NON-NLS-1$
		myConnectionFactory.connect(device);
		configManager.setDefaultDeviceName(device.getName());

	}

	/**
	 * @param string
	 *            : message to show on Dialog
	 */

	private void showProgressDialog(String string) {

		if (progressDialog == null || !progressDialog.isShowing()) {
			progressDialog = ProgressDialog.show(this, null, string
					+ "\nTouch outside to dismiss");
			progressDialog.setCancelable(true);
			progressDialog
					.setOnCancelListener(new DialogInterface.OnCancelListener() {

						@Override
						public void onCancel(DialogInterface dialog) {
							hideProgressDialog();
						}
					});

		} else if (progressDialog.isShowing())
			progressDialog.setMessage(string + "\nTouch outside to dismiss");

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		mMenuItemConnect = menu.getItem(0);
		menu_startDemo = menu.getItem(1);

		return true;
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (myConnectionFactory == null)
			myConnectionFactory = new BluetoothConnectionFactory(this,
					myHandler);
		switch (item.getItemId()) {
		case (R.id.connect):
			if (myConnectionFactory.getState() == BluetoothConnectionFactory.STATE_NONE) {
				if (!myBluetoothAdapter.isEnabled()) {
					enableBT();
				}
				// Else show the devices list to connect to
				else {
					if (demo == null)
						startManualConnectIntent();
					else
						AlertDialogHelper.showDialog(this,
								"Stop demo mode and connect?", "OK",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
										stopDemo();

										startManualConnectIntent();

									}
								}, null, null, null, null, null);
				}
			} else if (myConnectionFactory.getState() == BluetoothConnectionFactory.STATE_CONNECTED) {

				myConnectionFactory.stop();

				resetUI();
				Toast.makeText(getApplicationContext(),
						"Disconnected", Toast.LENGTH_SHORT).show(); //$NON-NLS-1$

			}
			break;
		case (R.id.demo):
			if (demo == null)
				startDemo();
			else {
				Toast.makeText(this, "Already in demo mode", Toast.LENGTH_SHORT)
						.show();
				menu_startDemo.setEnabled(false);
			}

			break;
		}

		return true;

	}

	protected void stopDemo() {
		demo._stop();
		resetUI();

	}

	/**
	 * Clears the views dependent of graph or streaming data
	 * 
	 */
	private void resetUI() {
		txtView_connected_to.setText(R.string.connected_to);
		txtView_receiving_msg.setText(R.string.receiving_msg);
		btn_ppr.setEnabled(false);
		btn_stop.setEnabled(false);

	}

}
