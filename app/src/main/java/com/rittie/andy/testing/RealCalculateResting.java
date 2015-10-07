package com.rittie.andy.testing;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

public class RealCalculateResting extends AppCompatActivity {

    private Toolbar toolbar;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private User u;
    private TextView tvHeartRate;
    private TextView tvStatus;
    private Button btnConnect;
    private boolean isConnect = false;
    private BandClient client = null;
    private double[] hr;
    private int i;
    DBAdapter db = new DBAdapter(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_calculate_resting);

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
                        Intent in = new Intent(RealCalculateResting.this, HomeScreen.class);
                        in.putExtra("user", u);
                        startActivity(in);
                        return true;

                    // When "View HR/Arousal" is selected from the nav drawer, Start the "RealViewArousal" class

                    case R.id.viewArousal:
                        Intent in2 = new Intent(RealCalculateResting.this, RealViewArousal.class);
                        in2.putExtra("user", u);
                        startActivity(in2);
                        return true;

                    // When "Calculate Resting HR" is selected from the nav drawer, Start the "RealCalculateResting" class

                    case R.id.calculateResting:
                        Intent in3 = new Intent(RealCalculateResting.this, RealCalculateResting.class);
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

        //calling sync state is necessay or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();

        Intent intent = getIntent();

        u = (User) intent.getParcelableExtra("user");

        tvStatus = (TextView) findViewById(R.id.xtvStatus);
        btnConnect = (Button) findViewById(R.id.xbtnStart);
        tvHeartRate = (TextView) findViewById(R.id.xtvSmoothed);
        hr = new double[30];
        i = 0;
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
            if (event != null) {
                if (i <= hr.length - 1) {
                    appendToUI(Integer.toString(event.getHeartRate()), 2);
                }
                else {
                    db.open();
                    //change restingHR[i] to input from band
                    for (int x=0; x<hr.length; x++) {
                        db.insertHeartRate(String.valueOf(u.getId()), String.valueOf(hr[x]));
                    }
                    db.close();

                    appendToUI(String.valueOf(u.calcAvg(hr)), 3);

                    db.open();
                    boolean b = db.updateUserRecord(u.getId(),u.getName(),u.getEmail(),u.getPassword(),String.valueOf(u.getAvgHR()));
                    db.close();
                    //finish();
                }
            }
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
            @Override
            public void run() {
                if(code == 1){
                    tvStatus.setText(string);
                }
                else if(code == 2) {
                    hr[i] = Double.parseDouble(string);
                    tvHeartRate.setText(string+" BPM");
                    i++;
                }
                else if(code == 3) {
                    tvStatus.setText("Finished, average:");
                    tvHeartRate.setText(string+" BPM");
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
                        client.getSensorManager().requestHeartRateConsent(RealCalculateResting.this, heartRateConsentListener);
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
        getMenuInflater().inflate(R.menu.menu_real_calculate_resting, menu);
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
