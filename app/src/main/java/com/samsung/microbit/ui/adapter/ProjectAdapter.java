package com.samsung.microbit.ui.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.Project;
import com.samsung.microbit.ui.activity.ProjectActivity;
import com.samsung.microbit.ui.PopUp;

import java.util.List;

public class ProjectAdapter extends BaseAdapter {

	private List<Project> projects;
	private ProjectActivity projectActivity;
    private LinearLayout lastActionBarLayout = null;


	private View.OnClickListener appNameClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {


            RelativeLayout r = (RelativeLayout) v.getParent().getParent();
            LinearLayout actionBarLayout = null;

            if (r != null){
                actionBarLayout = (LinearLayout)r.findViewById(R.id.actionBarForProgram);
                Log.d("Rohit", "Got Action bar");
                if (actionBarLayout != null){
                    if (lastActionBarLayout != null){
                        lastActionBarLayout.setVisibility(LinearLayout.GONE);
                    }
                    lastActionBarLayout = (LinearLayout)r.findViewById(R.id.actionBarForProgram);
                    actionBarLayout.setVisibility(LinearLayout.VISIBLE);
                }
            }
			//TODO AFTER DEMO EDIT PROJECT NAME
			/*
			Toast.makeText(MBApp.getContext(), "AppName Clicked: " + v.getTag(), Toast.LENGTH_SHORT).show();

			final EditText ed = (EditText) v.getTag(R.id.textedit);
			//v.setVisibility(View.INVISIBLE);
			ed.setVisibility(View.VISIBLE);
			ed.requestFocus();
			InputMethodManager imm = (InputMethodManager) projectActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(ed, InputMethodManager.SHOW_IMPLICIT);
			*/
		}
	};

	private View.OnClickListener sendBtnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//Toast.makeText(MBApp.getContext(), "sendBtn Clicked: " + v.getTag(), Toast.LENGTH_SHORT).show();
			((View.OnClickListener)projectActivity).onClick(v);

		}
	};

	private View.OnClickListener codeBtnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			((View.OnClickListener)projectActivity).onClick(v);
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
							if(Utils.deleteFile(proj.filePath)) {
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


		Button appName = (Button) convertView.findViewById(R.id.appNameButton);
		EditText appNameEdit = (EditText) convertView.findViewById(R.id.appNameEdit);

		appName.setTag(R.id.positionId, position);
		appName.setTag(R.id.textedit, appNameEdit);
		appName.setOnClickListener(appNameClickListener);

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

		appName.setText(p.name);
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
