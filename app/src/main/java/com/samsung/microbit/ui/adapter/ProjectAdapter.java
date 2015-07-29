package com.samsung.microbit.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.Project;
import com.samsung.microbit.ui.activity.ProjectActivity;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.control.ExtendedEditText;

import java.util.List;

public class ProjectAdapter extends BaseAdapter {

	private List<Project> projects;
	private ProjectActivity projectActivity;

	protected String TAG = "ProjectAdapter";
	protected boolean debug = true;

	protected void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	private TextView.OnEditorActionListener editorOnActionListener = new TextView.OnEditorActionListener() {

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

			boolean handled = false;
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				handled = true;
				dismissKeyBoard(v, true);
			} else if (actionId == -1) {
				hideControl(v, false);
			}

			return handled;
		}
	};

	private View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {

		@Override
		public void onFocusChange(View v, boolean hasFocus) {

			if (!hasFocus) {
				final int pos = (int) v.getTag(R.id.positionId);
				Project project = projects.get(pos);
				project.inEditMode = false;
				dismissKeyBoard(v, false);
			} else {
				final int pos = (int) v.getTag(R.id.positionId);
				Project project = projects.get(pos);
				project.inEditMode = true;
				showKeyBoard(v);
			}
		}
	};

	private void hideControl(final View v, final boolean done) {

		/*
		 * We use this method for hiding a edit control, when users presses Done or cancels a editiing session,
		 * to stop a screen flicker.
		 */
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				projectActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						v.setVisibility(View.INVISIBLE);
						if (done) {
							EditText ed = (EditText) v;
							int pos = (int) ed.getTag(R.id.positionId);
							String newName = ed.getText().toString();
							Project p = projects.get(pos);
							if (newName != null && newName.length() > 0) {
								if (p.name.compareToIgnoreCase(newName) != 0) {
									projectActivity.renameFile(p.filePath, newName);
								}
							}
						}
					}
				});
			}
		}).start();
	}

	private void dismissKeyBoard(View v, boolean done) {

		InputMethodManager imm = (InputMethodManager) projectActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
		hideControl(v, done);
	}

	private void showKeyBoard(View v) {



		v.setVisibility(View.VISIBLE);
		InputMethodManager imm = (InputMethodManager) projectActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
	}

	private View.OnClickListener appNameClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			if (v.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				changeActionBar(v);
			} else if (v.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				renameProject(v);
			}
		}
	};

	private View.OnLongClickListener appNameLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {

			if (v.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				renameProject(v);
			}

			return true;
		}
	};

	private void changeActionBar(View v) {

		final int pos = (int) v.getTag(R.id.positionId);
		Project project = projects.get(pos);
		project.actionBarExpanded = (project.actionBarExpanded) ? false : true;
		notifyDataSetChanged();
	}

	private void renameProject(View v) {

		final int pos = (int) v.getTag(R.id.positionId);
		Project project = projects.get(pos);
		project.inEditMode = (project.inEditMode) ? false : true;
		notifyDataSetChanged();
	}

	private View.OnClickListener sendBtnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			//Toast.makeText(MBApp.getContext(), "sendBtn Clicked: " + v.getTag(), Toast.LENGTH_SHORT).show();
			((View.OnClickListener) projectActivity).onClick(v);

		}
	};

	private View.OnClickListener deleteBtnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			final int pos = (int) v.getTag();
			PopUp.show(MBApp.getContext(),
				MBApp.getContext().getString(R.string.delete_project_message),
				MBApp.getContext().getString(R.string.delete_project_title),
				R.drawable.delete, R.drawable.red_btn,
				PopUp.TYPE_CHOICE,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						PopUp.hide();
						Project proj = projects.get(pos);
						if (Utils.deleteFile(proj.filePath)) {
							projects.remove(pos);
							notifyDataSetChanged();
						}
					}
				}, null);
		}
	};

	public ProjectAdapter(ProjectActivity projectActivity, List<Project> list) {

		this.projectActivity = projectActivity;
		projects = list;
	}

	@Override
	public int getCount() {

		return projects.size();
	}

	@Override
	public Object getItem(int position) {

		return projects.get(position);
	}

	@Override
	public long getItemId(int position) {

		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Project project = projects.get(position);
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(MBApp.getContext());
			convertView = inflater.inflate(R.layout.project_items, null);
		}

		Button appNameButton = (Button) convertView.findViewById(R.id.appNameButton);
		ExtendedEditText appNameEdit = (ExtendedEditText) convertView.findViewById(R.id.appNameEdit);

		LinearLayout actionBarLayout = (LinearLayout) convertView.findViewById(R.id.actionBarForProgram);
		if (actionBarLayout != null) {
			if (project.actionBarExpanded)
				actionBarLayout.setVisibility(View.VISIBLE);
			else
				actionBarLayout.setVisibility(View.GONE);
		}


		appNameButton.setText(project.name);
		appNameButton.setTag(R.id.positionId, position);
		appNameButton.setTag(R.id.textedit, appNameEdit);
		appNameButton.setOnClickListener(appNameClickListener);
		appNameButton.setOnLongClickListener(appNameLongClickListener);

		appNameEdit.setTag(R.id.positionId, position);
		appNameEdit.setTag(R.id.editbutton, appNameButton);
		appNameEdit.setOnEditorActionListener(editorOnActionListener);
		appNameEdit.setOnFocusChangeListener(focusChangeListener);


		if (project.inEditMode) {
			appNameEdit.setVisibility(View.VISIBLE);
			appNameEdit.setText(project.name);
			appNameEdit.requestFocus();
			appNameEdit.setSelection(project.name.length());
		} else {
			appNameEdit.setVisibility(View.INVISIBLE);
		}

		//appNameEdit.setOnClickListener(appNameClickListener);

		Button sendBtn = (Button) convertView.findViewById(R.id.sendBtn);
		sendBtn.setTag(position);
		sendBtn.setOnClickListener(sendBtnClickListener);

		ImageButton deleteBtn = (ImageButton) convertView.findViewById(R.id.deleteBtn);
		deleteBtn.setTag(position);
		deleteBtn.setOnClickListener(deleteBtnClickListener);
		deleteBtn.setEnabled(true);

		if (project.runStatus) {
			sendBtn.setText("");
			Drawable myIcon = convertView.getResources().getDrawable(R.drawable.green_btn);
			sendBtn.setBackground(myIcon);
		} else {
			sendBtn.setText(R.string.flash);
			Drawable myIcon = convertView.getResources().getDrawable(R.drawable.blue_btn);
			sendBtn.setBackground(myIcon);
		}

		sendBtn.setClickable(true);
		return convertView;
	}
}
