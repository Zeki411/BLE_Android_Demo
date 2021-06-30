package emgsignal.v3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;


import java.util.ArrayList;

public class ShowActivity extends AppCompatActivity{
    // This tag is used for debug messages
    private static final String TAG = ShowActivity.class.getSimpleName();

    private static Button mCmdDiagBtn;
    private static String mDeviceAddress;
    private static DataBLEService mDataBLEService;

    PieChart pieChart;
    ImageView mImageView;
    TextView mTextViewState;
    TextView mTextViewAccuracy;

    private float[] yData = {25.3f, 10.6f, 66.76f, 44.32f, 46.01f, 16.89f, 23.9f};
    private String[] xData = {"Mitch", "Jessica" , "Mohammad" , "Kelsey", "Sam", "Robert", "Ashley"};
    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object, initialize the service, and connect.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mDataBLEService = ((DataBLEService.LocalBinder) service).getService();
            if (!mDataBLEService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the car database upon successful start-up initialization.
            mDataBLEService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDataBLEService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_show);

        //mCmdDiagBtn = (Button) findViewById(R.id.text_state);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(ScanActivity.EXTRAS_BLE_ADDRESS);

        // Bind to the BLE service
        Log.i(TAG, "Binding Service");
        Intent DataBLEServiceIntent = new Intent(this, DataBLEService.class);
        bindService(DataBLEServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

//        mCmdDiagBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //Clear Old Data
//
//                //Request new Data
//                mDataBLEService.requestNewDiagnose();
//            }
//        });

        mTextViewAccuracy = findViewById(R.id.text_accuracy);
        mImageView = findViewById(R.id.imageView);
        mTextViewState = findViewById(R.id.text_state);
        pieChart = findViewById(R.id.idPieChart);

        ArrayList<PieEntry> psds = new ArrayList<>();
        psds.add(new PieEntry(20, "alpha"));
        psds.add(new PieEntry(20, "beta"));
        psds.add(new PieEntry(20, "theta"));
        psds.add(new PieEntry(20, "gamma"));
        psds.add(new PieEntry(20, "delta"));

        PieDataSet pieDataSet = new PieDataSet(psds, "Power Density Percent");
        pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        pieDataSet.setValueTextColor(Color.BLACK);
        pieDataSet.setValueTextSize(16f);

        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Power Density Percent (%)");
        pieChart.animate();

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mDataUpdateReceiver, makeRobotUpdateIntentFilter());
        if (mDataBLEService != null) {
            final boolean result = mDataBLEService.connect(mDeviceAddress);
            Log.i(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mDataUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mDataBLEService = null;
    }

    /**
     * Handle broadcasts from the EEG Data service object. The events are:
     * ACTION_CONNECTED: connected to the eeg device.
     * ACTION_DISCONNECTED: disconnected from the eeg device.
     * ACTION_DATA_AVAILABLE: received data from the eeg device.  This can be a result of a read
     * or notify operation.
     */
    private final BroadcastReceiver mDataUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case DataBLEService.ACTION_CONNECTED:
                    // No need to do anything here. Service discovery is started by the service.
                    break;
                case DataBLEService.ACTION_DISCONNECTED:
                    mDataBLEService.close();
                    break;
                case DataBLEService.ACTION_DATA_AVAILABLE:
                    // This is called after a Notify completes

                    //Update Accuracy
                    mTextViewAccuracy.setText("Accuracy: " + String.format("%d", Byte.toUnsignedInt(DataBLEService.mEEGDataBuffer[2])) + "%");
                    //Update Image
                    if(DataBLEService.mEEGDataBuffer[1] == 1)
                    {
                        mImageView.setImageResource(R.drawable.satisfied);
                        mTextViewState.setText("Positive State");
                    }
                    else
                    {
                        mImageView.setImageResource(R.drawable.smile_3);
                        mTextViewState.setText("Negative State");
                    }


                    // Update PieChart
                    PieChart pieChart = findViewById(R.id.idPieChart);
                    ArrayList<PieEntry> psds = new ArrayList<>();

                    psds.add(new PieEntry(Byte.toUnsignedInt(DataBLEService.mEEGDataBuffer[3]), "alpha"));
                    psds.add(new PieEntry(Byte.toUnsignedInt(DataBLEService.mEEGDataBuffer[4]), "beta"));
                    psds.add(new PieEntry(Byte.toUnsignedInt(DataBLEService.mEEGDataBuffer[5]), "theta"));
                    psds.add(new PieEntry(Byte.toUnsignedInt(DataBLEService.mEEGDataBuffer[6]), "gamma"));
                    psds.add(new PieEntry(Byte.toUnsignedInt(DataBLEService.mEEGDataBuffer[7]), "delta"));

                    PieDataSet pieDataSet = new PieDataSet(psds, "Power Density Percent");
                    pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                    pieDataSet.setValueTextColor(Color.BLACK);
                    pieDataSet.setValueTextSize(16f);

                    PieData pieData = new PieData(pieDataSet);
                    pieChart.setData(pieData);
                    pieChart.getDescription().setEnabled(false);
                    pieChart.setCenterText("Power Density Percent (%)");
                    pieChart.animate();
                    pieChart.invalidate();



                    break;
            }
        }
    };

    /**
     * This sets up the filter for broadcasts that we want to be notified of.
     * This needs to match the broadcast receiver cases.
     *
     * @return intentFilter
     */
    private static IntentFilter makeRobotUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DataBLEService.ACTION_CONNECTED);
        intentFilter.addAction(DataBLEService.ACTION_DISCONNECTED);
        intentFilter.addAction(DataBLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private static void UpdatePieChart()
    {


    }

}
