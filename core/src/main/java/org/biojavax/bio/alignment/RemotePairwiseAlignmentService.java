/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 */
package org.biojavax.bio.alignment;

import java.io.InputStream;

import org.biojava.bio.BioException;
import org.biojavax.bio.seq.RichSequence;

/**
 * This interface specifies minimal information needed to execute a pairwise alignment on a remote service.
 * 
 * Example of service: QBlast service at NCBI
 *                     Web Service at EBI
 * 
 * @author Sylvain Foisy
 * @since 1.8
 *
 */
public interface RemotePairwiseAlignmentService {

	/**
	 * Doing the actual analysis on the instantiated service using specified parameters and the RichSequence object
	 * 
	 * @throws BioException
	 */
	public String sendAlignmentRequest(RichSequence rs, RemotePairwiseAlignmentProperties rpa) throws BioException;
	
	/**
	 * Doing the actual analysis on the instantiated service using specified parameters on the string representation
	 * of the Sequence object
	 * 
	 * @throws BioException
	 */
	public String sendAlignmentRequest(String str, RemotePairwiseAlignmentProperties rpa) throws BioException;
	
	/**
	 * Simple method to check if the specified request has been completed by the service used.
	 * 
	 * @param id :an ID for an alignment request 
	 * @param present :a long integer value representing the actual time  
	 * @return a boolean value telling if this requestID has been completed or not.
	 * @throws BioException if the ID does not exist.
	 */
	public boolean isReady(String id,long present) throws BioException;
	
	/**
	 * Getting the actual alignment results from this instantiated service for a given ID with specific
	 * formatting parameters held in a RemotePairwiseAlignmentOutputProperties-implemented object. 
	 * 
	 * @param rid :a String with the request ID for this single alignment run
	 * @param out :a RemotePairwiseAlignmentOutputProperties with the specific output instructions.
	 * @return : an <code>InputStream</code> with the actual alignment results
	 * @throws BioException
	 */
	public InputStream getAlignmentResults(String rid,RemotePairwiseAlignmentOutputProperties out) throws BioException;
}
