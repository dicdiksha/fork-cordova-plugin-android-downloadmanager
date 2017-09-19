package com.github.emilbayes.downloadmanager;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.database.Cursor;
import android.net.Uri;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.DownloadManager;

import java.util.HashMap;
import java.util.Map;

import static android.app.DownloadManager.STATUS_FAILED;
import static android.app.DownloadManager.STATUS_PAUSED;
import static android.app.DownloadManager.STATUS_PENDING;
import static android.app.DownloadManager.STATUS_RUNNING;
import static android.app.DownloadManager.STATUS_SUCCESSFUL;

public class DownloadManagerPlugin extends CordovaPlugin {
    DownloadManager downloadManager;

    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
        super.initialize(cordova, webView);

        downloadManager = (DownloadManager) cordova.getActivity()
                .getApplication()
                .getApplicationContext()
                .getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      if (action.equals("enqueue")) return enqueue(args.getJSONObject(0), callbackContext);
      if (action.equals("query")) return query(args.getJSONObject(0), callbackContext);
      if (action.equals("remove")) return remove(args, callbackContext);

      callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
      return false;
    }

    protected boolean enqueue(JSONObject obj, CallbackContext callbackContext) throws JSONException {
        DownloadManager.Request req = deserialiseRequest(obj);

        long id = downloadManager.enqueue(req);

        callbackContext.success(Long.toString(id));

        return true;
    }

    protected boolean query(JSONObject obj, CallbackContext callbackContext) throws JSONException {
        DownloadManager.Query query = deserialiseQuery(obj);

        Cursor downloads = downloadManager.query(query);

        callbackContext.success(JSONFromCursor(downloads));

        return true;
    }

    protected boolean remove(JSONArray arr, CallbackContext callbackContext) throws JSONException {
        long[] ids = longsFromJSON(arr);

        int removed = downloadManager.remove(ids);
        callbackContext.success(removed);

        return true;
    }

    protected DownloadManager.Request deserialiseRequest(JSONObject obj) throws JSONException {
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(obj.getString("uri")));

        req.setTitle(obj.optString("title"));
        req.setDescription(obj.optString("description"));
        req.setMimeType(obj.optString("mimeType", null));

        if (obj.has("destinationInExternalFilesDir")) {
            Context context = cordova.getActivity()
                    .getApplication()
                    .getApplicationContext();

            JSONObject params = obj.getJSONObject("destinationInExternalFilesDir");

            req.setDestinationInExternalFilesDir(context, params.optString("dirType"), params.optString("subPath"));
        }
        else if (obj.has("destinationInExternalPublicDir")) {
            JSONObject params = obj.getJSONObject("destinationInExternalPublicDir");

            req.setDestinationInExternalPublicDir(params.optString("dirType"), params.optString("subPath"));
        }
        else if (obj.has("destinationUri")) req.setDestinationUri(Uri.parse(obj.getString("destinationUri")));

        req.setVisibleInDownloadsUi(obj.optBoolean("visibleInDownloadsUi", true));
        req.setNotificationVisibility(obj.optInt("notificationVisibility"));

        return req;
    }

    protected DownloadManager.Query deserialiseQuery(JSONObject obj) throws JSONException {
        DownloadManager.Query query = new DownloadManager.Query();

        long[] ids = longsFromJSON(obj.optJSONArray("ids"));
        query.setFilterById(ids);

        query.setFilterByStatus(obj.optInt("status", STATUS_FAILED | STATUS_PAUSED | STATUS_PENDING | STATUS_RUNNING | STATUS_SUCCESSFUL));

        return query;
    }

    private static PluginResult OK(Map obj) throws JSONException {
        return createPluginResult(obj, PluginResult.Status.OK);
    }

    private static PluginResult ERROR(Map obj) throws JSONException {
        return createPluginResult(obj, PluginResult.Status.ERROR);
    }

    private static PluginResult createPluginResult(Map map, PluginResult.Status status) throws JSONException {
        JSONObject json = new JSONObject(map);
        PluginResult result = new PluginResult(status, json);
        return result;
    }

    private static JSONArray JSONFromCursor(Cursor cursor) throws JSONException {
        JSONArray result = new JSONArray();

        cursor.moveToFirst();
        do {
            int totalColumns = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();
            for (int i = 0; i < totalColumns; i++) {
                if (cursor.getColumnName(i) != null) {
                    rowObject.put(cursor.getColumnName(i),
                                  cursor.getString(i));

                }
            }
            result.put(rowObject);
        } while (cursor.moveToNext());

        return result;
    }

    private static long[] longsFromJSON(JSONArray arr) throws JSONException {
        if (arr == null) return null;

        long[] longs = new long[arr.length()];

        for (int i = 0; i < arr.length(); i++) {
            String str = arr.getString(i);
            longs[i] = Long.valueOf(str);
        }

        return longs;
    }
}
