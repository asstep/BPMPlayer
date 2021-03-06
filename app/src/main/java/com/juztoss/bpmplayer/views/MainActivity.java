package com.juztoss.bpmplayer.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.juztoss.bpmplayer.services.PlaybackService;
import com.juztoss.bpmplayer.R;
import com.juztoss.bpmplayer.presenters.BPMPlayerApp;

public class MainActivity extends AppCompatActivity
{
    private DrawerArrowDrawable mHamburger;
    private BPMPlayerApp mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mApp = (BPMPlayerApp) getApplication();
        setContentView(R.layout.activity_main);

        if (!mApp.isPlaybackServiceRunning())
        {
            Intent playbackServiceIntent = new Intent(this, PlaybackService.class);
            startService(playbackServiceIntent);
        }

        //Add hamburger
        setupActionBar();
    }

    private void setupActionBar()
    {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHamburger = new DrawerArrowDrawable(this);
        getSupportActionBar().setHomeAsUpIndicator(mHamburger);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        View actionBarLayout = getLayoutInflater().inflate(R.layout.action_bar, null);
        TextView actionBarTitleview = (TextView) actionBarLayout.findViewById(R.id.actionbar_titleview);
        actionBarTitleview.setText("My Custom ActionBar Title");
        actionBar.setCustomView(actionBarLayout);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.openDrawer(GravityCompat.START);
            return true;
        }
        else if(id == R.id.settings_menu)
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.player_menu, menu);
        return true;
    }
}
