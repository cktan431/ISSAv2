package com.example.ufyp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.util.Log;

import com.google.android.maps.GeoPoint;

public class DataPost {

	private List<GeoPoint> _geoList = new ArrayList<GeoPoint>();
	static String url = "http://a.helloworld.sg:8000/testData/";
	JSONObject JsonGet = null;  //json object gotten from the server 
	String stringGet = null;   //string returned 
	
	public List<GeoPoint> geoList(){ //return the geoList retrieved from the server
		return this._geoList;
	}
	
	//json object from the server
	public JSONObject resultReturned(){
		return JsonGet;
	}
	
	public String stringReturned(){
		return stringGet;
	}
	
	//post the json data to server
	public void postData(JSONObject json) {
	    // Create a new HttpClient and Post Header
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(url); //the address
	    BufferedReader in1;
	    
	    try{
            StringEntity se = new StringEntity(json.toString());  
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            httppost.setEntity(se); 
            HttpResponse response = httpclient.execute(httppost); 
            if(response!=null){
                InputStream in = response.getEntity().getContent(); //Get the data in the entity
                //Log.d("debug","there is response");
                in1 = new BufferedReader(new InputStreamReader(in));
    			StringBuffer sb = new StringBuffer("");
    			String l = "";
    			//String nl = System.getProperty("line.separator");
    			while((l=in1.readLine())!= null){
    				sb.append(l); 
    			}
    			in.close();
    			//Log.d("debug",sb.toString());
    			stringGet = sb.toString();
    			JsonGet = new JSONObject(sb.toString());//things get from django server  
    			
        }}
        catch(Exception e){
            e.printStackTrace();
        }}
	

}
