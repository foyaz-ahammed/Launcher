package ch.deletescape.lawnchair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import ch.deletescape.lawnchair.iconpack.IconPackManager;

/**
 * Theme app 에서 설정할 theme 정보를 받아 처리하는 클라스
 */
public class ThemeChangeReceiver extends BroadcastReceiver {

    public static boolean isBroadcastReceived = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String whichTheme ="";
        String iconShape = "";
        String iconSize = "";
        if (intent.hasExtra("whichTheme")) {
            whichTheme = intent.getExtras().getString("whichTheme");
            iconShape = intent.getExtras().getString("iconShape");
            iconSize = intent.getExtras().getString("iconSize");

            isBroadcastReceived = true;
        }
        List<String> packs = new ArrayList<>();
        if (whichTheme.equals("theme1")) packs.add("");
        else {
            packs.add(whichTheme);
        }
        JSONArray arr = new JSONArray(packs);
        SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences("ch.deletescape.lawnchair.dev_preferences", Context.MODE_PRIVATE).edit();
        edit.putString("pref_iconPacks", arr.toString());
        edit.apply();
        IconPackManager.Companion.getInstance(context).getPackList().onPackListUpdated(packs, true);
    }
}
