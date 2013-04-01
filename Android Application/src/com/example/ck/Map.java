package com.example.ck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.ck.OnSubmitClickListener; 
import com.example.ufyp.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class Map extends Fragment {
	
	private LocationManager locationManager;
	private initialcenter onStartCenter;

	public Context mActivity;
	
	OnSubmitClickListener mCallback;
	private OnLocationChangedListener mListener;
	private OnClickListener startListener, redoListener, doneListener, submitListener;
	
	////////////////////////////////////////////////
	//////////////// UI VARIABLES //////////////////
	////////////////////////////////////////////////
    public TextView _tvIn1;
    public Button donebutton, redobutton;
	public GoogleMap mMap;
	public MapFragment mFrag;
	public GoogleMapOptions options = new GoogleMapOptions();

    ////////////////////////////////////////////////
    ////////////// ROUTING VARIABLES ///////////////
    ////////////////////////////////////////////////
    private int waypointcount=0;
    public List<LatLng> customroute = new ArrayList<LatLng>();
    private List<LatLng> waypointlist = new ArrayList<LatLng>();
	private List<LatLng> instrucpoints = new ArrayList<LatLng>();
	private List<String> instruc = new ArrayList<String>();
	private int[] endingpoints = {648886, 238873, 556083, 529510, 738099};
	//private int[] endingpoints = {119077,119077,119077,119077,119077};
	private Random randomno = new Random();
	private MarkerOptions endpointmarker;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		
		View myFragmentView = inflater.inflate(R.layout.map, container, false);
		
		_tvIn1 = (TextView) myFragmentView.findViewById(R.id.tap_text);
		donebutton = (Button) myFragmentView.findViewById(R.id.button1);
		redobutton = (Button) myFragmentView.findViewById(R.id.button2);
		
		//Set button onclick listeners
		createbuttonlisteners();
						
		donebutton.setEnabled(false);
		redobutton.setOnClickListener(startListener);
		donebutton.setOnClickListener(doneListener);
		
		return myFragmentView;
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mActivity = getActivity();
            
            onStartCenter = new initialcenter();
            
            options.rotateGesturesEnabled(false);
            
        	mFrag = MapFragment.newInstance(options);
    		FragmentTransaction ft = getFragmentManager().beginTransaction();
    		ft.add(R.id.mapbox, mFrag, "maptag");
    		ft.commit();
    		
    		mCallback = (OnSubmitClickListener) mActivity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onButtonPressed");
        }
    }
	
    @Override
    public void onResume()
    {
        super.onResume();
    }
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		/*FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment f = (Fragment) getFragmentManager().findFragmentByTag("map");
		if (f != null) {
			ft.remove(f);
			ft.commit();
		}
        /*if (f != null)
			getFragmentManager().beginTransaction().remove(f).commit();
        else
        	Log.v("test", "f= null");*/
	}
	
	////////////////////////////////////////////////
	/////////////// Helper Classes /////////////////
	////////////////////////////////////////////////
    private class initialcenter implements LocationSource, LocationListener {
    	
    	//Centers map/camera on user's area
		@Override
		public void onLocationChanged(Location location) {
			if( mListener != null )
		    {
		        mListener.onLocationChanged( location );

		        LatLng currentpos = new LatLng(location.getLatitude(), location.getLongitude());
		        LatLng endingpos = Geocoding(String.valueOf (endingpoints [randomno.nextInt(5)] ));
		        
		        //Move the camera to the user's location once it's available!
		        //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentpos, 16));
		        
		        //Move to center of start and end
		        double latitude = (currentpos.latitude + endingpos.latitude)/2;
		        double longitude = (currentpos.longitude + endingpos.longitude)/2;
		        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude), 10));
		        
		        mListener = null;
		        mMap.setLocationSource(null);
		        
				waypointlist.add(currentpos);
				waypointlist.add(endingpos);
				
				String roadname="NOTHING";
				List<Address> addresses = null;
				Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
				try {
					addresses = geocoder.getFromLocation(endingpos.latitude, endingpos.longitude, 1);
				} catch (IOException e) {
		            e.printStackTrace();
				}
				if (addresses != null && addresses.size() > 0) {
		            Address address = addresses.get(0);
		            roadname = address.getAddressLine(0);
		        }
				
				endpointmarker = (new MarkerOptions()
									.title("Destination")
									.snippet(roadname)
									.position( endingpos )
									.draggable(false));
				mMap.addMarker(endpointmarker);
				
		    }
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
			mListener = null;
		}
    	
    }

	////////////////////////////////////////////////
	/////////////// Misc Functions /////////////////
	////////////////////////////////////////////////
    public void createbuttonlisteners() {
    	
		redoListener = new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								_tvIn1.setText("");
								
								mMap.clear();
								mMap.addMarker(endpointmarker);
								registerlongclicklistener();
								
								donebutton.setText("Done");
								donebutton.setOnClickListener(doneListener);
								donebutton.setEnabled(true);
								
								instrucpoints.clear();
								instruc.clear();
								
								//Clear all Waypoints
								while (waypointlist.size() >2)
									waypointlist.remove(1);
								waypointcount = 0;
							}
						};

		startListener = new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								mMap = mFrag.getMap();
								if (mMap != null) {
									_tvIn1.setText("Long Tap to Select Roads");
									donebutton.setEnabled(true);
									donebutton.setOnClickListener(doneListener);
									redobutton.setText("Redo");
									redobutton.setOnClickListener(redoListener);
									setupmap();
								}
								else {
									_tvIn1.setText("Map not found, Please Retry");
									redobutton.setText("Retry");
								}
							}
						};

		doneListener = new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								(new DrawLineTask(mActivity)).execute();
								mMap.setOnMapLongClickListener(null);
								mMap.animateCamera(CameraUpdateFactory.zoomTo((float) 10.0));
								donebutton.setEnabled(false);
							}
						};
						
		submitListener = new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								mCallback.onSubmitClick();
								donebutton.setText("Done");
								donebutton.setEnabled(false);
							}
						};
}
    
	public void setupmap() {
        locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

        Criteria locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1L, 2F, onStartCenter);

        mMap.setLocationSource(onStartCenter);
        mMap.setMyLocationEnabled(true);
        
        mMap.clear();
        waypointcount = 0;
        
        //Register Long Click Listener
        registerlongclicklistener();

	}
    
	public void registerlongclicklistener() {
		mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
			@Override
			public void onMapLongClick(LatLng point) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && Geocoder.isPresent()) {
		            // Since the geocoding API is synchronous and may take a while.  You don't want to lock
		            // up the UI thread.  Invoking reverse geocoding in an AsyncTask.
		            (new ReverseGeocodingTask(mActivity)).execute(new LatLng[] {point});
				}
				
				else _tvIn1.setText("NOT PRESENT");
				
				waypointcount++;
				point = new LatLng (fivedp(point.latitude), fivedp(point.longitude));
				waypointlist.add(waypointcount, point);
			}
		});
	}
	
	public double fivedp (double number) {
		return Double.parseDouble(new DecimalFormat("###.#####").format(number));
	}
	
    private LatLng Geocoding (String string) {
    	Geocoder coder = new Geocoder(mActivity);
    	List<Address> address;

    	try {
    		address = coder.getFromLocationName(string,1);
	    	if (address == null) {
	    	    return null;
	    	}
	    	Address location = address.get(0);
	    	LatLng latlng = new LatLng (fivedp(location.getLatitude()),fivedp(location.getLongitude()));
	    	return latlng;
	    	
    	} catch (IOException e) {
    		e.printStackTrace();
		}
    	
    	return null;
    }	
	
	////////////////////////////////////////////////
	//////////// Line Drawing Related //////////////
	////////////////////////////////////////////////
	public String makeRequest(LatLng start, LatLng end) {
		StringBuffer urlString = new StringBuffer();
		urlString.append("http://maps.googleapis.com/maps/api/directions/json?");
		urlString.append("origin=");
		urlString.append(String.valueOf(start.latitude));
		urlString.append(",");
		urlString.append(String.valueOf(start.longitude));
		urlString.append("&destination=");
		urlString.append(String.valueOf(end.latitude));
		urlString.append(",");
		urlString.append(String.valueOf(end.longitude));
		urlString.append("&sensor=false");
		return urlString.toString();
	}    

    private LatLng decodePolySingle(String encoded) {
        
        int index = 0;
        int lat = 0, lng = 0;

        int b, shift = 0, result = 0;
        do {
            b = encoded.charAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
        lat += dlat;
           
        shift = 0;
        result = 0;
        do {
            b = encoded.charAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
        lng += dlng;

        LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));

        return p;
    }	
	
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }
        
        return poly;
    }	

	public boolean checkFurther (LatLng nextPoint, List<LatLng> polypoints) {

		//Check if current movement is nearer or further from next waypoint
		boolean further = true;
		LatLng last = polypoints.get(polypoints.size()-1);
		LatLng first = polypoints.get(0);
		
		//check if current route moves further away by comparing (start of current leg to end) versus (end of current leg to end)
		double firstend = Math.hypot((nextPoint.latitude - first.latitude), (nextPoint.longitude - first.longitude));
		double lastend = Math.hypot((nextPoint.latitude - last.latitude), (nextPoint.longitude - last.longitude));
		
		if (lastend < firstend) further = false;
		
		return further;
	}    
		
    public List<LatLng> drawLine() throws ClientProtocolException, IOException, JSONException {
    	
    	int i = 1;
    	int jsoncount;
    	
    	List<LatLng> testpoints = new ArrayList<LatLng>();
    	List<LatLng> currentdirectionlist;
    	
    	JSONObject currentleg;
    	JSONObject currentdirections;
    	
    	String instruction;
    	String polylinetest;
    	
    	
    	for (; i < waypointlist.size(); i++) {
    		jsoncount = 0;
    		
    		//Make request between every consecutive pairing
        	HttpPost httppost = new HttpPost( makeRequest( waypointlist.get(i-1), waypointlist.get(i) ) );
        	HttpClient httpclient = new DefaultHttpClient();
    		HttpResponse response = httpclient.execute(httppost);
        	HttpEntity entity = response.getEntity();
        	InputStream is = null;
        	is = entity.getContent();
        	BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
        	StringBuilder sb = new StringBuilder();
        	sb.append(reader.readLine() + "\n");
        	String line = "0";
        	while ((line = reader.readLine()) != null) {
        	    sb.append(line + "\n");
        	}
        	is.close();
        	reader.close();
        	String result = sb.toString();
        	
        	JSONObject jsonObject = new JSONObject(result);
        	JSONArray routeArray = jsonObject.getJSONArray("routes");
        	JSONObject routes = routeArray.getJSONObject(0);
        	
        	//JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
        	
        	JSONArray legsArray = routes.getJSONArray("legs");
        	JSONObject steps = legsArray.getJSONObject(0);
        	JSONArray stepsArray = steps.getJSONArray("steps");
        	
        	jsonloop:
        	while (jsoncount < stepsArray.length()) {
            	
        		currentleg = stepsArray.getJSONObject(jsoncount);
        		currentdirections = currentleg.getJSONObject("polyline");
        	
        		instruction = currentleg.getString("html_instructions");
        		polylinetest = currentdirections.getString("points");
        		
        		currentdirectionlist = decodePoly(polylinetest);
        		
        		Log.e("test", "before uturn check");
        		if (instruction.contains("U-turn") &&   (i != waypointlist.size()-1)) {
        			if (checkFurther(waypointlist.get(i+1), currentdirectionlist)) {
        				Log.e("test", "before checkfurther check");
        				//If further, get last point of previous leg and set it to current start request
        				//exit loop without adding polyline for current leg
        				waypointlist.set(i, testpoints.get(testpoints.size()-1));
        				break jsonloop;
        			}
        			else continue jsonloop;
        		}
        		
        		instrucpoints.add(decodePolySingle(polylinetest));
        		instruc.add(instruction.replaceAll("<[^>]*>", ""));
        		testpoints.addAll(decodePoly(polylinetest));
        		jsoncount++;
        	}
        	
        	//String encodedString = overviewPolylines.getString("points");
        	//List<LatLng> pointToDraw = decodePoly(encodedString);
            //Log.v("test", " " + pointToDraw.size());
    		
    	}
    	Log.v("test", " " + testpoints.size());
    	return testpoints;
    }    
    
	////////////////////////////////////////////////
	///////////////// ASYNC TASKS //////////////////
	////////////////////////////////////////////////
	private class ReverseGeocodingTask extends AsyncTask<LatLng, Void, String> {
    	Context mContext;
    	LatLng latlng;
    	
    	public ReverseGeocodingTask (Context context) {
    		super();
    		mContext = context;
    	}

		@Override
		protected String doInBackground(LatLng... params) {
			String roadname;
			roadname = "NOTHING";
			
			Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
			
			latlng = params[0];
			List<Address> addresses = null;
			
			try {
				addresses = geocoder.getFromLocation(latlng.latitude, latlng.longitude, 1);
			} catch (IOException e) {
	            e.printStackTrace();
	        }
			
			if (addresses != null && addresses.size() > 0) {
	            Address address = addresses.get(0);
	            roadname = address.getAddressLine(0);
	        }
			
			return roadname;
		}
		
		protected void onPostExecute(String result) {
			_tvIn1.setText(result);
	    }
    	
    }	

    private class addInstrucMarkers extends AsyncTask<Void, Void, List<LatLng>> {
    	@SuppressWarnings("unused")
		Context mContext;
    	int i = 0;
    	
    	public addInstrucMarkers (Context context) {
    		super();
    		mContext = context;
    	}
    	
		@Override
		protected List<LatLng> doInBackground(Void... arg0) {
			return null;
		}
		
		protected void onPostExecute(List<LatLng> drawpoints) {
			while ( i < instrucpoints.size() ) {
				
				mMap.addMarker(	new MarkerOptions()
								.position( instrucpoints.get(i) )
								.title( String.valueOf(i+1) )
								.snippet( instruc.get(i) )
								.icon( BitmapDescriptorFactory.fromAsset("white_dot.png") )
								.draggable(false)
								.anchor((float) 0.5, (float) 0.5));
				i++;
			}
	    }
    }	
	
    private class DrawLineTask extends AsyncTask<Void, Void, List<LatLng>> {
    	@SuppressWarnings("unused")
		Context mContext;
		final String TAG = "MyActivity";
    	
    	public DrawLineTask (Context context) {
    		super();
    		mContext = context;
    	}
    	
		@Override
		protected List<LatLng> doInBackground(Void... arg0) {

			Log.v(TAG, "In Draw Line Task 2");

			try {
				return drawLine();
			} catch (ClientProtocolException e) {
				Log.v(TAG, "Client Protocol Exception");
				e.printStackTrace();
			} catch (IOException e) {
				Log.v(TAG, "IOException");
				e.printStackTrace();
			} catch (JSONException e) {
				Log.v(TAG, "JSONException");
				e.printStackTrace();
			}
			return null;
		}
		
		protected void onPostExecute(List<LatLng> drawpoints) {
			customroute = drawpoints;
			
	    	PolylineOptions routepath = new PolylineOptions().addAll(drawpoints);
			routepath.color(Color.BLUE);
	    	mMap.addPolyline(routepath);
	    	
	    	(new addInstrucMarkers(mActivity)).execute();
	    	
	    	waypointcount = 0;
	    	
			donebutton.setText("Upload");
			donebutton.setOnClickListener(submitListener);
	    	donebutton.setEnabled(true);
			_tvIn1.setText("Ready to Upload");
	    }
    }
    
}
