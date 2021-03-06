package com.example.myapplication.util;

import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

public class DisplayController {

    public void addOverlay(TextView t,  String text) {
        Overlay overlay = new Overlay();
        overlay.draw(t, text);
    }

    public void addSnackBar(Snackbar snackbar, String text) {
        Overlay overlay = new Overlay();
        overlay.drawSnackBar(snackbar, text);
    }

    public void removeOverlay(TextView t) {
        t.setText("");

    }

    public void refresh() {

    }


}
