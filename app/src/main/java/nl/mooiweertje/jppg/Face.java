package nl.mooiweertje.jppg;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.palette.graphics.Palette;

import android.preference.PreferenceManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn"t
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class Face extends CanvasWatchFaceService {

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int FONTSIZE = 100;
    // public static final float PRESSURE_STANDARD_ATMOSPHERE_AMSTERDAM = 1015F;
    // public static final float PRESSURE_STANDARD_ATMOSPHERE_QNH = 1013.25F;
    public static final int ORANGE = Color.argb(255,255,95,0);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private static int speed = -1;
    private static float pressure = 0;
    private static boolean barofixed = false;
    private static float sealevelPressure = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;
    private static float northBearing = 0;
    private static float toBearing = 0;
    private static float fromBearing = 0;
    private static float toDistance = 0;
    private static float fromDistance = 0;
    private static Location toLocation;
    private static Location fromLocation;
    // LocationActivity locationActivity;

    private static boolean showCompass = false;
    private static boolean showTodirection = false;
    private static boolean showFromdirection = false;
    private static boolean showTimeDirection = false;

    private final PreferenceListener preferenceListener = new PreferenceListener();

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private void baroCalibrate(int caliAltitude) {
        float altitude = caliAltitude + 0.5F;
        if (SensorManager.getAltitude(sealevelPressure, pressure) < altitude) {
            while (SensorManager.getAltitude(sealevelPressure, pressure) < altitude) {
                sealevelPressure += 0.01;
            }
        } else {
            while (SensorManager.getAltitude(sealevelPressure, pressure) > altitude) {
                sealevelPressure -= 0.01;
            }
        }
    }

    private Location createLocation(String preferenceLocationString) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        // fromLocation = new Location(LocationManager.GPS_PROVIDER);
        String[] cooordinates = preferenceLocationString.split(",",3);
        // System.out.println("huh2: " + cooordinates[1] + " " + cooordinates[2]);
        location.setLatitude(Location.convert(cooordinates[1]));
        location.setLongitude(Location.convert(cooordinates[2]));

        // thuis
        //fromLocation.setLatitude(Location.convert("52.24321697210127"));
        //fromLocation.setLongitude(Location.convert("5.178221881420735"));

        // KPN Toren
        //toLocation.setLatitude(Location.convert("52.24259487638992"));
        //toLocation.setLongitude(Location.convert("5.164555975802909"));

        // Angela 52.21218396773057, 5.293164311690658
        //toLocation.setLatitude(Location.convert("52.21218396773057"));
        //toLocation.setLongitude(Location.convert("5.293164311690658"));

        // Jumbo klein,52.23799891403412,5.176062046858984
        //toLocation.setLatitude(Location.convert("52.23799891403412"));
        //toLocation.setLongitude(Location.convert("5.176062046858984"));

        // Jumbo groot,52.23334058287597,5.188652867731107
        //toLocation.setLatitude(Location.convert("52.23334058287597"));
        //toLocation.setLongitude(Location.convert("5.188652867731107"));
        return location;
    };

    private static class PressureListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            pressure = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    /*
    private static class ConfigActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            System.out.println("SettingActive!");
            super.onCreate(savedInstanceState);
            setContentView(R.layout.settings_activity);
            if (savedInstanceState == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.settings, new nl.mooiweertje.jppg.ConfigActivity.SettingsFragment())
                        .commit();
            }
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
            System.out.println("Key: " + keyEvent.toString());
            return super.onKeyDown(keyCode,keyEvent);
        }

        public static class SettingsFragment extends PreferenceFragmentCompat {
            @Override
            public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
                System.out.println("onCreatePreferences!");
                setPreferencesFromResource(R.xml.root_preferences, rootKey);
            }
        }
    }

     */
    private class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            /*
            System.out.println("huh: " + sharedPreferences.getBoolean("compass", false) + "  " + s);
            System.out.println("huh: " + sharedPreferences.getBoolean("to_direction", false) + "  " + s);
            System.out.println("huh: " + sharedPreferences.getBoolean("from_direction", false) + "  " + s);
            System.out.println("huh: " + sharedPreferences.getString("altitudeD1","5") + "  " + s);
            System.out.println("huh: " + sharedPreferences.getString("altitudeD2","5") + "  " + s);
*/
            // System.out.println("huh: " + sharedPreferences.getString("toLocation","5") + "  " + s);
            // System.out.println("huh: " + sharedPreferences.getString("fromLocation","5") + "  " + s);
            //if("toLocation".equals(s)) {
            fromLocation = createLocation(sharedPreferences.getString("fromLocation","Jannes,52.24321697210127,5.178221881420735"));
            //} else if("fromLocation".equals(s)) {
            toLocation = createLocation(sharedPreferences.getString("toLocation","Mast,52.24259487638992,5.164555975802909"));
            //} else
            showCompass = sharedPreferences.getBoolean("compass", false);
            showFromdirection = sharedPreferences.getBoolean("from_direction", false);
            showTodirection = sharedPreferences.getBoolean("to_direction", false);

            if("calibratenow".equals(s) && sharedPreferences.getBoolean("calibratenow", false)) {
                // System.out.println("yayssssssssss: " );
                // sharedPreferences.getBoolean("calibratenow", false);
                try {
                    Face.this.baroCalibrate(Integer.parseInt(sharedPreferences.getString("calibration_altitude", "17")));
                } catch(NumberFormatException e) {
                    Face.this.baroCalibrate(17);
                }
            }
        }
    }

    private static class MagnetListener implements SensorEventListener {

        private final float[] rotationMatrix = new float[9];
        private final float[] orientationAngles = new float[3];

        private float rotation = 0;
        private float xRot = 0f;
        private boolean vertical = false;

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            float force = Math.round(sensorEvent.values[0]);
            northBearing = force;
            //System.out.println("force: " + force);
            //System.out.println("northBearing: " + northBearing);
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] xRotAngles = new float[9];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);
                SensorManager.getOrientation(rotationMatrix, xRotAngles);

                xRot = (float) Math.round(Math.toDegrees(xRotAngles[1]));

                if (xRot <= -70 && xRot >= -80) {
                    vertical = true;
                } else if (xRot >= 70 && xRot <= 80) {
                    vertical = true;
                } else if (xRot <= -10 && xRot >= -20) {
                    vertical = false;
                } else if (xRot >= 10 && xRot <= 20) {
                    vertical = false;
                }

                if (vertical) {
                    float[] verticalMatrix = new float[9];
                    SensorManager.remapCoordinateSystem(rotationMatrix,
                            SensorManager.AXIS_X,
                            SensorManager.AXIS_Z,
                            verticalMatrix);
                    SensorManager.getOrientation(verticalMatrix, orientationAngles);
                    xRot = (float) Math.toDegrees(orientationAngles[0]);
                } else {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles);
                    xRot = (float) Math.toDegrees(orientationAngles[1]);
                }

                float rot = (float) (Math.toDegrees(orientationAngles[0]) + 360) % 360;
                northBearing = -rot;
                // rotateCompass(rotation, rot);
                //rotation = rot;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
