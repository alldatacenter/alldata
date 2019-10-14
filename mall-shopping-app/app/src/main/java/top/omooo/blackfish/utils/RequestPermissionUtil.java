package top.omooo.blackfish.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

/**
 * Created by SSC on 2018/3/21.
 */

public class RequestPermissionUtil {
    public static void reqPermission(final Activity activity, final String permission, String msg, final int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                new AlertDialog.Builder(activity)
                        .setTitle("提示")
                        .setMessage(msg)
                        .setPositiveButton("欣喜授权", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                // 用户同意继续申请
                                ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
                            }
                        })
                        .setNegativeButton("狠心拒绝", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                // 用户拒绝申请
                            }
                        }).show();
            } else {
                //申请权限
                ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
            }
        }
    }
}
