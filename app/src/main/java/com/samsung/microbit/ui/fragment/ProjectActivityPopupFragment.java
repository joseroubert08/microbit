package com.samsung.microbit.ui.fragment;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.samsung.microbit.R;


/**
 * A placeholder fragment containing a simple view.
 */
public class ProjectActivityPopupFragment extends Fragment {

	ImageButton btn;
	TextView currentStatus;

	public ProjectActivityPopupFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.fragment_flashing, container, false);
		currentStatus = (TextView) v.findViewById(R.id.currentStatus);
		btn = (ImageButton) v.findViewById(R.id.ok_cancel_button);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});

		return v;
	}

	public void changeMeaning() {
		Drawable myIcon = getResources().getDrawable(R.drawable.green_circle_btn);
		btn.setBackground(myIcon);
		btn.setImageResource(R.drawable.tick);
		currentStatus.setText("Success");
	}
}
