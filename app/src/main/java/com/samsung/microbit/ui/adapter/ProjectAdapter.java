package com.samsung.microbit.ui.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
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
import android.widget.TextView;

import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.EchoClientManager;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.Project;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.activity.ProjectActivity;
import com.samsung.microbit.ui.control.ExtendedEditText;

import java.util.List;

public class ProjectAdapter extends BaseAdapter {

    private List<Project> projects;
    private ProjectActivity projectActivity;
    int currentEditableRow = -1;
    private int mSendBtnLayoutPadding = 0;

    protected String TAG = "ProjectAdapter";
    protected boolean debug = BuildConfig.DEBUG;

    protected void logi(String message) {
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    private TextView.OnEditorActionListener editorOnActionListener = new TextView.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

            logi("onEditorAction() :: currentEditableRow=" + currentEditableRow);
            boolean handled = true;
            int pos = (int) v.getTag(R.id.positionId);
            Project project = projects.get(pos);
            project.inEditMode = false;
            currentEditableRow = -1;

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dismissKeyBoard(v, true, true);
            } else if (actionId == -1) {
                dismissKeyBoard(v, true, false);
            }

            return handled;
        }
    };

    private View.OnClickListener appNameClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            logi("OnClickListener() :: " + v.getClass().getName());

            boolean expandProjectItem = false;
            try {
                expandProjectItem = projectActivity.getResources().getBoolean(R.bool.expandProjectItem);
            } catch (Exception e) {
            }

            if (expandProjectItem) {
                changeActionBar(v);
            } else {
                if (currentEditableRow != -1) {
                    int i = (Integer) v.getTag(R.id.positionId);
                    if (i != currentEditableRow) {
                        renameProject(v);
                    }
                } else {
                    renameProject(v);
                }
            }
            /*
            logi("OnClickListener() :: " + v.getClass().getName());
			if (v.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				changeActionBar(v);
			} else if (v.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				if (currentEditableRow != -1) {
					int i = (Integer) v.getTag(R.id.positionId);
					if (i != currentEditableRow) {
						renameProject(v);
					}
				} else {
					renameProject(v);
				}
			}*/
        }
    };

    private View.OnLongClickListener appNameLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {

            logi("OnLongClickListener() :: " + v.getClass().getName());
            boolean rc = false;
            //if (v.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            renameProject(v);
            rc = true;
            //}

            return rc;
        }
    };

    private void HideEditTextView(View v) {
        Button bt = (Button) v.getTag(R.id.editbutton);
        bt.setVisibility(View.VISIBLE);
        v.setVisibility(View.INVISIBLE);
    }

    private void ShowEditTextView(View v) {
        Button bt = (Button) v.getTag(R.id.editbutton);
        bt.setVisibility(View.INVISIBLE);
        v.setVisibility(View.VISIBLE);
    }

    private void dismissKeyBoard(View v, boolean hide, boolean done) {

        logi("dismissKeyBoard() :: ");
        int pos = (Integer) v.getTag(R.id.positionId);
        logi("dismissKeyBoard() :: pos = " + pos + " currentEditableRow=" + currentEditableRow);


        InputMethodManager imm = (InputMethodManager) projectActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);

        if (hide) {
            HideEditTextView(v);
            //v.setVisibility(View.INVISIBLE);
        }

        if (done) {
            EditText ed = (EditText) v;
            pos = (int) ed.getTag(R.id.positionId);
            String newName = ed.getText().toString();
            Project p = projects.get(pos);
            if (newName != null && newName.length() > 0) {
                if (p.name.compareToIgnoreCase(newName) != 0) {
                    projectActivity.renameFile(p.filePath, newName);
                }
            }
        }
    }

    private void showKeyBoard(final View v) {

        logi("showKeyBoard() :: " + v.getClass().getName());
        int pos = (Integer) v.getTag(R.id.positionId);
        logi("showKeyBoard() :: pos = " + pos + " currentEditableRow=" + currentEditableRow);

        //v.setVisibility(View.VISIBLE);
        ShowEditTextView(v);

        final InputMethodManager imm = (InputMethodManager) projectActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                imm.showSoftInput(v, 0);
                v.requestFocus();
            }
        }, 100);
    }

    private void changeActionBar(View v) {

        logi("changeActionBar() :: ");

        int pos = (int) v.getTag(R.id.positionId);
        logi("changeActionBar() :: pos = " + pos + " currentEditableRow=" + currentEditableRow);

        Project project = projects.get(pos);
        project.actionBarExpanded = (project.actionBarExpanded) ? false : true;
        if (currentEditableRow != -1) {
            project = projects.get(currentEditableRow);
            project.inEditMode = false;
            currentEditableRow = -1;
            dismissKeyBoard(v, false, false);
        }

        notifyDataSetChanged();
    }

    private void renameProject(View v) {

        logi("renameProject() :: ");

        int pos = (int) v.getTag(R.id.positionId);
        logi("renameProject() :: pos = " + pos + " currentEditableRow=" + currentEditableRow);

        Project project;
        if (currentEditableRow != -1) {
            project = projects.get(currentEditableRow);
            project.inEditMode = false;
            currentEditableRow = -1;
        }

        project = projects.get(pos);
        project.inEditMode = (project.inEditMode) ? false : true;
        currentEditableRow = pos;
        View ev = (View) v.getTag(R.id.textEdit);
        showKeyBoard(ev);
        notifyDataSetChanged();
    }

    private View.OnClickListener sendBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            logi("sendBtnClickListener() :: ");
            projectActivity.onClick(v);

        }
    };

    private View.OnClickListener deleteBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            logi("deleteBtnClickListener() :: ");
            final int pos = (int) v.getTag();
            //Update Stats
            if (EchoClientManager.getInstance().getEcho() != null) {
                EchoClientManager.getInstance().getEcho().userActionEvent("click", "DeleteProject", null);
            }
            PopUp.show(MBApp.getContext(),
                    MBApp.getContext().getString(R.string.delete_project_message),
                    MBApp.getContext().getString(R.string.delete_project_title),
                    R.drawable.delete_project, R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_NONE,
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

        mSendBtnLayoutPadding = (int) projectActivity.getResources().getDimension(R.dimen.custom_button_padding);
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
        appNameButton.setTypeface(MBApp.getApp().getTypeface());

        ExtendedEditText appNameEdit = (ExtendedEditText) convertView.findViewById(R.id.appNameEdit);
        appNameEdit.setTypeface(MBApp.getApp().getTypeface());

        LinearLayout actionBarLayout = (LinearLayout) convertView.findViewById(R.id.actionBarForProgram);
        if (actionBarLayout != null) {
            if (project.actionBarExpanded) {
                actionBarLayout.setVisibility(View.VISIBLE);
                appNameButton.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(MBApp.getContext(), R.drawable.down_arrow), null);
            } else {
                actionBarLayout.setVisibility(View.GONE);
                appNameButton.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(MBApp.getContext(), R.drawable.side_arrow), null);
            }
        }

        appNameButton.setText(project.name);
        appNameButton.setTag(R.id.positionId, position);
        appNameButton.setTag(R.id.textEdit, appNameEdit);
        appNameButton.setOnClickListener(appNameClickListener);
        appNameButton.setOnLongClickListener(appNameLongClickListener);

        appNameEdit.setTag(R.id.positionId, position);
        appNameEdit.setTag(R.id.editbutton, appNameButton);
        appNameEdit.setOnEditorActionListener(editorOnActionListener);

        if (project.inEditMode) {
            appNameEdit.setVisibility(View.VISIBLE);

            appNameEdit.setText(project.name);
            appNameEdit.setSelection(project.name.length());
            appNameEdit.requestFocus();
            appNameButton.setVisibility(View.INVISIBLE);

        } else {
            appNameEdit.setVisibility(View.INVISIBLE);
            appNameButton.setVisibility(View.VISIBLE);
            //dismissKeyBoard(appNameEdit, false);
        }

        //appNameEdit.setOnClickListener(appNameClickListener);

        TextView flashBtnText = (TextView) convertView.findViewById(R.id.project_item_text);
        flashBtnText.setTypeface(MBApp.getApp().getTypeface());
        LinearLayout sendBtnLayout = (LinearLayout) convertView.findViewById(R.id.sendBtn);
        sendBtnLayout.setTag(position);
        sendBtnLayout.setOnClickListener(sendBtnClickListener);

        ImageButton deleteBtn = (ImageButton) convertView.findViewById(R.id.deleteBtn);
        deleteBtn.setTag(position);
        deleteBtn.setOnClickListener(deleteBtnClickListener);
        deleteBtn.setEnabled(true);


        Drawable myIcon;
        if (project.runStatus) {
            flashBtnText.setText("");
            myIcon = convertView.getResources().getDrawable(R.drawable.green_btn);
        } else {
            flashBtnText.setText(R.string.flash);
            myIcon = convertView.getResources().getDrawable(R.drawable.blue_btn);
        }
        sendBtnLayout.setBackground(myIcon);
        sendBtnLayout.setPadding(mSendBtnLayoutPadding, mSendBtnLayoutPadding, mSendBtnLayoutPadding, mSendBtnLayoutPadding);

        sendBtnLayout.setClickable(true);
        return convertView;
    }
}
