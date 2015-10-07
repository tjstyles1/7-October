package com.rittie.andy.testing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.graphics.Color;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.LinearLayout;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.notifications.VibrationType;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.BandException;

public class RealViewArousal extends AppCompatActivity {

    private Toolbar toolbar;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private User u;
    private TextView baseline;
    private TextView tvHeartRate;
    private TextView tvSmoothedHR;
    private TextView tvBaseline;
    private TextView tvStatus;
    private TextView tvLevel;
    private Button btnConnect;
    private int level;
    private boolean isConnect = false;
    private BandClient client = null;
    private double[] hr;
    private double[] smoothed;
    private int outerColor = Color.rgb(0,0,0);
    private int innerColor = Color.rgb(0,0,0);
    private int midColor = Color.rgb(0,0,0);




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_view_arousal); //Sets the layout to activity_real_view_arousal

        Intent intent = getIntent();

        u = (User) intent.getParcelableExtra("user");

        level=0;
        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        //Initializing NavigationView
        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {


                //Checking if the item is in checked state or not, if not make it in checked state
                if(menuItem.isChecked()) menuItem.setChecked(false);
                else menuItem.setChecked(true);

                //Closing drawer on item click
                drawerLayout.closeDrawers();

                //Check to see which item was being clicked and perform appropriate action
                switch (menuItem.getItemId()){


                    //I've made this button go to the homescreen activity
                    case R.id.home:
                        Intent in = new Intent(RealViewArousal.this, HomeScreen.class);
                        in.putExtra("user", u);
                        startActivity(in);
                        return true;

                    // When "View HR/Arousal" is selected from the nav drawer, Start the "RealViewArousal" class

                    case R.id.viewArousal:
                        Intent in2 = new Intent(RealViewArousal.this, RealViewArousal.class);
                        in2.putExtra("user", u);
                        startActivity(in2);
                        return true;

                    // When "Calculate Resting HR" is selected from the nav drawer, Start the "RealCalculateResting" class

                    case R.id.calculateResting:
                        Intent in3 = new Intent(RealViewArousal.this, RealCalculateResting.class);
                        in3.putExtra("user", u);
                        startActivity(in3);
                        return true;


                    default:
                        Toast.makeText(getApplicationContext(),"Somethings Wrong",Toast.LENGTH_SHORT).show();
                        return true;

                }
            }
        });



        // Initializing Drawer Layout and ActionBarToggle
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.openDrawer, R.string.closeDrawer){

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank

                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessary or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();

        smoothed = new double[] {0,0,0,0,0};

        tvStatus = (TextView) findViewById(R.id.xtvStatus);
        btnConnect = (Button) findViewById(R.id.xbtnStart);
        tvBaseline = (TextView) findViewById(R.id.xtvBaseline);
        tvBaseline.setText(String.valueOf(u.getAvgHR()));
        tvHeartRate = (TextView) findViewById(R.id.xtvActual);
        tvSmoothedHR = (TextView) findViewById(R.id.xtvSmoothed);
        tvLevel = (TextView) findViewById(R.id.xtvLevel);
        btnConnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //TODO Auto-generated method stub
                if (isConnect == false) {
                    new ListenerTask().execute();
                    btnConnect.setText("Exit");
                } else {
                    finish();
                }
            }
        });
    }

    private HeartRateConsentListener heartRateConsentListener = new HeartRateConsentListener() {
        @Override
        public void userAccepted(boolean b) {
        }
    };

    private BandHeartRateEventListener heartRateListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null)
                appendToUI(Integer.toString(event.getHeartRate()), 2);
            else finish();

        }
    };

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.", 1);
                tvStatus.setTextColor(Color.parseColor("#d04545"));
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        } else if(ConnectionState.UNBOUND == client.getConnectionState())
            return false;

        appendToUI("Band is connecting...", 1);
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private void unRegisterListeners(){
        try {
            client.getSensorManager().unregisterAllListeners();
        } catch (BandIOException e) {
            appendToUI(e.getMessage(), 1);
        }
    }

    private void appendToUI(final String string, final int code) {
        // code : 1 = status, 2 = hr, 3 = step, 4 = distance, 5 = speed, 6 = temperature


        this.runOnUiThread(new Runnable() {

            private void Circ() {
                //width and height
                // DisplayMetrics metrics = new DisplayMetrics();
                // getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                // int width = metrics.widthPixels;
                // int height = metrics.heightPixels;

                int width1 = 240;// (width/2);
                int height1 = 600;// ((height/3)*5);
                int radius = 100;
                //outer colour
                Paint Opaint = new Paint();
                Opaint.setColor(outerColor);
                Bitmap circle = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(circle);
                canvas.drawCircle(width1, height1, radius, Opaint);
                //inner colour
                Paint Ipaint = new Paint();
                Ipaint.setColor(innerColor);
                canvas.drawCircle(width1, height1, (radius / 3) * 2, Ipaint);
                //midpoint colour
                Paint Mpaint = new Paint();
                Mpaint.setColor(midColor);
                canvas.drawCircle(width1, height1, radius / 3, Mpaint);
                //black outline
                Paint paint2 = new Paint();
                paint2.setColor(Color.BLACK);
                paint2.setStyle(Paint.Style.STROKE);
                paint2.setStrokeWidth(7);
                canvas.drawCircle(width1, height1, radius, paint2);
                LinearLayout ll = (LinearLayout) findViewById(R.id.circle);
                ll.setBackgroundDrawable(new BitmapDrawable(circle));
            }


            double arousalLevelStep = 0.01875;
            double currentAverage;
            double averageHeartRate = u.getAvgHR();
            double currentSum = 0;

            @Override
            public void run() {
                if(code == 1){
                    tvStatus.setText(string);
                }
                else if(code == 2) {

                    averageHeartRate = u.getAvgHR();

                    for (int x = 0; x < smoothed.length - 1; x++) {
                        smoothed[x] = smoothed[x + 1];
                    }
                    smoothed[smoothed.length - 1] = Double.parseDouble(string);
                    currentSum = 0;
                    for (int x = 0; x < smoothed.length; x++) {
                        currentSum = currentSum + smoothed[x];
                    }
                    currentAverage = currentSum / smoothed.length;

                    if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 9)) { //Level 10
//                        outerColor =Color.rgb(255,0,0);
//                        innerColor =Color.rgb(255,0,0);
//                        midColor =Color.rgb(255,0,0);
//                        Circ();
                        tvLevel.setBackgroundColor(Color.rgb(255,0,0));
                        tvLevel.setText("LEVEL: 10");
                        if (level < 10) {
                            try {
                                client.getNotificationManager().vibrate(VibrationType.TWO_TONE_HIGH).await();
                            } catch (InterruptedException e) {
                                //handle it
                            } catch (BandException e){
                                //handle it
                            }
                        }
                        level = 10;
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 8)) { //Level 9
                          outerColor =Color.rgb(255,102,0);
                          innerColor =Color.rgb(255,51,0);
                          midColor =Color.rgb(255,0,0);
                          Circ();
                        tvLevel.setBackgroundColor(Color.rgb(255,102,0));
                        tvLevel.setText("LEVEL: 9");
                        if (level > 9) {
                            try {
                                client.getNotificationManager().vibrate(VibrationType.ONE_TONE_HIGH).await();
                            } catch (InterruptedException e) {
                                //handle it
                            } catch (BandException e){
                                //handle it
                            }
                        }
                        level = 9;
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 7)) { //Level 8
                          outerColor =Color.rgb(255,153,0);
                          innerColor =Color.rgb(255,102,0);
                          midColor =Color.rgb(255,51,0);
                          Circ();
                        tvLevel.setBackgroundColor(Color.rgb(255,153,0));
                        tvLevel.setText("LEVEL: 8");
                        if (level < 8) {
                            try {
                                client.getNotificationManager().vibrate(VibrationType.ONE_TONE_HIGH).await();
                            } catch (InterruptedException e) {
                                //handle it
                            } catch (BandException e){
                                //handle it
                            }
                        }
                        level = 8;

                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 6)) { //Level 7
                          outerColor =Color.rgb(255,255,0);
                          innerColor =Color.rgb(255,204,0);
                          midColor =Color.rgb(255,153,0);
                          Circ();
                        tvLevel.setBackgroundColor(Color.rgb(255,255,0));
                        tvLevel.setText("LEVEL: 7");
                        level = 7;
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 5)) { //Level 6
                          outerColor =Color.rgb(204,255,0);
                          innerColor =Color.rgb(255,255,0);
                          midColor =Color.rgb(255,204,0);
                          Circ();
                        tvLevel.setBackgroundColor(Color.rgb(204,255,0));
                        tvLevel.setText("LEVEL: 6");
                        if (level > 6) {
                            try {
                                client.getNotificationManager().vibrate(VibrationType.RAMP_DOWN).await();
                            } catch (InterruptedException e) {
                                //handle it
                            } catch (BandException e){
                                //handle it
                            }
                        } else if (level < 6) {
                            try {
                                client.getNotificationManager().vibrate(VibrationType.ONE_TONE_HIGH).await();
                            } catch (InterruptedException e) {
                                //handle it
                            } catch (BandException e) {
                                //handle it
                            }
                        }
                        level = 6;
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 4)) { //Level 5
                          outerColor =Color.rgb(153,255,0);
                          innerColor =Color.rgb(204,255,0);
                          midColor =Color.rgb(255,255,0);
                          Circ();
                        tvLevel.setBackgroundColor(Color.rgb(153,255,0));
                        tvLevel.setText("LEVEL: 5");
                        level = 5;
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 3)) { //Level 4
                          outerColor =Color.rgb(102,255,0);
                          innerColor =Color.rgb(153,255,0);
                          midColor =Color.rgb(204,255,0);
                          Circ();
                        tvLevel.setBackgroundColor(Color.rgb(102,255,0));
                        tvLevel.setText("LEVEL: 4");
                        if (level > 4) {
                            try {
                                client.getNotificationManager().vibrate(VibrationType.RAMP_DOWN).await();
                            } catch (InterruptedException e) {
                                //handle it
                            } catch (BandException e){
                                //handle it
                            }
                        } else if (level < 4) {
                            try {
                                client.getNotificationManager().vibrate(VibrationType.NOTIFICATION_ONE_TONE).await();
                            } catch (InterruptedException e) {
                                //handle it
                            } catch (BandException e) {
                                //handle it
                            }
                        }
                        level = 4;
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 2)) { //Level 3
                          outerColor =Color.rgb(51,255,0);
                          innerColor =Color.rgb(102,255,0);
                          midColor =Color.rgb(153,255,0);
                          Circ();
                        tvLevel.setBackgroundColor(Color.rgb(51,255,0));
                        tvLevel.setText("LEVEL: 3");
                        level = 3;
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep)) { //Level 2
                          outerColor =Color.rgb(0,255,0);
                          innerColor =Color.rgb(51,255,0);
                          midColor =Color.rgb(102,255,0);
                          Circ();
                        tvLevel.setBackgroundColor(Color.rgb(0,255,0));
                        tvLevel.setText("LEVEL: 2");
                        if (level > 2) {
                            try {
                                client.getNotificationManager().vibrate(VibrationType.RAMP_DOWN).await();
                            } catch (InterruptedException e) {
                                //handle it
                            } catch (BandException e) {
                                //handle it
                            }
                        } else if (level < 2) {
                            try {
                                client.getNotificationManager().vibrate(VibrationType.NOTIFICATION_ONE_TONE).await();
                            } catch (InterruptedException e) {
                                //handle it
                            } catch (BandException e) {
                                //handle it
                            }
                        }
                        level = 2;
                    } else if (currentAverage > averageHeartRate) { //Level 1
                          outerColor =Color.rgb(0,255,0);
                          innerColor =Color.rgb(0,255,0);
                          midColor =Color.rgb(51,255,0);
                          Circ();
                        tvLevel.setBackgroundColor(Color.rgb(0,255,0));
                        tvLevel.setText("LEVEL: 1");
                        level = 1;
                    } else { //Level 0
                          outerColor =Color.rgb(0,255,0);
                          innerColor =Color.rgb(0,255,0);
                          midColor =Color.rgb(0,255,0);
                          Circ();
                        tvLevel.setBackgroundColor(Color.rgb(0,255,0));
                        tvLevel.setText("LEVEL: 0");
                        if (level > 0) {
                            try {
                                client.getNotificationManager().vibrate(VibrationType.NOTIFICATION_ONE_TONE).await();
                            } catch (InterruptedException e) {
                                //handle it
                            } catch (BandException e){
                                //handle it
                            }
                        }
                        level = 0;
                    }

                    tvHeartRate.setText(string+" BPM");
                    tvSmoothedHR.setText(String.valueOf(currentAverage)+" BPM");
                }
            }

        });
    }

    // execute thread di asynctask

    private class ListenerTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.", 1);
                    isConnect = true;
                    if(client.getSensorManager().getCurrentHeartRateConsent() !=
                            UserConsent.GRANTED) {
                        client.getSensorManager().requestHeartRateConsent(RealViewArousal.this, heartRateConsentListener);
                    }

                    client.getSensorManager().registerHeartRateEventListener(heartRateListener);
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.", 1);
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                        break;
                    default:
                        exceptionMessage = e.getMessage();
                        break;
                }
                appendToUI(e.getMessage() + "\nAccept permision of Microsoft Health Service, then restart counting", 1);

            } catch (Exception e) {
                appendToUI(e.getMessage(), 1);
            }

            return null;
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unRegisterListeners();
    }

    @Override
    public void onBackPressed(){
        Intent in = new Intent(this, HomeScreen.class);
        in.putExtra("user", u);
        startActivity(in);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
