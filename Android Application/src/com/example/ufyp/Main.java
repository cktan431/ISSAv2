package com.example.ufyp;

  
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ck.Weatherboard;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.maps.GeoPoint;

//public class Main extends android.support.v4.app.FragmentActivity implements OnItemSelectedListener,LocationListener{
public class Main extends android.support.v4.app.FragmentActivity implements OnItemSelectedListener{	

	Spinner spinner;
	//MapView map;
	GoogleMap mMap;
	LocationManager locationManager;
	public initialcenter onStartCenter;
	public OnLocationChangedListener mListener;
	
    //declare time variables
	int hour,minute,second;
	long milisecond;
	
	//flag for recording
	int startRecord, recording, stopRecord=0;
	
	//variable used in locationlistener
	int postalcode;
	String display;
	
	//hijack ini
	Weatherboard weatherData;
	
	//write to file
    public File root = Environment.getExternalStorageDirectory();
    public File file = new File(root, "test.txt");
    
    TextView tv1,tv2,tv3;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
		tv1 = (TextView) findViewById(R.id.textView1);
		tv2 = (TextView) findViewById(R.id.textView2);
		tv3 = (TextView) findViewById(R.id.textView3);
		//tv1.setVisibility(View.GONE);
		//tv2.setVisibility(View.GONE);
		//tv3.setVisibility(View.GONE);
        
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        onStartCenter = new initialcenter();
        
        //code for data connection
        StrictMode.ThreadPolicy policy = new StrictMode.
		ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		
		//element registering
		spinner =(Spinner)findViewById(R.id.sAction);
		//map = (MapView)findViewById(R.id.mvMain);
		
		//initialize the spinner
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.choice_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
        spinner.setAdapter(adapter); //configure settings of spinner
        spinner.setOnItemSelectedListener(this);
        
        //initialize the MapView
        //map.setBuiltInZoomControls(true); 
        
        weatherData = new Weatherboard();
        weatherData.ckapp = false;
        
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(weatherData, "hijackwb");
        ft.commit();
 
