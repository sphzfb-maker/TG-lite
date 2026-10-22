package com.phsuvo.tg;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.drinkless.tdlib.TdApi;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends Activity implements Tg.UpdateListener {
    private final List<TdApi.Message> msgs = new ArrayList<>();
    private long chatId;
    private BaseAdapter adapter;
    private EditText input;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        chatId = getIntent().getLongExtra("chatId", 0);
        setTitle(getIntent().getStringExtra("title"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        ListView list = new ListView(this);
        list.setDividerHeight(0);
        list.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return msgs.size(); }
            @Override public Object getItem(int i) { return msgs.get(i); }
            @Override public long getItemId(int i) { return msgs.get(i).id; }

            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                LinearLayout wrap;
                TextView tv;
                if (cv == null) {
                    wrap = new LinearLayout(ChatActivity.this);
                    wrap.setPadding(dp(8), dp(4), dp(8), dp(4));
                    tv = new TextView(ChatActivity.this);
                    tv.setPadding(dp(12), dp(8), dp(12), dp(8));
                    tv.setMaxWidth(dp(280));
                    tv.setTextSize(15);
                    tv.setTextIsSelectable(false);
                    wrap.addView(tv);
                    wrap.setTag(tv);
                } else {
                    wrap = (LinearLayout) cv;
                    tv = (TextView) wrap.getTag();
                }
                TdApi.Message m = msgs.get(pos);
                tv.setText(Tg.preview(m.content));
                tv.setBackgroundColor(m.isOutgoing ? 0xFFD7ECFB : 0xFFEFEFEF);
                wrap.setGravity(m.isOutgoing ? Gravity.END : Gravity.START);
                return wrap;
            }
        };
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        input = new EditText(this);
        input.setHint("Message");
        input.setMaxLines(4);
        bar.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button send = new Button(this);
        send.setText("Send");
        send.setOnClickListener(v -> sendText());
        bar.addView(send);
        root.addView(bar);

        setContentView(root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Tg.addListener(this);
        TdApi.OpenChat oc = new TdApi.OpenChat();
        oc.chatId = chatId;
        Tg.send(oc, null);
        loadHistory();
    }

    @Override
    protected void onStop() {
        Tg.removeListener(this);
        TdApi.CloseChat cc = new TdApi.CloseChat();
        cc.chatId = chatId;
        Tg.send(cc, null);
        super.onStop();
    }

    private void loadHistory() {
        TdApi.GetChatHistory g = new TdApi.GetChatHistory();
        g.chatId = chatId;
        g.fromMessageId = 0;
        g.offset = 0;
        g.limit = 50;
        g.onlyLocal = false;
        Tg.send(g, r -> {
            if (r instanceof TdApi.Messages) {
                TdApi.Message[] arr = ((TdApi.Messages) r).messages;
                msgs.clear();
                if (arr != null) {
                    for (int i = arr.length - 1; i >= 0; i--) {
                        if (arr[i] != null) msgs.add(arr[i]);
                    }
                }
                adapter.notifyDataSetChanged();
            } else {
                String err = Tg.errorText(r);
                if (err != null) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onUpdate(TdApi.Object u) {
        if (u instanceof TdApi.UpdateNewMessage) {
            TdApi.Message m = ((TdApi.UpdateNewMessage) u).message;
            if (m.chatId != chatId) return;
            for (TdApi.Message x : msgs) if (x.id == m.id) return;
            msgs.add(m);
            adapter.notifyDataSetChanged();
        } else if (u instanceof TdApi.UpdateMessageSendSucceeded) {
            TdApi.UpdateMessageSendSucceeded s = (TdApi.UpdateMessageSendSucceeded) u;
            for (int i = 0; i < msgs.size(); i++) {
                if (msgs.get(i).id == s.oldMessageId) {
                    msgs.set(i, s.message);
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    private void sendText() {
        String t = input.getText().toString().trim();
        if (t.isEmpty()) return;
        TdApi.InputMessageText imt = new TdApi.InputMessageText();
        imt.text = new TdApi.FormattedText();
        imt.text.text = t;
        imt.text.entities = new TdApi.TextEntity[0];
        imt.clearDraft = true;
        TdApi.SendMessage sm = new TdApi.SendMessage();
        sm.chatId = chatId;
        sm.inputMessageContent = imt;
        Tg.send(sm, r -> {
            String err = Tg.errorText(r);
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
        input.setText("");
    }
}
