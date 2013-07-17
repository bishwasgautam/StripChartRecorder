/*Developer: Bishwas Gautam 
 * 2012*/

package bishwas.gcdc.stripchartrecorder.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import bishwas.gcdc.stripchartrecorder.core.StripChartRecorder;

/**
 * Handles most of the bluetooth related operations
 * Connects and sets up connection to the bluetooth device
 * Receives messages from the device and forwards it to the main activity
 * Sends commands to the device as well
 * 
 * @author Bishwas Gautam
 * 
 *  Copyright (C) 2011 Gulf Coast Data Concepts Licensed under the GNU Lesser
 *  General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
@SuppressWarnings("serial")
public class BluetoothConnectionFactory implements Serializable {

	// Debugging variables
	private static final String TAG = "BluetoothConnectionFactory";
	private static final boolean D = true;
	public static boolean isPreferenceRead = false;
	public static boolean userDisconnected;
	private BluetoothAdapter myAdapter;
	// private BluetoothDevice myDevice;
	private Handler myHandler;

	public static final int STATE_NONE = 0;
	public static final int STATE_LISTEN = 1;
	public static final int STATE_CONNECTING = 2;
	public static final int STATE_CONNECTED = 3;

	public static final int CONNECTION_LOST = 1;
	public static final int CONNECTION_FAILED = 2;

	private final int READ_AVAILABLE_SAMPLE_RATES = 1;
	private final int READ_CONFIGURATION_INFO = 2;
	private final int READ_STATUS_INFO = 3;
	private static int commandReference = 0;

	private BluetoothSocket connected_socket_for_connection = null;

	private final UUID myUUID = UUID
			.fromString("00001101-0000-1000-8000-00805f9b34fb");

	private ConnectThread myConnectThread;
	private ConnectedThread myConnectedThread;

	private int state;
	private boolean is_streaming_data = false;

	public static boolean start_reading_preferences = false;

	/**
	 * @param context : Main activity context
	 * @param handler: Handler to pass messages to main activity
	 */
	public BluetoothConnectionFactory(Context context, Handler handler) {
		myAdapter = BluetoothAdapter.getDefaultAdapter();
		myHandler = handler;
		setState(STATE_NONE);

	}

	/**
	 * @return current device connection status
	 */
	public synchronized int getState() {
		return state;
	}

	/**
	 * @param state: Sets current device connection status
	 */
	private synchronized void setState(int state) {
		Log.i(TAG, "State changed from " + this.state + " to " + state);
		this.state = state;

		// Give the new state to the Handler so the UI Activity can update
		myHandler.obtainMessage(StripChartRecorder.MESSAGE_STATE_CHANGE, state,
				-1).sendToTarget();

	}

	/**
	 * @param val: Sets what command values is being read from RN42 device
	 */
	private void setCommandReference(int val) {
		commandReference = val;
	}

	/**
	 * @return:  what command values is being read from RN42 device
	 */
	private int getCommandReference() {
		return commandReference;
	}

	/**
	 * resets command reference to default value 0
	 */
	public static void resetCommandReference() {

		commandReference = 0;
	}

	/**
	 * @param toastMsg: Pushes a message bundle indicating a toast to the main activity via handler
	 */
	public void toastMessage(String toastMsg) {

		// Send a failure message back to the Activity
		Message msg = myHandler.obtainMessage(StripChartRecorder.MESSAGE_TOAST);
		Bundle bundle = new Bundle();

		bundle.putString(StripChartRecorder.TOAST, toastMsg);
		msg.setData(bundle);
		myHandler.sendMessage(msg);
	}

	/**
	 * @return: current value of is_streaming_data : which indicates is data sample is being read from RN42 device 
	 */
	public boolean isRead_trigger() {
		return is_streaming_data;
	}

	/**
	 * Starts a new connected thread for read/write operations
	 */
	private void initiateConnectedThread() {
		if (myConnectedThread == null
				&& connected_socket_for_connection != null) {
			myConnectedThread = new ConnectedThread(
					connected_socket_for_connection);
			myConnectedThread.start();
		}
	}

	/**
	 * @param out: Writes byte out to the RN42 device via connected thread
	 */
	private void write(byte[] out) {

		// Create temporary object
		ConnectedThread r;

		// Start the connected thread to start reading and writing if not
		// started already
		initiateConnectedThread();

		synchronized (this) {
			if (getState() != STATE_CONNECTED)
				return;
			r = myConnectedThread;

		}
		// Perform the write unsynchronized
		r.write(out);

		// Nullify the object, because it's not needed
		r = null;
	}

	/**
	 * Sends device signal to start sending live data samples
	 */
	public void startStreaming() {
		// read_trigger=true;

		if (!is_streaming_data) {
			// Send the signal to the device to start streaming
			is_streaming_data = true;
			write("d".getBytes());
			// Start reading
			// Signal the method inside ConnectedThread to start reading

			start_reading_preferences = false;

		}

	}

	/**
	 * Send device signal to stop sending live data samples
	 */
	public void stopStreaming() {

		if (is_streaming_data) {
			// Stop Reading the streamed data
			is_streaming_data = false;
			// Send the signal to the device to stop sending data
			write("D".getBytes());
		}

	}

	/**
	 * Doubles current device sample rate
	 */
	public void doubleSampleRate() {
		if (is_streaming_data)
			stopStreaming();
		write("+".getBytes());
	}

	/**
	 * Halves current device sample rate
	 */
	public void halveSampleRate() {
		if (is_streaming_data)
			stopStreaming();
		write("-".getBytes());
	}

	/**
	 * Sends device a query to send sample rate info
	 */
	public void readSampleRates() {
		
		if (is_streaming_data)
			stopStreaming();
		
		Log.d(TAG, "requesting available sample rates");

		setCommandReference(READ_AVAILABLE_SAMPLE_RATES);
		start_reading_preferences = true;
		write("q".getBytes());

	}

	
	/**
	 * Sends device a query to send device status info
	 */
	public void getStatusInfo() {
		
		if (is_streaming_data)
			stopStreaming();
		setCommandReference(READ_STATUS_INFO);
		start_reading_preferences = true;
		write("s".getBytes());

	}
	/**
	 * Sends device a query to send device configuration info
	 */
	public void getConfigurationInfo() {
		BluetoothConnectionFactory.isPreferenceRead = false;
		if (is_streaming_data)
			stopStreaming();
		setCommandReference(READ_CONFIGURATION_INFO);
		start_reading_preferences = true;
		write("c".getBytes());

	}

	/**
	 * @param device: Starts new Connect thread with specified bluetooth device
	 */
	public synchronized void connect(BluetoothDevice device) {
		if (D)
			Log.d(TAG, "Connecting to " + device);

		// this.myDevice=device;

		// Cancel any thread attempting to make a connection
		if (state == STATE_CONNECTING) {
			if (myConnectThread != null) {
				myConnectThread.cancel();
				myConnectThread = null;

			}
		}
		// Cancel any thread currently running a connection
		if (myConnectedThread != null) {
			myConnectedThread.cancel();
			myConnectedThread = null;

		}

		setState(STATE_CONNECTING);

		// Start the thread to connect with the given device
		myConnectThread = new ConnectThread(device);

		myConnectThread.start();

	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connected");

		// Send the name of the connected device back to the UI Activity
		Message msg = myHandler
				.obtainMessage(StripChartRecorder.MESSAGE_DEVICE_INFO);
		Bundle bundle = new Bundle();
		bundle.putString(StripChartRecorder.DEVICE_NAME, device.getName());
		bundle.putString(StripChartRecorder.DEVICE_ADDRESS, device.getAddress());
		msg.setData(bundle);
		myHandler.sendMessage(msg);

		setState(STATE_CONNECTED);

		// Cancel the thread that completed the connection
		if (myConnectThread != null) {
			myConnectThread.cancel();
			myConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (myConnectedThread != null) {
			myConnectedThread.cancel();
			myConnectedThread = null;
		}

		// Save the connected socket for later use (When user wants to start
		// streaming)
		this.connected_socket_for_connection = socket;

	}

	/**
	 * Called when attemt to connect device fails
	 */
	private void connectionFailed() {
		// Notify the handler that the connection failed
		/* So that it can retry connecting again */

		setState(STATE_NONE);
		toastMessage("Connection failed, attempting a reconnect");

		Message msg = myHandler
				.obtainMessage(StripChartRecorder.MESSAGE_CONNECTION_ERROR);
		Bundle bundle = new Bundle();
		bundle.putInt(StripChartRecorder.CONNECTION_ERROR_TYPE,
				CONNECTION_FAILED);
		msg.setData(bundle);
		myHandler.sendMessage(msg);

	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		setState(STATE_NONE);
		toastMessage("Connection was lost, attempting a reconnect");

		Message msg = myHandler
				.obtainMessage(StripChartRecorder.MESSAGE_CONNECTION_ERROR);
		Bundle bundle = new Bundle();
		bundle.putInt(StripChartRecorder.CONNECTION_ERROR_TYPE, CONNECTION_LOST);
		msg.setData(bundle);
		myHandler.sendMessage(msg);
		// Attempt to re-establish the connection
		initiateConnectedThread();

	}

	
	/**
	 * Stop all running threads
	 */
	public synchronized void stop() {
		if (D)
			Log.e(TAG, "STOP");
		setState(STATE_NONE);
		userDisconnected = true;
		if (this.is_streaming_data)
			stopStreaming();
		if (myConnectedThread != null) {
			myConnectedThread.cancel();
			myConnectedThread = null;
		}
		if (myConnectThread != null) {
			myConnectThread.cancel();
			myConnectThread = null;
		}

	}

	

	/**
	 * @author Bishwas
	 *This thread runs while trying to make an outgoing connection with a
	 * device. The connection succeeds or fails
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket connectionSocket;
		private final BluetoothDevice connectToDevice;

		public ConnectThread(BluetoothDevice device) {
			Log.d(TAG, "create ConnectThread");

			this.connectToDevice = device;
			BluetoothSocket tmp = null;

			try {
				tmp = device.createRfcommSocketToServiceRecord(myUUID);

			} catch (IOException e) {
				Log.e(TAG, "Create() failed", e);

			}
			connectionSocket = tmp;

		}

		@Override
		public void run() {
			Log.i(TAG, "Starting myConnectThread");
			setName("ConnectThread");

			// Cancel discovery because it slows down connection
			myAdapter.cancelDiscovery();

			// Make a connection to bluetooth socket
			try {
				connectionSocket.connect();
				// connectRequestSuccessful=true;

			} catch (IOException e) {

				if (!userDisconnected)
					connectionFailed();

				try {
					connectionSocket.close();

				} catch (IOException e1) {

					Log.e(TAG, "Cannot close connectionSocket");

				}

				return;
			} catch (NullPointerException en) {
				en.printStackTrace();
			}

			// Reset the connect thread because its not required anymore
			synchronized (BluetoothConnectionFactory.this) {

				myConnectThread = null;
			}

			// Start the connected thread
			connected(connectionSocket, connectToDevice);
		}

		public void cancel() {
			// TODO Auto-generated method stub
			try {
				connectionSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}

		}
	}

	
	 
	/**
	 * @author Bishwas
	 *This thread runs after a connection is established and reads or writes
	 * into default input and output stream respectively
	 */
	private class ConnectedThread extends Thread {

		private final BluetoothSocket mySocket;
		private final InputStream myInStream;
		private final BufferedReader bufferedReaderWrap;
		private final OutputStream myOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "Create ConnectedThread");

			mySocket = socket;

			InputStream tempIn = null;
			OutputStream tempOut = null;

			// Get InputStream and OutputStream from the bluetooth socket

			try {
				tempIn = mySocket.getInputStream();
				tempOut = mySocket.getOutputStream();

			} catch (IOException e) {
				Log.e(TAG, "Input and Output streams not created");

			}

			myInStream = tempIn;
			myOutStream = tempOut;
			bufferedReaderWrap = new BufferedReader(new InputStreamReader(
					myInStream));
		}

		@Override
		public void run() {
			Log.i(TAG, "BEGIN myConnectedThread");
			setName("ConnectedThread");

			int bytes = 0;
			String readMessage;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					readMessage = bufferedReaderWrap.readLine();
					bytes = readMessage.length();
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					if (!userDisconnected) {
						connectionLost();
						continue;
					} else
						break;

				}

				if (bytes <= 1) {
					Log.w(TAG, "single byte received<" + readMessage + ">");
					continue;
				}

				if (Character.isDigit(readMessage.charAt(0))) {

					myHandler.obtainMessage(StripChartRecorder.MESSAGE_READ,
							bytes, -1, readMessage).sendToTarget();
				}
				// Else if : message is a command query, and a read preference
				// has been requested
				else if ((readMessage.charAt(0) == ';')
						&& start_reading_preferences) {
					readMessage = readMessage.substring(1);

					switch (getCommandReference()) {
					case (READ_AVAILABLE_SAMPLE_RATES): {
						Log.d(TAG, "SAMPLE_RATE , bytes read: " + bytes + "  <"
								+ readMessage + ">");
						myHandler.obtainMessage(
								StripChartRecorder.MESSAGE_AVAILABLE_SAMPLE_RATES, bytes,
								-1, readMessage).sendToTarget();
						break;
					}
					case (READ_STATUS_INFO): {
						myHandler.obtainMessage(
								StripChartRecorder.MESSAGE_STATUS_INFO, bytes,
								-1, readMessage).sendToTarget();
						break;
					}
					case (READ_CONFIGURATION_INFO): {
						myHandler.obtainMessage(
								StripChartRecorder.MESSAGE_CONFIGURATION_INFO,
								bytes, -1, readMessage).sendToTarget();
						break;
					}
					default: {
						Log.i(TAG,
								"info from device don't know what to do with it<"
										+ readMessage + ">");
						break;
					}

					}
				}

				else {
					Log.w(TAG, "Unmatched data<" + readMessage + ">");
				}

			} // end while
		} // end run()

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		private void write(byte[] buffer) {

			Log.e(TAG, "Writing...");

			try {
				myOutStream.write(buffer);

			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);

			}
		}

		public void cancel() {
			try {
				myInStream.close();
				myOutStream.close();
				mySocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}

	}

}
