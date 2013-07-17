package bishwas.gcdc.stripchartrecorder.helpers;

import java.text.SimpleDateFormat;

/**
 * 
 * Responsible for formatting time in millis with the supplied format
 * 
 * @author Bishwas Gautam
 * 
 *  Copyright (C) 2011 Gulf Coast Data Concepts Licensed under the GNU Lesser
 *  General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
public class DateFormatter {

	private static SimpleDateFormat formatter;
	private static String dateNow;

	/**
	 * @param format, The desired format
	 * @param timeInMillis, time in milli seconds
	 * @return
	 */
	public static String getDate(String format, double timeInMillis) {

		formatter = new SimpleDateFormat(format);
		dateNow = formatter.format(new java.util.Date((long) timeInMillis));
		return dateNow;
	}
}
