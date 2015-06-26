package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.Project;
import com.samsung.microbit.ui.adapter.ProjectAdapter;
import com.samsung.microbit.ui.fragment.ProjectActivityPopupFragment;

import java.util.ArrayList;
import java.util.List;


public class ProjectActivity extends Activity implements View.OnClickListener {

	List<Project> projectList = new ArrayList<Project>();
	ProjectAdapter projectAdapter;
	private ListView projectListView;

	ProjectActivityPopupFragment fragment;
	LinearLayout popupOverlay;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MBApp.setContext(this);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_projects);

		LinearLayout mainContentView = (LinearLayout) findViewById(R.id.mainContentView);
		mainContentView.getBackground().setAlpha(128);

		projectListView = (ListView) findViewById(R.id.projectListView);
		populateDummyData();
		projectAdapter = new ProjectAdapter(this, projectList);
		projectListView.setAdapter(projectAdapter);
	}

	public void onHomeBtnClicked(View v){

	}

	public void onClick(final View v) {

		switch (v.getId()) {
			case R.id.createProject:
				break;
			case R.id.homeBtn:
				Intent intent = new Intent(this, HomeActivity.class);
				startActivity(intent);
				finish();
				break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		/*
		if (id == R.id.action_settings) {
			return true;
		} else if (id == R.id.load_fragment) {
			Log.i("ProjectActivity", "###### onOptionsItemSelected load_fragment");

			if (popupOverlay == null) {
				popupOverlay = (LinearLayout) findViewById(R.id.popup_overlay);
				popupOverlay.getBackground().setAlpha(224);
				popupOverlay.setOnTouchListener(new View.OnTouchListener() {
					@OverridemainContentView
					public boolean onTouch(View v, MotionEvent event) {
						return true;
					}
				});
			}

			popupOverlay.setVisibility(View.VISIBLE);

			if (fragment == null) {
				FragmentManager fragmentManager = getFragmentManager();
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

				fragment = new ProjectActivityPopupFragment();
				fragmentTransaction.add(R.id.popup_overlay, fragment);
				fragmentTransaction.commit();
			}
		} else if (id == R.id.hide_load_fragment) {
			Log.i("ProjectActivity", "#### onOptionsItemSelected() : id == R.id.hide_load_fragment");
			if (popupOverlay != null && popupOverlay.getVisibility() == View.VISIBLE) {
				popupOverlay.setVisibility(View.INVISIBLE);
			}

		} else if (id == R.id.change_button_meaning) {
			Log.i("ProjectActivity", "#### onOptionsItemSelected() : id == R.id.change_button_meaning");
			if (popupOverlay != null && popupOverlay.getVisibility() == View.VISIBLE) {
				fragment.changeMeaning();
			}
		}
		*/

		return super.onOptionsItemSelected(item);
	}

	private void populateDummyData() {
		projectList.clear();
		projectList.add(new Project("My First project", "http://microbit.com/apps/1", false));
		projectList.add(new Project("My Second project", "http://microbit.com/apps/2", false));
		projectList.add(new Project("My Third project", "http://microbit.com/apps/3", true));
		projectList.add(new Project("My Forth project", "http://microbit.com/apps/4", false));
		projectList.add(new Project("My Forth project", "http://microbit.com/apps/4", false));
		projectList.add(new Project("My Fifth project", "http://microbit.com/apps/4", false));
		projectList.add(new Project("My Sixth project", "http://microbit.com/apps/4", false));
		projectList.add(new Project("My Seventh project", "http://microbit.com/apps/4", false));
	}
}
