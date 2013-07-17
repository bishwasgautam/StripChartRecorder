package bishwas.gcdc.stripchartrecorder.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;

import android.content.Context;
import android.os.Handler;
import bishwas.gcdc.stripchartrecorder.core.StripChartRecorder;

/**
 * Sends demo data values to the main activity to be plotted in the graph View
 * 
 * @author Bishwas Gautam
 * 
 *  Copyright (C) 2011 Gulf Coast Data Concepts Licensed under the GNU Lesser
 *  General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
 
public class DemoDataSimulator extends Thread implements Serializable {

	
	private static final long serialVersionUID = 1L;
	
	private Handler handler;

	
	InputStream is;
	boolean stopRequested;

	private boolean running;

	public DemoDataSimulator(Handler handler) {

		this.handler = handler;
		

	}

	/**
	 * @param context, Application context used to get Assets
	 * Opens a text file with demo values from assets folder
	 */
	public void getFileData(Context context)  {
		/* InputStream is = null; */
		try {
			is = context.getAssets().open("demo.txt");
		} catch (Exception e) {
			e.printStackTrace();
		}
		context=null;
	}

	

	@Override
	public void run() {
		if (is != null) {

			InputStreamReader input = new InputStreamReader(is);
			BufferedReader buff = new BufferedReader(input);
			String line;

			try {
				while ((!stopRequested && (line = buff.readLine()) != null)) {

					handler.obtainMessage(StripChartRecorder.MESSAGE_READ, 0,
							0, line).sendToTarget();
					try {
						sleep(3);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					running=true;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		handler.obtainMessage(StripChartRecorder.MESSAGE_DEMO_RUN_FINISH).sendToTarget();

	}

	
	public void _stop() {
		stopRequested=true;
		
	}
	
	public boolean isRunning(){
		return running;
	}

}
