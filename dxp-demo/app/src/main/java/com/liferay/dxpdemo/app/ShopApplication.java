package com.liferay.dxpdemo.app;

import android.app.Application;
import android.os.RemoteException;
import android.util.Log;

import com.liferay.dxpdemo.R;
import com.liferay.dxpdemo.beacon.NotificationUtil;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

/**
 * @author Javier Gamarra
 */
public class ShopApplication extends Application implements BeaconConsumer {

	protected static final String TAG = "ShopApp";
	private static final double MIN_DISTANCE = 1D;
	private Date lastNotificationSent = new Date(0);
	private BeaconManager beaconManager;
	private Region region1;
	private Region region2;

	@Override
	public void onCreate() {
		super.onCreate();

		bindBeacon();
	}

	@Override
	public void onBeaconServiceConnect() {
		beaconManager.setMonitorNotifier(new MonitorNotifier() {
			@Override
			public void didEnterRegion(Region region) {
				Log.i(TAG, "I just saw a beacon for the first time!");
			}

			@Override
			public void didExitRegion(Region region) {
				Log.i(TAG, "I no longer see a beacon");
			}

			@Override
			public void didDetermineStateForRegion(int state, Region region) {
				Log.i(TAG, "I have just switched from seeing/not seeing beacons: " + state);
			}
		});
		
		beaconManager.setRangeNotifier(new RangeNotifier() {
			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
				processNearBeacons(collection, region);
			}
		});

		try {
			String[] beacon_id1 = getResources().getStringArray(R.array.beacon_test1);
			String[] beacon_id2 = getResources().getStringArray(R.array.beacon_test2);

			Identifier[] identifiers1 = createIdentifier(beacon_id1);
			Identifier[] identifiers2 = createIdentifier(beacon_id2);
			region1 = new Region("shop1", identifiers1[0], identifiers1[1], identifiers1[2]);
			region2 = new Region("shop2", identifiers2[0], identifiers2[1], identifiers2[2]);
			beaconManager.startMonitoringBeaconsInRegion(region1);
			beaconManager.startMonitoringBeaconsInRegion(region2);
			beaconManager.startRangingBeaconsInRegion(region1);
			beaconManager.startRangingBeaconsInRegion(region2);
		} catch (RemoteException e) {
			Log.e(TAG, "Error reading beacons", e);
		}
	}

	protected Identifier[] createIdentifier(String[] beacon_array) {
		Identifier[] identifiers = new Identifier[3];

		identifiers[0] = Identifier.fromUuid(UUID.fromString(beacon_array[0]));
		identifiers[1] = Identifier.fromInt(Integer.parseInt(beacon_array[1]));
		identifiers[2] = Identifier.fromInt(Integer.parseInt(beacon_array[2]));

		return identifiers;
	}

	private void bindBeacon() {
		beaconManager = BeaconManager.getInstanceForApplication(this);
		beaconManager.getBeaconParsers().add(new BeaconParser().
				setBeaconLayout(getString(R.string.beacon_layout)));
		beaconManager.bind(this);
	}

	private void processNearBeacons(Collection<Beacon> collection, Region region) {
		for (Beacon beacon : collection) {
			double distance = beacon.getDistance();
			if (distance < MIN_DISTANCE) {

				Log.e("Beacon close, at: ", String.valueOf(distance) + " meters");

				if (lastNotificationSent.before(getTimeFiveMinutesAgo())) {

					String message = region.equals(region1) ? getString(R.string.app_text_msg1)
							: getString(R.string.app_text_msg2);

					NotificationUtil.sendNotificationMsg(getApplicationContext(), message);

					lastNotificationSent = new Date();
				}
			}
		}
	}

	private Date getTimeFiveMinutesAgo() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, -30);
		return calendar.getTime();
	}
}
