@import hexlet.code.utils.NamedRoutes
@import hexlet.code.dto.BuildUrlPage

@param BuildUrlPage page

@template.layout.page(
page = page,
content = @`
    <div class="container-fluid bg-white p-5">
        <div class="row">
            <div class="col-md-10 col-lg-8 mx-auto text-black">
                <h1 class="display-3 mb-0">Анализатор страниц</h1>
                <p class="lead">Бесплатно проверяйте сайты на SEO пригодность</p>
                <form action="${NamedRoutes.urlsPath()}" method="post" class="rss-form text-body">
                    <div class="row">
                        <div class="col">
                            <div class="form-floating">
                                <input id="url-input" autofocus="" type="url" required="" name="url" aria-label="url"
                                       class="form-control w-100" placeholder="URL" autocomplete="off"
                                       value="${page.getName()}" pattern="https?://(www\.)?\S+\.\S+">
                                <label for="url-input">URL</label>
                            </div>
                        </div>
                        <div class="col-auto">
                            <button type="submit" class="h-100 btn btn-lg btn-primary px-sm-5">Проверить</button>
                        </div>
                    </div>
                </form>
                <p class="mt-2 mb-0 text-muted">Only http[s]://example.com[:port] is allowed</p>
            </div>
        </div>
    </div>
`
)