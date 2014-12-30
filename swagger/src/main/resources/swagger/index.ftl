<#-- @ftlvariable name="" type="dev.dropwizard.bundler.swagger.SwaggerView" -->
<!DOCTYPE html>
<html>
<head>
  <title>Swagger UI</title>
  <link href='//fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'/>
  <link href='${swaggerStaticPath}/css/highlight.default.css' media='screen' rel='stylesheet' type='text/css'/>
  <link href='${swaggerStaticPath}/css/screen.css' media='screen' rel='stylesheet' type='text/css'/>
  <script type="text/javascript" src="${swaggerStaticPath}/lib/shred.bundle.js"></script>
  <script src='${swaggerStaticPath}/lib/jquery-1.8.0.min.js' type='text/javascript'></script>
  <script src='${swaggerStaticPath}/lib/jquery.slideto.min.js' type='text/javascript'></script>
  <script src='${swaggerStaticPath}/lib/jquery.wiggle.min.js' type='text/javascript'></script>
  <script src='${swaggerStaticPath}/lib/jquery.ba-bbq.min.js' type='text/javascript'></script>
  <script src='${swaggerStaticPath}/lib/handlebars-1.0.0.js' type='text/javascript'></script>
  <script src='${swaggerStaticPath}/lib/underscore-min.js' type='text/javascript'></script>
  <script src='${swaggerStaticPath}/lib/backbone-min.js' type='text/javascript'></script>
  <script src='${swaggerStaticPath}/lib/swagger.js' type='text/javascript'></script>
  <script src='${swaggerStaticPath}/swagger-ui.js' type='text/javascript'></script>
  <script src='${swaggerStaticPath}/lib/highlight.7.3.pack.js' type='text/javascript'></script>
  <script type="text/javascript">
    $(function () {
      window.swaggerUi = new SwaggerUi({
      url: "${swaggerUrl}",
      dom_id: "swagger-ui-container",
      supportedSubmitMethods: ['get', 'post', 'put', 'delete'],
      onComplete: function(swaggerApi, swaggerUi){
        if(console) {
          console.log("Loaded SwaggerUI")
        }
        $('pre code').each(function(i, e) {hljs.highlightBlock(e)});
      },
      onFailure: function(data) {
        if(console) {
          console.log("Unable to Load SwaggerUI");
          console.log(data);
        }
      },
      docExpansion: "none"
    });

    $('#input_apiKey').change(function() {
      var key = $('#input_apiKey')[0].value;
      console.log("key: " + key);
      if(key && key.trim() != "") {
        console.log("added key " + key);
        window.authorizations.add("key", new ApiKeyAuthorization("api_key", key, "query"));
      }
    })
    window.swaggerUi.load();
  });

  </script>
</head>

<body>
<div id='header'>
  <div class="swagger-ui-wrap">
    <a id="logo" href="http://swagger.wordnik.com"/>

    <#list domains as x>
        <#if x[0] == "!">
            <#assign xx = x?substring(1)>
            <a id="${xx}" class="domain-selected" href="http://localhost:8090/swagger/${xx}">${xx}</a>
        <#else>
            <a id="${x}" class="domain-tabbed" href="http://localhost:8090/swagger/${x}">${x}</a>
        </#if>
    </#list>

    <form id='api_selector'>
      <div class='input'><input placeholder="http://example.com/api" id="input_baseUrl" name="baseUrl" type="text"/></div>
      <div class='input'><input placeholder="api_key" id="input_apiKey" name="apiKey" type="text"/></div>
      <div class='input'><a id="explore" href="#">Explore</a></div>
    </form>
  </div>
</div>

<div id="message-bar" class="swagger-ui-wrap">
  &nbsp;
</div>

<div id="swagger-ui-container" class="swagger-ui-wrap">

</div>

</body>

</html>
