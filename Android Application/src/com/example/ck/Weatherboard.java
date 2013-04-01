package com.example.ck;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.framjackck.ApplicationInterface;
import com.example.framjackck.ApplicationInterface.UpdateListener;
import com.example.ufyp.R;

public class Weatherboard extends Fragment {
	
	private ApplicationInterface _appInterface;
	public TextView temptv, humiditytv, lighttv, statustv;
	public List<String> weatherboard = new ArrayList<String>();
	
	//Variable to determine which activity is using this fragment
	public boolean ckapp = true;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		 
		View myFragmentView = inflater.inflate(R.layout.routing, container, false);
		
		//Initialise weatherboard list
		weatherboard.add("0");
		weatherboard.add("0");
		weatherboard.add("0");
		
        _appInterface = new ApplicationInterface();
        
        bindControls(myFragmentView);
        
        _appInterface.registerOnUpdateListener(_listener);
		
		return myFragmentView;
	}
        
    @Override
    public void onPause() {
    	if (ckapp)
    		_appInterface.stop();
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	if (ckapp)
    		_appInterface.start();
    	super.onResume();
    }
    
    public void starthijack () {
    	//Method for activity to start hijack
    	_appInterface.start();
    }
    
    public void stophijack(){
    	//Method for activity to stop hijack
    	_appInterface.stop();
    }

    private void bindControls(View myFragmentView) {
		temptv = (TextView) myFragmentView.findViewById(R.id.textViewTemp);
		humiditytv = (TextView) myFragmentView.findViewById(R.id.textViewHum);
		lighttv = (TextView) myFragmentView.findViewById(R.id.textViewLight);
		statustv = (TextView) myFragmentView.findViewById(R.id.textViewStatus);
    }
    
    private void updateBindings() {
    	
    	DecimalFormat decimalFormat = new DecimalFormat("##.#");
    	
    	String temptemp = decimalFormat.format(_appInterface.getTemperature());
    	String temphum = decimalFormat.format(_appInterface.getAnalogInput(3));
    	String templight = "Night";

    	if (_appInterface.getAnalogInput(2)>2.35)
    		templight = "Bright";
    	else {
    		if (_appInterface.getAnalogInput(3)>1.75)
    			templight = "Day";
    		else {
        		if (_appInterface.getAnalogInput(3)>0.6)
        			templight = "Room";
        		else
            		templight = "Dark";
    		}
    	}
    	
    	temptv.setText(temptemp);
    	humiditytv.setText(temphum);
    	lighttv.setText(templight);
    	
    	weatherboard.set(0, temptemp);
    	weatherboard.set(1, temphum);
    	weatherboard.set(2, templight);
    	
    	statustv.setText(_appInterface.getIsConnected() ? R.string.status_connected : R.string.status_disconnected);
    	statustv.setTextColor(_appInterface.getIsConnected() ? Color.parseColor("#11FF11") : Color.parseColor("#FF1111"));
    }    
   
    public List<String> getweather() {
    	//Method of activity to get weatherboard data
    	return weatherboard;
    }

	private UpdateListener _listener = new UpdateListener() {
		public void Update() {
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					updateBindings();
				}
			});
		}
    };
}
