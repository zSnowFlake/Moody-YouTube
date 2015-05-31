package com.zawar.moodyyoutube;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.zawar.moodyyoutube.auth.AbstractGetNameTask;
import com.zawar.moodyyoutube.auth.AuthPreferences;
import com.zawar.moodyyoutube.auth.GetNameInForeground;

public class SearchActivity extends Activity {

	private Spinner spinner;  //For dropdown list
	String mood;
	
	private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/youtube";
	public static final String EXTRA_ACCOUNTNAME = "extra_accountname";

	static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
	static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;

	private String mEmail;
	
	ProgressDialog mDialog;

	public static String TYPE_KEY = "AIzaSyAlCVtQQ4wv755v-naVCrQNC2NLfczDJPU"; //Api key 

	private static final int AUTHORIZATION_CODE = 1993;
	private static final int ACCOUNT_CODE = 1601;

	private AuthPreferences authPreferences;
	private AccountManager accountManager;

	SharedPreferences preferences;
	SharedPreferences.Editor editor;
	
	private Activity mActivity;

	@Override //
	protected void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		
		mActivity = this;

		moodDropBox();

		Button btnSearch = (Button) findViewById(R.id.btnSearch);

		btnSearch.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isDeviceOnline()) {
					getUsername();
				} else {
					Toast.makeText(mActivity, "No internet connection", Toast.LENGTH_LONG);
				}
			}
		});

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		editor = preferences.edit();

		accountManager = AccountManager.get(this);

		authPreferences = new AuthPreferences(this);

	}

	private void moodDropBox() {
		String[] values = { "Happy", "Calm", "Sad", "Sleep", "Relax", "Piano",
				"Studying", "Focus", "Romantic", "Peace", "Classical",
				"Evening"};

		spinner = (Spinner) findViewById(R.id.mood_spinner);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, values);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				mood = parent.getItemAtPosition(pos).toString();
				editor.putString("mood", mood);
				editor.apply();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == AUTHORIZATION_CODE) {
				mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				getUsername();
			} else if (requestCode == ACCOUNT_CODE) {
				String accountName = data
						.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				authPreferences.setUser(accountName);

				// invalidate old tokens which might be cached. we want a fresh
				// one, which is guaranteed to work
				invalidateToken();

				mDialog = new ProgressDialog(mActivity);
				mDialog.setMessage("Please wait...");
				mDialog.setCancelable(true);
				mDialog.show();
				requestToken();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Attempt to get the user name. If the email address isn't known yet, then
	 * call pickUserAccount() method so the user can pick an account.
	 */
	private void getUsername() {
		if (mEmail == null) {
			pickUserAccount();
		} else {
			if (isDeviceOnline()) {
				getTask(SearchActivity.this, mEmail, SCOPE).execute();
			} else {
				Toast.makeText(this, "No network connection available",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Starts an activity in Google Play Services so the user can pick an
	 * account
	 */
	private void pickUserAccount() {
		String[] accountTypes = new String[] { "com.google" };
		Intent intent = AccountManager.newChooseAccountIntent(null, null,
				accountTypes, false, null, null, null, null);
		startActivityForResult(intent, ACCOUNT_CODE);
	}

	/** Checks whether the device currently has a network connection */
	private boolean isDeviceOnline() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			return true;
		}
		return false;
	}

	/**
	 * This method is a hook for background threads and async tasks that need to
	 * update the UI. It does this by launching a runnable under the UI thread.
	 */
	public void show(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Intent intent = new Intent(getApplicationContext(),
						PlaylistActivity.class);
				intent.putExtra("mood", mood);
				startActivity(intent);
			}
		});
	}

	/**
	 * This method is a hook for background threads and async tasks that need to
	 * provide the user a response UI when an exception occurs.
	 */
	public void handleException(final Exception e) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (e instanceof GooglePlayServicesAvailabilityException) {
					// The Google Play services APK is old, disabled, or not
					// present.
					// Show a dialog created by Google Play services that allows
					// the user to update the APK
					int statusCode = ((GooglePlayServicesAvailabilityException) e)
							.getConnectionStatusCode();
					Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
							statusCode, SearchActivity.this,
							REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
					dialog.show();
				} else if (e instanceof UserRecoverableAuthException) {
					// Unable to authenticate, such as when the user has not yet
					// granted
					// the app access to the account, but the user can fix this.
					// Forward the user to an activity in Google Play services.
					Intent intent = ((UserRecoverableAuthException) e)
							.getIntent();
					startActivityForResult(intent,
							REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
				}
			}
		});
	}

	/**
	 * Note: This approach is for demo purposes only. Clients would normally not
	 * get tokens in the background from a Foreground activity.
	 */
	private AbstractGetNameTask getTask(SearchActivity activity, String email,
			String scope) {
		return new GetNameInForeground(activity, email, scope);
	}

	private void requestToken() {
		Account userAccount = null;
		String user = authPreferences.getUser();
		for (Account account : accountManager.getAccountsByType("com.google")) {
			if (account.name.equals(user)) {
				userAccount = account;

				break;
			}
		}

		accountManager.getAuthToken(userAccount, SCOPE, null, this,
				new OnTokenAcquired(), null);
	}

	/**
	 * call this method if your token expired, or you want to request a new
	 * token for whatever reason. call requestToken() again afterwards in order
	 * to get a new token.
	 */
	private void invalidateToken() {
		AccountManager accountManager = AccountManager.get(this);
		accountManager.invalidateAuthToken("com.google",
				authPreferences.getToken());

		authPreferences.setToken(null);
	}

	private class OnTokenAcquired implements AccountManagerCallback<Bundle> {

		@Override
		public void run(AccountManagerFuture<Bundle> result) {
			try {
				Bundle bundle = result.getResult();

				Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);
				if (launch != null) {
					startActivityForResult(launch, AUTHORIZATION_CODE);
				} else {
					String token = bundle
							.getString(AccountManager.KEY_AUTHTOKEN);

					authPreferences.setToken(token);

					Log.e("AuthApp", authPreferences.getToken());

					if (authPreferences.getToken() != null) {
						Intent intent = new Intent(getApplicationContext(),
								PlaylistActivity.class);
						intent.putExtra("mood", mood);
						intent.putExtra("token", authPreferences.getToken());
						intent.putExtra("user", authPreferences.getUser());
						startActivity(intent);
					} else {
						Toast.makeText(mActivity, "Youtube not responding", Toast.LENGTH_LONG);
					}
					mDialog.hide();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
