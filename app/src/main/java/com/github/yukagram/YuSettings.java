package com.github.yukagram;

import android.os.SystemClock;
import androidx.annotation.Nullable;

import org.thunderdog.challegram.util.DeviceStorageError;
import org.thunderdog.challegram.Log;
import org.thunderdog.challegram.unsorted.AppContext;
import me.vkryl.leveldb.LevelDB;
import me.vkryl.core.FileUtils;
import org.drinkmore.Tracer;

import java.io.File;
import java.io.FileNotFoundException;

public class YuSettings implements LevelDB.ErrorHandler {

    public static final int VERSION = 1;

    public static final String KEY_VERSION = "version";

    public enum Toggle {
        HideConnectionStatusBar("hide_connection_status_bar", true),
        HideCameraButton("hide_camera_button", true),
        MoveArchiveToSidebar("move_archive_to_sidebar", true),
        MoveCallsToSidebar("move_calls_to_sidebar", true),
        HidePhoneNumbers("hide_phone_numbers", true),
        HideSidebarButtonInviteFriends("hide_sidebar_button_invite_friends", true),
        HideSidebarButtonNightMode("hide_sidebar_button_night_mode", true),
        HideSidebarButtonHelp("hide_sidebar_button_help", true),
        NewEmojiKeyboard("new_emoji_keyboard", true),
        HideTrending("hide_trending", true);

        private final String key;
        private final boolean defValue;

        Toggle(String key, boolean defValue) {
            this.key = key;
            this.defValue = defValue;
        }

        public boolean is() {
            return YuSettings.instance().leveldb.getBoolean(key, defValue);
        }

        public void set(boolean value) {
            YuSettings.instance().leveldb.putBoolean(key, value);
        }
    }

    private static YuSettings instance;

    public static YuSettings instance() {
        return instance;
    }

    private final LevelDB leveldb;

