package bishwas.gcdc.stripchartrecorder.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;

/**
 * Constructs and shows a alert dialog 
 * 
 * @author Bishwas Gautam
 *
 *  Copyright (C) 2011 Gulf Coast Data Concepts Licensed under the GNU Lesser
 *  General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
public class AlertDialogHelper {
	static AlertDialog alert;

	/**
	 * @param context : the context that the dialog interface will be binded to
	 * @param title :  dialog title
	 * @param positiveButtonText
	 * @param positiveListener
	 * @param negativeButtonText
	 * @param negativeListener
	 * @param neutralButtonText
	 * @param neutralListener
	 * @param content : Custom View for the dialog
	 */
	public static void showDialog(Context context, String title,
			String positiveButtonText, OnClickListener positiveListener,
			String negativeButtonText, OnClickListener negativeListener,
			String neutralButtonText, OnClickListener neutralListener,
			View content) {
		if(alert!=null)hideDialog();

		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		if (content != null)
			builder.setView(content);

		builder.setMessage(title).setCancelable(false);

		if (negativeButtonText != null && negativeListener != null)
			builder.setNegativeButton(negativeButtonText, negativeListener);
		else
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();

						}
					});

		if (positiveButtonText != null && positiveListener != null) {

			builder.setPositiveButton(positiveButtonText, positiveListener);
		}
		if(neutralButtonText!=null && neutralListener!=null)
			builder.setNeutralButton(neutralButtonText, neutralListener);
		alert = builder.create();

		alert.show();

	}

	/**
	 * Hide the dialog if its showing
	 */
	public static void hideDialog() {

		if (alert != null && alert.isShowing()) {
			alert.dismiss();
			alert = null;
		}
	}

}
