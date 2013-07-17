package bishwas.gcdc.stripchartrecorder.core;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * About activity consists of information about the company, the application and link to purchase hardware.
 * 
 * @author Bishwas Gautam
 * 
 *  Copyright (C) 2011 Gulf Coast Data Concepts Licensed under the GNU Lesser
 *  General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
public class About extends Activity {

	private Button aboutApp, aboutGCDC, buyDevice, ok;
	private View.OnClickListener myListener;
	private PopupWindow pop;
	private TextView tv;
	LayoutInflater factory;
	View content;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		initializePopupWindow();
		myListener = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.btn_about_scr:

					showPopupWindow(
							getString(R.string.about_scr_info));

					break;
				case R.id.btn_gcdcinfo:

					showPopupWindow(
							getString(R.string.about_gcdc_info));
					break;
				case R.id.btn_buy:
					Uri uri = Uri
							.parse("http://yhst-21371435622982.stores.yahoo.net/data-acquisition.html");
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
					break;

				}

			}
		};
		aboutApp = (Button) findViewById(R.id.btn_about_scr);
		aboutApp.setOnClickListener(myListener);
		aboutGCDC = (Button) findViewById(R.id.btn_gcdcinfo);
		aboutGCDC.setOnClickListener(myListener);
		buyDevice = (Button) findViewById(R.id.btn_buy);
		buyDevice.setOnClickListener(myListener);

		// initializePopupWindow();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_test_tab_, menu);
		return true;
	}

	/**
	 * Initializes all the necessary elements to show a new popup window
	 */
	private void initializePopupWindow() {
		
		//Get layout inflater
		factory = LayoutInflater.from(this);
		//Grab the layout file
		content = factory.inflate(R.layout.about_popup_content, null);
		//Grab the textview within layout file
		tv = (TextView) content.findViewById(R.id.tv_about_info);
		//Create a new popup window
		pop = new PopupWindow(this);
		//Makes sure window does not go out of screen bounds
		pop.setClippingEnabled(false);
		//Wraps content of the PopUpwindow
		pop.setWindowLayoutMode(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		//Grab Ok button from layout
		ok = (Button) content.findViewById(R.id.btn_ok);
		ok.setText("Ok");
		ok.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				pop.dismiss();

			}
		});

	}

	/**
	 * @param message: Message to show in the textview
	 * Shows a popup window with message
	 */
	private void showPopupWindow(String message) {

		tv.setText(message);

		pop.setContentView(content);
		
		pop.showAtLocation(findViewById(R.id.relativelayout_testtab),
				Gravity.CENTER, 0, 0);
		pop.update(0,0);
		Log.i("About", "Showing popup");

	}

}
