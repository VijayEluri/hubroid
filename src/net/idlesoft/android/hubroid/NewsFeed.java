/**
 * Hubroid - A GitHub app for Android
 * 
 * Copyright (c) 2010 Eddie Ringle.
 * 
 * Licensed under the New BSD License.
 */

package net.idlesoft.android.hubroid;

import java.io.File;

import org.idlesoft.libraries.ghapi.APIAbstract.Response;
import org.idlesoft.libraries.ghapi.GitHubAPI;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

public class NewsFeed extends Activity {
	private ActivityFeedAdapter _privateActivityAdapter;
	private SharedPreferences _prefs;
	private SharedPreferences.Editor _editor;
	private String _username;
	private String _password;
	private boolean _isLoggedIn;
	public JSONArray _privateJSON;
	public JSONArray _displayedPrivateJSON;
	private LoadPrivateFeedTask _loadPrivateTask;
	public View _loadingItem;
	public GitHubAPI _gapi = new GitHubAPI();

	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!menu.hasVisibleItems()) {
			if (_isLoggedIn)
				menu.add(0, 1, 0, "Sign out");
			menu.add(0, 2, 0, "Clear Cache");
		}
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			_editor.clear().commit();
			Intent intent = new Intent(NewsFeed.this, Hubroid.class);
			startActivity(intent);
			finish();
        	return true;
		case 2:
			File root = Environment.getExternalStorageDirectory();
			if (root.canWrite()) {
				File hubroid = new File(root, "hubroid");
				if (!hubroid.exists() && !hubroid.isDirectory()) {
					return true;
				} else {
					hubroid.delete();
					return true;
				}
			}
		}
		return false;
	}

	private OnItemClickListener onPrivateActivityItemClick = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			try {
				Intent intent = new Intent(getApplicationContext(), SingleActivityItem.class);
				intent.putExtra("item_json", _displayedPrivateJSON.getJSONObject(arg2).toString());
				startActivity(intent);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	};

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.news_feed);

        _prefs = getSharedPreferences(Hubroid.PREFS_NAME, 0);
        _editor = _prefs.edit();
        _username = _prefs.getString("username", "");
        _password = _prefs.getString("password", "");

    	TextView title = (TextView)findViewById(R.id.tv_top_bar_title);

    	_gapi.authenticate(_username, _password);

		title.setText("News Feed");

        _loadingItem = getLayoutInflater().inflate(R.layout.loading_feed_item, null);

        _loadPrivateTask = (LoadPrivateFeedTask) getLastNonConfigurationInstance();
        if (_loadPrivateTask == null)
        	_loadPrivateTask = new LoadPrivateFeedTask();
        _loadPrivateTask.activity = this;
		if (_loadPrivateTask.getStatus() == AsyncTask.Status.PENDING)
			_loadPrivateTask.execute();
    }

	private static class LoadPrivateFeedTask extends AsyncTask<Void, Void, Void> {
		public NewsFeed activity;

		protected void setLoadingView()
		{
			((ListView)activity.findViewById(R.id.lv_activity_feeds_private_list)).addHeaderView(activity._loadingItem);
	        ((ListView)activity.findViewById(R.id.lv_activity_feeds_private_list)).setAdapter(null);
		}

		protected void removeLoadingView()
		{
			((ListView)activity.findViewById(R.id.lv_activity_feeds_private_list)).removeHeaderView(activity._loadingItem);
		}

		protected void setAdapter()
		{
			((ListView)activity.findViewById(R.id.lv_activity_feeds_private_list)).setAdapter(activity._privateActivityAdapter);
			((ListView)activity.findViewById(R.id.lv_activity_feeds_private_list)).setOnItemClickListener(activity.onPrivateActivityItemClick);
		}

		@Override
		protected void onPreExecute()
		{
			setLoadingView();
		}

		@Override
		protected void onPostExecute(Void result)
		{
			setAdapter();
			removeLoadingView();
		}

		@Override
		protected Void doInBackground(Void...params)
		{
			if (activity._privateJSON == null) {
				try {
					Response resp = activity._gapi.user.private_activity();
					if (resp.statusCode != 200) {
						/* Let the user know something went wrong */
						return null;
					}
					activity._privateJSON = new JSONArray(resp.resp);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			if (activity._displayedPrivateJSON == null)
				activity._displayedPrivateJSON = new JSONArray();
			int length = activity._displayedPrivateJSON.length();
			for (int i = length; i < length + 10; i++) {
				if (activity._privateJSON.isNull(i))
					break;
				try {
					activity._displayedPrivateJSON.put(activity._privateJSON.get(i));
				} catch (JSONException e) {
					e.printStackTrace();
					break;
				}
			}
			activity._privateActivityAdapter = new ActivityFeedAdapter(activity.getApplicationContext(), activity._displayedPrivateJSON, false);
			return null;
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return _loadPrivateTask;
	}

	@Override
    public void onStart()
    {
       super.onStart();
       FlurryAgent.onStartSession(this, "K8C93KDB2HH3ANRDQH1Z");
    }

    @Override
    public void onStop()
    {
       super.onStop();
       FlurryAgent.onEndSession(this);
    }
}