package com.samsung.microbit.ui.adapter;

import android.app.Activity;
import android.content.Context;
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
	private LinearLayout lastActionBarLayout = null;

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
				logi("OnFocusChangeListener.onFocusChange() :: " + v.getTag(R.id.positionId));
				dismissKeyBoard(v, false);
			} else {
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

			RelativeLayout r = (RelativeLayout) v.getParent().getParent();
			LinearLayout actionBarLayout = null;

			if (r != null) {
				actionBarLayout = (LinearLayout) r.findViewById(R.id.actionBarForProgram);
				if (actionBarLayout != null) {  //Potrait Layout
					if (lastActionBarLayout != null) {
						lastActionBarLayout.setVisibility(LinearLayout.GONE);
					}

					lastActionBarLayout = (LinearLayout) r.findViewById(R.id.actionBarForProgram);
					actionBarLayout.setVisibility(LinearLayout.VISIBLE);
				} else {
                    EditText ed = (EditText) v.getTag(R.id.textedit);
                    ed.setVisibility(View.VISIBLE);
                    String currentText = (String) ((Button) v).getText();
                    ed.setText(currentText);
                    ed.requestFocus();
                    ed.setSelection(ed.getText().length());
				}
			}

		}
	};

	private View.OnClickListener sendBtnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//Toast.makeText(MBApp.getContext(), "sendBtn Clicked: " + v.getTag(), Toast.LENGTH_SHORT).show();
			((View.OnClickListener) projectActivity).onClick(v);

		}
	};

	private View.OnClickListener codeBtnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			((View.OnClickListener) projectActivity).onClick(v);
			//Toast.makeText(MBApp.getContext(), "codeBtn Clicked: " + v.getTag(), Toast.LENGTH_SHORT).show();
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

		Project p = projects.get(position);
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(MBApp.getContext());
			convertView = inflater.inflate(R.layout.project_items, null);
		}

		Button appNameButton = (Button) convertView.findViewById(R.id.appNameButton);
		ExtendedEditText appNameEdit = (ExtendedEditText) convertView.findViewById(R.id.appNameEdit);


		appNameButton.setTag(R.id.positionId, position);
		appNameButton.setTag(R.id.textedit, appNameEdit);
		appNameButton.setOnClickListener(appNameClickListener);


		appNameEdit.setTag(R.id.positionId, position);
		appNameEdit.setTag(R.id.editbutton, appNameButton);
		appNameEdit.setOnEditorActionListener(editorOnActionListener);
		appNameEdit.setOnFocusChangeListener(focusChangeListener);

		//appNameEdit.setOnClickListener(appNameClickListener);

		Button codeBtn = (Button) convertView.findViewById(R.id.codeBtn);
		codeBtn.setTag(position);
		codeBtn.setOnClickListener(codeBtnClickListener);

		Button sendBtn = (Button) convertView.findViewById(R.id.sendBtn);
		sendBtn.setTag(position);
		sendBtn.setOnClickListener(sendBtnClickListener);

		ImageButton deleteBtn = (ImageButton) convertView.findViewById(R.id.deleteBtn);
		deleteBtn.setTag(position);
		deleteBtn.setOnClickListener(deleteBtnClickListener);
		deleteBtn.setEnabled(true);

		appNameButton.setText(p.name);
		//appNameEdit.setText(p.name);
		codeBtn.setText("Code");
		if (p.runStatus) {
			sendBtn.setText("Running");
			Drawable myIcon = convertView.getResources().getDrawable(R.drawable.green_btn);
			sendBtn.setBackground(myIcon);
		} else {
			sendBtn.setText("Send");
			Drawable myIcon = convertView.getResources().getDrawable(R.drawable.lightblue_btn);
			sendBtn.setBackground(myIcon);
		}

		sendBtn.setClickable(true);
		return convertView;
	}
}
