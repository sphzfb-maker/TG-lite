package com.phsuvo.tg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.drinkless.tdlib.TdApi;

public class MainActivity extends Activity implements Tg.UpdateListener {
    private TextView title;
    private EditText input;
    private Button next;
    private int step = 0; // 0 loading, 1 phone, 2 code, 3 password

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));

        title = new TextView(this);
        title.setTextSize(18);
        root.addView(title);

        input = new EditText(this);
        input.setSingleLine(true);
        root.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        next = new Button(this);
        next.setText("Next");
        next.setOnClickListener(v -> onNext());
        root.addView(next);

        setContentView(root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Tg.addListener(this);
        render(Tg.getAuthState());
    }

    @Override
    protected void onStop() {
        Tg.removeListener(this);
        super.onStop();
    }

    @Override
    public void onUpdate(TdApi.Object u) {
        if (u instanceof TdApi.UpdateAuthorizationState) {
            render(((TdApi.UpdateAuthorizationState) u).authorizationState);
        }
    }

    private void setStep(int s, String text, int inputType, String hint) {
        boolean changed = s != step;
        step = s;
        title.setText(text);
        boolean show = s != 0;
        input.setVisibility(show ? View.VISIBLE : View.GONE);
        next.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            input.setInputType(inputType);
            input.setHint(hint);
            if (changed) input.setText("");
        }
    }

    private void render(TdApi.AuthorizationState s) {
        if (s instanceof TdApi.AuthorizationStateReady) {
            startActivity(new Intent(this, ChatsActivity.class));
            finish();
        } else if (s instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            setStep(1, "Enter your phone number with country code", InputType.TYPE_CLASS_PHONE, "+8801XXXXXXXXX");
        } else if (s instanceof TdApi.AuthorizationStateWaitCode) {
            setStep(2, "Enter the code Telegram sent you", InputType.TYPE_CLASS_NUMBER, "Code");
        } else if (s instanceof TdApi.AuthorizationStateWaitPassword) {
            setStep(3, "Enter your 2-step verification password",
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, "Password");
        } else if (s == null || s instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
            setStep(0, "Loading...", 0, "");
        } else {
            setStep(0, "Unsupported step: " + s.getClass().getSimpleName(), 0, "");
        }
    }

    private void onNext() {
        String t = input.getText().toString().trim();
        if (t.isEmpty()) return;
        TdApi.Function<?> f;
        if (step == 1) {
            TdApi.SetAuthenticationPhoneNumber p = new TdApi.SetAuthenticationPhoneNumber();
            p.phoneNumber = t;
            f = p;
        } else if (step == 2) {
            TdApi.CheckAuthenticationCode c = new TdApi.CheckAuthenticationCode();
            c.code = t;
            f = c;
        } else if (step == 3) {
            TdApi.CheckAuthenticationPassword c = new TdApi.CheckAuthenticationPassword();
            c.password = t;
            f = c;
        } else {
            return;
        }
        Tg.send(f, r -> {
            String err = Tg.errorText(r);
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }
}
