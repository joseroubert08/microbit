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
    private String mDeviceCodeArray[];

    public LEDAdapter(Context c, String deviceCodeArray[]) {
        mContext = c;
        mDeviceCodeArray = deviceCodeArray;
    }

    public int getCount() {
        return 25;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return position;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            final float scale = mContext.getResources().getDisplayMetrics().density;
            int led_size = mContext.getResources().getInteger(R.integer.led_size);
            int pixels = (int) (led_size * scale + 0.5f);
            imageView.setLayoutParams(new GridView.LayoutParams(pixels, pixels));
        } else {
            imageView = (ImageView) convertView;
        }

        if(mDeviceCodeArray[position].equals("1")) {
            imageView.setBackground(mContext.getResources().getDrawable(R.drawable.red_white_led_btn));

        } else {

            imageView.setBackground(mContext.getResources().getDrawable(R.drawable.white_red_led_btn));

            int startIndex =  position -5;
            while(startIndex >= 0) {
                if(mDeviceCodeArray[startIndex].equals("1")){
                    imageView.setBackground(mContext.getResources().getDrawable(R.drawable.red_white_led_btn));
                }

                startIndex-=5;
            }
        }

        return imageView;
    }
}