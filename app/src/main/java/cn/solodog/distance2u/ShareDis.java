package cn.solodog.distance2u;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import com.squareup.picasso.Picasso;


public class ShareDis extends AppCompatActivity {

    int imagewidth;
    int imageheight;
    int heightneed;
    Button save;
    TextView show;
    RelativeLayout relaout;
    static Handler myhandler;
    NotificationManager nm;
    String filename;
    static final int NOTIFICATION_ID = 0x234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_dis);
        init();
        Intent intent = getIntent();
        int dis = intent.getIntExtra("dis", 0);
        show.setText(getString(R.string.RealityDistance) + dis + getString(R.string.Meter));
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            File destDir = new File("sdcard/Pictures/distance2u/");
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
        } else {
            Toast.makeText(ShareDis.this, R.string.NoFileOperationAuthority, Toast.LENGTH_SHORT).show();
        }
        ViewTreeObserver vto = relaout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                relaout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                heightneed = relaout.getHeight();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyMMddhhmmss");
                String filen = sdf.format(new java.util.Date());
                filename = filen;
                new Thread() {
                    @Override
                    public void run() {

                        savePic(takeScreenShot(ShareDis.this), "sdcard/Pictures/distance2u/" + filename + ".png");
                    }
                }.start();
            }
        });
        myhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0x123) {
                    Toast.makeText(ShareDis.this, R.string.Saved, Toast.LENGTH_SHORT).show();
                    send();
                }
            }
        };
    }

    public void send() {
        File file = new File("sdcard/Pictures/distance2u/" + filename + ".png");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(file), "image/*");
//                    startActivity(intent);
        PendingIntent pi = PendingIntent.getActivity(ShareDis.this, 0, intent, 0);
        Notification notify1 = new Notification.Builder(this)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.notify)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.notifyb))
                .setContentTitle(getString(R.string.ImagesSaved))
                .setContentText(getString(R.string.ClickToViewImage))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pi)
                .build();
        nm.notify(NOTIFICATION_ID, notify1);
    }

    private void init() {
        show = (TextView) findViewById(R.id.resultshow);
        ImageView imageView = (ImageView) findViewById(R.id.img);
        relaout = (RelativeLayout) findViewById(R.id.relaout);
        WindowManager wm = getWindowManager();
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        imagewidth = dm.widthPixels;
        imageheight = imagewidth;
        imageView.setMinimumWidth(imagewidth);
        imageView.setMinimumHeight(imageheight);
        save = (Button) findViewById(R.id.save);
        Picasso.with(ShareDis.this).load("http://solodog.cn/Distance2U/img/back1.jpg").into(imageView);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    }

    // 获取指定Activity的截屏，保存到png文件
    private Bitmap takeScreenShot(Activity activity) {
        // View是你需要截图的View
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap b1 = view.getDrawingCache();
        // 获取状态栏高度
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;
        Log.i("TAG", "" + statusBarHeight);
        // 获取屏幕长和高

        Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, imagewidth, heightneed);
        view.destroyDrawingCache();
        return b;
    }

    // 保存到sdcard
    private static void savePic(Bitmap b, String strFileName) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(strFileName);
            if (null != fos) {
                b.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
                fos.close();
                myhandler.sendEmptyMessage(0x123);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}