package com.welen.it.pullanimation;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;


public class MainActivity extends Activity {

    PullAnimationView mView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_pull_animation_view);
        mView = (PullAnimationView) findViewById(R.id.pull_view);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    float y ;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                y = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float v = y - event.getY();
                float v1 = Math.abs(v) / 600;
                Log.i("welen","v1 = "+v1);
                if(v1 < 1f){
                    mView.pulling(v1);
                }else{
                    y = event.getY();
                    mView.setCurrentState(PullAnimationView.AnimationState.RELEASE);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if(mView.getCurrentState() == PullAnimationView.AnimationState.PULLING_DOWN){
                    mView.setCurrentState(PullAnimationView.AnimationState.NORMAL);
                }
                break;
        }
        return true;
    }
}
