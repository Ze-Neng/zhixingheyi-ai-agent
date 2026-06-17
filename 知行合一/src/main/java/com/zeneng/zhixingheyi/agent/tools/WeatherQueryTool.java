package com.zeneng.zhixingheyi.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
@Slf4j
public class WeatherQueryTool implements Tool {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private static final String GEOCODING_BASE = "https://geocoding-api.open-meteo.com";
    private static final String WEATHER_BASE = "https://api.open-meteo.com";
    private static final String IP_API = "http://ip-api.com/json";

    public WeatherQueryTool(WebClient.Builder builder, ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "weatherQueryTool";
    }

    @Override
    public String getDescription() {
        return "查询实时天气和天气预报。如果用户未指定城市，自动通过IP定位获取所在城市。需要日期（可选，格式 yyyy-MM-dd）。";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @org.springframework.ai.tool.annotation.Tool(
            name = "weatherQuery",
            description = "查询天气。city 参数可选：如果用户明确说了城市名则传入，如果用户没指定（如只说'今天天气怎么样'）则不传 city，系统会自动IP定位。date 参数可选：格式yyyy-MM-dd，不传默认当天。"
    )
    public String queryWeather(String city, String date) {
        // 解析日期
        String targetDate = date;
        if (targetDate != null && !targetDate.trim().isEmpty()) {
            targetDate = targetDate.trim();
            try {
                LocalDate.parse(targetDate, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                return "错误：日期格式无效，请使用 yyyy-MM-dd 格式（如 2026-06-17）";
            }
        } else {
            targetDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        // 如果没有指定城市，通过 IP 自动定位
        if (city == null || city.trim().isEmpty()) {
            IpLocation ipLocation = locateByIp();
            if (ipLocation == null) {
                return "错误：无法自动定位，请明确指定城市名（如'北京'）";
            }
            city = ipLocation.city;
            log.info("IP 自动定位: city={}, lat={}, lon={}", city, ipLocation.lat, ipLocation.lon);
            return fetchWeather(city, ipLocation.lat, ipLocation.lon, targetDate);
        }

        city = city.trim();

        // 指定了城市，通过 geocoding 获取经纬度
        double[] latLon = geocode(city);
        if (latLon == null) {
            return "错误：未找到城市「" + city + "」的坐标信息，请检查城市名是否正确（支持中英文城市名）";
        }

        return fetchWeather(city, latLon[0], latLon[1], targetDate);
    }

    /**
     * 通过 ip-api.com 获取当前 IP 的位置信息（免费，无需 API Key）
     */
    private IpLocation locateByIp() {
        try {
            String response = webClient.get()
                    .uri(IP_API + "?fields=city,lat,lon,country&lang=zh-CN")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String status = root.get("status").asText();
            if (!"success".equals(status)) {
                log.warn("IP 定位失败: status={}", status);
                return null;
            }
            String city = root.get("city").asText();
            double lat = root.get("lat").asDouble();
            double lon = root.get("lon").asDouble();
            return new IpLocation(city, lat, lon);
        } catch (Exception e) {
            log.error("IP 定位请求失败", e);
            return null;
        }
    }

    /**
     * 调用 Open-Meteo Geocoding API 将城市名转为经纬度
     */
    private double[] geocode(String city) {
        try {
            String response = webClient.get()
                    .uri(GEOCODING_BASE + "/v1/search?name={city}&count=1&language=zh&format=json", city)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");
            if (results == null || results.isEmpty()) {
                return null;
            }
            JsonNode first = results.get(0);
            double latitude = first.get("latitude").asDouble();
            double longitude = first.get("longitude").asDouble();
            log.info("地理编码成功: city={}, lat={}, lon={}", city, latitude, longitude);
            return new double[]{latitude, longitude};
        } catch (Exception e) {
            log.error("地理编码查询失败: city={}", city, e);
            return null;
        }
    }

    /**
     * 调用 Open-Meteo Weather API 获取天气数据
     */
    private String fetchWeather(String city, double lat, double lon, String date) {
        try {
            String response = webClient.get()
                    .uri(WEATHER_BASE + "/v1/forecast?latitude={lat}&longitude={lon}" +
                            "&current_weather=true" +
                            "&daily=temperature_2m_max,temperature_2m_min,weathercode" +
                            "&timezone=auto",
                            lat, lon)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode currentWeather = root.get("current_weather");
            JsonNode daily = root.get("daily");

            double currentTemp = currentWeather.get("temperature").asDouble();
            int weatherCode = currentWeather.get("weathercode").asInt();
            double windSpeed = currentWeather.get("windspeed").asDouble();
            String weatherDesc = weatherCodeToDescription(weatherCode);

            String dailyHigh = "N/A";
            String dailyLow = "N/A";
            if (daily != null && daily.has("time")) {
                for (int i = 0; i < daily.get("time").size(); i++) {
                    if (date.equals(daily.get("time").get(i).asText())) {
                        dailyHigh = daily.get("temperature_2m_max").get(i).asText() + "°C";
                        dailyLow = daily.get("temperature_2m_min").get(i).asText() + "°C";
                        break;
                    }
                }
            }

            log.info("天气查询成功: city={}, date={}, temp={}°C", city, date, currentTemp);
            return String.format(
                    "【%s】%s 天气\n" +
                    "当前温度：%.1f°C\n" +
                    "天气状况：%s\n" +
                    "风速：%.1f km/h\n" +
                    "当日最高：%s\n" +
                    "当日最低：%s",
                    city, date, currentTemp, weatherDesc, windSpeed, dailyHigh, dailyLow
            );
        } catch (Exception e) {
            log.error("天气查询失败: city={}, lat={}, lon={}", city, lat, lon, e);
            return "错误：查询天气失败 - " + e.getMessage();
        }
    }

    /**
     * WMO 天气代码转中文描述
     */
    private String weatherCodeToDescription(int code) {
        if (code == 0) return "晴朗";
        if (code <= 3) return "多云";
        if (code <= 48) return "雾/霾";
        if (code <= 57) return "毛毛雨";
        if (code <= 67) return "雨";
        if (code <= 77) return "雪";
        if (code <= 82) return "阵雨";
        if (code <= 86) return "阵雪";
        return "暴风雨/冰雹";
    }

    /**
     * IP 定位结果
     */
    private record IpLocation(String city, double lat, double lon) {}
}
