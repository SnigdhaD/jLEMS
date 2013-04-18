/**
 * 
 */
package org.lemsml.jlems.core.api.interfaces;

import java.io.IOException;
import java.net.URL;

import org.lemsml.jlems.core.sim.ContentError;

/**
 * @author matteocantarelli
 *
 */
public interface ILEMSDocumentReader
{
	
	/**
	 * Read a LEMS model from a URL into an object
	 * @param modelURL the URL of the model to read
	 * @return the lems model
	 * @throws IOException
	 * @throws ContentError
	 */
	public ILEMSDocument readModel(URL modelURL) throws IOException, ContentError;
	
}