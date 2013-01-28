package com.freeandroapp.smsscheduler.activities;

import com.freeandroapp.smsscheduler.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class SplashActivity extends Activity {
	 
	   private static String TAG = SplashActivity.class.getName();
	   private static long SLEEP_TIME = 3;    // Sleep for some time
	 
	   @Override
	   protected void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);
	 
	      this.requestWindowFeature(Window.FEATURE_NO_TITLE);    // Removes title bar
	      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);    // Removes notification bar
	 
	      setContentView(R.layout.splash);
	 
	      // Start timer and launch main activity
	      IntentLauncher launcher = new IntentLauncher();
	      launcher.start();
	   }
	 
	   private class IntentLauncher extends Thread {
	      @Override
	      /**
	       * Sleep for some time and than start new activity.
	       */
	      public void run() {
	         try {
	            // Sleeping
	            Thread.sleep(SLEEP_TIME*1000);
	         } catch (Exception e) {
	            Log.e(TAG, e.getMessage());
	         }
	 
	         // Start main activity
	         Intent intent = new Intent(SplashActivity.this, Home.class);
	         SplashActivity.this.startActivity(intent);
	         SplashActivity.this.finish();
	      }
	   }
	   
	   /*@Override
	   protected void onDestroy() {
	   android.os.Process.killProcess(android.os.Process.myPid());
	   }*/
	   
	   @Override
	   public boolean onKeyDown(int keyCode, KeyEvent event) {
	       if ((keyCode == KeyEvent.KEYCODE_BACK)) {
	    	   android.os.Process.killProcess(android.os.Process.myPid());
	       	}
	       return super.onKeyDown(keyCode, event);
	   }
	}
