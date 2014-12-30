package dev.dropwizard.bundler.swagger;

import com.google.common.base.Charsets;
import io.dropwizard.views.View;

import java.util.Set;

public class SwaggerView extends View {

    private SwaggerBundle.Context ctx;
    private final SwaggerBundle swaggerBundle;
    private final String domain;

    public SwaggerView(SwaggerBundle.Context ctx, SwaggerBundle swaggerBundle, String domain) {
        super("/swagger/index.ftl", Charsets.UTF_8);
        this.ctx = ctx;
        this.swaggerBundle = swaggerBundle;
        this.domain = domain;
    }

    public String getSwaggerStaticPath() {
        if (usingRootPath()) {
            return SwaggerBundle.PATH;
        }
        return getContextPath() + SwaggerBundle.PATH;
    }

    public String getContextPath() {
        if (usingRootPath()) {
            return "";
        }
        return ctx.swaggerBasePath;
    }

    public String getSwaggerUrl() {
        return getContextPath() + "/api-docs/" + ctx.domain;
    }

    public String getDomain() {
        return ctx.domain;
    }

    public String[] getDomains() {
        Set<String> keySet = swaggerBundle.swaggerContextMap.keySet();
        String[] domains = new String[keySet.size()];
        int i = 0;
        for (String s : keySet) {
            domains[i++] = (s.equals(domain) ? "!" : "") + s;
        }
        return domains;
    }

    private boolean usingRootPath() {
        return ctx.swaggerBasePath.equals("/");
    }
}