/*
        private void rotateCompass(float lastRotation, float currentRotation){
            RotateAnimation ra = new RotateAnimation(
                    -lastRotation,
                    -currentRotation,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            ra.setRepeatCount(0);
            ra.setFillAfter(true);
            ra.setInterpolator(new LinearInterpolator());
            ra.setDuration(500);

            binding.imgCompassIn.setAnimation(ra);
            binding.txtDegree.setText(Math.round(currentRotation) + "°");
        }
        */
    }

    private class GPSListener implements LocationListener {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            speed = (int) (location.getSpeed() * 3.6f);
            if(showCompass) {
                toBearing = location.bearingTo(toLocation);
                fromBearing = location.bearingTo(fromLocation);
                toDistance = location.distanceTo(toLocation);
                fromDistance = location.distanceTo(fromLocation);
                toLocation.setBearing(northBearing + toBearing);
                fromLocation.setBearing(northBearing + fromBearing);
            }
        }
    }

    /*
    public static class ButtonActivity extends Activity {
        @Override
        public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
            System.out.println("hey 2");
            return false;
        }
    }

    private static class ButtonHandler extends Handler {
    }

    private class ButtonListener implements KeyListener {

        @Override
        public int getInputType() {
            System.out.println("hey 1");
            return 0;
        }

        @Override
        public boolean onKeyDown(View view, Editable editable, int i, KeyEvent keyEvent) {
            System.out.println("hey 2");
            return false;
        }

        @Override
        public boolean onKeyUp(View view, Editable editable, int i, KeyEvent keyEvent) {
            System.out.println("hey 3");
            return false;
        }

        @Override
        public boolean onKeyOther(View view, Editable editable, KeyEvent keyEvent) {
            System.out.println("hey 4");
            return false;
        }

        @Override
        public void clearMetaKeyState(View view, Editable editable, int i) {
            System.out.println("hey 5");
        }
    }
     */

    private static class EngineHandler extends Handler {
        private final WeakReference<Face.Engine> mWeakReference;

        public EngineHandler(Face.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            Face.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_TICK_STROKE_WIDTH = 2f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

        private static final int SHADOW_RADIUS = 6;

        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;
        private float mSecondHandLength;
        private float sMinuteHandLength;
        private float sHourHandLength;
        /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
        /*
        private int mWatchHandColor;
        private int mWatchHandHighlightColor;
        private int mWatchHandShadowColor;
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mTickAndCirclePaint;
        */
        private Paint mBackgroundPaint;
        private Paint mQuantityPaint;
        private Paint mAltiPaint;
        private Paint mTinyPaint;
        private Paint mNorthPaint;
        private Paint mToPaint;
        private Paint mFromPaint;
        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        @SuppressLint({"NewApi", "MissingPermission"})
        @Override
        public void onCreate(SurfaceHolder holder) {

            super.onCreate(holder);

            // Face.setLocations();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(Face.this);
            preferenceListener.onSharedPreferenceChanged(preferences, "");
            preferences.registerOnSharedPreferenceChangeListener(preferenceListener);


            //Intent intent=new Intent(Face.this, ButtonActivity.class);
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // startActivity(intent);

            //ntext context =Context.
            //int b = WearableButtons.getButtonCount(Face.this);
            //System.out.println("butts: " + b);

            /*
            WearableButtons.ButtonInfo buttonInfo =
                    WearableButtons.getButtonInfo(Face.this, KeyEvent.KEYCODE_STEM_1);
            System.out.println("1: " + buttonInfo.getKeycode());
            buttonInfo = WearableButtons.getButtonInfo(Face.this, KeyEvent.KEYCODE_STEM_2);
            System.out.println("2: " + buttonInfo.getKeycode());
            //buttonInfo = WearableButtons.getButtonInfo(Face.this, KeyEvent.KEYCODE_STEM_3);
            //System.out.println("3: " + buttonInfo.getKeycode());
            CharSequence c = WearableButtons.getButtonLabel (Face.this,KeyEvent.KEYCODE_STEM_PRIMARY);
            System.out.println("labelP: " + c);
            c = WearableButtons.getButtonLabel (Face.this,KeyEvent.KEYCODE_STEM_1);
            System.out.println("label1: " + c);
            c = WearableButtons.getButtonLabel (Face.this,KeyEvent.KEYCODE_STEM_2);
            System.out.println("label2: " + c);

 */

            //Context context = (Context) getSystemService(Context.ACTIVITY_SERVICE);
            //WearableButtons.getButtonCount(inputManager);

            setWatchFaceStyle(new WatchFaceStyle.Builder(Face.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();

            SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            PressureListener pressureListener = new PressureListener();
            sensorManager.registerListener(pressureListener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);

            // magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            Sensor magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            MagnetListener magnetListener = new MagnetListener();
            sensorManager.registerListener(magnetListener, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);
            // KeyEvent.Callback

            // KeyguardManager k = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            // k.
            /*
            inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
            ButtonListener buttonListener = new ButtonListener() ;
            ButtonHandler buttonHandler = new ButtonHandler();
            inputManager.registerInputDeviceListener(buttonListener, buttonHandler);
            // locationActivity=new LocationActivity();
            */

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // LocationProvider lp = locationManager.getBestProvider(false);
            /*
            List<String> lps = locationManager.getAllProviders();
            for (String s : lps) {
                System.out.println(s);
            }
            Criteria c = new Criteria();
            c.setSpeedRequired(true);
            System.out.println(locationManager.getBestProvider(c,false));
            System.out.println(locationManager.getGnssYearOfHardware());
            System.out.println(locationManager.isLocationEnabled());
            System.out.println(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
            System.out.println("sats: " + locationManager.getProvider(LocationManager.GPS_PROVIDER).requiresSatellite());
            System.out.println(locationManager.getProvider(LocationManager.GPS_PROVIDER).requiresNetwork());
            System.out.println(locationManager.getProvider(LocationManager.GPS_PROVIDER).supportsSpeed());
*/
            try {
                // ActivityCompat.requestPermissions(locationActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1234);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new GPSListener());
                speed=-1;
            }catch (SecurityException ex) {
                ex.printStackTrace();
                Toast.makeText(getApplicationContext(), R.string.location_message, Toast.LENGTH_LONG).show();
                speed=-2;
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                speed=-3;
            }
            // ActivityCompat.requestPermissions()

            /*
//            String permissions [] = {Manifest.permission.ACCESS_FINE_LOCATION};
  //          ActivityCompat.requestPermissions(new Activity(), permissions,1);
            if (ActivityCompat.checkSelfPermission(Face.this, Manifest.permission.ACCESS_FINE_LOCATION)  == PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new SpeedListener());
            } else {
                speed=666;
            }
*/
            initializeBackground();
            initializeWatchFace();
        }

        private void initializeBackground() {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);
            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.face);

            /* Extracts colors from background image to improve watchface style. */
            Palette.from(mBackgroundBitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    if (palette != null) {
                        //mWatchHandHighlightColor = palette.getVibrantColor(Color.RED);
                        //mWatchHandColor = palette.getLightVibrantColor(Color.WHITE);
                        //mWatchHandShadowColor = palette.getDarkMutedColor(Color.BLACK);
                        updateWatchHandStyle();
                    }
                }
            });
        }

        private void initializeWatchFace() {
            /* Set defaults for colors */
            /*
            mWatchHandColor = Color.WHITE;
            mWatchHandHighlightColor = Color.RED;
            mWatchHandShadowColor = Color.BLACK;

            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHandColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchHandHighlightColor);
            mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
*/
            mAltiPaint = new Paint();
            mAltiPaint.setColor(Color.WHITE);
            mAltiPaint.setAntiAlias(true);
            mAltiPaint.setTextSize(FONTSIZE);

            mTinyPaint = new Paint();
            mTinyPaint.setColor(Color.CYAN);
            mTinyPaint.setAntiAlias(true);
            mTinyPaint.setTextSize(50);

            mQuantityPaint = new Paint();
            mQuantityPaint.setColor(Color.GRAY);
            mQuantityPaint.setAntiAlias(true);
            mQuantityPaint.setTextSize(30);

            mNorthPaint = new Paint();
            mNorthPaint.setColor(Color.RED);
            mNorthPaint.setAntiAlias(true);
            mNorthPaint.setTextSize(50);

            mToPaint = new Paint();
            mToPaint.setColor(Color.GREEN);
            mToPaint.setAntiAlias(true);
            mToPaint.setStrokeWidth(10);
            mToPaint.setTextSize(FONTSIZE);

            mFromPaint = new Paint();
            mFromPaint.setColor(ORANGE);
            mFromPaint.setAntiAlias(true);
            mFromPaint.setTextSize(FONTSIZE);

            /*
            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchHandColor);
            mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mTickAndCirclePaint.setAntiAlias(true);
            mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            */

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;

            updateWatchHandStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                /*
                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                mSecondPaint.setColor(Color.WHITE);
                mTickAndCirclePaint.setColor(Color.WHITE);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondPaint.setAntiAlias(false);
                mTickAndCirclePaint.setAntiAlias(false);

                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mSecondPaint.clearShadowLayer();
                mTickAndCirclePaint.clearShadowLayer();
*/
                mAltiPaint.setColor(Color.GRAY);
                mAltiPaint.setAntiAlias(false);
                mTinyPaint.setColor(Color.GRAY);
                mTinyPaint.setAntiAlias(false);

            } else {
                /*
                mHourPaint.setColor(mWatchHandColor);
                mMinutePaint.setColor(mWatchHandColor);
                mSecondPaint.setColor(mWatchHandHighlightColor);
                mTickAndCirclePaint.setColor(mWatchHandColor);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondPaint.setAntiAlias(true);
                mTickAndCirclePaint.setAntiAlias(true);

                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                */
                mAltiPaint.setColor(Color.WHITE);
                mAltiPaint.setAntiAlias(true);
                mTinyPaint.setColor(Color.CYAN);
                mTinyPaint.setAntiAlias(true);
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                //mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                //mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                //mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            mSecondHandLength = (float) (mCenterX * 0.875);
            sMinuteHandLength = (float) (mCenterX * 0.75);
            sHourHandLength = (float) (mCenterX * 0.5);

            /* Scale loaded background image (more efficient) if surface dimensions change. */
            float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * scale),
                    (int) (mBackgroundBitmap.getHeight() * scale), true);

            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don"t want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren"t
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            if (!mBurnInProtection && !mLowBitAmbient) {
                initGrayBackgroundBitmap();
            }
        }

        private void initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(
                    mBackgroundBitmap.getWidth(),
                    mBackgroundBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
            Paint grayPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            grayPaint.setColorFilter(filter);
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    //System.out.println("TAPT");
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // Toast.makeText(getApplicationContext(), R.string.messageF, Toast.LENGTH_SHORT).show();
                    // barofixed=true;
                    break;
                case TAP_TYPE_TAP:
                    if(showTimeDirection) {
                        showTimeDirection=false;
                    } else {
                        showTimeDirection=true;
                    }

                    /*
                    if(!barofixed) {
                        Face.this.baroCalibrate(156);
                        Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.messageF, Toast.LENGTH_SHORT).show();
                    }
                     */
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            mCalendar.setTimeInMillis(System.currentTimeMillis());

            drawBackground(canvas);
            drawWatchFace(canvas);
        }



        private void drawBackground(Canvas canvas) {
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
            }
            canvas.drawText("m", mCenterX + 135, 220, mQuantityPaint);

            canvas.drawText("K", mCenterX + 80, 70, mQuantityPaint); // 150
            canvas.drawText("m", mCenterX + 76, 90, mQuantityPaint);
            canvas.drawText("_", mCenterX + 82, 96, mQuantityPaint);
            canvas.drawText("u", mCenterX + 80, 120, mQuantityPaint);
        }

        private void drawWatchFace(Canvas canvas) {


            //northBearing = 90;
            //toBearing = 275;
            //fromBearing = 350;


            if(showCompass) {
                canvas.save();
                canvas.rotate(fromBearing + northBearing, mCenterX, mCenterY);
                if (showTodirection) {
                    canvas.drawText("^", mCenterX - 15, 75, mFromPaint);
                }
                // canvas.drawCircle(mCenterX, 30, 20, mFromPaint);
                canvas.rotate(toBearing - fromBearing, mCenterX, mCenterY);
                if (showFromdirection) {
                    canvas.drawText("^", mCenterX - 15, 75, mToPaint);
                }
                //canvas.drawCircle(mCenterX, 30, 20, mToPaint);
                canvas.rotate(-toBearing, mCenterX, mCenterY);
                canvas.drawText("N", mCenterX - 15, 40, mNorthPaint);
                // canvas.drawLines( triangle, mToPaint);
                canvas.restore();
            }


            /*
            canvas.save();
            canvas.rotate(northBearing, mCenterX, mCenterY);
            canvas.drawText("N", mCenterX, 30, mNorthPaint);
            canvas.rotate(toBearing, mCenterX, mCenterY);
            canvas.drawCircle(mCenterX, 30, 20, mToPaint);
            canvas.rotate(fromBearing, mCenterX, mCenterY);
            canvas.drawCircle(mCenterX, 30, 20, mFromPaint);
            canvas.restore();
            */

            // System.out.println("To " + toBearing);

            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
            float innerTickRadius = mCenterX - 10;
            float outerTickRadius = mCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint);
            }
            */

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;
             */

            /*
             * Save the canvas state before we can begin to rotate it.
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sMinuteHandLength,
                    mMinutePaint);

             */
            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint);

            }
            canvas.drawCircle(
                    mCenterX,
                    mCenterY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    mTickAndCirclePaint);
             */

            /* Restore the canvas" original orientation. */
            //canvas.restore();
            /*
            if( mAltiPaint.setFontVariationSettings("'wdth' 300")) {
                String debug = mAltiPaint.getFontVariationSettings();
                canvas.drawText(debug.substring(0, 5), 10, 140, mAltiPaint);
            }

            canvas.drawText(pressureSensor.getName().substring(0,5), 10, 200, mAltiPaint);

             */


