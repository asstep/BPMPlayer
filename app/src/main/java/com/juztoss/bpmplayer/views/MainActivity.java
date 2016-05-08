package com.juztoss.bpmplayer.views;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.view.MenuItem;

import com.juztoss.bpmplayer.PlaybackService;
import com.juztoss.bpmplayer.R;
import com.juztoss.bpmplayer.presenters.BPMPlayerApp;

public class MainActivity extends Activity {
    private DrawerArrowDrawable mHamburger;
    private BPMPlayerApp mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApp = (BPMPlayerApp) getApplication();
        setContentView(R.layout.decor);

        if (!mApp.isPlaybackServiceRunning()) {
            Intent playbackServiceIntent = new Intent(this, PlaybackService.class);
            startService(playbackServiceIntent);
        }

        BrowserFragment explorerView = (BrowserFragment) getFragmentManager().findFragmentById(R.id.file_tree);
        mApp.getFileTreePresenter().init(explorerView);
        explorerView.init(mApp.getFileTreePresenter());

        PlayerFragment playerFragment = (PlayerFragment) getFragmentManager().findFragmentById(R.id.drawer_content);
        playerFragment.init(mApp.getPlayerPresenter());


        //Add hamburger
        mHamburger = new DrawerArrowDrawable(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeAsUpIndicator(mHamburger);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}