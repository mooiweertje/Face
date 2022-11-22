package nl.mooiweertje.jppg;

import static android.hardware.SensorManager.PRESSURE_STANDARD_ATMOSPHERE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.hardware.input.InputManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.palette.graphics.Palette;

import android.support.wearable.input.WearableButtons;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.Editable;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.List;
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
    public  static final float PRESSURE_STANDARD_ATMOSPHERE_AMSTERDAM = 1015F;
    public  static final float PRESSURE_STANDARD_ATMOSPHERE_QNH = 1013.25F;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private InputManager inputManager;
    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private Sensor magnetSensor;
    private PressureListener pressureListener;
    private MagnetListener magnetListener;
    private static int speed = 100;
    private static float pressure = 0;
    private static float northBearing = 0;
    // LocationActivity locationActivity;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class PressureListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            pressure = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

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
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
                float[] xRotAngles = new float[9];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);
                SensorManager.getOrientation(rotationMatrix, xRotAngles);

                xRot = (float) Math.round(Math.toDegrees(xRotAngles[1]));

                if (xRot <= -70 && xRot>= -80){
                    vertical = true;
                }else if (xRot >= 70 && xRot <= 80){
                    vertical = true;
                }else if (xRot <= -10 && xRot >= -20){
                    vertical =false;
                } else if (xRot >= 10 && xRot <= 20) {
                    vertical = false;
                }

                if (vertical){
                    float[] verticalMatrix = new float[9];
                    SensorManager.remapCoordinateSystem(rotationMatrix,
                            SensorManager.AXIS_X,
                            SensorManager.AXIS_Z,
                            verticalMatrix);
                    SensorManager.getOrientation(verticalMatrix, orientationAngles);
                    xRot = (float) Math.toDegrees(orientationAngles[0]);
                }else {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles);
                    xRot = (float) Math.toDegrees(orientationAngles[1]);
                }

                float rot = (float) (Math.toDegrees(orientationAngles[0])+360)%360;
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

    private class SpeedListener implements LocationListener {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            speed = (int) (location.getSpeed()*3.6f);
        }
    }

    private static class ButtonActivity extends Activity {
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
        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        @SuppressLint("NewApi")
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);




            //ntext context =Context.
            int b = WearableButtons.getButtonCount(Face.this);
            System.out.println("butts: " + b);

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

            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            pressureListener = new PressureListener();
            sensorManager.registerListener(pressureListener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);

            // magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            magnetListener = new MagnetListener();
            sensorManager.registerListener(magnetListener, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);

            /*
            inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
            ButtonListener buttonListener = new ButtonListener() ;
            ButtonHandler buttonHandler = new ButtonHandler();
            inputManager.registerInputDeviceListener(buttonListener, buttonHandler);
            // locationActivity=new LocationActivity();
            */

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // LocationProvider lp = locationManager.getBestProvider(false);
            List<String> lps =locationManager.getAllProviders();
            for (String s :lps) {
                System.out.println(s);
            }
/*
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
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new SpeedListener());
                speed=1;
            }catch (SecurityException ex) {
                ex.printStackTrace();
                speed=111;
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                speed=222;
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
            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.watchface_service_bg);

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
            mAltiPaint.setColor(Color.YELLOW);
            mAltiPaint.setAntiAlias(true);
            mAltiPaint.setTextSize(FONTSIZE);

            mTinyPaint = new Paint();
            mTinyPaint.setColor(Color.CYAN);
            mTinyPaint.setAntiAlias(true);
            mTinyPaint.setTextSize(30);

            mQuantityPaint = new Paint();
            mQuantityPaint.setColor(Color.GRAY);
            mQuantityPaint.setAntiAlias(true);
            mQuantityPaint.setTextSize(30);

            mNorthPaint = new Paint();
            mNorthPaint.setColor(Color.RED);
            mNorthPaint.setAntiAlias(true);
            mNorthPaint.setTextSize(50);
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
                mAltiPaint.setColor(Color.WHITE);
                mAltiPaint.setAntiAlias(false);
                mTinyPaint.setColor(Color.WHITE);
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
                mAltiPaint.setColor(Color.YELLOW);
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
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

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
        }

        private void drawWatchFace(Canvas canvas) {


            canvas.save();
            canvas.rotate(northBearing, mCenterX, mCenterY);
            canvas.drawText("N", mCenterX, 30, mNorthPaint);
            canvas.restore();

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

            // MeasuredText speedT = new MeasuredText.Builder((Integer.toString(speed) + "").toCharArray()).build();
            canvas.drawText(String.valueOf(mCalendar.get(Calendar.HOUR_OF_DAY)), 100, 100, mTinyPaint);
            canvas.drawText(String.valueOf(mCalendar.get(Calendar.MINUTE)), 300, 100, mTinyPaint);

            String speedString = Integer.toString(speed);
            int speedPos = 250-speedString.length()*50;
            canvas.drawText(speedString , speedPos, 200, mAltiPaint);
            canvas.drawText("km/u", 270, 200, mQuantityPaint);
            String altString = String.valueOf(Float.valueOf (SensorManager.getAltitude(PRESSURE_STANDARD_ATMOSPHERE , pressure)).intValue());
            int altPos = 250-altString.length()*50;
            canvas.drawText(altString, altPos, 300, mAltiPaint);
            canvas.drawText("m", 270, 300, mQuantityPaint);
            canvas.drawText(String.valueOf(pressure).concat("0000000").substring(0,7), 100, 350, mTinyPaint);
            canvas.drawText(String.valueOf(PRESSURE_STANDARD_ATMOSPHERE).concat("0000000").substring(0,7), 220, 350, mTinyPaint);
            canvas.drawText(String.valueOf(northBearing), 150, 390, mTinyPaint);
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