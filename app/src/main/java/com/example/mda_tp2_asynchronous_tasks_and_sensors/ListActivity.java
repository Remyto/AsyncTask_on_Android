package com.example.mda_tp2_asynchronous_tasks_and_sensors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;

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
import java.util.Vector;

public class ListActivity extends AppCompatActivity {

    private Button b_getList;
    private MyAdapter adapter;
    private ListView l_list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list2);


        l_list = findViewById(R.id.l_list);
        adapter = new MyAdapter();
        b_getList = findViewById(R.id.b_getList);
        b_getList.setOnClickListener(v -> {
            l_list.setAdapter(adapter);
            AsyncFlickrJSONDataForList task = new AsyncFlickrJSONDataForList(adapter);
            task.execute("https://www.flickr.com/services/feeds/photos_public.gne?tags=cats&format=json");
        });

    }






    class MyAdapter extends BaseAdapter {

        private Vector <String> vect;    /// Stores the url from JSONObject


        public MyAdapter () {
            vect = new Vector<>();
        }


        public void add(String url) {
            this.vect.add(url);
        }

        public int getCount() {
            return vect.size();
        }

        @Override
        public Object getItem(int position) {
            return vect.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            /// Display list of images' url
            /*if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.textviewlayout, parent, false);
            }
            ((TextView) convertView.findViewById(R.id.tv_url)).setText((String) getItem(position));*/

            /// Display images
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.bitmaplayout, parent, false);
            }
            ImageView img = (ImageView) convertView.findViewById(R.id.i_imageViewForList);
            Response.Listener<Bitmap> rep_listener = response -> img.setImageBitmap(response.copy(Bitmap.Config.RGB_565, false));
            ImageRequest imgRequest = new ImageRequest(
                    (String) getItem(position),
                    rep_listener,
                    0,
                    0,
                    img.getScaleType(),
                    Bitmap.Config.RGB_565,
                    null
            );
            MySingleton.getInstance(parent.getContext()).addToRequestQueue(imgRequest);


            return convertView;
        }

    }


    class AsyncFlickrJSONDataForList extends AsyncTask<String, Void, JSONObject> {

        private MyAdapter adapter;


        public AsyncFlickrJSONDataForList (MyAdapter adapter) {
            this.adapter = adapter;
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

            try {
                /// Log JSON in Logcat
                Log.i("JSONObject", jsObj.toString());
                JSONArray jArray = jsObj.getJSONArray("items");

                /// Get url of each Image in JS array and place in adapter
                for (int j=0; j<jArray.length()-1; j++) {
                    String url = jArray.getJSONObject(j).getJSONObject("media").getString("m");
                    adapter.add(url);
                    Log.i("JFL", "Added to adapter url : " + url);
                }
                adapter.notifyDataSetChanged();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

}