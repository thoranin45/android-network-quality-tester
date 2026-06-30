package com.example.signalxpert.servers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GetSpeedTestHostsHandler extends Thread {
    private final HashMap<Integer, String> mapKey = new HashMap<>();
    private final HashMap<Integer, List<String>> mapValue = new HashMap<>();
    private double selfLat = 0.0;
    private double selfLon = 0.0;
    private boolean finished = false;
    private final String BASE_URL_SPEEDTEST = "https:///www.speedtest.net/";
    private final String BASE_URL_SPIRANOID = "https://sparanoid.com/";

    private SpeedTestService createService(String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(SpeedTestService.class);
    }

    @Override
    public void run() {
        fetchSpeedTestServers();
        fetchSparanoidServers();
        finished = true;
    }

    private void fetchSpeedTestServers() {
        SpeedTestService service = createService(BASE_URL_SPEEDTEST);

        // Get latitude, longitude
        try {
            Call<ResponseBody> configCall = service.getConfig(BASE_URL_SPEEDTEST + "speedtest-config.php");
            ResponseBody configResponse = configCall.execute().body();
            if (configResponse != null) {
                String configXml = configResponse.string();
                Document doc = Jsoup.parse(configXml, "", org.jsoup.parser.Parser.xmlParser());
                selfLat = Double.parseDouble(doc.select("client").attr("lat"));
                selfLon = Double.parseDouble(doc.select("client").attr("lon"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Get servers
        try {
            Call<ResponseBody> serversCall = service.getServers(BASE_URL_SPEEDTEST + "speedtest-servers-static.php");
            ResponseBody serversResponse = serversCall.execute().body();
            if (serversResponse != null) {
                String serversXml = serversResponse.string();
                Document doc = Jsoup.parse(serversXml, "", org.jsoup.parser.Parser.xmlParser());
                int count = mapKey.size();
                for (Element server : doc.select("server")) {
                    String uploadAddress = server.attr("url");
                    String lat = server.attr("lat");
                    String lon = server.attr("lon");
                    String name = server.attr("name");
                    String country = server.attr("country");
                    String cc = server.attr("cc");
                    String sponsor = server.attr("sponsor");
                    String host = server.attr("host");

                    List<String> ls = Arrays.asList(lat, lon, name, country, cc, sponsor, host);
                    mapKey.put(count, uploadAddress);
                    mapValue.put(count, ls);
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchSparanoidServers() {
        try {
            Document doc = Jsoup.connect(BASE_URL_SPIRANOID + "lab/speedtest-list/").get();
            Elements rows = doc.select("table tbody tr");
            int count = mapKey.size();
            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() < 8) continue; // Skip incomplete rows

                String lat = cols.get(4).text();
                String lon = cols.get(5).text();
                String name = cols.get(0).text();
                String country = cols.get(1).text();
                String sponsor = cols.get(3).text();
                String uploadAddress = cols.get(6).text();
                String host = cols.get(7).text();

                List<String> ls = Arrays.asList(lat, lon, name, country, "", sponsor, host);
                mapKey.put(count, uploadAddress);
                mapValue.put(count, ls);
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<Integer, String> getMapKey() {
        return mapKey;
    }

    public HashMap<Integer, List<String>> getMapValue() {
        return mapValue;
    }

    public double getSelfLat() {
        return selfLat;
    }

    public double getSelfLon() {
        return selfLon;
    }

    public boolean isFinished() {
        return finished;
    }
}

interface SpeedTestService {
    @retrofit2.http.GET
    Call<ResponseBody> getConfig(@retrofit2.http.Url String url);

    @retrofit2.http.GET
    Call<ResponseBody> getServers(@retrofit2.http.Url String url);
}

