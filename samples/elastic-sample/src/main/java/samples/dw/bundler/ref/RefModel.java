//generated using dev.dropwizard.bundler.refmodel.RefModelSerializer [Tue Dec 30 11:54:49 IST 2014]
//SHA1: 4570aa00f1a2d68d66411b0bf874a16613ab8fbf
package samples.dw.bundler.ref;

import dev.dropwizard.bundler.refmodel.RefPackage;

public interface RefModel {

	@RefPackage("samples.dw.bundler.model")
	public enum User {
		age, 
		name, 
		id, 
	}
}
