package com.zawar.moodyyoutube;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.squareup.picasso.Picasso;

public class PlaylistActivity extends Activity {

	private ListView videosFound;

	private Handler handler;

	private List<VideoItem> searchResults;

	String mood;

	PlaylistCreate pc;

	ProgressDialog mDialog;

	TextView detail;

	Boolean playlistcreated = false;
	
	Activity mActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playlist);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mood = extras.getString("mood");
		}
		
		mActivity = this;

		videosFound = (ListView) findViewById(R.id.videos_found);

		TextView title = (TextView) findViewById(R.id.playlistTitle);
		title.setText(mood);

		detail = (TextView) findViewById(R.id.detail);
		detail.setText("Please wait, while we are creating your playlist.");

		handler = new Handler();

		addClickListener();

		mDialog = new ProgressDialog(PlaylistActivity.this);
		mDialog.setMessage("Please wait...");
		mDialog.setCancelable(true);
		mDialog.show();

		searchOnYoutube(mood);

		String token = extras.getString("token");
		String user = extras.getString("user");

		Log.e("user", user);

		pc = new PlaylistCreate(this, token, user);

	}

	private void addClickListener() {
		videosFound
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> av, View v, int pos,
							long id) {

//						if (playlistcreated) {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							if (pc.getChannelID() != null) {
								Log.i("channel id", pc.getChannelID());
								// intent.setData(Uri.parse("http://www.youtube.com/user/"
								// + pc.getChannelID()));
								intent.setData(Uri
										.parse("http://www.youtube.com/playlist?list="
												+ pc.getChannelID()));
								startActivity(intent);
							}
