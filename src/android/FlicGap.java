package com.imaketherules.cordova.flic;

import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.flic.lib.FlicButton;
import io.flic.lib.FlicButtonCallback;
import io.flic.lib.FlicButtonCallbackFlags;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;

public class FlicGap extends CordovaPlugin {
    private static final String TAG = "FlicGapPlugin";
	private static final String FLIC_APP_ID = "";
	private static final String FLIC_APP_SECRET = "";
	private static final String FLIC_APP_NAME = "";
    private CallbackContext flicGapCallbackContext;
    private FlicManager manager;
    private int clickCount = 0;

    private void setButtonCallback(FlicButton button) {
        button.removeAllFlicButtonCallbacks();
        button.addFlicButtonCallback(new FlicButtonCallback() {
            @Override
            public void onButtonUpOrDown(FlicButton button, boolean wasQueued, int timeDiff, boolean isUp, boolean isDown) {
                if(isDown) return;
                clickCount += 1;
                final int currentClickCount = clickCount;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(currentClickCount == clickCount){
                            fireEvent("flic");
                        }else if(clickCount != 0){
                            fireEvent("dblflic");
                        }
                        clickCount = 0;
                    }
                }, 500);
            }
        });
        button.setFlicButtonCallbackFlags(FlicButtonCallbackFlags.UP_OR_DOWN);
        button.setActiveMode(true);
    }

    private void setFlicManager(FlicManager manager){
        this.manager = manager;
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("init")) {
            flicGapCallbackContext = callbackContext;
            FlicManager.getInstance(this.cordova.getActivity(), new FlicManagerInitializedCallback() {

                @Override
                public void onInitialized(FlicManager manager) {
                    Log.d(TAG, "Ready to use manager");

                    setFlicManager(manager);

                    // Restore buttons grabbed in a previous run of the activity
                    List<FlicButton> buttons = manager.getKnownButtons();
                    for (FlicButton button : buttons) {
                        String status = null;
                        switch (button.getConnectionStatus()) {
                            case FlicButton.BUTTON_DISCONNECTED:
                                status = "disconnected";
                                break;
                            case FlicButton.BUTTON_CONNECTION_STARTED:
                                status = "connection started";
                                break;
                            case FlicButton.BUTTON_CONNECTION_COMPLETED:
                                status = "connection completed";
                                break;
                        }
                        Log.d(TAG, "Found an existing button: " + button + ", status: " + status);
                        setButtonCallback(button);
                    }

                    manager.initiateGrabButton(cordova.getActivity());
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.d(TAG, "Flic credentials setting");
        super.initialize(cordova, webView);
        FlicManager.setAppCredentials(FLIC_APP_ID, FLIC_APP_SECRET, FLIC_APP_NAME);
        cordova.setActivityResultCallback(this);
    }

    @Override
    public void onDestroy() {
        FlicManager.destroyInstance();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        FlicButton button = manager.completeGrabButton(requestCode, resultCode, data);
        if (button != null) {
            setButtonCallback(button);
        }else{
            fireErrorEvent("noflicgrabbed");
        }
    }

    private void fireEvent(String type) {
        JSONObject event = new JSONObject();
        try {
            event.put("type",type);
        } catch (JSONException e) {
            // this will never happen
        }
        PluginResult pr = new PluginResult(PluginResult.Status.OK, event);
        pr.setKeepCallback(true);
        this.flicGapCallbackContext.sendPluginResult(pr);
    }

    private void fireErrorEvent(String message) {
        JSONObject event = new JSONObject();
        try {
            event.put("type","error");
			event.put("message","message");
        } catch (JSONException e) {
            // this will never happen
        }
        PluginResult pr = new PluginResult(PluginResult.Status.ERROR, event);
        pr.setKeepCallback(false);
        this.flicGapCallbackContext.sendPluginResult(pr);
    }

}