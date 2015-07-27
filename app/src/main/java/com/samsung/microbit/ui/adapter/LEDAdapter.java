package com.samsung.microbit.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.samsung.microbit.R;

/**
 * Created by Kupes on 21/06/2015.
 */
public class LEDAdapter extends BaseAdapter {
    private Context mContext;

    public LEDAdapter(Context c) {
        mContext = c;
    }

    public int getCount() {
        return 25;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            final float scale = mContext.getResources().getDisplayMetrics().density;
            int pixels = (int) (50 * scale + 0.5f);
            imageView.setLayoutParams(new GridView.LayoutParams(pixels, pixels));
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setBackground(mContext.getResources().getDrawable(R.drawable.white_red_led_btn));
        return imageView;
    }
}