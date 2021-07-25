package com.google.android.apps.nexuslauncher;

import android.os.Bundle;
import android.support.annotation.Nullable;
import com.android.launcher3.Launcher;
import com.google.android.apps.nexuslauncher.smartspace.SmartspaceView;
import com.google.android.libraries.gsa.launcherclient.LauncherClient;

/**
 * 기본 Activity
 *
 * @see Launcher
 * @see ch.deletescape.lawnchair.LawnchairLauncher
 */
public class NexusLauncherActivity extends Launcher {
    private final NexusLauncher mLauncher;

    public NexusLauncherActivity() {
        mLauncher = new NexusLauncher(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    public LauncherClient getGoogleNow() {
        return mLauncher.mClient;
    }

    public void registerSmartspaceView(SmartspaceView smartspace) {
        mLauncher.registerSmartspaceView(smartspace);
    }
}
