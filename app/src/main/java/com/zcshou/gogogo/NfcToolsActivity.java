package com.zcshou.gogogo;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.nio.charset.StandardCharsets;

public class NfcToolsActivity extends BaseActivity {
    private static final String PREFS_NAME = "NfcMookPrefs";
    private static final String PREF_URL = "last_url";
    private static final String PREF_PACKAGE = "last_package";

    private NfcAdapter nfcAdapter;
    private TextView hardwareStatus;
    private TextView operationStatus;
    private TextInputEditText urlInput;
    private TextInputEditText packageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LicenseManager.requireAuthorization(this)) {
            return;
        }

        setContentView(R.layout.activity_nfc_tools);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_nfc);
        toolbar.setNavigationOnClickListener(view -> finish());

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        hardwareStatus = findViewById(R.id.nfc_tv_hardware_status);
        operationStatus = findViewById(R.id.nfc_tv_mock_status);
        urlInput = findViewById(R.id.nfc_et_mock_url);
        packageInput = findViewById(R.id.nfc_et_mock_package);

        String savedUrl = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(PREF_URL, "");
        String savedPackage = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(PREF_PACKAGE, "");
        urlInput.setText(savedUrl);
        packageInput.setText(savedPackage);

        findViewById(R.id.nfc_btn_copy_url).setOnClickListener(view ->
                copyText(getString(R.string.nfc_copy_link), urlInput));
        findViewById(R.id.nfc_btn_copy_package).setOnClickListener(view ->
                copyText(getString(R.string.nfc_copy_package), packageInput));
        MaterialButton mockButton = findViewById(R.id.nfc_btn_mock);
        mockButton.setOnClickListener(view -> simulateNfc());

        updateNfcStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            Intent intent = new Intent(this, getClass())
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
            try {
                nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
            } catch (Exception ignored) {
            }
        }
        updateNfcStatus();
    }

    @Override
    protected void onPause() {
        if (nfcAdapter != null) {
            try {
                nfcAdapter.disableForegroundDispatch(this);
            } catch (Exception ignored) {
            }
        }
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readNfcIntent(intent);
    }

    private void updateNfcStatus() {
        if (nfcAdapter == null) {
            hardwareStatus.setText(R.string.nfc_status_no_hardware);
        } else if (nfcAdapter.isEnabled()) {
            hardwareStatus.setText(R.string.nfc_status_ready);
        } else {
            hardwareStatus.setText(R.string.nfc_status_disabled);
        }
    }

    private void copyText(String label, TextInputEditText input) {
        CharSequence value = input.getText();
        String text = value == null ? "" : value.toString().trim();
        if (TextUtils.isEmpty(text)) {
            showStatus(getString(R.string.nfc_copy_empty), true);
            return;
        }
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
            showStatus(getString(R.string.nfc_copied, label), false);
        }
    }

    private void simulateNfc() {
        vibrate();
        saveInputs();
        String url = textOf(urlInput);
        String packageName = textOf(packageInput);
        if (url.isEmpty()) {
            showStatus(getString(R.string.nfc_mock_need_url), true);
            return;
        }
        if (packageName.isEmpty()) {
            showStatus(getString(R.string.nfc_mock_need_package), true);
            return;
        }

        try {
            NdefMessage message = new NdefMessage(
                    NdefRecord.createUri(url),
                    NdefRecord.createApplicationRecord(packageName));
            Intent ndefIntent = new Intent(NfcAdapter.ACTION_NDEF_DISCOVERED)
                    .setData(android.net.Uri.parse(url))
                    .putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES,
                            new Parcelable[]{message})
                    .setPackage(packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(ndefIntent);
            showStatus(getString(R.string.nfc_mock_sent), false);
        } catch (ActivityNotFoundException | SecurityException firstFailure) {
            try {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        .setPackage(packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(viewIntent);
                showStatus(getString(R.string.nfc_mock_opened_browser), false);
            } catch (Exception secondFailure) {
                showStatus(getString(R.string.nfc_mock_failed)
                        + secondFailure.getMessage(), true);
            }
        } catch (Exception exception) {
            showStatus(getString(R.string.nfc_mock_failed) + exception.getMessage(), true);
        }
    }

    private void readNfcIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            return;
        }

        Parcelable[] rawMessages =
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages != null) {
            NdefMessage[] messages = new NdefMessage[rawMessages.length];
            for (int i = 0; i < rawMessages.length; i++) {
                messages[i] = (NdefMessage) rawMessages[i];
            }
            readMessages(messages);
            return;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            showStatus(getString(R.string.nfc_tag_not_ndef), true);
            return;
        }
        try {
            ndef.connect();
            NdefMessage message = ndef.getNdefMessage();
            if (message != null) {
                readMessages(new NdefMessage[]{message});
            }
        } catch (Exception exception) {
            showStatus(getString(R.string.nfc_read_failed, exception.getMessage()), true);
        } finally {
            try {
                ndef.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void readMessages(NdefMessage[] messages) {
        String foundUrl = "";
        String foundPackage = "";
        for (NdefMessage message : messages) {
            for (NdefRecord record : message.getRecords()) {
                if (record.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE
                        && java.util.Arrays.equals(record.getType(),
                        "android.com:pkg".getBytes(StandardCharsets.US_ASCII))) {
                    foundPackage = new String(record.getPayload(), StandardCharsets.US_ASCII);
                } else if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN
                        && java.util.Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                    foundUrl = String.valueOf(record.toUri());
                } else if (record.getTnf() == NdefRecord.TNF_ABSOLUTE_URI) {
                    foundUrl = new String(record.getPayload(), StandardCharsets.UTF_8);
                } else if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN
                        && java.util.Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                    String text = decodeTextRecord(record);
                    if (!text.isEmpty() && foundUrl.isEmpty() && text.contains("://")) {
                        foundUrl = text;
                    } else if (!text.isEmpty() && foundPackage.isEmpty() && text.contains(".")) {
                        foundPackage = text;
                    }
                }
            }
        }

        boolean found = false;
        if (!foundUrl.isEmpty()) {
            urlInput.setText(foundUrl);
            found = true;
        }
        if (!foundPackage.isEmpty()) {
            packageInput.setText(foundPackage);
            found = true;
        }
        if (found) {
            saveInputs();
            showStatus(getString(R.string.nfc_read_success), false);
        } else {
            showStatus(getString(R.string.nfc_read_empty), true);
        }
    }

    private String decodeTextRecord(NdefRecord record) {
        byte[] payload = record.getPayload();
        if (payload == null || payload.length == 0) {
            return "";
        }
        int languageLength = payload[0] & 0x3F;
        int offset = languageLength + 1;
        return offset >= payload.length
                ? ""
                : new String(payload, offset, payload.length - offset,
                StandardCharsets.UTF_8);
    }

    private String textOf(TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }

    private void saveInputs() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(PREF_URL, textOf(urlInput))
                .putString(PREF_PACKAGE, textOf(packageInput))
                .apply();
    }

    private void showStatus(String message, boolean error) {
        operationStatus.setVisibility(View.VISIBLE);
        operationStatus.setText(message);
        operationStatus.setTextColor(ContextCompat.getColor(this,
                error ? R.color.half_black : R.color.colorPrimary));
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(80,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(80);
        }
    }
}
