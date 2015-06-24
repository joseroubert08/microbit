package com.samsung.microbit.ui.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.Project;

import java.util.List;

public class ProjectAdapter extends BaseAdapter {

	private List<Project> projects;

	public ProjectAdapter(List<Project> list) {
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

		Button appName = (Button) convertView.findViewById(R.id.appName);
		Button codeBtn = (Button) convertView.findViewById(R.id.codeBtn);
		Button sendBtn = (Button) convertView.findViewById(R.id.sendBtn);
		ImageButton deleteBtn = (ImageButton) convertView.findViewById(R.id.deleteBtn);

		appName.setText(p.getName());
		codeBtn.setText("Code");
		if (p.isRunStatus()) {
			sendBtn.setText("Running");
			Drawable myIcon = convertView.getResources().getDrawable(R.drawable.green_btn);
			sendBtn.setBackground(myIcon);
		} else {
			sendBtn.setText("Send");
			Drawable myIcon = convertView.getResources().getDrawable(R.drawable.lightblue_btn);
			sendBtn.setBackground(myIcon);
		}

		return convertView;
	}
}
