package com.example.bt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.support.v7.app.ActionBarActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
	
	Button btnShowLocation;
	
	GPSTracker gps;

	//this is what occurs after a bt connection is made
	public final BroadcastReceiver mReceiver = new BroadcastReceiver(){
		Set<BluetoothDevice> btDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
		
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			
			if(BluetoothDevice.ACTION_FOUND.equals(action)){
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				Context mContext = getApplicationContext();
				CharSequence text = "Found " + device.getName() + " " + device.getAddress();
				int duration = Toast.LENGTH_SHORT;
				
				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
				
				//also try to connect if it is a bonded device
				if(btDevices.contains(device)){
					connectThread ct = new connectThread(device, mContext);
					ct.start();
				}
			}
		}
	};
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
		

		
		BluetoothAdapter BtAdapter = BluetoothAdapter.getDefaultAdapter();
		
		discoveryThread dt = new discoveryThread(BtAdapter);
		dt.start();
		
		//create the button for showing gps
		btnShowLocation = (Button)findViewById(R.id.button1);
		
		btnShowLocation.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				gps = new GPSTracker(MainActivity.this);
				
				if(gps.canGetLocation()){
					double latitude = gps.getLatitude();
					double longitude = gps.getLongitude();
					
					Toast.makeText(getApplicationContext(), "Location" + latitude + longitude, Toast.LENGTH_LONG).show();
				}else{
					gps.showSettingsAlert();
				}
				
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}

class discoveryThread extends Thread{
	private BluetoothAdapter bt;
	
	discoveryThread(BluetoothAdapter bta){
		bt = bta;
	}
	public Runnable getSocket() {
		// TODO Auto-generated method stub
		return null;
	}
	public void run(){
		bt.startDiscovery();
	}
}

class connectThread extends Thread{
	private BluetoothDevice internalDevice;
	public BluetoothSocket internalSocket;
	private OutputStream out;
	private InputStream in;
	private Context context;
	private String outText = "Victory";
	private byte[] inText = " ".getBytes();
	
	connectThread(BluetoothDevice device, Context inContext){
		internalDevice = device;
		context = inContext;
	}
	public void run(){
		try {
			internalSocket = internalDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
			internalSocket.connect();
			
			out = internalSocket.getOutputStream();
			in = internalSocket.getInputStream();
			ConnectedThread ct = new ConnectedThread(internalSocket, context);
			
			ct.start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Context inContext;
    GPSTracker gps;
 
    //the last argument is a string containing data that will be parsed by the server
    public ConnectedThread(BluetoothSocket socket, Context inputContext) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        
        inContext = inputContext;
        
        //GPS tracker so the app can send back gps coordinates
        gps = new GPSTracker(inContext);
 
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }
 
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }
 
    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        char[] data = new char[1024];
        String dataString;
        int bytes; // bytes returned from read()
 
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
            } catch (IOException e) {
                break;
            }
            
            if(buffer != null){
            
            }
        }
    }
 
    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }
 
    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}


