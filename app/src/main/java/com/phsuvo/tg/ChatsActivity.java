package com.phsuvo.tg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.drinkless.tdlib.TdApi;

import java.util.ArrayList;
import java.util.List;

public class ChatsActivity extends Activity implements Tg.UpdateListener {
    private final List<TdApi.Chat> chats = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = this::load;
    private ArrayAdapter<TdApi.Chat> adapter;
    private int gen = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle(getString(R.string.app_name));

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<TdApi.Chat>(this, android.R.layout.simple_list_item_2, chats) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                if (cv == null) {
                    cv = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                TdApi.Chat c = getItem(pos);
                String t = c.title + (c.unreadCount > 0 ? "  (" + c.unreadCount + ")" : "");
                ((TextView) cv.findViewById(android.R.id.text1)).setText(t);
                String last = c.lastMessage != null ? Tg.preview(c.lastMessage.content) : "";
                TextView t2 = cv.findViewById(android.R.id.text2);
                t2.setText(last);
                t2.setSingleLine(true);
                return cv;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            TdApi.Chat c = chats.get(pos);
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("chatId", c.id);
            i.putExtra("title", c.title);
            startActivity(i);
        });
        setContentView(list);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Tg.addListener(this);
        load();
    }

    @Override
    protected void onStop() {
        Tg.removeListener(this);
        handler.removeCallbacks(refresh);
        super.onStop();
    }

    @Override
    public void onUpdate(TdApi.Object u) {
        if (u instanceof TdApi.UpdateNewMessage
                || u instanceof TdApi.UpdateChatLastMessage
                || u instanceof TdApi.UpdateNewChat) {
            handler.removeCallbacks(refresh);
            handler.postDelayed(refresh, 700);
        }
    }

    private void load() {
        final int myGen = ++gen;
        TdApi.LoadChats lc = new TdApi.LoadChats();
        lc.chatList = new TdApi.ChatListMain();
        lc.limit = 100;
        Tg.send(lc, r1 -> {
            TdApi.GetChats gc = new TdApi.GetChats();
            gc.chatList = new TdApi.ChatListMain();
            gc.limit = 100;
            Tg.send(gc, res -> {
                if (!(res instanceof TdApi.Chats) || myGen != gen) return;
                final long[] ids = ((TdApi.Chats) res).chatIds;
                final TdApi.Chat[] out = new TdApi.Chat[ids.length];
                final int[] left = {ids.length};
                if (ids.length == 0) {
                    chats.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }
                for (int i = 0; i < ids.length; i++) {
                    final int idx = i;
                    TdApi.GetChat g = new TdApi.GetChat();
                    g.chatId = ids[i];
                    Tg.send(g, c -> {
                        if (c instanceof TdApi.Chat) out[idx] = (TdApi.Chat) c;
                        if (--left[0] == 0 && myGen == gen) {
                            chats.clear();
                            for (TdApi.Chat x : out) if (x != null) chats.add(x);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        });
    }
}
