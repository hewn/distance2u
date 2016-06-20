package cn.solodog.distance2u;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ResponseDelivery;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;

public class About extends AppCompatActivity {
    RequestQueue mQueue;
    String ab;
    Handler myhandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        final TextView aboutshow = (TextView) findViewById(R.id.aboutcontent);
        mQueue = Volley.newRequestQueue(About.this);
        myhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0x123) {
                    aboutshow.setText(ab);
                }
            }
        };

        myStringRequest sr = new myStringRequest("http://solodog.cn/distance2u/distance2uabout.html",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response != "") {
                            ab = response;
                            myhandler.sendEmptyMessage(0x123);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }
        );
        mQueue.add(sr);


    }

    public class myStringRequest extends StringRequest {

        public myStringRequest(String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
            super(url, listener, errorListener);
        }

        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            String parsed;
            String returnw;
            try {
                parsed = new String(response.data, "utf-8");
            } catch (UnsupportedEncodingException e) {
                parsed = new String(response.data);
            }
            return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
        }
    }
}

