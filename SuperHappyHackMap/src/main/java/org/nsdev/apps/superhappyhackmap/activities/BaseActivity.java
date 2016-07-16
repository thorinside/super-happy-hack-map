package org.nsdev.apps.superhappyhackmap.activities;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nealsanche on 2016-07-16.
 */

public class BaseActivity extends AppCompatActivity {
    public Map<Integer, Runnable> mPermissionsRequests = new HashMap<>();
    public Map<Integer, Runnable> mPermissionsDeniedHandlers = new HashMap<>();

    public void checkPermissions(int permissionsRequestId, String[] permissions, Runnable action, CanDisplayRationale rationaleHandler, Runnable permissionDeniedHandler) {

        Runnable requestPermissionsAction = () -> ActivityCompat.requestPermissions(this, permissions, permissionsRequestId);

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
            }
        }

        if (!allGranted) {
            boolean showRationale = false;
            for (String permission : permissions) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    showRationale = true;
                }
            }

            mPermissionsRequests.put(permissionsRequestId, action);
            mPermissionsDeniedHandlers.put(permissionsRequestId, permissionDeniedHandler);

            if (showRationale) {
                rationaleHandler.displayRationale(requestPermissionsAction);
            } else {
                requestPermissionsAction.run();
            }

            return;
        }

        action.run();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (mPermissionsRequests.containsKey(requestCode) && mPermissionsDeniedHandlers.containsKey(requestCode)) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                }
            }
            if (!allGranted) {
                mPermissionsDeniedHandlers.get(requestCode).run();
            } else {
                mPermissionsRequests.get(requestCode).run();
            }
            mPermissionsDeniedHandlers.remove(requestCode);
            mPermissionsRequests.remove(requestCode);
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public interface CanDisplayRationale {
        void displayRationale(Runnable requestPermissionsRunnable);
    }

}
