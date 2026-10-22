package com.phsuvo.tg;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

/** Thin wrapper around the TDLib client. All callbacks arrive on the main thread. */
public final class Tg {
    public interface Callback { void on(TdApi.Object result); }
    public interface UpdateListener { void onUpdate(TdApi.Object update); }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final CopyOnWriteArrayList<UpdateListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static Client client;
    private static Context appContext;
    private static volatile TdApi.AuthorizationState authState;

    private Tg() {}

    public static synchronized void init(Context ctx) {
        if (client != null) return;
        appContext = ctx.getApplicationContext();

        TdApi.SetLogVerbosityLevel v = new TdApi.SetLogVerbosityLevel();
        v.newVerbosityLevel = 1;
        Client.execute(v);

        client = Client.create(Tg::handleUpdate, null, null);
    }

    private static void handleUpdate(final TdApi.Object o) {
        if (o instanceof TdApi.UpdateAuthorizationState) {
            authState = ((TdApi.UpdateAuthorizationState) o).authorizationState;
            if (authState instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
                sendParameters();
            }
        }
        MAIN.post(() -> {
            for (UpdateListener l : LISTENERS) l.onUpdate(o);
        });
    }

    private static void sendParameters() {
        TdApi.SetTdlibParameters p = new TdApi.SetTdlibParameters();
        p.databaseDirectory = new File(appContext.getFilesDir(), "td").getAbsolutePath();
        p.filesDirectory = new File(appContext.getFilesDir(), "tdfiles").getAbsolutePath();
        p.databaseEncryptionKey = new byte[0];
        p.useFileDatabase = false;
        p.useChatInfoDatabase = true;
        p.useMessageDatabase = true;
        p.useSecretChats = false;
        p.apiId = BuildConfig.TG_API_ID;
        p.apiHash = BuildConfig.TG_API_HASH;
        p.systemLanguageCode = "en";
        p.deviceModel = Build.MODEL;
        p.systemVersion = Build.VERSION.RELEASE;
        p.applicationVersion = BuildConfig.VERSION_NAME;
        client.send(p, null);
    }

    public static void send(TdApi.Function<?> f, final Callback cb) {
        if (cb == null) {
            client.send(f, null);
        } else {
            client.send(f, r -> MAIN.post(() -> cb.on(r)));
        }
    }

    public static TdApi.AuthorizationState getAuthState() { return authState; }

    public static void addListener(UpdateListener l) { LISTENERS.addIfAbsent(l); }

    public static void removeListener(UpdateListener l) { LISTENERS.remove(l); }

    /** Returns the error text if the result is an Error, otherwise null. */
    public static String errorText(TdApi.Object r) {
        if (r instanceof TdApi.Error) {
            TdApi.Error e = (TdApi.Error) r;
            return e.code + ": " + e.message;
        }
        return null;
    }

    public static String preview(TdApi.MessageContent c) {
        if (c == null) return "";
        if (c instanceof TdApi.MessageText) {
            TdApi.MessageText t = (TdApi.MessageText) c;
            return t.text != null ? t.text.text : "";
        }
        return "[" + c.getClass().getSimpleName().replace("Message", "") + "]";
    }
}
