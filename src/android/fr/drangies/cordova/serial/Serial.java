package fr.drangies.cordova.serial;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.ProlificSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;


/**
 * Cordova plugin to communicate with the android serial port
 * @author Xavier Seignard <xavier.seignard@gmail.com>
 */
public class Serial extends CordovaPlugin {
	// logging tag
	private final String TAG = Serial.class.getSimpleName();
	// actions definitions
	private static final String ACTION_REQUEST_PERMISSION = "requestPermission";
	private static final String ACTION_OPEN = "openSerial";
	private static final String ACTION_READ = "readSerial";
	private static final String ACTION_CLOSE = "closeSerial";
	private static final String ACTION_READ_CALLBACK = "registerReadCallback";

	// UsbManager instance to deal with permission and opening
	private UsbManager manager;
	// The current driver that handle the serial port
	private UsbSerialDriver driver;
	
	/**
	 * Overridden execute method
	 * @param action the string representation of the action to execute
	 * @param args
	 * @param callbackContext the cordova {@link CallbackContext}
	 * @return true if the action exists, false otherwise
	 * @throws JSONException if the args parsing fails
	 */
	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		Log.d(TAG, "Action: " + action);
		JSONObject arg_object = args.optJSONObject(0);
		// request permission
		if (ACTION_REQUEST_PERMISSION.equals(action)) {
			JSONObject opts = arg_object.has("opts")? arg_object.getJSONObject("opts") : new JSONObject();
			requestPermission(opts, callbackContext);
			return true;
		}
		// the action doesn't exist
		return false;
	}

	/**
	 * Request permission the the user for the app to use the USB/serial port
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void requestPermission(final JSONObject opts, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				// get UsbManager from Android
				manager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
				UsbSerialProber prober;

				if (opts.has("vid") && opts.has("pid")) {
					ProbeTable customTable = new ProbeTable();
					Object o_vid = opts.opt("vid"); //can be an integer Number or a hex String
					Object o_pid = opts.opt("pid"); //can be an integer Number or a hex String
					int vid = o_vid instanceof Number ? ((Number) o_vid).intValue() : Integer.parseInt((String) o_vid,16);
					int pid = o_pid instanceof Number ? ((Number) o_pid).intValue() : Integer.parseInt((String) o_pid,16);
					String driver = opts.has("driver") ? (String) opts.opt("driver") : "CdcAcmSerialDriver";

					if (driver.equals("FtdiSerialDriver")) {
						customTable.addProduct(vid, pid, FtdiSerialDriver.class);
					}
					else if (driver.equals("CdcAcmSerialDriver")) {
						customTable.addProduct(vid, pid, CdcAcmSerialDriver.class);
					}
					else if (driver.equals("Cp21xxSerialDriver")) {
                    	customTable.addProduct(vid, pid, Cp21xxSerialDriver.class);
					}
					else if (driver.equals("ProlificSerialDriver")) {
                    	customTable.addProduct(vid, pid, ProlificSerialDriver.class);
					}
					else if (driver.equals("Ch34xSerialDriver")) {
						customTable.addProduct(vid, pid, Ch34xSerialDriver.class);
					}
                    else {
                        Log.d(TAG, "Unknown driver!");
                        callbackContext.error("Unknown driver!");
                    }

					prober = new UsbSerialProber(customTable);

				}
				else {
					// find all available drivers from attached devices.
					prober = UsbSerialProber.getDefaultProber();
				}

				List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(manager);

				if (!availableDrivers.isEmpty()) {
					// get the first one as there is a high chance that there is no more than one usb device attached to your android
					driver = availableDrivers.get(0);
					UsbDevice device = driver.getDevice();
					// create the intent that will be used to get the permission
					PendingIntent pendingIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(UsbBroadcastReceiver.USB_PERMISSION), 0);
					// and a filter on the permission we ask
					IntentFilter filter = new IntentFilter();
					filter.addAction(UsbBroadcastReceiver.USB_PERMISSION);
					// this broadcast receiver will handle the permission results
					UsbBroadcastReceiver usbReceiver = new UsbBroadcastReceiver(callbackContext, cordova.getActivity());
					cordova.getActivity().registerReceiver(usbReceiver, filter);
					// finally ask for the permission
					manager.requestPermission(device, pendingIntent);
				}
				else {
					// no available drivers
					Log.d(TAG, "No device found!");
					callbackContext.error("No device found!");
				}
			}
		});
	}
}
