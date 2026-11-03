package com.zcshou.gogogo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AuthActivity extends AppCompatActivity {
    static final String EXTRA_AUTO_ENTER =
            "com.zcshou.gogogo.extra.AUTO_ENTER_AFTER_AUTH";
    private static final long ENTRY_DELAY_MS = 650L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordInput;
    private MaterialButton verifyButton;
    private View statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (LicenseManager.isAuthorized(this)) {
            openOriginalEntry();
            return;
        }

        setContentView(R.layout.activity_auth);

        passwordLayout = findViewById(R.id.password_layout);
        passwordInput = findViewById(R.id.password_input);
        verifyButton = findViewById(R.id.verify_button);
        statusText = findViewById(R.id.auth_status);

        verifyButton.setOnClickListener(view -> verifyPassword());
        passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verifyPassword();
                return true;
            }
            return false;
        });
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                passwordLayout.setError(null);
                statusText.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void verifyPassword() {
        String candidate = passwordInput.getText() == null
                ? ""
                : passwordInput.getText().toString();

        if (candidate.isEmpty()) {
            passwordLayout.setError(getString(R.string.auth_password_required));
            passwordInput.requestFocus();
            return;
        }

        hideKeyboard();
        verifyButton.setEnabled(false);
        boolean passwordCorrect = LicenseManager.verifyPassword(candidate);
        if (!passwordCorrect) {
            passwordLayout.setError(getString(R.string.auth_password_error));
            passwordInput.setText("");
            passwordInput.requestFocus();
            verifyButton.setEnabled(true);
            return;
        }

        if (!LicenseManager.persistAuthorization(this)) {
            statusText.setVisibility(View.VISIBLE);
            ((android.widget.TextView) statusText).setText(R.string.auth_storage_error);
            ((android.widget.TextView) statusText).setTextColor(Color.RED);
            verifyButton.setEnabled(true);
            return;
        }

        passwordLayout.setError(null);
        passwordInput.setEnabled(false);
        statusText.setVisibility(View.VISIBLE);
        ((android.widget.TextView) statusText).setText(R.string.auth_success);
        ((android.widget.TextView) statusText).setTextColor(
                getResources().getColor(R.color.auth_success, getTheme()));
        mainHandler.postDelayed(() -> {
            if (!isFinishing()) {
                openOriginalEntry();
            }
        }, ENTRY_DELAY_MS);
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focusedView = getCurrentFocus();
        if (inputMethodManager != null && focusedView != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    focusedView.getWindowToken(), 0);
        }
    }

    private void openOriginalEntry() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra(EXTRA_AUTO_ENTER, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
