package hcmute.edu.vn.thanh0456.zaloclone.utilities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Permissions {
    // Vérifier si des autorisations d’enregistrement ont été accordées à l’application
    public static boolean isRecordingok (Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    // Si ce n’est pas le cas, demandez l’autorisation pour l’application
    public static void requestRecording(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
    }
}
