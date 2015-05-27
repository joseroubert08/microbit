package com.samsung.microbit.ui;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import android.os.Message;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.ui.fragment.FlashSectionFragment;
import com.samsung.microbit.R;
import com.samsung.microbit.service.DfuService;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
	private LeDeviceListAdapter mLeDeviceListAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;
	private int mPosition = -1;

	private static final int REQUEST_ENABLE_BT = 1;

	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;

	Button continueFlash;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.listitem_device);

		final DeviceScanActivity ds = this;
		continueFlash = (Button) findViewById(R.id.continueFlash);
		continueFlash.setEnabled(false);
		continueFlash.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("DeviceScanActivity", "#### continueFlash.onClick");
				int position = mPosition;
				final BluetoothDevice mSelectedDevice = mLeDeviceListAdapter.getDevice(position);
				if (mSelectedDevice == null) {
					return;
				}

				final Intent service = new Intent(ds, DfuService.class);
				service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, mSelectedDevice.getAddress());
				service.putExtra(DfuService.EXTRA_DEVICE_NAME, mSelectedDevice.getName());
				service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
				service.putExtra(DfuService.EXTRA_FILE_PATH, FlashSectionFragment.BINARY_FILE_NAME); // a path or URI must be provided.
				service.putExtra(DfuService.EXTRA_KEEP_BOND, false);

				continueFlash.setEnabled(false);
				service.putExtra("com.samsung.resultReceiver", resultReceiver);
				service.putExtra("com.samsung.runonly.phase", 2);
				Log.i("DeviceScanActivity", "##### onListItemClick() - startService(service)");
				startService(service);
				Log.i("DeviceScanActivity", "##### onListItemClick() - startService(service) end");
			}
		});


		getActionBar().setTitle(R.string.title_devices);
		mHandler = new Handler();
		MBApp.setContext(this);

		// Use this check to determine whether BLE is supported on the device.  Then you can
		// selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}

		// Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (!mScanning) {
			menu.findItem(R.id.menu_stop).setVisible(false);
			menu.findItem(R.id.menu_scan).setVisible(true);
			menu.findItem(R.id.menu_refresh).setActionView(null);
		} else {
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_scan).setVisible(false);
			menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_scan:
				mLeDeviceListAdapter.clear();
				scanLeDevice(true);
				break;

			case R.id.menu_stop:
				scanLeDevice(false);
				break;
		}

		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
		// fire an intent to display a dialog asking the user to grant permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}

		// Initializes list view adapter.
		mLeDeviceListAdapter = new LeDeviceListAdapter();
		setListAdapter(mLeDeviceListAdapter);
		scanLeDevice(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
		mLeDeviceListAdapter.clear();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		if (mScanning) {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			mScanning = false;
			invalidateOptionsMenu();
		}

		final BluetoothDevice mSelectedDevice = mLeDeviceListAdapter.getDevice(position);
		if (mSelectedDevice == null)
			return;

		mPosition = position;
		final Intent service = new Intent(this, DfuService.class);
		service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, mSelectedDevice.getAddress());
		service.putExtra(DfuService.EXTRA_DEVICE_NAME, mSelectedDevice.getName());
		service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
		service.putExtra(DfuService.EXTRA_FILE_PATH, FlashSectionFragment.BINARY_FILE_NAME); // a path or URI must be provided.
		service.putExtra(DfuService.EXTRA_KEEP_BOND, false);

		service.putExtra("com.samsung.resultReceiver", resultReceiver);
		service.putExtra("com.samsung.runonly.phase", 1);
		getListView().setEnabled(false);

		Log.i("DeviceScanActivity", "##### onListItemClick() - startService(service)");
		startService(service);
		Log.i("DeviceScanActivity", "##### onListItemClick() - startService(service) end");
	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					invalidateOptionsMenu();
				}
			}, SCAN_PERIOD);

			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}

		invalidateOptionsMenu();
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
		new BluetoothAdapter.LeScanCallback() {

			@Override
			public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mLeDeviceListAdapter.addDevice(device);
						mLeDeviceListAdapter.notifyDataSetChanged();
					}
				});
			}
		};

	private List<UUID> parseUuids(byte[] advertisedData) {
		List<UUID> uuids = new ArrayList<UUID>();

		ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
		while (buffer.remaining() > 2) {
			byte length = buffer.get();
			if (length == 0)
				break;

			byte type = buffer.get();
			switch (type) {
				case 0x02: // Partial list of 16-bit UUIDs
				case 0x03: // Complete list of 16-bit UUIDs
					while (length >= 2) {
						uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
						length -= 2;
					}

					break;

				case 0x06: // Partial list of 128-bit UUIDs
				case 0x07: // Complete list of 128-bit UUIDs
					while (length >= 16) {
						long lsb = buffer.getLong();
						long msb = buffer.getLong();
						uuids.add(new UUID(msb, lsb));
						length -= 16;
					}

					break;

				default:
					buffer.position(buffer.position() + length - 1);
					break;
			}
		}

		return uuids;
	}

	// Adapter for holding devices found through scanning.
	private class LeDeviceListAdapter extends BaseAdapter {
		private ArrayList<BluetoothDevice> mLeDevices;
		private LayoutInflater mInflator;

		public LeDeviceListAdapter() {
			super();
			mLeDevices = new ArrayList<BluetoothDevice>();
			mInflator = DeviceScanActivity.this.getLayoutInflater();
		}

		public void addDevice(BluetoothDevice device) {
			if (!mLeDevices.contains(device)) {
				mLeDevices.add(device);
			}
		}

		public BluetoothDevice getDevice(int position) {
			return mLeDevices.get(position);
		}

		public void clear() {
			mLeDevices.clear();
		}

		@Override
		public int getCount() {
			return mLeDevices.size();
		}

		@Override
		public Object getItem(int i) {
			return mLeDevices.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			ViewHolder viewHolder;
			// General ListView optimization code.
			if (view == null) {
				view = mInflator.inflate(R.layout.row_layout, null);
				viewHolder = new ViewHolder();
				viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
				viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			BluetoothDevice device = mLeDevices.get(i);
			final String deviceName = device.getName();
			if (deviceName != null && deviceName.length() > 0)
				viewHolder.deviceName.setText(deviceName);
			else
				viewHolder.deviceName.setText(R.string.unknown_device);

			viewHolder.deviceAddress.setText(device.getAddress());
			return view;
		}
	}

	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
	}


	ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {

			int phase = resultCode & 0x03;
			Log.i("DeviceScanActivity", "##### onReceiveResult() - OK : " + phase);

			if ((phase & 0x01) != 0) {
				getListView().setEnabled(false);
				continueFlash.setEnabled(true);
			}

			if ((phase & 0x02) != 0) {
				continueFlash.setEnabled(false);
				getListView().setEnabled(true);
			}

			super.onReceiveResult(resultCode, resultData);
		}
	};

}