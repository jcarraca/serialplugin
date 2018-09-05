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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;

import com.morpho.morphosmart.sdk.*;
import com.morpho.android.usb.*;


/**
 * Cordova plugin to communicate with the android serial port
 * @author Xavier Seignard <xavier.seignard@gmail.com>
 */
public class Serial extends CordovaPlugin {
	// logging tag
	private final String TAG = Serial.class.getSimpleName();
	
	// actions definitions
	private static final String ACTION_REQUEST_PERMISSION = "requestPermission";

	// UsbManager instance to deal with permission and opening
	private UsbManager manager;
	
	// Morpho Device
	private MorphoDevice morphoDevice;
  
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
			
			USBManager.getInstance().initialize(cordova.getActivity(), "com.morpho.morphosample.USB_ACTION", true);
			if (USBManager.getInstance().isDevicesHasPermission()) {
				callbackContext.success("Permission granted!");
				morphoDevice = new MorphoDevice();
				return true;
			} else {
				callbackContext.error("Permission denied!");
				return false;
			}
		}
		
		return false;
	}
}
