package hexlet.code.core.controllers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import hexlet.code.core.dal.UrlCheckRepository;
import hexlet.code.core.dal.UrlRepository;
import hexlet.code.core.dto.BuildUrlPage;
import hexlet.code.core.dto.UrlPage;
import hexlet.code.core.dto.UrlsPage;
import hexlet.code.core.models.Url;
import hexlet.code.core.models.UrlCheck;
import hexlet.code.core.utils.NamedRoutes;
import hexlet.code.core.utils.ViewUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.validation.ValidationException;
import org.javatuples.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static io.javalin.rendering.template.TemplateUtil.model;

public final class UrlController {

    private UrlController() {
    }

    public static void start(Context context) {
        context.render("main.jte", model("page", new BuildUrlPage()));
    }

    public static void create(Context context) {
        try {
            String name = context.formParamAsClass("url", String.class)
                    .check(Objects::nonNull, "Некорректный URL")
                    .get();
            URL url = URI.create(name).toURL();
            String urlString = buildUrlString(url);
            Url entity = new Url(urlString);

            if (UrlRepository.save(entity)) {
                ViewUtil.setFlashesInContext(context, "Страница успешно добавлена", "success");
                context.redirect(NamedRoutes.urlsPath(), HttpStatus.FOUND);
            } else {
                ViewUtil.setFlashesInContext(context, "Страница уже существует", "danger");
                context.redirect(NamedRoutes.urlsPath(), HttpStatus.FOUND);
            }
        } catch (MalformedURLException | IllegalArgumentException | ValidationException exception) {
            String name = context.formParam("url");
            BuildUrlPage page = new BuildUrlPage(name);
            ViewUtil.setFlashes(page, context, "Некорректный URL", "danger");
            context.render("main.jte", model("page", page));
        }
    }

    private static String buildUrlString(URL input) {
        String urlNoPort = String.format("%s://%s", input.getProtocol(), input.getHost());
        int port = input.getPort();
        return port == -1 ? urlNoPort : String.format("%s:%d", urlNoPort, port);
    }

    public static void index(Context context) {
        List<Pair<Url, UrlCheck>> urlTuples = new ArrayList<>();
        List<Url> urls = UrlRepository.getEntities();
        urls.forEach(u -> {
            Optional<UrlCheck> check = UrlCheckRepository.findLast(u.getId());
            check.ifPresentOrElse(c -> urlTuples.add(new Pair<>(u, c)),
                    () -> urlTuples.add(new Pair<>(u, null)));
        });
        UrlsPage page = new UrlsPage(urlTuples);
        ViewUtil.setFlashesInPage(context, page);
        context.render("urls/index.jte", model("page", page));
    }

    public static void show(Context context) {
        Long id = context.pathParamAsClass("id", Long.class).get();
        Url url = UrlRepository.find(id).orElseThrow(() ->
                new NotFoundResponse("Could not find URL :("));
        List<UrlCheck> checks = UrlCheckRepository.getChecks(id);
        UrlPage page = new UrlPage(id, url.getName(), checks);
        ViewUtil.setFlashesInPage(context, page);
        context.render("urls/show.jte", model("page", page));
    }

    public static void check(Context context) {
        Long id = context.pathParamAsClass("id", Long.class).get();
        AtomicReference<String> atomicUrl = new AtomicReference<>();
        try {
            UrlRepository.find(id).ifPresentOrElse(
                    entity -> atomicUrl.set(entity.getName()),
                    () -> {
                        throw new NotFoundResponse("Could not find URL :(");
                    }
            );
            String url = atomicUrl.get();

            HttpResponse<String> response = Unirest.get(url).asString();
            String body = response.getBody();
            Document doc = Jsoup.parse(body);

            int code = response.getStatus();
            String title = doc.title();
            Element firstSelectedH1 = doc.select("h1").first();
            String firstLevelHeader = firstSelectedH1 == null ? null : firstSelectedH1.text();
            Element firstDescription = doc.select("meta[name=description]").first();
            String description = firstDescription == null ? null : firstDescription.attr("content");
            UrlCheck check = new UrlCheck(code, title, firstLevelHeader, description);
            check.setUrlId(id);

            UrlCheckRepository.save(check);
            ViewUtil.setFlashesInContext(context, "Проверка успешно добавлена", "success");
            context.redirect(NamedRoutes.urlPath(id), HttpStatus.FOUND);
        } catch (UnirestException e) {
            ViewUtil.setFlashesInContext(context, "Проверка не удалась", "danger");
            context.redirect(NamedRoutes.urlPath(id), HttpStatus.FOUND);
        }
    }
}
