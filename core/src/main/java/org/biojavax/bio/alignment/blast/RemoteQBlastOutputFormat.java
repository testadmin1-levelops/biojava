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
package org.biojavax.bio.alignment.blast;
/**
 * The RemoteQBlastOutputFormat enum acts like static fields for specifiying various
 * values for certain output options.
 *  
 */
public enum RemoteQBlastOutputFormat {
	TEXT,
	XML,
	HTML,
	PAIRWISE,
	QUERY_ANCHORED,
	QUERY_ANCHORED_NO_IDENTITIES,
	FLAT_QUERY_ANCHORED,
	FLAT_QUERY_ANCHORED_NO_IDENTITIES,
	TABULAR;
}
