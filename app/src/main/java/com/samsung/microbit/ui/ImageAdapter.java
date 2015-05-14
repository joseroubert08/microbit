package com.samsung.microbit.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.samsung.microbit.R;

public class ImageAdapter extends BaseAdapter {
    private Context mContext;

    // Keep all Images in array
    public Integer[] mThumbIds = {
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff, R.drawable.ledoff,
            R.drawable.ledoff
    };

    // Constructor
    public ImageAdapter(Context c){
        mContext = c;
    }

    @Override
    public int getCount() {
        return mThumbIds.length;
    }

    @Override
    public Object getItem(int position) {
        return mThumbIds[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView = new ImageView(mContext);
        imageView.setImageResource(mThumbIds[position]);
        imageView.setAdjustViewBounds(true);

        return imageView;
    }

}