        //initialize the locationManager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mMap.setMyLocationEnabled(true);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
	//start or end locationlistener
  	public void startUpdate(){  
  		weatherData.starthijack();
        Criteria locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, onStartCenter);
  		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, onStartCenter);
      	//locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, onStartCenter);
  		mMap.setLocationSource(onStartCenter);
      }
  	
  	public void stopUpdate(){
  		weatherData.stophijack();
  	    locationManager.removeUpdates(onStartCenter);
      }
    
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
		if(pos==1){ //to record 
			spinner.setSelection(0);
			startUpdate(); 
			startRecord = 1;
			stopRecord = 0;
			recording = 0;
			
		}
		if(pos==2){
			spinner.setSelection(0);
			stopUpdate();
			recording = 0;
			startRecord = 0;
			stopRecord = 1;
			Thread stopInform = new Thread(){
				public void run(){
					JSONObject _json = new JSONObject(); 
					try {
						_json.put("ck",0);
				    	_json.put("record","record");
						_json.put("stopRecord", stopRecord);
						DataPost post = new DataPost(); 
						//post the data to server 
						post.postData(_json);
						showToast(post.stringGet, 50, "aaa","bbb","ccc");
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			};
			stopInform.start();
		} 
		if(pos==3){
			
		} 
		if(pos==4){
			spinner.setSelection(0); 
			Intent intent = new Intent(Main.this,com.example.ck.MainActivity.class); //start the new intent
			Log.d("debug", "intent defined");
			startActivity(intent);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {	
	}
	
	public void showToast(final String toast, final int time, final String a, final String b, final String c)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
            	tv1.setText(a);
            	tv2.setText(b);
            	tv3.setText(c);
                final Toast t= Toast.makeText(Main.this, toast, Toast.LENGTH_SHORT);
                t.setDuration(1);
                t.show();
                Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
		           @Override
		           public void run() {
		               t.cancel(); 
		           }
		    }, time);
            }
        });
    }

    private class initialcenter implements LocationSource, LocationListener {

    	@Override
    	public void onLocationChanged(Location location) {
    		//mListener.onLocationChanged( location );
    		//initialize variables
    		Log.v("test", "in listener");
			if( mListener != null )
		    {
		        mListener.onLocationChanged( location );
		        
		        //Move the camera to the user's location once it's available!
		        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()), 13));

		    }
    		
    		final int latitude=(int)(location.getLatitude()*1E6);
            final int longitude=(int)(location.getLongitude()*1E6);
            final GeoPoint gP = new GeoPoint(latitude, longitude);
            final Geocoder geocoder = new Geocoder(getBaseContext(),Locale.getDefault());
            gettime(); 
            
            Thread displayAddr = new Thread(){
            	public void run(){
            		try{
    					List<Address> address = geocoder.getFromLocation(gP.getLatitudeE6()/1E6, gP.getLongitudeE6()/1E6, 1);
    					if(address.size()>0){
    						display ="";
    						for(int i=0; i<address.get(0).getMaxAddressLineIndex();i++){
    							display +=address.get(0).getAddressLine(i);
    						}
    						if(address.get(0).getPostalCode()!=null){
    							String postal = address.get(0).getPostalCode();
    							postalcode = Integer.parseInt(postal); //get the integer of postal code
    						}
    						JSONObject _json = new JSONObject(); 
    					    try { 
    					    	_json.put("ck", 0);
    					    	_json.put("record","record");
    							_json.put("startRecord", startRecord);
    							_json.put("stopRecord", stopRecord);
    							_json.put("recording", recording);
    							_json.put("address",display);
    							_json.put("postalcode",postalcode); 
    							_json.put("hour",hour);
    							_json.put("minute",minute);
    							_json.put("second",second);
    							_json.put("milisecond", ""+milisecond);
    							_json.put("lati",latitude);
    							_json.put("longi",longitude);
    							
    							//appending weather data  

    					  		Log.v("debug","work? 1");
    							List<String> weatherList = weatherData.getweather();
    							_json.put("temperature",weatherList.get(0));
    							Log.v("debug",weatherList.get(0)); 
    							_json.put("humidity",weatherList.get(1));
    							_json.put("brightness",weatherList.get(2));
    					    	
    					    	//List<String> weatherList = weatherData.getweather();
    							
    							write2file(latitude,longitude,weatherList.get(0),weatherList.get(1),weatherList.get(2));
     
    							startRecord = 0 ;
    							recording = 1;
    							stopRecord = 0; 
    							DataPost post = new DataPost();
    							post.postData(_json);
    							showToast(post.stringGet, 50, weatherList.get(0),weatherList.get(1),weatherList.get(2));
    							
    						} catch (JSONException e) {
    							e.printStackTrace();
    						}
    						 
    					}
    				}catch(IOException e){
    					e.printStackTrace();
    				}finally{
    					
    				}
            	}
            };
            displayAddr.start();
    		
    	}

		@Override
		public void onProviderDisabled(String arg0) {
		}

		@Override
		public void onProviderEnabled(String arg0) {	
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {	
		}

		@Override
		public void activate(OnLocationChangedListener listener) {
			mListener = listener;
		}

		@Override
		public void deactivate() {
			//mListener = null;
		}
    	
    }	

	public void gettime(){
	    	Calendar calendar=Calendar.getInstance();
	        Date date=calendar.getTime();
	        second = date.getSeconds();
	        minute = date.getMinutes();
	        hour = date.getHours();
	        milisecond = date.getTime();
	    }

    public void write2file(int lat, int lng, String temp, String hum, String light){
        try { // catches IOException below
        	Log.i("Test", String.valueOf(root.canWrite()));
        	if (root.canWrite()) {
        		FileWriter filewriter = new FileWriter(file, true);
        		BufferedWriter out = new BufferedWriter(filewriter);
        		out.write(String.valueOf(lat) + "\t\t");
        		out.write(String.valueOf(lng) + "\t\t");
        		out.write(temp + "\t\t");
        		out.write(hum + "\t\t");
        		out.write(light + "\r\n");
        		out.close();
        	}
        	
        } catch (IOException e) {
            Log.e("TAG", "Could not write file " + e.getMessage());
        }
    }
	
    @Override
    public void onResume()
    {
        super.onResume();
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
    }
}