//						} else {
//							Toast.makeText(mActivity,
//									"Please wait, while we are creating your playlist.",
//									Toast.LENGTH_SHORT).show();
//						}

						// Intent intent = new Intent(getApplicationContext(),
						// PlayerActivity.class);
						// intent.putExtra("VIDEO_ID", searchResults.get(pos)
						// .getId());
						// startActivity(intent);
					}

				});
	}

	private void searchOnYoutube(final String keywords) {

		new Thread() {
			public void run() {
				YoutubeConnector yc = new YoutubeConnector(
						PlaylistActivity.this);
				searchResults = yc.search(keywords);
				// if (searchResults.size() == 0) {
				handler.post(new Runnable() {
					public void run() {
						updateVideosFound();
						createPlaylist();
					}
				});
				// }
			}
		}.start();
	}

	private void updateVideosFound() {
		ArrayAdapter<VideoItem> adapter = new ArrayAdapter<VideoItem>(
				getApplicationContext(), R.layout.video_item, searchResults) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = getLayoutInflater().inflate(
							R.layout.video_item, parent, false);
				}
				ImageView thumbnail = (ImageView) convertView
						.findViewById(R.id.video_thumbnail);
				TextView title = (TextView) convertView
						.findViewById(R.id.video_title);

				VideoItem searchResult = searchResults.get(position);

				Picasso.with(getApplicationContext())
						.load(searchResult.getThumbnailURL()).into(thumbnail);
				title.setText(searchResult.getTitle());
				// description.setText(searchResult.getDescription());
				return convertView;
			}
		};

		videosFound.setAdapter(adapter);
	}

	private void createPlaylist() {

		pc.setPlaylistTitle(mood);

		pc.setPlayListItems(searchResults);

		pc.execute();
		
		if(pc.getStatus() == AsyncTask.Status.FINISHED){
			detail.setText("Your playlist has been created. Please click on any song to check it in youtube application.");
		}

		mDialog.hide();

	}

	public class PlaylistCreate extends AsyncTask<String, String, String> {

		private YouTube youtube;

		public static final String API_KEY = "AIzaSyAlCVtQQ4wv755v-naVCrQNC2NLfczDJPU";

		String user = "";
		String key = "";

		Context context;

		String playlistId;

		List<VideoItem> playListItems;

		Playlist playlistInserted;

		private PlaylistSnippet playlistSnippet = new PlaylistSnippet();

		PlaylistCreate(Context ctx, String accessToken, String usr) {
			key = accessToken;
			context = ctx;
			user = usr;
		}

		/**
		 * Create a playlist and add it to the authorized account.
		 */
		private void insertPlaylist() throws IOException {

			playlistSnippet
					.setDescription("A public playlist created by MoodyYoutube application.");
			PlaylistStatus playlistStatus = new PlaylistStatus();
			playlistStatus.setPrivacyStatus("public");

			Playlist youTubePlaylist = new Playlist();
			youTubePlaylist.setSnippet(playlistSnippet);
			youTubePlaylist.setStatus(playlistStatus);

			// Call the API to insert the new playlist. In the API call, the
			// first
			// argument identifies the resource parts that the API response
			// should
			// contain, and the second argument is the playlist being inserted.
			YouTube.Playlists.Insert playlistInsertCommand = youtube
					.playlists().insert("snippet,status", youTubePlaylist);
			playlistInserted = playlistInsertCommand.execute();

			// Print data from the API response and return the new playlist's
			// unique playlist ID.
			System.out.println("New Playlist name: "
					+ playlistInserted.getSnippet().getTitle());
			System.out.println(" - Privacy: "
					+ playlistInserted.getStatus().getPrivacyStatus());
			System.out.println(" - Description: "
					+ playlistInserted.getSnippet().getDescription());
			System.out.println(" - Posted: "
					+ playlistInserted.getSnippet().getPublishedAt());
			System.out.println(" - Channel: "
					+ playlistInserted.getSnippet().getChannelId() + "\n");

			playlistId = playlistInserted.getId();

		}

		/**
		 * Create a playlist item with the specified video ID and add it to the
		 * specified playlist.
		 * 
		 * @param playlistId
		 *            assign to newly created playlistitem
		 * @param videoId
		 *            YouTube video id to add to playlistitem
		 */
		public void insertPlaylistItem() {

			try {

				// Call the API to add the playlist item to the specified
				// playlist.
				// In the API call, the first argument identifies the resource
				// parts
				// that the API response should contain, and the second argument
				// is
				// the playlist item being inserted.
				YouTube.PlaylistItems.Insert playlistItemsInsertCommand = null;

				for (int i = 0; i < playListItems.size(); i++) {

					// Define a resourceId that identifies the video being added
					// to the
					// playlist.
					ResourceId resourceId = new ResourceId();
					resourceId.setKind("youtube#video");
					resourceId.setVideoId(playListItems.get(i).getId());

					// Set fields included in the playlistItem resource's
					// "snippet"
					// part.
					PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
					playlistItemSnippet.setTitle(playListItems.get(i)
							.getTitle());
					playlistItemSnippet.setPlaylistId(playlistId);
					playlistItemSnippet.setResourceId(resourceId);

					// Create the playlistItem resource and set its snippet to
					// the
					// object created above.
					PlaylistItem playlistItem = new PlaylistItem();
					playlistItem.setSnippet(playlistItemSnippet);

					playlistItemsInsertCommand = youtube.playlistItems()
							.insert("snippet,contentDetails", playlistItem);

					PlaylistItem returnedPlaylistItem = playlistItemsInsertCommand
							.execute();

					// Print data from the API response and return the new
					// playlist
					// item's unique playlistItem ID.

					// System.out.println("New PlaylistItem name: " +
					// returnedPlaylistItem.getSnippet().getTitle());
					// System.out.println(" - Video id: " +
					// returnedPlaylistItem.getSnippet().getResourceId().getVideoId());
					// System.out.println(" - Posted: " +
					// returnedPlaylistItem.getSnippet().getPublishedAt());
					// System.out.println(" - Channel: " +
					// returnedPlaylistItem.getSnippet().getChannelId());
					if (i == 49) {
						playlistcreated = true;
						detail.setText("Your playlist has been created. Please click on any song to check it in youtube application.");
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		@Override
		protected String doInBackground(String... params) {
			try {

				GoogleCredential credential = new GoogleCredential();
				credential.setAccessToken(key);

				// String client_id =
				// "407143739384-kit3m9ph6ic3l39nv6bqgqkau5s9aaff.apps.googleusercontent.com";

				HttpTransport transport = AndroidHttp.newCompatibleTransport();
				JsonFactory jsonFactory = new AndroidJsonFactory();
				youtube = new YouTube.Builder(transport, jsonFactory,
						credential).setApplicationName("MoodyYoutube").build();

				insertPlaylist();

				insertPlaylistItem();

			} catch (GoogleJsonResponseException e) {
				System.err.println("There was a service error: "
						+ e.getDetails().getCode() + " : "
						+ e.getDetails().getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("IOException: " + e.getMessage());
				e.printStackTrace();
			} catch (Throwable t) {
				System.err.println("Throwable: " + t.getMessage());
				t.printStackTrace();
			}
			return null;
		}

		public void setPlaylistTitle(String title) {
			playlistSnippet.setTitle(title);
		}

		public String getChannelID() {
			return playlistId;
		}

		public void setPlayListItems(List<VideoItem> list) {
			playListItems = list;
		}
	}

}
