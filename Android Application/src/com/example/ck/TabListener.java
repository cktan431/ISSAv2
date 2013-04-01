package com.example.ck;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;

public class TabListener<T> implements ActionBar.TabListener {

	private Fragment mFragment;
    private final Activity myActivity;
    private final String myTag;
    private final Class<T> myClass;
	
    public TabListener(Activity activity, String tag, Class<T> cls) {
        myActivity = activity;
        myTag = tag;
        myClass = cls;
    }
    
	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
        // Check if the fragment is already initialized
        if (mFragment == null) {
            // If not, instantiate and add it to the activity
            mFragment = Fragment.instantiate(myActivity, myClass.getName());
            ft.add(android.R.id.content, mFragment, myTag);
        } else {
            // If it exists, simply attach it in order to show it
            //ft.attach(mFragment);
        	ft.show(mFragment);
        }
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {	
        if (mFragment != null) {
            // Detach the fragment, because another one is being attached
            //ft.detach(mFragment);
        	ft.hide(mFragment);
        }
	}
}
