package com.projeto.egoodapp.views.common.session;

import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import java.util.List;

/** Forwards the public Android Window contract, preserving Material/AppCompat dialog behavior. */
class WindowEvents implements Window.Callback {
    private final Window.Callback next;
    WindowEvents(Window.Callback next) { this.next = next; }
    @Override public boolean dispatchKeyEvent(KeyEvent event) { return next.dispatchKeyEvent(event); }
    @Override public boolean dispatchKeyShortcutEvent(KeyEvent event) { return next.dispatchKeyShortcutEvent(event); }
    @Override public boolean dispatchTouchEvent(MotionEvent event) { return next.dispatchTouchEvent(event); }
    @Override public boolean dispatchTrackballEvent(MotionEvent event) { return next.dispatchTrackballEvent(event); }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) { return next.dispatchGenericMotionEvent(event); }
    @Override public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) { return next.dispatchPopulateAccessibilityEvent(event); }
    @Override public View onCreatePanelView(int featureId) { return next.onCreatePanelView(featureId); }
    @Override public boolean onCreatePanelMenu(int featureId, Menu menu) { return next.onCreatePanelMenu(featureId, menu); }
    @Override public boolean onPreparePanel(int featureId, View view, Menu menu) { return next.onPreparePanel(featureId, view, menu); }
    @Override public boolean onMenuOpened(int featureId, Menu menu) { return next.onMenuOpened(featureId, menu); }
    @Override public boolean onMenuItemSelected(int featureId, MenuItem item) { return next.onMenuItemSelected(featureId, item); }
    @Override public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) { next.onWindowAttributesChanged(attrs); }
    @Override public void onContentChanged() { next.onContentChanged(); }
    @Override public void onWindowFocusChanged(boolean focused) { next.onWindowFocusChanged(focused); }
    @Override public void onAttachedToWindow() { next.onAttachedToWindow(); }
    @Override public void onDetachedFromWindow() { next.onDetachedFromWindow(); }
    @Override public void onPanelClosed(int featureId, Menu menu) { next.onPanelClosed(featureId, menu); }
    @Override public boolean onSearchRequested() { return next.onSearchRequested(); }
    @Override public boolean onSearchRequested(SearchEvent event) { return next.onSearchRequested(event); }
    @Override public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) { return next.onWindowStartingActionMode(callback); }
    @Override public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) { return next.onWindowStartingActionMode(callback, type); }
    @Override public void onActionModeStarted(ActionMode mode) { next.onActionModeStarted(mode); }
    @Override public void onActionModeFinished(ActionMode mode) { next.onActionModeFinished(mode); }
    @Override public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> groups, Menu menu, int deviceId) { next.onProvideKeyboardShortcuts(groups, menu, deviceId); }
    @Override public void onPointerCaptureChanged(boolean captured) { next.onPointerCaptureChanged(captured); }
}
