package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leo on 2017/7/19.
 */

public class FlickrFetcher {

    private static final String TAG = "FlickrFetchr";

    private static final String API_KEY = "80aa62163f93e98070e9bfed539bfcfe";


    //通过指定URL获取原始数据，并返回一个字节流数组。
    public byte[] getUrlBytes(String urlSpec)throws IOException{

        //根据传入的字符串参数，创建一个URL对象
        URL url = new URL(urlSpec);
        //通过url.openConnection()方法得到HttpUrlConnection对象。
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
           /*
           *  虽然 HttpURLConnection 对象提供了一个连接，但只有在调用 getInputStream() 方法时
           *   （如果是POST请求，则调用 getOutputStream() 方法），它才会真正连接到指定的URL地址。
            */
            InputStream in = connection.getInputStream();

            //如果连接失败就抛出错误
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + ": with" + urlSpec);
            }

            //写入
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) > 0){
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();
        }finally {
            connection.disconnect();
        }
    }

    //将getUrlBytes(String)方法返回的结果转换成String
    public String getUrlString(String urlSpec)throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems(){

        List<GalleryItem> items = new ArrayList<>();

        try{
            //原地址为：https://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=80aa62163f93e98070e9bfed539bfcfe&format=json&nojsoncallback=1%E3%80%82
            //构建完整的Flickr API请求URL。
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    //appendQueryParameter()方法可自动转义查询字符串。
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    //添加了一个值为url_s的extras参数，这个参数告诉Flickr:如有小尺寸图片，也一并返回其URL。
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON" + jsonString);
            //
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items,jsonBody);
        }
        catch (JSONException je){
            Log.e(TAG,"Failed to parse JSON",je );
        }catch (IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        }

        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)throws IOException,JSONException{

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for(int i=0; i<photoJsonArray.length(); i++){
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();

            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            //并不是每个图片都有对应的url_s连接，所以需要添加一个检查。
            if(!photoJsonObject.has("url_s")){
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }
}
