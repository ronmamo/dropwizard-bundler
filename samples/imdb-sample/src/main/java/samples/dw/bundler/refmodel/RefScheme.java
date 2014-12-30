//generated using dev.dropwizard.bundler.refmodel.RefModelSerializer [Tue Dec 30 11:55:16 IST 2014]
//SHA1: ec51a656eed817ff8ffd09f7461a75805d9de77f
package samples.dw.bundler.refmodel;

import dev.dropwizard.bundler.refmodel.RefPackage;

public interface RefScheme {

	@RefPackage("samples.dw.bundler.model")
	public enum ImdbInfo {
		Plot, 
		Title, 
		Director, 
	}
}
