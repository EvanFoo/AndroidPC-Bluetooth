/* 
This code is the simplest android code that will connect to 
an external bluetooth server without requiring that the external
server's address is hard coded. The major requirement for this work 
is that the Android phone is paired to the device that you want to 
connect to AND, that none of the other devices the phone is paired to 
are on or within range. This code will pair to the first device that 
it detects and is paired to.

*/


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
				
				//pull a bluetooth device out of the info given to this class 
				//by the device discovery method
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				//all this does is create a message that a device was found
				Context mContext = getApplicationContext();
				CharSequence text = "Found " + device.getName() + " " + device.getAddress();
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
				
				//try to connect only if the found bluetooth Device is paired
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
		
		//Here we are registering the mReciever (written at the top of the code)
		//so that when a Bluetooth Device is found the mReceiver's onReceive function
		//will run.
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
		
		//all this does is create an object for the physical receiver on the phone
		BluetoothAdapter BtAdapter = BluetoothAdapter.getDefaultAdapter();
		
		//run a new discoveryThread, find the discoveryThread class
		//to see what this does
		discoveryThread dt = new discoveryThread(BtAdapter);
		dt.start();
		
		//create the button for showing gps
		//this is irrelevant for the bluetooth functionality
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
	
	//this function is standard
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	//this function also standard
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

//this class runs bluetooth device discovery
//this is a lengthy process, which is why it needs to be run 
//in a thread.
//If this find a bluetooth devie, then the mReceivers class will 
//be created to handle the newly found bluetooth device.
class discoveryThread extends Thread{
	private BluetoothAdapter bt;
	
	//you need to pass in a bluetooth adapter for this class
	discoveryThread(BluetoothAdapter bta){
		bt = bta;
	}
	
	//standard, you can ignore this
	public Runnable getSocket() {
		// TODO Auto-generated method stub
		return null;
	}
	
	//this is what happens on when the thread is started
	public void run(){
		
		//this is the bluetooth device discovery function
		//it is built into android, and is part of the BluetoothAdapter
		//class
		bt.startDiscovery();
	}
}

//this thread connect the phone to a known bluetooth device
//once the connection is made, it triggers connectedThread which manages the 
//connection
class connectThread extends Thread{
	private BluetoothDevice serverDevice;
	public BluetoothSocket serverSocket;
	private OutputStream out;
	private InputStream in;
	private Context context;
	private String outText = "Victory";
	private byte[] inText = " ".getBytes();
	
	connectThread(BluetoothDevice device, Context inContext){
		serverDevice = device;
		context = inContext;
	}
	public void run(){
		try {
			
			//in this case, the serverSocket represents the end of the stream 
			//on the server side, ie the side of the remote device
			//you create this socket using the serverDevice, which contains
			//information like the MAC address of the server, and a UUID, which 
			//represents a service on the server.
			//The UUID on the server MUST match the UUID on the client.
			//In the case of the arduino, you never specify the UUID in code
			//but it is the default value used here.
			serverSocket = serverDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
			
			//this is the meat of the thread, as it creates the actual connection
			serverSocket.connect();
			
			//also create input and output stream
			//NOTE: never use these, not sure why I put them here
			out = serverSocket.getOutputStream();
			in = serverSocket.getInputStream();
			
			//now, start the connected thread, passing in the serverSocket, 
			//and a context
			//the context is purely an Android idea, that will allow us to 
			//use GPS
			//The context is not really relevant to the pure bluetooth functionality
			ConnectedThread ct = new ConnectedThread(serverSocket, context);
			ct.start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

//connectedThread manages the connection
class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Context inContext;
    GPSTracker gps;
 
   //need to take in a BluetoothSocket, again, ignore the context
    public ConnectedThread(BluetoothSocket socket, Context inputContext) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        
        inContext = inputContext;
        
        //GPS tracker so the app can send back gps coordinates
        //ignore this
        gps = new GPSTracker(inContext);
 
        // Get the input and output streams, using temp objects because
        // member streams are final
        //this streams are what will allow us to send and receive
        //think of these like iostream or fstream in C++
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
        while (true) {                            //use this code to
        										  //connect to the 
            try {								  //python bluetooth server
                // Read from the InputStream	  //		|
                bytes = mmInStream.read(buffer);  //		|
            } catch (IOException e) {             //		|
                break;                            //		|
            }                                     //		|
                                                  // 		|
			//if you receive some from the server,			|
            //send the string "Received" back				|	
            if(buffer != null){                   //		|	
            	write("Received".getBytes());     //		|
            }									  //		|
        }										  //		^
        
        ////uncomment the following code to write a one to the arduino 
        ////this will turn on the light if the arduino is running the 
        ////code that is in the repo
        //write("1".getBytes());
        
        
        
    }
 
    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
        	//this writes an array of bytes to the output stream
        	//in other words, it sends these bytes to the remote
        	//bluetooth device
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


