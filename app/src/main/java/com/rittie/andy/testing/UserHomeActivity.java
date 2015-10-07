package com.rittie.andy.testing;
//Andy Rittie
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.view.View;

public class UserHomeActivity extends AppCompatActivity {

    private Band band;
    User u;
    double[] restingHR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.original_activity_user_home);

        Intent intent = getIntent();

        u = (User) intent.getParcelableExtra("user");

        TextView tvName = (TextView) findViewById(R.id.txtName);
        tvName.setText(u.getName());

        TextView tvEmail = (TextView) findViewById(R.id.txtEmail);
        tvEmail.setText(u.getEmail());

        TextView tvAvg = (TextView) findViewById(R.id.txtAvgHR);
        tvAvg.setText(String.format("%.2f",u.getAvgHR()));

        //mHandler = new Handler();


    }

    public void connectBand(View view) {
        band = new Band();
        TextView tvConn = (TextView) findViewById(R.id.txtConn);
        tvConn.setText(band.getTypeBand());
    }

    public void calcHR(View view) {

        Intent in = new Intent(this, RestingHeartRate.class);
        in.putExtra("user", u);
        startActivity(in);

    }

    public void monitorHR(View view) {

        Intent in = new Intent(this,HeartRateMonitor.class);
        in.putExtra("user", u);
        startActivity(in);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user_home, menu);
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