    public YuSettings() {
        instance = this;

        File pmcDir = new File(AppContext.get().getFilesDir(), "yupmc");
        boolean didNotExist = !pmcDir.exists();

        boolean fatalError;
        try {
            fatalError = !FileUtils.createDirectory(pmcDir);
        } catch (SecurityException e) {
            e.printStackTrace();
            fatalError = true;
        }

        if (fatalError) {
            throw new DeviceStorageError("Unable to create YuKaGrAmDb (Yukagram Kitty Addicted Grayish Absorbed Mini Data Base) working directory");
        }

        long ms = SystemClock.uptimeMillis();
        leveldb = new LevelDB(new File(pmcDir, "db").getPath(), true, this);

        int pmcVersion;
        if (didNotExist) {
            pmcVersion = VERSION;
            leveldb.putInt(KEY_VERSION, VERSION);
        } else {
            try {
                pmcVersion = Math.max(0, leveldb.tryGetInt(KEY_VERSION));
            } catch (FileNotFoundException e) {
                pmcVersion = VERSION;
                leveldb.putInt(KEY_VERSION, pmcVersion);
            }
        }

        Log.i("Opened database in %dms", SystemClock.uptimeMillis() - ms);

        Log.i("⠀⠀⠀⠀⢀⣀⣠⣤⣤⣤⣀⣀⡀⠀⠀⠀⣀⣤⡀⠀⠀⠀⢀⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀");
        Log.i("⠀⢀⣴⣾⡿⠟⠛⠋⠉⠛⠛⠛⠿⢿⣶⣾⡿⢛⣿⣷⣶⣿⠿⣷⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀");
        Log.i("⢀⣾⡿⠁⣀⣠⣤⢤⡄⠀⠀⠀⠀⠀⠀⠀⣠⣾⣿⣷⠙⠃⢰⣿⠃⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀");
        Log.i("⢼⣿⣱⣿⣿⠟⢡⣿⡇⠀⠀⣿⡇⣶⣶⡿⠟⣫⣿⣿⡄⠀⢸⣿⣶⣶⣶⣦⣀⣴⣶⣶⣦⣄⣤⣶⡶⣶⣄⠀⣀⣤⣶⣿⡿⣶⣤⣄⣠⣤⣀⠀");
        Log.i("⠀⠙⣿⣿⠃⢀⣿⣿⡇⠀⠀⣿⡇⣿⡇⠀⠸⣿⣿⣀⡀⠀⢸⡿⣛⠁⠈⢻⣿⢟⠁⠀⢙⣿⣿⡋⠀⠈⣻⣿⡿⠟⠋⣅⠀⠀⠉⠛⠿⠛⢹⣇");
        Log.i("⠀⢰⣿⡇⠀⢸⣿⣿⡇⠀⠀⣿⡇⣿⡇⠀⠀⠈⣿⣿⠀⠀⢨⣾⣿⡇⠀⠀⣿⣿⠀⠀⢸⣿⣿⡇⠀⠀⣿⣿⡇⠀⠀⣿⣿⣿⠂⠀⢹⣶⡾⠟");
        Log.i("⠀⢸⣿⠀⠀⠘⣿⣿⡇⠀⢠⣿⡇⣿⡇⢀⣤⣤⣿⣿⠀⠀⢸⣿⣿⡇⠀⠀⣿⣿⠀⠀⢸⣿⣿⡇⠀⠀⣿⣿⡇⠀⠀⣿⣿⣿⠀⠀⢸⡏⠀⠀");
        Log.i("⠀⠸⣿⣆⠀⠀⠹⣿⠃⣠⣿⣿⡇⣿⣧⣾⡿⣹⣿⣿⠀⠀⢸⣿⣿⡇⠀⠀⣿⣿⠀⠀⢸⣿⣿⡇⠀⠀⣿⣿⡇⠀⠀⣿⣿⣿⠀⠀⢸⡇⠀⠀");
        Log.i("⠀⠀⢻⣿⣄⠀⠀⠀⠘⠿⣿⣿⡇⣿⠟⠉⣰⣿⣿⣿⠀⠀⢸⣿⣿⡇⠀⠀⣿⣿⡄⠀⠸⣿⢟⣃⠀⠀⣿⣿⣇⠀⠀⠻⣿⢟⡁⠀⢸⣧⠀⠀");
        Log.i("⠀⠀⠀⠙⢿⣷⣤⣀⠀⠀⠀⠀⠀⢀⣠⣾⡿⠋⣿⣧⣄⢀⣴⣿⣿⠇⠀⣼⡿⣿⣷⡀⣠⣼⣿⣿⣄⢀⣴⣿⣿⣷⣄⢀⣴⣿⣧⡀⠀⢻⣷⠀");
        Log.i("⠀⠀⠀⠀⠀⠉⠛⠿⢿⣿⣿⣿⣿⣿⣟⣩⣤⣤⣬⣿⣿⣿⣿⢿⣋⣴⣾⣟⡁⠘⠻⠿⠟⢋⣈⣿⠿⠿⢋⣼⣿⡿⠁⠛⠛⠛⠛⠷⠄⢀⣽⡇");
        Log.i("⠀⠀⠀⠀⠀⠀⠀⢠⣾⡟⠋⠉⠉⠙⠛⠟⣫⡿⠛⣯⣿⣿⣾⡿⠟⣿⡿⠿⢿⡄⢀⣤⣾⡿⠿⠿⢿⣦⢸⣿⣯⣠⣤⣤⣤⣤⣄⣠⣴⡿⠋⠀");
        Log.i("⠀⠀⠀⠀⠀⠀⠀⢿⣿⣴⣿⣶⣶⣶⠋⣸⡟⠀⠀⣿⡏⠉⠀⠀⢸⣿⢇⣴⣿⠟⣿⡟⠉⠀⣤⣀⣼⣿⠀⠉⠛⠋⠉⠉⠉⠙⠛⠋⠁⠀⠀⠀");
        Log.i("⠀⠀⠀⠀⠀⠀⠀⠈⠉⠉⠀⢸⣿⠃⠀⣿⡇⠀⠀⣿⡇⠀⠀⢀⣼⣿⢿⣿⣧⣴⣿⡇⠀⠀⣿⣿⣿⡁⣀⣤⣾⡿⢿⣶⣄⠀⠀⠀⠀⠀⠀⠀");
        Log.i("⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⣶⣿⣿⠀⠀⣿⡇⠀⠀⣿⡇⠀⠀⢸⣿⡁⠀⠈⣿⣿⣤⡄⠀⠀⣤⣤⣿⣿⡿⠛⢩⡀⠀⠙⢿⣷⡄⠀⠀⠀⠀⠀");
        Log.i("⠀⠀⠀⠀⠀⠀⠀⠀⠀⠻⣿⣤⣄⠀⠀⢹⡇⠀⢀⣿⡇⠀⠀⠘⣿⠁⠀⠀⣿⡏⣿⡇⠀⠀⣿⡟⠉⣿⡇⠀⠀⣿⣦⡀⣠⣿⡇⠀⠀⠀⠀⠀");
        Log.i("⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢻⣟⡛⠃⠀⣾⡇⠀⣼⣿⠁⠀⠀⠀⣿⠀⠀⠀⣿⡇⣿⡇⠀⠀⣿⡇⠀⣿⡇⠀⠰⣟⣵⣾⠿⠋⠀⠀⠀⠀⠀⠀");
        Log.i("⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⢿⣿⡆⢠⡿⢁⣼⣿⣷⣦⣄⣀⢀⣿⠀⠀⠀⣿⡇⣿⡇⠀⠀⣿⣧⢀⣿⡇⠀⠀⣿⡟⣁⣀⣀⠀⠀⠀⠀⠀⠀");
        Log.i("⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⣾⡿⢠⠟⠒⠋⠉⠉⠉⠉⠻⠿⢿⣿⠀⠀⠀⠿⣿⣿⠁⠀⠀⠛⠿⢟⣿⠇⠀⠀⠻⠿⠿⣛⣿⠁⠀⠀⠀⠀⠀");
        Log.i("⠀⠀⠀⠀⠀⠀⠀⠀⠀⣾⡿⣋⣤⣴⣶⣶⣶⣶⣦⣄⡀⢀⣴⣿⣿⣷⣄⣠⣾⡿⠿⣿⣶⣤⣀⣴⣿⢿⣶⣦⣄⣀⣴⣾⡿⠋⠀⠀⠀⠀⠀⠀");
        Log.i("⠀⠀⠀⠀⠀⠀⠀⠀⠀⠛⠻⠿⠛⠉⠉⠀⠀⠉⠙⠻⠿⠿⠟⠁⠈⠛⠛⠛⠋⠀⠀⠀⠉⠛⠛⠛⠁⠀⠉⠙⠛⠛⠛⠁⠀⠀⠀⠀⠀⠀⠀⠀");

        Log.i("Yusettings version: %d", pmcVersion);
    }

    @Override
    public boolean onFatalError (LevelDB levelDB, Throwable error) {
        Tracer.onDatabaseError(error);
        return true;
    }

    @Override
    public void onError (LevelDB levelDB, String message, @Nullable Throwable error) {
        Log.e(Log.LOG_TAG, message, error);
    }
}
