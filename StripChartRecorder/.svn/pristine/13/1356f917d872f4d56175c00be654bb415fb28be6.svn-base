package bishwas.gcdc.stripchartrecorder.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import bishwas.gcdc.stripchartrecorder.core.R;
import bishwas.gcdc.stripchartrecorder.core.StripChartRecorder;
import bishwas.gcdc.stripchartrecorder.helpers.ConfigManager;
import bishwas.gcdc.stripchartrecorder.helpers.DateFormatter;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;
import com.jjoe64.graphview.GraphView.GraphViewStyle;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.LineGraphView;

/**
 * Instantiates and handles the line graph view, receives data and plots on the line graph view.
 * Also takes care of graph control handles such as zoom in, zoom out, reset etc.
 * 
 * @author Bishwas Gautam
 * 
 *  Copyright (C) 2011 Gulf Coast Data Concepts Licensed under the GNU Lesser
 *  General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
public class GraphAssistant extends Thread implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	public static final int INDEX_OF_X = 0;
	public static final int INDEX_OF_Y = 1;
	public static final int INDEX_OF_Z = 2;
	public static final int INDEX_OF_RMS = 3;

	private double defaultZoomIndex = 12;
	// Graph title
	private String title;

	private Context context;

	// Graph data fields
	private List<GraphViewData> xData, yData, zData, rmsData;

	/**
	 * Private class, gives current readable time corresponding to epoch set
	 */
	private TimeManager timeManager;

	private int sampleRate;
	private final int secondsPerScreen = 10;
	private final int defaultSamplesPerScreen = 128;
	private final int samplesPerScreen = (sampleRate = ConfigManager
			.getConfigManager().getCurrentSampleRate()) == 0 ? defaultSamplesPerScreen
			: secondsPerScreen * sampleRate;
	private final int maxSeriesLength = 10000;

	private double rms = 0.0;
	private double max, min;
	private int xSeriesColor, ySeriesColor, zSeriesColor, rmsSeriesColor;

	// GraphView fields
	private GraphViewStyle xStyle, yStyle, zStyle, rmsStyle;
	private GraphViewSeries xSeries, ySeries, zSeries, rmsSeries;
	private GraphView stripChart;
	private boolean moveChart;
	private boolean running;
	private Handler myHandler;

	/**
	 * @param context: the application context
	 * @param title : title of the graph
	 * Initialize graphView, set title for graph
	 */
	public GraphAssistant(Context context,String title) {
		this.title = title;
		this.context = context;
		moveChart=true;
		if (title == null)

			this.title = "My Line Graph";

		initializeGraphView();
	}
	
	@Override
	public void run(){
		running=true;
	}

	public void initializeGraphView() {

		defineStyles();
		initAndSetChartProperties();

	}

	/**
	 * Initilizes a  new line graph and applies styles
	 */
	private void initAndSetChartProperties() {
		// Initialize chart
		stripChart = new LineGraphView(context, title) {
			@Override
			protected String formatLabel(double value, boolean isValueX) {
				if (isValueX && timeManager != null) {
					double maxX = stripChart.getMaxX(false);
					double minX = stripChart.getMinX(false);
					double diff = maxX - minX;
					String label;
					if (diff < 1)
						label = DateFormatter.getDate("HH:mm:ss.SSS",
								timeManager.getCurrentTimeMillis(value));

					else if (diff > 1 && diff < 600)
						label = DateFormatter.getDate("HH:mm:ss",
								timeManager.getCurrentTimeMillis(value));
					else if (diff >= 600 && diff <= 3600)
						label = DateFormatter.getDate("HH:mm",
								timeManager.getCurrentTimeMillis(value));
					else
						label = DateFormatter.getDate("HH",
								timeManager.getCurrentTimeMillis(value));
					return label;

				}

				return super.formatLabel(value, isValueX);

			}
		};
		// Chart Properties
		setYAxisAutoScale(ConfigManager.getConfigManager()
				.isVerticalAxisAutoScaled());
		stripChart.setAcceptsLiveData(true);
		zoomReset();
		stripChart.setScrollable(true);
		stripChart.setScalable(true);
		stripChart.setShowLegend(true);
		stripChart.setLegendAlign(LegendAlign.BOTTOM);
		stripChart.setLegendWidth(100);

		// Initialize vectors which will store live streaming data
		xData = new ArrayList<GraphViewData>();
		yData = new ArrayList<GraphViewData>();
		zData = new ArrayList<GraphViewData>();
		rmsData = new ArrayList<GraphViewData>();

		List<GraphViewData> temp = new ArrayList<GraphViewData>();
		temp.add(new GraphViewData(0, 0));
		// Series within chart
		xSeries = new GraphViewSeries("X", xStyle, temp);
		ySeries = new GraphViewSeries("Y", yStyle, temp);
		zSeries = new GraphViewSeries("Z", zStyle, temp);
		rmsSeries = new GraphViewSeries("RMS", rmsStyle, temp);

		addSeriesToGraphView();

	}

	/**
	 * @param value
	 * Sets if Y scale is auto scaled or not
	 */
	public void setYAxisAutoScale(boolean value) {

		// Set the max and min bounds
		if (value)
			stripChart.setManualYAxisBounds(stripChart.getMaxY(),
					stripChart.getMinY());
		// Regenerate the vertical labels
		stripChart.regenerateVerLabels();
		stripChart.setVerticalAxisAutoScale(value);
		stripChart.invalidate();
	}

	public void setHandler(Handler handler){
		
		this.myHandler=handler;
		
	}
	/**
	 * @return
	 * Zoom value ratio
	 */
	public double getDefaultIndex() {
		return defaultZoomIndex;
	}

	public void setDefaultIndex(double defaultIndex) {
		this.defaultZoomIndex = defaultIndex;
	}

	/**
	 * Adds four series x,y,z,rms to line graph
	 */
	private void addSeriesToGraphView() {

		// Add all series to the chart
		addSeriesToStripChart(INDEX_OF_X);
		addSeriesToStripChart(INDEX_OF_Y);
		addSeriesToStripChart(INDEX_OF_Z);
		addSeriesToStripChart(INDEX_OF_RMS);

	}

	/**
	 * @param in indicates true for zoom in , zoom out for otherwise
	 * @return if zoom operation was successful
	 * Handles zooming into and out of graph
	 */
	public boolean zoom(boolean in) {

		if (in) {
			// Decrease Y axis bounds by a unit power of two
			max = Math.pow(2, --defaultZoomIndex);
			min = Math.pow(2, defaultZoomIndex) * (-1);
		} else {
			// Increase Y axis bounds by a unit power of two
			max = Math.pow(2, ++defaultZoomIndex);
			min = (defaultZoomIndex == 16) ? Math.pow(2, defaultZoomIndex)
					* (-1) - 1 : Math.pow(2, defaultZoomIndex) * (-1);

		}
		// Set the bounds
		stripChart.setManualYAxisBounds(max, min);

		// Regenerate the vertical labels
		stripChart.regenerateVerLabels();

		// Refresh the view
		//stripChart.invalidate();

		return (defaultZoomIndex > 0 && defaultZoomIndex < 16);
	}

	/**
	 * @return
	 * Resets zoom value to default
	 */
	public boolean zoomReset() {
		if (ConfigManager.getConfigManager().isVerticalAxisAutoScaled()) {
			setYAxisAutoScale(true);
			stripChart.invalidate();
			return false;
		}

		defaultZoomIndex = 13;
		return zoom(true);
	}

	/**
	 * Assign colors and line width to series
	 */
	private void defineStyles() {
		// Get color int values from string resources
		xSeriesColor = Integer.valueOf(context
				.getString(R.string.xSeriesColorCode));
		ySeriesColor = Integer.valueOf(context
				.getString(R.string.ySeriesColorCode));
		zSeriesColor = Integer.valueOf(context
				.getString(R.string.zSeriesColorCode));
		rmsSeriesColor = Integer.valueOf(context
				.getString(R.string.rmsSeriesColorCode));
		// Define styles for each series
		xStyle = new GraphViewStyle(xSeriesColor, 2);
		yStyle = new GraphViewStyle(ySeriesColor, 2);
		zStyle = new GraphViewStyle(zSeriesColor, 2);
		rmsStyle = new GraphViewStyle(rmsSeriesColor, 2);

	}

	/**
	 * Remove all series from graph
	 */
	public void resetGraphView() {

		
		stripChart=null;
		
		this.initializeGraphView();
		
		//Notify main UI thread that stripchart has changed
		myHandler.obtainMessage(StripChartRecorder.MESSAGE_NEW_STRIPCHART).sendToTarget();
		//do not need the handler anymore
		myHandler=null;

	}

	/**
	 * @param index
	 * Add a series to graph according to index supplied
	 */
	public void addSeriesToStripChart(int index) {
		try {
			switch (index) {
			case INDEX_OF_X:
				stripChart.addSeries(xSeries);
				break;
			case INDEX_OF_Y:
				stripChart.addSeries(ySeries);
				break;
			case INDEX_OF_Z:
				stripChart.addSeries(zSeries);
				break;
			case INDEX_OF_RMS:
				stripChart.addSeries(rmsSeries);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		stripChart.invalidate();

	}

	/**
	 * @param index: Index of series to be removed
	 * Remove series from graph according to index
	 */
	public void removeSeriesFromStripChart(int index) {
		try {
			switch (index) {
			case INDEX_OF_X:
				stripChart.removeSeries(xSeries);
				break;
			case INDEX_OF_Y:
				stripChart.removeSeries(ySeries);
				break;
			case INDEX_OF_Z:
				stripChart.removeSeries(zSeries);
				break;
			case INDEX_OF_RMS:
				stripChart.removeSeries(rmsSeries);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		stripChart.invalidate();

	}
	
	/**
	 * Pause the view port
	 */
	public void _pause(){
		this.moveChart=false;
	}

	/**
	 * Move the view port
	 */
	public void _resume(){
		
		this.moveChart=true;
	}
	
	/**
	 * Reset Graph View and reset ArrayLists
	 */
	public void clear(){
		resetGraphView();
		
	}
	
	

	/**
	 * Move the chart and remove old data
	 */
	public void redraw() {

		// xData = xSeries.getDataList();

		while (xData.size() > maxSeriesLength) {

			xData.remove(0);
			yData.remove(0);
			zData.remove(0);
			rmsData.remove(0);
		}

		if(!moveChart)return;
		// Move the chart
		double start;
		if (xData.size() > samplesPerScreen) {
			start = xData.get(xData.size() - samplesPerScreen).valueX;
			double end = xData.get(xData.size() - 1).valueX;
			stripChart.setViewPort(start, end - start);
		}
		
	    //stripChart.invalidate();
		
	}

	/**
	 * @param index
	 * @param time
	 * @param val
	 * 
	 * Add new values to corresponding series
	 */
	public void newValue(int index, double time, double val) {

		if (this.timeManager == null)
			timeManager = new TimeManager(time);
		GraphViewData datum = new GraphViewData(time, val);
		rms += Math.pow(val, 2);

		switch (index) {
		case INDEX_OF_X: {

			xData.add(datum);

			xSeries.resetData(xData);

			if ((ConfigManager.getConfigManager()
					.isChannelChecked(ConfigManager.KEY_CHANNEL_X))) {
				// Reset Series Data

				stripChart.addSeries(xSeries);

			} else {

				stripChart.removeSeries(xSeries);

			}
			break;
		}
		case INDEX_OF_Y: {

			yData.add(datum);

			ySeries.resetData(yData);

			if ((ConfigManager.getConfigManager()
					.isChannelChecked(ConfigManager.KEY_CHANNEL_Y))) {

				stripChart.addSeries(ySeries);

			} else {

				stripChart.removeSeries(ySeries);

			}
			break;
		}
		case INDEX_OF_Z: {

			zData.add(datum);
			zSeries.resetData(zData);

			if ((ConfigManager.getConfigManager()
					.isChannelChecked(ConfigManager.KEY_CHANNEL_Z))) {

				stripChart.addSeries(zSeries);

			} else {

				stripChart.removeSeries(zSeries);

			}
			rms = Math.sqrt(rms);

			rmsData.add(new GraphViewData(time, rms));
			rmsSeries.resetData(rmsData);

			if ((ConfigManager.getConfigManager()
					.isChannelChecked(ConfigManager.KEY_CHANNEL_RMS))) {

				stripChart.addSeries(rmsSeries);

			} else {

				stripChart.removeSeries(rmsSeries);

			}
			rms = 0;
			break;
		}
		default:
			break;
		}
		
	}

	/**
	 * @return: lineGraphView : StripChart
	 * 
	 */
	public LineGraphView getStripChart() {
		return (LineGraphView) this.stripChart;

	}

	
	/**
	 * @return true if the thread is running
	 */
	public boolean hasStarted(){
		return running;
	}
	/**
	 * @author Bishwas Gautam
	 *
	 */
	private class TimeManager {
		private double epoch;
		private double sys_time_ref;
		private double offset;

		public TimeManager(double time) {
			this.epoch = time;
			this.sys_time_ref = System.currentTimeMillis() / 1000;
			this.setOffset(sys_time_ref - epoch);
		}

		/**
		 * @param time
		 * @return
		 * Return corresponding time to epoch
		 */
		public double getCurrentTimeMillis(double time) {
			return (time + offset) * 1000;
		}

		/**
		 * @param offset, time lapsed
		 */
		private void setOffset(double offset) {
			this.offset = offset;
		}

	}

}
