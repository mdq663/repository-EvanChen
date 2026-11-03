package com.zcshou.gogogo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.slider.Slider;

import java.util.Locale;

public class StepSimActivity extends BaseActivity {
    private static final String PREFS = "sensor_sim_prefs";
    private static final String PREF_MIN = "min_cadence";
    private static final String PREF_MAX = "max_cadence";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LicenseManager.requireAuthorization(this)) {
            return;
        }
        setContentView(R.layout.activity_step_sim);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_step_sim);
        toolbar.setNavigationOnClickListener(view -> finish());
        Slider slider = findViewById(R.id.step_sim_cadence_slider);
        TextView summary = findViewById(R.id.step_sim_cadence_summary);

        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        float min = preferences.getFloat(PREF_MIN, 172f);
        float max = preferences.getFloat(PREF_MAX, 182f);
        slider.setValue(Math.max(100f, Math.min(220f, (min + max) / 2f)));
        updateSummary(summary, slider.getValue());
        slider.addOnChangeListener((control, value, fromUser) -> {
            updateSummary(summary, value);
            preferences.edit()
                    .putFloat(PREF_MIN, value)
                    .putFloat(PREF_MAX, value)
                    .apply();
        });
    }

    private void updateSummary(TextView summary, float value) {
        summary.setText(String.format(Locale.CHINA, "%.0f 步/分钟", value));
    }
}
