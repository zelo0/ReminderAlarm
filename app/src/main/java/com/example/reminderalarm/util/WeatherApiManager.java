package com.example.reminderalarm.util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.example.reminderalarm.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class WeatherApiManager extends AsyncTask<String, Void, WeatherApiManager.WeatherResponse> {
    private static final String WEATHER_API_ENDPOINT = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";
    private static final String API_KEY = BuildConfig.WEATHER_API_KEY;

    public static int TO_GRID = 0;
    public static int TO_GPS = 1;

    // 강수 확률 몇 % 이상이면 우산 챙기라 할 지
    public static int RAIN_DROP_BASE_PERCENT = 50;

    private Context applicationContext;

    private String dateString;
    private String currentHourString;
    private double lat;
    private double lng;

    private String madeApiURL;
    private WeatherResponse weatherResponse;

    /* dateString: 오늘 날짜 (20220708 형태), currentHourString: 0200 형태, lat: 위도, lng: 경도  */
    public WeatherApiManager(Context applicationContext, String dateString, String currentHourString, double lat, double lng) {
        this.applicationContext = applicationContext;
        this.dateString = dateString;
        this.currentHourString = currentHourString;
        this.lat = lat;
        this.lng = lng;
    }


    /* api에 패러미터 추가해서 요청할 url 만들기 */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        try {
            madeApiURL = makeApiURL(dateString, lat, lng);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /* url로 요청 보내기 */
    @Override
    protected WeatherResponse doInBackground(String... strings) {
        try {
            String apiResponse = fetchWeatherApi(madeApiURL);
            weatherResponse = makeWeatherResponse(apiResponse, dateString, currentHourString);
            return weatherResponse;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


    /* 날씨 정보를 얻어온 후 getWeatherResponse 호출 */
    public WeatherResponse getWeatherResponse() {
        return weatherResponse;
    }

    /*  @Nullable
    public WeatherResponse requestWeather(String dateString, String currentHourString, double lat, double lng) {
        try {

            String apiResult;



        } catch (Exception e) {
            System.out.println("e = " + e);
            Toast.makeText(applicationContext, "날씨 정보를 받아올 수 없습니다", Toast.LENGTH_LONG).show();
            return null;
        }
    }*/

    @Nullable
    private WeatherResponse makeWeatherResponse(String apiResponse, String dateString, String currentHourString) throws JSONException {
        /* 응답 JSON 파싱 */
        JSONObject jsonObject = new JSONObject(apiResponse);
        JSONObject response = jsonObject.getJSONObject("response");
        String resultCode = response.getJSONObject("header").getString("resultCode");
        // 응답 코드가 오류 코드면
        if (!resultCode.equals("00")) {
            Toast.makeText(applicationContext, "날씨 정보를 받아올 수 없습니다", Toast.LENGTH_LONG).show();
            return null;
        }

        /* 반환해줄 날씨 정보 */
        WeatherResponse weatherResponse = new WeatherResponse();

        JSONArray itemArr = response.getJSONObject("body").getJSONObject("items").getJSONArray("item");

        for (int i = 0; i < itemArr.length(); i++) {
            JSONObject itemObject = itemArr.getJSONObject(i);
            String forecastDate = itemObject.getString("fcstDate");
            // 오늘에 대한 예보
            // 나중에는 정확한 개수를 찾아 numOfRows로 넘겨주자
            if (forecastDate.equals(dateString)) {
                String category = itemObject.getString("category");

                /* 최저, 최고 기온 */
                if (category.equals("TMN")) {
                    weatherResponse.setMinTemperature(itemObject.getString("fcstValue"));
                } else if (category.equals("TMX")) {
                    weatherResponse.setMaxTemperature(itemObject.getString("fcstValue"));
                }

                String fcstTime = itemObject.getString("fcstTime");
                /* 현재 시간의 기온 */
                if (fcstTime.equals(currentHourString) && category.equals("TMP")) {
                    weatherResponse.setCurrentTemperature(itemObject.getString("fcstValue"));
                }

                /* 현재 시간 이후로 오늘 강수 확률이 50% 이상인 시간이 있나 확인 */
                if (fcstTime.compareTo(currentHourString) >= 0 && category.equals("POP")) {
                    if (Integer.parseInt(itemObject.getString("fcstValue")) >= RAIN_DROP_BASE_PERCENT) {
                        weatherResponse.setWillRain(true);
                    }
                }

            }
        }

        return weatherResponse;
    }


    @NonNull
    private String fetchWeatherApi(String madeApiURL) throws IOException {

        /* api 요청하고 응답 받기 */
        URL url = new URL(madeApiURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();
        String apiResult = sb.toString();

        return apiResult;
    }

    @NonNull
    private String makeApiURL(String dateString, double lat, double lng) throws UnsupportedEncodingException {
        /* 위도, 경도를 좌표로 변환 */
        LatXLngY latXLngY = convertGRID_GPS(TO_GRID, lat, lng);
        Log.i("GRID", "x 좌표: " + Integer.toString((int) latXLngY.getX()) + "y 좌표: " + Integer.toString((int) latXLngY.getY()));

        /* api 요청 설정 */
        // 날짜, 예보지점 변경 필요!
        StringBuilder urlBuilder = new StringBuilder(WEATHER_API_ENDPOINT);
        urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8") + "=" + URLEncoder.encode(API_KEY, "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("pageNo", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")); /*페이지번호*/
        urlBuilder.append("&" + URLEncoder.encode("numOfRows", "UTF-8") + "=" + URLEncoder.encode("1000", "UTF-8")); /*한 페이지 결과 수*/
        urlBuilder.append("&" + URLEncoder.encode("dataType", "UTF-8") + "=" + URLEncoder.encode("JSON", "UTF-8")); /*요청자료형식(XML/JSON) Default: XML*/
        urlBuilder.append("&" + URLEncoder.encode("base_date", "UTF-8") + "=" + URLEncoder.encode(dateString, "UTF-8")); /*오늘 발표*/
        urlBuilder.append("&" + URLEncoder.encode("base_time", "UTF-8") + "=" + URLEncoder.encode("0200", "UTF-8")); /*02시 발표(정시단위) */
        urlBuilder.append("&" + URLEncoder.encode("nx", "UTF-8") + "=" + URLEncoder.encode(Integer.toString((int) latXLngY.getX()), "UTF-8")); /*예보지점의 X 좌표값*/
        urlBuilder.append("&" + URLEncoder.encode("ny", "UTF-8") + "=" + URLEncoder.encode(Integer.toString((int) latXLngY.getY()), "UTF-8")); /*예보지점의 Y 좌표값*/

        return urlBuilder.toString();
    }


    public class WeatherResponse {
        private String minTemperature; // 오늘 최저 기온
        private String maxTemperature; // 오늘 최고 기온
        private String currentTemperature; // 현재 기온
        private boolean willRain = false; // 현재 시간부터 오늘이 끝날 때까지 매 시간별 강수 확률 중 50% 이상이 있다면 true

        public WeatherResponse() {
        }

        public String getMinTemperature() {
            return minTemperature;
        }

        public void setMinTemperature(String minTemperature) {
            this.minTemperature = minTemperature;
        }

        public String getMaxTemperature() {
            return maxTemperature;
        }

        public void setMaxTemperature(String maxTemperature) {
            this.maxTemperature = maxTemperature;
        }

        public String getCurrentTemperature() {
            return currentTemperature;
        }

        public void setCurrentTemperature(String currentTemperature) {
            this.currentTemperature = currentTemperature;
        }

        public boolean getWillRain() {
            return willRain;
        }

        public void setWillRain(boolean willRain) {
            this.willRain = willRain;
        }

        @Override
        public String toString() {
            return "WeatherResponse{" +
                    "minTemperature='" + minTemperature + '\'' +
                    ", maxTemperature='" + maxTemperature + '\'' +
                    ", currentTemperature='" + currentTemperature + '\'' +
                    ", willRain=" + willRain +
                    '}';
        }
    }


    /* https://gist.github.com/fronteer-kr/14d7f779d52a21ac2f16 */
    private LatXLngY convertGRID_GPS(int mode, double lat_X, double lng_Y) {
        double RE = 6371.00877; // 지구 반경(km)
        double GRID = 5.0; // 격자 간격(km)
        double SLAT1 = 30.0; // 투영 위도1(degree)
        double SLAT2 = 60.0; // 투영 위도2(degree)
        double OLON = 126.0; // 기준점 경도(degree)
        double OLAT = 38.0; // 기준점 위도(degree)
        double XO = 43; // 기준점 X좌표(GRID)
        double YO = 136; // 기1준점 Y좌표(GRID)

        //
        // LCC DFS 좌표변환 ( code : "TO_GRID"(위경도->좌표, lat_X:위도,  lng_Y:경도), "TO_GPS"(좌표->위경도,  lat_X:x, lng_Y:y) )
        //


        double DEGRAD = Math.PI / 180.0;
        double RADDEG = 180.0 / Math.PI;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);
        LatXLngY rs = new LatXLngY();

        if (mode == TO_GRID) {
            rs.lat = lat_X;
            rs.lng = lng_Y;
            double ra = Math.tan(Math.PI * 0.25 + (lat_X) * DEGRAD * 0.5);
            ra = re * sf / Math.pow(ra, sn);
            double theta = lng_Y * DEGRAD - olon;
            if (theta > Math.PI) theta -= 2.0 * Math.PI;
            if (theta < -Math.PI) theta += 2.0 * Math.PI;
            theta *= sn;
            rs.x = Math.floor(ra * Math.sin(theta) + XO + 0.5);
            rs.y = Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
        } else {
            rs.x = lat_X;
            rs.y = lng_Y;
            double xn = lat_X - XO;
            double yn = ro - lng_Y + YO;
            double ra = Math.sqrt(xn * xn + yn * yn);
            if (sn < 0.0) {
                ra = -ra;
            }
            double alat = Math.pow((re * sf / ra), (1.0 / sn));
            alat = 2.0 * Math.atan(alat) - Math.PI * 0.5;

            double theta = 0.0;
            if (Math.abs(xn) <= 0.0) {
                theta = 0.0;
            } else {
                if (Math.abs(yn) <= 0.0) {
                    theta = Math.PI * 0.5;
                    if (xn < 0.0) {
                        theta = -theta;
                    }
                } else theta = Math.atan2(xn, yn);
            }
            double alon = theta / sn + olon;
            rs.lat = alat * RADDEG;
            rs.lng = alon * RADDEG;
        }
        return rs;
    }


    class LatXLngY {
        public double lat;
        public double lng;

        public double x;
        public double y;

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }
}
