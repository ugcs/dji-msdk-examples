package com.example.ugcssample.drone;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ugcssample.model.utils.AppUtils;
import com.example.ugcssample.model.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dji.common.error.DJIError;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.GetCallback;
import dji.keysdk.callback.KeyListener;
import timber.log.Timber;

public abstract class DjiSdkKeyGroup {

    static final String UNEXPECTED_CLASS_ERROR = "Unexpected class instance! key = %s expected %s, got %s";

    public final class KeyAndKeyListener implements KeyListener, GetCallback {
        final String key;
        final Class<?> clazz;
        final DJIKey sdkKey;

        KeyAndKeyListener(String key, Class<?> clazz) {
            this.key = key;
            this.clazz = clazz;
            this.sdkKey = create(key);
        }

        @Override
        public void onValueChange(@Nullable Object oldValue, @Nullable Object newValue) {
            DjiSdkKeyGroup.this.listener.onValueChange(key, newValue);
        }

        @Override
        public void onSuccess(@NonNull Object o) {
            if (clazz != null && !clazz.isInstance(o)) {
                final String error = String.format(UNEXPECTED_CLASS_ERROR,
                        key, clazz.getName(), o.getClass().getName());
                Timber.e(error);
                if (AppUtils.debug) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            throw new RuntimeException(error);
                        }
                    });
                } else {
                    DjiSdkKeyGroup.this.listener.onFailure(key, error);
                }
                return;
            }
            DjiSdkKeyGroup.this.listener.onValueChange(key, o);
        }

        @Override
        public void onFailure(@NonNull DJIError djiError) {
            DjiSdkKeyGroup.this.listener.onFailure(key, djiError.getDescription());
        }
    }

    public interface Listener {
        void onValueChange(String key, @NonNull Object newValue);

        void onFailure(String key, String errorDescription);
    }

    private final List<KeyAndKeyListener> keyAndKeyListeners;
    public final Listener listener;

    public DjiSdkKeyGroup(String[] keyToListen, Class<?>[] expectedTypes, Listener listener) {
        this.listener = listener;
        this.keyAndKeyListeners = new ArrayList<>(keyToListen.length);

        if (expectedTypes != null && expectedTypes.length != keyToListen.length) {
            final String error = "DjiSdkKeyGroup init failed: expectedTypes.length != keyToListen.length";
            Timber.e(error);
            if (AppUtils.debug) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        throw new RuntimeException(error);
                    }
                });
            } else {
                DjiSdkKeyGroup.this.listener.onFailure("", error);
            }
            return;
        }

        int ii;
        String oneKey;
        Class<?> clazz;
        for (ii = 0; ii < keyToListen.length; ii++) {
            oneKey = keyToListen[ii];
            clazz = expectedTypes == null ? null : expectedTypes[ii];
            keyAndKeyListeners.add(new KeyAndKeyListener(oneKey, clazz));
        }
    }

    public abstract DJIKey create(String key);

    public DJIKey findKey(String key) {
        for (KeyAndKeyListener k : keyAndKeyListeners) {
            if (k.key.equals(key)) {
                return k.sdkKey;
            }
        }
        return null;
    }

    public void setUpKeyListeners(KeyManager km) {
        for (KeyAndKeyListener one : keyAndKeyListeners) {
            km.getValue(one.sdkKey, one);
            km.addListener(one.sdkKey, one);
        }
    }

    public void tearDownKeyListeners() {
        KeyManager km = KeyManager.getInstance();
        if (km == null)
            return;
        for (KeyAndKeyListener one : keyAndKeyListeners) {
            km.removeListener(one);
        }
    }

    public void forceGetAll(KeyManager km) {
        for (KeyAndKeyListener one : keyAndKeyListeners) {
            km.getValue(one.sdkKey, one);
        }
    }

    public void forceGet(String key, KeyManager km) {
        for (KeyAndKeyListener one : keyAndKeyListeners) {
            if (one.key.equals(key)) {
                km.getValue(one.sdkKey, one);
                return;
            }
        }
    }

    public static String defaultErrorMsg(String key, String errorDescription) {
        return String.format("%s ERROR : %s", key, errorDescription);
    }

    public static String defaultErrorMsg(String key, int index, String errorDescription) {
        return String.format(Locale.US, "%s [%d] ERROR : %s", key, index, errorDescription);
    }

    public static String defaultLogMsg(String... keys) {
        return String.format("setUpKeyListeners for: %s", StringUtils.join(", ", keys));
    }

}