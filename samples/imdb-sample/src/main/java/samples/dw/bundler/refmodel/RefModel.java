//generated using dev.dropwizard.bundler.refmodel.RefModelSerializer [Tue Dec 30 11:55:16 IST 2014]
//SHA1: 3da38507deb85e1b6ce6de1dea8c930c1d5920e8
package samples.dw.bundler.refmodel;

import dev.dropwizard.bundler.refmodel.RefPackage;

public interface RefModel {

	@RefPackage("samples.dw.bundler.model")
	public enum ImdbInfo {
		Director, 
		Title, 
		imdbID, 
		Plot, 
		Type, 
		imdbRating, 
		Year, 
		Actors, 
	}
}
