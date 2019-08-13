package com.android.inputmethod.pinyin;

import android.content.Context;
import android.content.res.Configuration;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public abstract class InputMethodServiceProxy {

    private Proxy mProxy;

    public Proxy getProxy() {
        return mProxy;
    }

    public void setProxy(Proxy proxy) {
        this.mProxy = proxy;
    }

    public interface Proxy {
        Context getApplicationContext();

        Context getIMEContext();

        InputConnection getCurrentInputConnection();

        boolean isInputViewShown();

        void sendKeyChar(char c);

        LayoutInflater getLayoutInflater();

        void setCandidatesViewShown(boolean value);

        boolean isFullscreenMode();

        void hideStatusIcon();

        void showStatusIcon(int iconId);
    }

    public void onCreate() {

    }

    public void onDestroy() {

    }

    public void onConfigurationChanged(Configuration newConfig) {

    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public abstract View onCreateCandidatesView();

    public abstract View onCreateInputView();

    public abstract void onStartInput(EditorInfo editorInfo, boolean restarting);

    public abstract void onStartInputView(EditorInfo editorInfo, boolean restarting);

    public void onFinishInputView(boolean finishingInput) {

    }

    public void onFinishInput() {

    }

    public void onFinishCandidatesView(boolean finishingInput) {

    }

    public abstract void onDisplayCompletions(CompletionInfo[] completions);

    public void requestHideSelf(int flags) {

    }

    Context getApplicationContext() {
        return getProxy().getApplicationContext();
    }

    Context getIMEContext() {
        return getProxy().getIMEContext();
    }

    InputConnection getCurrentInputConnection() {
        return getProxy().getCurrentInputConnection();
    }

    boolean isInputViewShown() {
        return getProxy().isInputViewShown();
    }

    void sendKeyChar(char c) {
        getProxy().sendKeyChar(c);
    }

    LayoutInflater getLayoutInflater() {
        return getProxy().getLayoutInflater();
    }

    void setCandidatesViewShown(boolean value) {
        getProxy().setCandidatesViewShown(value);
    }

    boolean isFullscreenMode() {
        return getProxy().isFullscreenMode();
    }

    void hideStatusIcon() {
        getProxy().hideStatusIcon();
    }

    void showStatusIcon(int iconId) {
        getProxy().showStatusIcon(iconId);
    }
}
