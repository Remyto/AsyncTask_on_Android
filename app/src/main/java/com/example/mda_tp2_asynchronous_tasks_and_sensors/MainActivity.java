package com.example.mda_tp2_asynchronous_tasks_and_sensors;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity{

    private EditText et_username, et_password;
    private Button b_authenticate;
    private TextView tv_result;
    private String result;

    private Button b_getAnImage;
    private GetImageOnClickListener myGetImageOnClickListener;
    private ImageView i_image;

    private Button b_listActivity;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_username = findViewById(R.id.et_username);
        et_password = findViewById(R.id.et_pwd);
        b_authenticate = findViewById(R.id.b_authentificate);
        tv_result = findViewById(R.id.tv_result);

        b_getAnImage = findViewById(R.id.b_getAnImage);
        myGetImageOnClickListener = new GetImageOnClickListener();
        b_getAnImage.setOnClickListener(myGetImageOnClickListener);
        i_image = findViewById(R.id.image);

        b_listActivity = findViewById(R.id.b_listActivity);
        b_listActivity.setOnClickListener(v -> {
                Intent intent = new Intent(this, ListActivity.class);
                startActivity(intent);
        });

    }


    private String readStream (InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();

            int i = is.read();
            while(i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void AuthenticateOnClickListener (View myView) {

        Thread t = new Thread(){
            @Override
            public void run() {
                URL url = null;
                HttpURLConnection urlConnection;

                String username, password, sendMsg;
                String basicAuth;


                /// Establish connection
                try {
                    //url = new URL("http://www.android.com/");
                    //url = new URL("");
                    url = new URL("https://httpbin.org/basic-auth/bob/sympa");
                    urlConnection = (HttpURLConnection) url.openConnection();

                    username = et_username.getText().toString();
                    password = et_password.getText().toString();
                    sendMsg = username + ":" + password;


                    //basicAuth = "Basic " + Base64.encodeToString("bob:sympa".getBytes(), Base64.NO_WRAP);
                    basicAuth = "Basic " + Base64.encodeToString(sendMsg.getBytes(), Base64.NO_WRAP);
                    urlConnection.setRequestProperty ("Authorization", basicAuth);

                    /// Get result
                    try {
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        result = readStream(in);
                        Log.i("JFL", result);
                    } finally {
                        urlConnection.disconnect();

                        runOnUiThread(new Runnable() {
                            Boolean res = false;


                            @Override
                            public void run() {
                                try {
                                    if (result != null) {
                                        JSONObject jsObj = new JSONObject(result);
                                        res = jsObj.getBoolean("authenticated");

                                        b_authenticate.setBackgroundColor(Color.rgb(0, 255, 0));
                                    }
                                    else {
                                        b_authenticate.setBackgroundColor(Color.rgb(255, 0, 0));
                                    }

                                    tv_result.setText(res.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch(MalformedURLException e) {
                    e.printStackTrace();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        };

        t.start();
    }





    /// New Flickr Activity, get image

    class GetImageOnClickListener implements View.OnClickListener {


        @Override
        public void onClick(View v) {
            AsyncFlickrJSONData asyncTask = new AsyncFlickrJSONData();
            asyncTask.execute("https://www.flickr.com/services/feeds/photos_public.gne?tags=cats&format=json");
        }
    }

    class AsyncFlickrJSONData extends AsyncTask <String, Void, JSONObject> {

        private String readStream (InputStream is) {
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();

                int i = is.read();
                while(i != -1) {
                    bo.write(i);
                    i = is.read();
                }
                return bo.toString();

            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            /// Perform the HTTP connection, and re-instantiate the JSON object

            URL url;
            HttpURLConnection urlConnection;

            InputStream in;
            String result;

            JSONObject jsObj=null;


            /// Establish connection
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();


                /// Get result
                try {
                    in = new BufferedInputStream(urlConnection.getInputStream());
                    result = readStream(in);

                    /// File starts with "jsonFlickrFeed(" and ends with ")", which we need to delete
                    result = result.substring("jsonFlickrFeed(".length(), result.length() - 1);

                    jsObj = new JSONObject(result);
                    return jsObj;


                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }

            } catch(MalformedURLException e) {
                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            }

            return jsObj;
        }

        @Override
        protected void onPostExecute(JSONObject jsObj) {
            /// Log the obtained JSON object in Logcat & change the imageView

            try {
                /// Log JSON in Logcat
                Log.i("JSONObject", jsObj.toString());
                JSONArray jArray = jsObj.getJSONArray("items");

                /// Get url of an Image
                int rdn = (int) (Math.random() * (jArray.length()-1));
                String test = ""+rdn;
                Log.i("rdn", test);

                JSONObject media = jArray.getJSONObject(rdn).getJSONObject("media");
                Log.i("url", media.toString());


                AsyncBitmapDownloader bitmapDownloader = new AsyncBitmapDownloader();
                bitmapDownloader.execute(media.getString("m"));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    class AsyncBitmapDownloader extends AsyncTask<String, Void, Bitmap> {


        private String readStream (InputStream is) {
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();

                int i = is.read();
                while(i != -1) {
                    bo.write(i);
                    i = is.read();
                }
                return bo.toString();

            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }


        @Override
        protected Bitmap doInBackground(String... strings) {
            /// Connect and get a bitmap of the image

            URL url;
            HttpURLConnection urlConnection;

            InputStream in;
            Bitmap bm = null;


            try {
                Log.i("received url", strings[0]);
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                in = new BufferedInputStream(urlConnection.getInputStream());
                bm = BitmapFactory.decodeStream(in);

                in.close();
                urlConnection.disconnect();


                Log.i("in =", in.toString());

                Boolean bool;
                if(in != null) bool=true;
                else bool=false;
                Log.i("in?", bool.toString());

                if(bm != null) bool=true;
                else bool=false;
                Log.i("bm?", bool.toString());


            } catch (MalformedURLException e) {
                Log.i("url Err", "url Err");
                e.printStackTrace();
            } catch (IOException e) {
                Log.i("urlConnection Err", "urlConnection Err");
                e.printStackTrace();
            }


            return bm;
        }


        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Log.i("onPostExecute", "reached");
            Log.i("bitmap", bitmap.toString());

            i_image.setImageBitmap(bitmap);
        }
    }

}