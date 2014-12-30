//generated using dev.dropwizard.bundler.refmodel.RefModelSerializer [Tue Dec 30 11:54:22 IST 2014]
//SHA1: b8868ef5a5450a85e5cd677c319a78efb9370ad9
package samples.dw.bundler.ref;

import dev.dropwizard.bundler.refmodel.RefPackage;

public interface RefScheme {

	@RefPackage("samples.dw.bundler.model")
	public enum User {
		name, 
		age, 
	}
}
