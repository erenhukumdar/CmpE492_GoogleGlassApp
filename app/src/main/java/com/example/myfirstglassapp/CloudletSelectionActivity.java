package com.example.myfirstglassapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

public class CloudletSelectionActivity extends Activity {
	private static final String TAG = "CloudletSelectionActivity";
	private List<Card> mCards;
	private CardScrollView mCardScrollView;
	NsdHelper mNsdHelper;
	private Context mCtx;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCtx = this;
		createCards();

		mCardScrollView = new CardScrollView(this);
		ExampleCardScrollAdapter adapter = new ExampleCardScrollAdapter();
		mCardScrollView.setAdapter(adapter);
		mCardScrollView.activate();
		mCardScrollView.setOnItemClickListener(new OnItemClickListener() 
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
			{
				//if user does not click first and second page
				if(position != 0){
					String serverName = (String) mCards.get(position).getFootnote();
					String serverIp = (String) mCards.get(position).getText();
					Log.d(TAG, "Selected server is: " + serverName + " - " + serverIp);

					SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
					Editor editor = sharedPref.edit();
					editor.putString(getString(R.string.cloudlet_ip), serverIp);
					editor.commit();
				}
				
				Intent intent = new Intent(mCtx, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.putExtra("keep", false);
				startActivity(intent);
				finish();
			}
		});
		setContentView(mCardScrollView);


	}

	@Override
	protected void onPause() {
		super.onPause();

		if(mNsdHelper != null)
			return;

		mNsdHelper.stopDiscovery();
		mNsdHelper = null;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(mNsdHelper != null)
			mNsdHelper.stopDiscovery();

		mNsdHelper = new NsdHelper(this);
		mNsdHelper.initializeNsd();
		mNsdHelper.discoverServices();
	}

	public void onServerFound(String name,String ip){
		Log.d(TAG, "Server found: " + name + " - " + ip);

		Card card;
		card = new Card(this);
		card.setText(ip);
		card.setFootnote(name);
		card.setImageLayout(Card.ImageLayout.LEFT);
		card.addImage(R.drawable.cloudlet);
		mCards.add(card);
	}

	private void createCards() {
		mCards = new ArrayList<Card>();

		Card card;

		card = new Card(this);
		card.setText("mDNS search is activated");
		card.setFootnote("Swipe right to select a server...");
		card.setImageLayout(Card.ImageLayout.FULL);
		card.addImage(R.drawable.touch_screen);
		mCards.add(card);
	}

	private class ExampleCardScrollAdapter extends CardScrollAdapter {

		@Override
		public int getPosition(Object item) {
			return mCards.indexOf(item);
		}

		@Override
		public int getCount() {
			return mCards.size();
		}

		@Override
		public Object getItem(int position) {
			return mCards.get(position);
		}

		@Override
		public int getViewTypeCount() {
			return Card.getViewTypeCount();
		}

		@Override
		public int getItemViewType(int position){
			return mCards.get(position).getItemViewType();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return  mCards.get(position).getView(convertView, parent);
		}
	}


}