//            int speedPos = 300-speedString.length()*50;


            // Speed y 200
            String speedString = Integer.toString(speed);
            int speedPos = (int) (mCenterX-speedString.length()*25); // 300
            canvas.drawText(speedString , speedPos, 120, mAltiPaint); // 160

            // Altitude y 300
            String altString = String.valueOf(Float.valueOf (SensorManager.getAltitude(sealevelPressure, pressure)).intValue());
            int altPos = (int) (mCenterX-altString.length()*25); // 300
            canvas.drawText(altString, altPos, 220, mAltiPaint); // 300
            // canvas.drawText(String.valueOf(sealevelPressure).concat("0000000").substring(0,7), 150, 350, mTinyPaint);

            // Distance y200
            // toBearing
            //if(northBearing<0) northBearing+=360;
            /*
            System.out.println("bearN: " + northBearing);
            System.out.println("bearF: " + fromBearing);
            System.out.println("bearT: " + toBearing);
            System.out.println("myBearF: " + fromLocation.getBearing());
            System.out.println("myBearT: " + toLocation.getBearing());
             */

            if(fromLocation.getBearing()>180) fromLocation.setBearing(360-fromLocation.getBearing());
            if(toLocation.getBearing()>180) toLocation.setBearing(360-toLocation.getBearing());
            String distanceString;
            int corX;
            Paint paint;
            if(showTodirection||showFromdirection) {
                float distance;
                if (toLocation.getBearing() < fromLocation.getBearing()) {
                    distance = toDistance;
                    paint = mToPaint;
                } else {
                    distance = fromDistance;
                    paint = mFromPaint;
                }
                if(showTimeDirection) {
                    String timeString;
                    float time = distance/speed/1000F;
                    int hour = (int) time;
                    if(hour < 9) {
                        int minutes = (int) ((time - hour) * 60);
                        timeString = hour + ":" + minutes;
                    } else {
                        timeString = ">9H";
                    }
                    corX = (int) (mCenterX - timeString.length() * 25);
                    canvas.drawText(timeString, corX, 310, paint);

                } else {
                    if (distance > 999F) {
                        distanceString = String.valueOf(Float.valueOf(distance / 1000F).intValue()) + "K";
                    } else {
                        distanceString = String.valueOf(Float.valueOf(distance).intValue());
                    }
                    corX = (int) (mCenterX - distanceString.length() * 25);
                    canvas.drawText(distanceString, corX, 310, paint);
                }


            }  else {
                // Time
                String hour = String.valueOf(mCalendar.get(Calendar.HOUR_OF_DAY));
                corX = 180 - hour.length() * 50;
                canvas.drawText(hour, corX, 310, mAltiPaint);
                canvas.drawText(":", 190, 310, mAltiPaint);
                canvas.drawText(String.valueOf(mCalendar.get(Calendar.MINUTE)), 220, 310, mAltiPaint);
            }
            /*
            if(toDistance<=fromDistance) {
                String distanceString;
                if(toDistance>999F) {
                    distanceString = String.valueOf(Float.valueOf(toDistance/1000F).intValue()) + "K";
                } else {
                    distanceString = String.valueOf(Float.valueOf(toDistance).intValue());
                }
                int disPos = 270-distanceString.length()*50;
                // System.out.println(distanceString);
                canvas.drawText(distanceString, disPos, 310, mToPaint);
            } else {
                String distanceString;
                if(fromDistance>999F) {
                    distanceString = String.valueOf(Float.valueOf(fromDistance/1000F).intValue()) + "K";
                } else {
                    distanceString = String.valueOf(Float.valueOf(fromDistance).intValue());
                }

                int disPos = 270-distanceString.length()*50;
                // System.out.println(distanceString);
                canvas.drawText(distanceString, disPos, 310, mFromPaint);
            }
            String distanceString = String.valueOf(Float.valueOf(toDistance).intValue());
            int disPos = 270-distanceString.length()*50;
            System.out.println(distanceString);
            canvas.drawText(distanceString, disPos, 320, mAltiPaint);

             */

            // battery charge y 370
            BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            canvas.drawText(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + "%", 170, 370, mTinyPaint);

            /*
            canvas.drawText(String.valueOf(pressure).concat("0000000").substring(0,7), 100, 350, mTinyPaint);
            canvas.drawText(String.valueOf(PRESSURE_STANDARD_ATMOSPHERE).concat("0000000").substring(0,7), 220, 350, mTinyPaint);
            canvas.drawText(String.valueOf(northBearing), 150, 390, mTinyPaint);
             */
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren"t visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            Face.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            Face.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}