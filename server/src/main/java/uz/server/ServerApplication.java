package uz.server;

import com.moandjiezana.toml.Toml;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        String path = Arrays.stream(args).filter(s -> s.startsWith("--config=")).findFirst().orElseThrow(
                () -> new RuntimeException("Config path not provided")
        );

        Map<String, Object> props = loadToml(path.replace("--config=", ""));

        SpringApplication app = new SpringApplication(ServerApplication.class);
        app.setDefaultProperties(props);
        app.run(args);
    }

    public static Map<String, Object> loadToml(String path){
        Toml toml = new Toml().read(new File(path));
        Map<String, Object> map = new HashMap<>();
        map.put("spring.datasource.url", toml.getString("spring.datasource.url"));
        map.put("github.client-id", toml.getString("github.client-id"));
        map.put("github.client-secret", toml.getString("github.client-secret"));
        map.put("github.redirect-uri", toml.getString("github.redirect-uri"));
        map.put("server.port", toml.getString("app.port"));
        return map;
    }
}