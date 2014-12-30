package samples.dw.bundler.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wordnik.swagger.annotations.ApiModel;
import dev.dropwizard.bundler.elastic.Elastic;
import dev.dropwizard.bundler.redis.Redis;
import dev.dropwizard.bundler.refmodel.Id;

import javax.xml.bind.annotation.XmlRootElement;

/**
*
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("IMDB media record")
@Redis
@Elastic
public class ImdbInfo {
    public String Title;
    public Integer Year;
    public String Director;
    public String Actors;
    public String Plot;
    public Float imdbRating;
    @Id public String imdbID;
    public String Type;
}
