package hexlet.code;

import hexlet.code.url.UrlRepository;
import hexlet.code.url.Url;
import hexlet.code.utils.NamedRoutes;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class UrlControllerTest {
    Javalin app;
    static MockWebServer mockWebServer;
    final String correctTestUrl1 = "https://metanit.com";
    final String correctTestUrl2 = "https://123.org";
    final String correctTestUrl3 = "https://google.com";
    final String correctTestUrl4 = "https://github.com";
    final String invalidUrl = "ht:/hexlet.test";
    static String fixturePath = "src/test/resources/test.html";
    static String testBody;

    @BeforeAll
    public static void setUpMock() throws IOException {
        mockWebServer = new MockWebServer();
        testBody = Files.readString(Paths.get(fixturePath).toAbsolutePath().normalize());
    }

    @AfterAll
    public static void shutdownMock() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    public final void setUp() {
        app = App.getApp();
    }

    @Test
    void testMainPage() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get(NamedRoutes.rootPath());
            Assertions.assertEquals(200, response.code());
        });
    }

    @Test
    void testCreateUrl() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=" + correctTestUrl1)) {
                Assertions.assertTrue(UrlRepository.findByName(correctTestUrl1).isPresent());
                var body = response.body().string();
                Assertions.assertEquals(200, response.code());
                Assertions.assertTrue(body.contains(correctTestUrl1));
            }
            try (var response = client.post(NamedRoutes.urlsPath(), "url=" + invalidUrl)) {
                Assertions.assertTrue(UrlRepository.findByName(invalidUrl).isEmpty());
                var body = response.body().string();
                Assertions.assertEquals(200, response.code());
                Assertions.assertTrue(body.contains("Некорректный URL"));
            }
            try (var response = client.post(NamedRoutes.urlsPath())) {
                var body = response.body().string();
                Assertions.assertEquals(200, response.code());
                Assertions.assertTrue(body.contains("Некорректный URL"));
            }
        });
    }


    @Test
    void testShowUrls() {
        JavalinTest.test(app, (server, client) -> {
            UrlRepository.save(new Url(correctTestUrl1));
            UrlRepository.save(new Url(correctTestUrl2));
            UrlRepository.save(new Url(correctTestUrl3));

            Assertions.assertTrue(UrlRepository.findByName(correctTestUrl2).isPresent());
            Assertions.assertTrue(UrlRepository.findByName(correctTestUrl1).isPresent());
            Assertions.assertTrue(UrlRepository.findByName(correctTestUrl3).isPresent());

            var response = client.get(NamedRoutes.urlsPath());
            var body = response.body().string();
            Assertions.assertEquals(200, response.code());
            Assertions.assertTrue(body.contains(correctTestUrl1));
            Assertions.assertTrue(body.contains(correctTestUrl2));
            Assertions.assertTrue(body.contains(correctTestUrl3));
        });
    }


    @Test
    void testShowUrl() {
        JavalinTest.test(app, (server, client) -> {
            var entity = new Url(correctTestUrl4);
            UrlRepository.save(entity);
            Assertions.assertTrue(UrlRepository.findByName(correctTestUrl4).isPresent());
            var response = client.get(NamedRoutes.urlPath(entity.getId()));
            var body = response.body().string();
            Assertions.assertEquals(200, response.code());
            Assertions.assertTrue(body.contains(correctTestUrl4));

            var response1 = client.get(NamedRoutes.urlPath("42"));
            Assertions.assertEquals(404, response1.code());
        });
    }

    @Test
    void testCheckUrl() throws IOException {
        MockResponse response = new MockResponse()
                .setResponseCode(302)
                .setBody(testBody);
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);
        mockWebServer.start();

        JavalinTest.test(app, ((server, client) -> {
            var entity = new Url(mockWebServer.url("/").toString());
            UrlRepository.save(entity);
            Assertions.assertTrue(UrlRepository.findById(entity.getId()).isPresent());
            try (var req = client.post(NamedRoutes.urlCheckPath(entity.getId()), "url=" + entity.getName())) {
                var body = req.body().string();
                Assertions.assertTrue(body.contains("test"));
                Assertions.assertTrue(body.contains("302"));
            }
            try (var req = client.post(NamedRoutes.urlCheckPath(entity.getId()))) {
                Assertions.assertTrue(UrlRepository.findById(entity.getId()).isPresent());
                var body = req.body().string();
                Assertions.assertTrue(body.contains("test"));
                Assertions.assertTrue(body.contains("302"));
            }
        }));
    }

}
