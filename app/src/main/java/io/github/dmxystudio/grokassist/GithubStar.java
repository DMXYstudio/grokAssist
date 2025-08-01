package io.github.dmxystudio.grokassist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

public class GithubStar {
    public static void setAskForStar(boolean askForStar, Context context){
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefManager.edit();
        editor.putBoolean("askForStar", askForStar);
        editor.apply();
    }

    static boolean shouldShowStarDialog(Context context) {
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        int versionCode = prefManager.getInt("versionCode",0);
        boolean askForStar = prefManager.getBoolean("askForStar", true);
        if (prefManager.contains("versionCode") && BuildConfig.VERSION_CODE > versionCode && askForStar){
            SharedPreferences.Editor editor = prefManager.edit();
            editor.putInt("versionCode", BuildConfig.VERSION_CODE);
            editor.apply();
            return true;
        } else {
            SharedPreferences.Editor editor = prefManager.edit();
            editor.putInt("versionCode", BuildConfig.VERSION_CODE);
            editor.apply();
            return false;
        }
    }

    static void starDialog(Context context, String url){
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefManager.getBoolean("askForStar", true)){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder.setMessage(R.string.dialog_StarOnGitHub);
            alertDialogBuilder.setPositiveButton(context.getString(R.string.dialog_OK_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    setAskForStar(false, context);
                }
            });
            alertDialogBuilder.setNegativeButton(context.getString(R.string.dialog_NO_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setAskForStar(false, context);
                }
            });
            alertDialogBuilder.setNeutralButton(context.getString(R.string.dialog_Later_button), null);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }
}
