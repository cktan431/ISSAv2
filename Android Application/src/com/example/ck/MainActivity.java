package com.example.ck;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.example.ck.Map;
import com.example.ck.OnSubmitClickListener;
import com.example.ck.Weatherboard;
import com.example.ck.TabListener;
import com.example.ufyp.DataPost;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends android.support.v4.app.FragmentActivity implements OnSubmitClickListener {
    
    //Weatherboard _routing = new Weatherboard();
    public File root = Environment.getExternalStorageDirectory();
    public File file = new File(root, "test.txt");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Get handle to ActionBar
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        //Adding ActionBar Tabs
        Tab weathertab = actionBar.newTab();
        weathertab.setText("Weather");
        weathertab.setTabListener(new TabListener<Weatherboard>(this, "Weather", Weatherboard.class));
        actionBar.addTab(weathertab);
        
        Tab maptab = actionBar.newTab();
        maptab.setText("Map");
        maptab.setTabListener(new TabListener<Map>(this, "MapTab", Map.class));
        actionBar.addTab(maptab);
        
        if (savedInstanceState != null) {
            int savedIndex = savedInstanceState.getInt("SAVED_INDEX");
            getActionBar().setSelectedNavigationItem(savedIndex);
        }
        
    }
    
    @Override
    public void onSubmitClick () {
    	//Get handle to Fragments
    	//Weatherboard fweather = (Weatherboard) getFragmentManager().findFragmentByTag("Weather");
    	Map fmap = (Map) getFragmentManager().findFragmentByTag("MapTab");
    	
    	//final List<String> weatherlist = fweather.weatherboard;
    	final List<LatLng> routeList = fmap.customroute;
    	
    	//Posting custom route to server
    	//final int a = weatherlist.size();
    	final int b = routeList.size();
    	final DataPost dataPost = new DataPost();
    	//Log.v("debug", ""+a);
    	
    	Thread posting = new Thread(){
    		public void run(){
    			JSONObject _json = new JSONObject();
    			try {
					_json.put("ck", 1); 
					_json.put("latlngdata",b);
					for(int i=0;i<b;i++){
						_json.put("latlngdata"+i,routeList.get(i));
					}
	    			
				} catch (JSONException e) {
					e.printStackTrace();
				}
    			
    			dataPost.postData(_json);
    			
    			Log.v("debug", dataPost.stringReturned());
    		}
    	};
    	posting.start();
    	
    	//Debugging Purposes
    	fmap._tvIn1.setText("Submitted");
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putInt("SAVED_INDEX", getActionBar().getSelectedNavigationIndex());
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
    
///////////////////////////////////////////////
//////FUNCTIONS NOT CURRENTLY USED/////////////
///////////////////////////////////////////////
    /*
	public String extractRoad(String road) {
		int index = 0;
		String extractedRoad = null;
		char c;
		while (index < road.length()) {								//Remove Leading Numbers
			c = road.charAt(index);
			if ((c>47 && c<58)||c==32) {
				index++;
			}
			else {
				extractedRoad = road.substring(index);
				break;
			}
		}
		
		extractedRoad = extractedRoad.replace(" ", "%20");			//Replace Space with %20 for URL
		Log.v("MyActivity", "Road = " + extractedRoad);
		
		return extractedRoad;
	}
	
	public String formURLhead (int start, int end) {
		StringBuffer urlString = new StringBuffer();
		urlString.append("http://maps.googleapis.com/maps/api/directions/json?");
		urlString.append("origin=");
		urlString.append(Integer.toString(start));
		urlString.append("&destination=");
		urlString.append(Integer.toString(end));
		return urlString.toString();
	}
	
	public String formURLwaypoint (String waypointname) {
		StringBuffer urlString = new StringBuffer();
		
		if (waypointcount == 1) {
			urlString.append("&waypoints=via:");
			urlString.append(waypointname);
		}
		
		else {
			urlString.append("%7C");
			urlString.append(waypointname);
		}
		
		if (waypointcount == 8) { //8 waypoints max, Google Web Service limitation
			urlString.append("&sensor=false");
			waypointcount = 0;
		}
		
		return urlString.toString();
	}
	
	public String formURLtail () {
		StringBuffer urlString = new StringBuffer();
		urlString.append("&sensor=false");
		waypointcount = 0;
		return urlString.toString();
	}	
	
	private class DrawLineTask extends AsyncTask<Void, Void, List<LatLng>> {
    	Context mContext;
		final String TAG = "MyActivity";
    	
    	public DrawLineTask (Context context) {
    		super();
    		mContext = context;
    	}
		@Override
		protected List<LatLng> doInBackground(Void... arg0) {

			Log.v(TAG, "In Line Async Task");

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
			Log.v(TAG, "in Post Execute");
			
	    	PolylineOptions routepath = new PolylineOptions().addAll(drawpoints);
			routepath.color(Color.BLUE);
	    	mMap.addPolyline(routepath);
	    	
	    	requestURL = null;
	    	waypointcount = 0;
	    }

    }

    public List<LatLng> drawLine() throws ClientProtocolException, IOException, JSONException {
    	
    	HttpPost httppost = new HttpPost(requestURL);
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
    	JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
    	
    	JSONArray legsArray = routes.getJSONArray("legs");
    	JSONObject steps = legsArray.getJSONObject(0);
    	JSONArray stepsArray = steps.getJSONArray("steps");
    	
    	int jsoncount = 0;
    	List<LatLng> testpoints = new ArrayList<LatLng>();
    	List<LatLng> instrucpoints = new ArrayList<LatLng>();
    	List<String> instruc = new ArrayList<String>();
    	
    	while (jsoncount < stepsArray.length()) {
    	
    		JSONObject temp = stepsArray.getJSONObject(jsoncount);
    		JSONObject temp2 = temp.getJSONObject("polyline");
    	
    		String instruction = temp.getString("html_instructions");
    		String polylinetest = temp2.getString("points");
    	
    		instrucpoints.add(decodePolySingle(polylinetest));
    		instruc.add(instruction.replaceAll("<[^>]*>", ""));
    		testpoints.addAll(decodePoly(polylinetest));
    	
    		jsoncount++;
    	}
    	
		Log.v("MyAcitivity", "instrucpoints = " + instrucpoints);
		Log.v("MyAcitivity", "instruction = " + instruc);
		Log.v("MyAcitivity", "polyline = " + testpoints);
    	
    	
    	String encodedString = overviewPolylines.getString("points");
    	List<LatLng> pointToDraw = decodePoly(encodedString);
    	
		//return testpoints;
		return pointToDraw;
    }
*/
}
