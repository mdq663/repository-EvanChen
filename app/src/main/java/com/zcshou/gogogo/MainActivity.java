package com.zcshou.gogogo;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.MaterialToolbar;
import com.zcshou.utils.GoUtils;

public class MainActivity extends BaseActivity {
    private NestedScrollView homeScroll;
    private View homeScrollOuter;
    private View homeScrollContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LicenseManager.requireAuthorization(this)) {
            return;
        }

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setTitle(R.string.app_name);
        toolbar.setLogo(R.drawable.ic_app_mark);
        setSupportActionBar(toolbar);

        homeScroll = findViewById(R.id.home_scroll);
        homeScrollOuter = findViewById(R.id.home_scroll_outer);
        homeScrollContent = findViewById(R.id.home_scroll_content);

        View nfcTools = findViewById(R.id.card_nfc_tools);
        nfcTools.setOnClickListener(view ->
                startActivity(new Intent(this, NfcToolsActivity.class)));

        View routeRun = findViewById(R.id.card_route_run);
        routeRun.setOnClickListener(view ->
                startActivity(new Intent(this, RouteRunActivity.class)));

        View stepSim = findViewById(R.id.card_step_sim);
        stepSim.setOnClickListener(view ->
                startActivity(new Intent(this, StepSimActivity.class)));

        View routeCreate = findViewById(R.id.card_route_create);
        routeCreate.setOnClickListener(view ->
                startActivity(new Intent(this, RouteCreateActivity.class)));

        View settings = findViewById(R.id.card_settings);
        settings.setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class)));

        View developerOptions = findViewById(R.id.card_dev);
        developerOptions.setOnClickListener(view -> openDeveloperOptions());

        homeScroll.post(this::centerHomeContent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (homeScroll != null) {
            homeScroll.post(this::centerHomeContent);
        }
    }

    private void openDeveloperOptions() {
        if (!GoUtils.isDeveloperOptionsEnabled(this)) {
            Toast.makeText(this, R.string.app_error_dev, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception exception) {
            Toast.makeText(this, R.string.app_error_dev, Toast.LENGTH_LONG).show();
        }
    }

    private void centerHomeContent() {
        if (homeScroll == null || homeScrollOuter == null || homeScrollContent == null
                || !homeScroll.isAttachedToWindow()) {
            return;
        }
        int height = homeScroll.getHeight() - homeScroll.getPaddingTop()
                - homeScroll.getPaddingBottom();
        int width = homeScroll.getWidth() - homeScroll.getPaddingLeft()
                - homeScroll.getPaddingRight() - homeScrollOuter.getPaddingLeft()
                - homeScrollOuter.getPaddingRight();
        if (height <= 0 || width <= 0) {
            return;
        }
        homeScrollContent.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int padding = Math.max(0, (height - homeScrollContent.getMeasuredHeight()) / 2);
        homeScrollOuter.setPadding(
                homeScrollOuter.getPaddingLeft(),
                padding,
                homeScrollOuter.getPaddingRight(),
                padding);
    }
}
