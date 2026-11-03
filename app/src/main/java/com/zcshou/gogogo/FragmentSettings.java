package com.zcshou.gogogo;

import android.os.Bundle;
import android.content.Intent;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.baidu.mapapi.SDKInitializer;
import com.elvishew.xlog.XLog;
import com.zcshou.utils.GoUtils;

import java.util.Objects;

public class FragmentSettings extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_main);
        Preference routeManage = findPreference("pref_route_manage");
        if (routeManage != null) {
            routeManage.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireContext(), RouteActivity.class));
                return true;
            });
        }
    }
}
