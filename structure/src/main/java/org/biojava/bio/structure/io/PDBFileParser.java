/*
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
 * Created on 16.03.2004
 *
 */
package org.biojava.bio.structure.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import java.util.logging.Logger;
import org.biojava.bio.structure.AminoAcid;
import org.biojava.bio.structure.AminoAcidImpl;
import org.biojava.bio.structure.AtomImpl;
import org.biojava.bio.structure.Author;
import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.ChainImpl;
import org.biojava.bio.structure.DBRef;
import org.biojava.bio.structure.Element;
import org.biojava.bio.structure.Group;
import org.biojava.bio.structure.GroupIterator;
import org.biojava.bio.structure.HetatomImpl;
import org.biojava.bio.structure.Compound;
import org.biojava.bio.structure.JournalArticle;
import org.biojava.bio.structure.NucleotideImpl;
import org.biojava.bio.structure.PDBHeader;
import org.biojava.bio.structure.SSBond;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.StructureImpl;
import org.biojava.bio.structure.StructureTools;


/**
 * This class implements the actual PDB file parsing. Do not access it directly, but
 * via the PDBFileReader class.
 *
 * <h2>Parsing</h2>
 *
 * During the PDBfile parsing several Flags can be set:
 * <ul>
 * <li> {@link #setParseCAOnly(boolean)} - parse only the Atom records for C-alpha atoms</li>
 * <li> {@link #setParseSecStruc(boolean)} - a flag if the secondary structure information from the PDB file (author's assignment) should be parsed.
 *      If true the assignment can be accessed through {@link AminoAcid}.getSecStruc(); </li>
 * <li> {@link #setAlignSeqRes(boolean)} - should the AminoAcid sequences from the SEQRES
 *      and ATOM records of a PDB file be aligned? (default:yes)</li>
 * </ul>
 *
 * <p>
 * To provide excessive memory usage for large PDB files, there is the ATOM_CA_THRESHOLD.
 * If more Atoms than this threshold are being parsed in a PDB file, the parser will automatically
 * switch to a C-alpha only representation.
 * </p>
 *
 * <p>
 * The result of the parsing of the PDB file is a new {@link Structure} object.
 * </p>
 *
 *
 * For more documentation on how to work with the Structure API please
 * see <a href="http://biojava.org/wiki/BioJava:CookBook#Protein_Structure" target="_top">
 * http://biojava.org/wiki/BioJava:CookBook#Protein_Structure</a>
 *
 *
 *
 *
 * <h2>Example</h2>
 * <p>
 * Q: How can I get a Structure object from a PDB file?
 * </p>
 * <p>
 * A:
 * <pre>
 public {@link Structure} loadStructure(String pathToPDBFile){
 	    // The PDBFileParser is wrapped by the PDBFileReader
		{@link PDBFileReader} pdbreader = new {@link PDBFileReader}();

		{@link Structure} structure = null;
		try{
			structure = pdbreader.getStructure(pathToPDBFile);
			System.out.println(structure);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return structure;
	}
 </pre>
 *
 *
 * @author Andreas Prlic
 * @author Jules Jacobsen
 * @since 1.4
 */
public class PDBFileParser  {

	private final boolean DEBUG = false;

	private Logger logger = Logger.getLogger(PDBFileParser.class.getName());

	// required for parsing:
	private String pdbId; //the actual id of the entry
	private Structure     structure;
	private List<Chain>   current_model; // contains the ATOM records for each model
	private Chain         current_chain;
	private Group         current_group;

	private List<Chain>   seqResChains; // contains all the chains for the SEQRES records



	// for printing
	private static final String NEWLINE;

	private Map <String,Object>  header ;
	private PDBHeader pdbHeader;
	private JournalArticle journalArticle;
	private List<Map<String, Integer>> connects ;
	private List<Map<String,String>> helixList;
	private List<Map<String,String>> strandList;
	private List<Map<String,String>> turnList;

	private int lengthCheck ;

	private boolean isLastCompndLine = false;
	private boolean isLastSourceLine = false;
	private Compound current_compound;
	private List<Compound> compounds = new ArrayList<Compound>();
	private List<String> compndLines = new ArrayList<String>();
	private List<String> sourceLines = new ArrayList<String>();
	private List<String> journalLines = new ArrayList<String>();
	private List<DBRef> dbrefs;

	// for parsing COMPOUND and SOURCE Header lines
	private int molTypeCounter = 1;
	//private int continuationNo;
	private String continuationField;
	private String continuationString = "";
	private DateFormat dateFormat;

	private static  final List<String> compndFieldValues = new ArrayList<String>(
			Arrays.asList(
					"MOL_ID:", "MOLECULE:", "CHAIN:", "SYNONYM:",
					"EC:", "FRAGMENT:", "ENGINEERED:", "MUTATION:",
					"BIOLOGICAL_UNIT:", "OTHER_DETAILS:"
			));


	private static final List<String> ignoreCompndFieldValues = new ArrayList<String>(
			Arrays.asList(
					"HETEROGEN:","ENGINEEREED:","FRAGMENT,",
					"MUTANT:","SYNTHETIC:"
			));
	// ENGINEEREED in pdb219d

	private static final List<String> sourceFieldValues = new ArrayList<String>(
			Arrays.asList("ENGINEERED:", "MOL_ID:", "SYNTHETIC:", "FRAGMENT:",
					"ORGANISM_SCIENTIFIC:", "ORGANISM_COMMON:",
					"ORGANISM_TAXID:","STRAIN:",
					"VARIANT:", "CELL_LINE:", "ATCC:", "ORGAN:", "TISSUE:",
					"CELL:", "ORGANELLE:", "SECRETION:", "GENE:",
					"CELLULAR_LOCATION:", "EXPRESSION_SYSTEM:",
					"EXPRESSION_SYSTEM_TAXID:",
					"EXPRESSION_SYSTEM_STRAIN:", "EXPRESSION_SYSTEM_VARIANT:",
					"EXPRESSION_SYSTEM_CELL_LINE:",
					"EXPRESSION_SYSTEM_ATCC_NUMBER:",
					"EXPRESSION_SYSTEM_ORGAN:", "EXPRESSION_SYSTEM_TISSUE:",
					"EXPRESSION_SYSTEM_CELL:", "EXPRESSION_SYSTEM_ORGANELLE:",
					"EXPRESSION_SYSTEM_CELLULAR_LOCATION:",
					"EXPRESSION_SYSTEM_VECTOR_TYPE:",
					"EXPRESSION_SYSTEM_VECTOR:", "EXPRESSION_SYSTEM_PLASMID:",
					"EXPRESSION_SYSTEM_GENE:", "OTHER_DETAILS:"));


	boolean parseSecStruc;

	boolean alignSeqRes;

	private String previousContinuationField = "";

	/** Secondary strucuture assigned by the PDB author/
	 *
	 */
	public static final String PDB_AUTHOR_ASSIGNMENT = "PDB_AUTHOR_ASSIGNMENT";

	/** Helix secondary structure assignment.
	 *
	 */
	public static final String HELIX  = "HELIX";

	/** Strand secondary structure assignment.
	 *
	 */
	public static final String STRAND = "STRAND";

	/** Turn secondary structure assignment.
	 *
	 */
	public static final String TURN   = "TURN";

	int atomCount;

	/** the maximum number of atoms that will be parsed before the parser switches to a CA-only
	 * representation of the PDB file. If this limit is exceeded also the SEQRES groups will be
	 * ignored.
	 */
	public static final int ATOM_CA_THRESHOLD = 500000;

	/**  the maximum number of atoms we will add to a structure
     this protects from memory overflows in the few really big protein structures.
	 */
	public static final int MAX_ATOMS = 700000; // tested with java -Xmx300M

	private boolean atomOverflow;

	/** Set the flag to only read in Ca atoms - this is useful for parsing large structures like 1htq.
	 *
	 */
	public boolean parseCAOnly;

	private boolean headerOnly;


	static {

		NEWLINE = System.getProperty("line.separator");

	}



	public PDBFileParser() {

		structure     = null           ;
		current_model = new ArrayList<Chain>();
		current_chain = null           ;
		current_group = null           ;
		header        = init_header() ;
		pdbHeader 	  = new PDBHeader();
		connects      = new ArrayList<Map<String,Integer>>() ;

		parseSecStruc = false;
		alignSeqRes   = true;

		helixList     = new ArrayList<Map<String,String>>();
		strandList    = new ArrayList<Map<String,String>>();
		turnList      = new ArrayList<Map<String,String>>();
		current_compound = new Compound();
		dbrefs        = new ArrayList<DBRef>();

		dateFormat = new SimpleDateFormat("dd-MMM-yy", java.util.Locale.ENGLISH);
		atomCount = 0;
		atomOverflow = false;

		parseCAOnly = false;

	}
	/** the flag if only the C-alpha atoms of the structure should be parsed.
	 *
	 * @return the flag
	 */
	public boolean isParseCAOnly() {
		return parseCAOnly;
	}
	/** the flag if only the C-alpha atoms of the structure should be parsed.
	 *
	 * @param parseCAOnly boolean flag to enable or disable C-alpha only parsing
	 */
	public void setParseCAOnly(boolean parseCAOnly) {
		this.parseCAOnly = parseCAOnly;
	}



	/** Flag if the SEQRES amino acids should be aligned with the ATOM amino acids.
	 *
	 * @return flag if SEQRES - ATOM amino acids alignment is enabled
	 */
	public boolean isAlignSeqRes() {
		return alignSeqRes;
	}



	/** define if the SEQRES in the structure should be aligned with the ATOM records
	 * if yes, the AminoAcids in structure.getSeqRes will have the coordinates set.
	 * @param alignSeqRes
	 */
	public void setAlignSeqRes(boolean alignSeqRes) {
		this.alignSeqRes = alignSeqRes;
	}




	/** is secondary structure assignment being parsed from the file?
	 * default is null
	 * @return boolean if HELIX STRAND and TURN fields are being parsed
	 */
	public boolean isParseSecStruc() {
		return parseSecStruc;
	}

	/** a flag to tell the parser to parse the Author's secondary structure assignment from the file
	 * default is set to false, i.e. do NOT parse.
	 * @param parseSecStruc if HELIX STRAND and TURN fields are being parsed
	 */
	public void setParseSecStruc(boolean parseSecStruc) {
		this.parseSecStruc = parseSecStruc;
	}

	/** initialize the header. */
	private Map<String,Object> init_header(){


		HashMap<String,Object> header = new HashMap<String,Object> ();
		header.put ("idCode","");
		header.put ("classification","")         ;
		header.put ("depDate","0000-00-00");
		header.put ("title","");
		header.put ("technique","");
		header.put ("resolution",null);
		header.put ("modDate","0000-00-00");
		//header.put ("journalRef","");
		//header.put ("author","");
		//header.put ("compound","");
		return header ;
	}


	/**
	 * Returns a time stamp.
	 * @return a String representing the time stamp value
	 */
	protected String getTimeStamp(){

		Calendar cal = Calendar.getInstance() ;
		// Get the components of the time
		int hour24 = cal.get(Calendar.HOUR_OF_DAY);     // 0..23
		int min = cal.get(Calendar.MINUTE);             // 0..59
		int sec = cal.get(Calendar.SECOND);             // 0..59
		String s = "time: "+hour24+" "+min+" "+sec;
		return s ;
	}

	/** initiate new group, either Hetatom, Nucleotide, or AminoAcid */
	private Group getNewGroup(String recordName,Character aminoCode1) {

		Group group;
		if ( recordName.equals("ATOM") ) {
			if (aminoCode1 == null)  {
				// it is a nucleotide
				NucleotideImpl nu = new NucleotideImpl();
				group = nu;

			} else if (aminoCode1 == StructureTools.UNKNOWN_GROUP_LABEL){
				group = new HetatomImpl();

			} else {
				AminoAcidImpl aa = new AminoAcidImpl() ;
				aa.setAminoType(aminoCode1);
				group = aa ;
			}
		}
		else {
			if (aminoCode1 != null ) {
				AminoAcidImpl aa = new AminoAcidImpl() ;
				aa.setAminoType(aminoCode1);
				group = aa ;
			} else {
				group = new HetatomImpl();
			}
		}
		//System.out.println("new group type: "+ group.getType() );
		return  group ;
	}



	// Handler methods to deal with PDB file records properly.
	/**
	 Handler for
	 HEADER Record Format

	 COLUMNS        DATA TYPE       FIELD           DEFINITION
	 ----------------------------------------------------------------------------------
	 1 -  6        Record name     "HEADER"
	 11 - 50        String(40)      classification  Classifies the molecule(s)
	 51 - 59        Date            depDate         Deposition date.  This is the date
	 the coordinates were received by
	 the PDB
	 63 - 66        IDcode          idCode          This identifier is unique within PDB

	 */
	private void pdb_HEADER_Handler(String line) {
		//System.out.println(line);

		String classification  = line.substring (10, 50).trim() ;
		String deposition_date = line.substring (50, 59).trim() ;
		String pdbCode         = line.substring (62, 66).trim() ;

		pdbId = pdbCode;
		if (DEBUG) {
			System.out.println("Parsing entry " + pdbId);
		}
		header.put("idCode",pdbCode);
		structure.setPDBCode(pdbCode);
		header.put("classification",classification);
		header.put("depDate",deposition_date);

		pdbHeader.setIdCode(pdbCode);
		pdbHeader.setClassification(classification);


		try {
			Date dep = dateFormat.parse(deposition_date);
			pdbHeader.setDepDate(dep);
			header.put("depDate",deposition_date);

		} catch (ParseException e){
			e.printStackTrace();
		}

	}


	/** parses the following record:
	 <pre>
	 COLUMNS      DATA  TYPE      FIELD         DEFINITION
------------------------------------------------------------------------------------
 1 -  6      Record name     "AUTHOR"
 9 - 10      Continuation    continuation  Allows concatenation of multiple records.
11 - 79      List            authorList    List of the author names, separated
                                           by commas.

</pre>
	 * @param line
	 */
	private void pdb_AUTHOR_Handler(String line) {

		String authors = line.substring(10).trim();

		String auth = pdbHeader.getAuthors();
		if (auth == null){
			pdbHeader.setAuthors(authors);
		} else {
			auth +=  authors;
			pdbHeader.setAuthors(auth);
		}

	}



	/** parses the following record:

	 <pre>
    COLUMNS       DATA TYPE        FIELD        DEFINITION
    --------------------------------------------------------------------
     1 -  6       Record name      "HELIX "
     8 - 10       Integer          serNum       Serial number of the helix.
                                                This starts at 1 and increases
                                                incrementally.
    12 - 14       LString(3)       helixID      Helix identifier. In addition
                                                to a serial number, each helix is
                                                given an alphanumeric character
                                                helix identifier.
    16 - 18       Residue name     initResName  Name of the initial residue.
    20            Character        initChainID  Chain identifier for the chain
                                                containing this helix.
    22 - 25       Integer          initSeqNum   Sequence number of the initial
                                                residue.
    26            AChar            initICode    Insertion code of the initial
                                                residue.
    28 - 30       Residue name     endResName   Name of the terminal residue of
                                                the helix.
    32            Character        endChainID   Chain identifier for the chain
                                                containing this helix.
    34 - 37       Integer          endSeqNum    Sequence number of the terminal
                                                residue.
    38            AChar            endICode     Insertion code of the terminal
                                                residue.
    39 - 40       Integer          helixClass   Helix class (see below).
    41 - 70       String           comment      Comment about this helix.
    72 - 76       Integer          length       Length of this helix.
</pre>
	 */

	private void pdb_HELIX_Handler(String line){
		String initResName = line.substring(15,18).trim();
		String initChainId = line.substring(19,20);
		String initSeqNum  = line.substring(21,25).trim();
		String initICode   = line.substring(25,26);
		String endResName  = line.substring(27,30).trim();
		String endChainId  = line.substring(31,32);
		String endSeqNum   = line.substring(33,37).trim();
		String endICode    = line.substring(37,38);

		//System.out.println(initResName + " " + initChainId + " " + initSeqNum + " " + initICode + " " +
		//        endResName + " " + endChainId + " " + endSeqNum + " " + endICode);

		Map<String,String> m = new HashMap<String,String>();

		m.put("initResName",initResName);
		m.put("initChainId", initChainId);
		m.put("initSeqNum", initSeqNum);
		m.put("initICode", initICode);
		m.put("endResName", endResName);
		m.put("endChainId", endChainId);
		m.put("endSeqNum",endSeqNum);
		m.put("endICode",endICode);

		helixList.add(m);

	}

	/**
      Handler for
      <pre>
      COLUMNS     DATA TYPE        FIELD           DEFINITION
--------------------------------------------------------------
 1 -  6     Record name      "SHEET "
 8 - 10     Integer          strand       Strand number which starts at 1
                                          for each strand within a sheet
                                          and increases by one.
12 - 14     LString(3)       sheetID      Sheet identifier.
15 - 16     Integer          numStrands   Number of strands in sheet.
18 - 20     Residue name     initResName  Residue name of initial residue.
22          Character        initChainID  Chain identifier of initial
                                          residue in strand.
23 - 26     Integer          initSeqNum   Sequence number of initial
                                          residue in strand.
27          AChar            initICode    Insertion code of initial residue
                                          in strand.
29 - 31     Residue name     endResName   Residue name of terminal residue.
33          Character        endChainID   Chain identifier of terminal
                                          residue.
34 - 37     Integer          endSeqNum    Sequence number of terminal
                                          residue.
38          AChar            endICode     Insertion code of terminal
                                          residue.
39 - 40     Integer          sense        Sense of strand with respect to
                                          previous strand in the sheet. 0
                                          if first strand, 1 if parallel,
                                          -1 if anti-parallel.
42 - 45     Atom             curAtom      Registration. Atom name in
                                          current strand.
46 - 48     Residue name     curResName   Registration. Residue name in
                                          current strand.
50          Character        curChainId   Registration. Chain identifier in
                                          current strand.
51 - 54     Integer          curResSeq    Registration. Residue sequence
                                          number in current strand.
55          AChar            curICode     Registration. Insertion code in
                                          current strand.
57 - 60     Atom             prevAtom     Registration. Atom name in
                                          previous strand.
61 - 63     Residue name     prevResName  Registration. Residue name in
                                          previous strand.
65          Character        prevChainId  Registration. Chain identifier in
                                          previous strand.
66 - 69     Integer          prevResSeq   Registration. Residue sequence
                                          number in previous strand.
70          AChar            prevICode    Registration. Insertion code in
                                              previous strand.
</pre>


	 */
	private void pdb_SHEET_Handler( String line){


		String initResName = line.substring(17,20).trim();
		String initChainId = line.substring(21,22);
		String initSeqNum  = line.substring(22,26).trim();
		String initICode   = line.substring(26,27);
		String endResName  = line.substring(28,31).trim();
		String endChainId  = line.substring(32,33);
		String endSeqNum   = line.substring(33,37).trim();
		String endICode    = line.substring(37,38);

		//System.out.println(initResName + " " + initChainId + " " + initSeqNum + " " + initICode + " " +
		//        endResName + " " + endChainId + " " + endSeqNum + " " + endICode);

		Map<String,String> m = new HashMap<String,String>();

		m.put("initResName",initResName);
		m.put("initChainId", initChainId);
		m.put("initSeqNum", initSeqNum);
		m.put("initICode", initICode);
		m.put("endResName", endResName);
		m.put("endChainId", endChainId);
		m.put("endSeqNum",endSeqNum);
		m.put("endICode",endICode);

		strandList.add(m);
	}


	/**
	 * Handler for TURN lines
     <pre>
     COLUMNS      DATA TYPE        FIELD         DEFINITION
--------------------------------------------------------------------
 1 -  6      Record name      "TURN "
 8 - 10      Integer          seq           Turn number; starts with 1 and
                                            increments by one.
12 - 14      LString(3)       turnId        Turn identifier
16 - 18      Residue name     initResName   Residue name of initial residue in
                                            turn.
20           Character        initChainId   Chain identifier for the chain
                                            containing this turn.
21 - 24      Integer          initSeqNum    Sequence number of initial residue
                                            in turn.
25           AChar            initICode     Insertion code of initial residue
                                            in turn.
27 - 29      Residue name     endResName    Residue name of terminal residue
                                            of turn.
31           Character        endChainId    Chain identifier for the chain
                                            containing this turn.
32 - 35      Integer          endSeqNum     Sequence number of terminal
                                            residue of turn.
36           AChar            endICode      Insertion code of terminal residue
                                            of turn.
41 - 70      String           comment       Associated comment.

     </pre>
	 * @param line
	 */
	private void pdb_TURN_Handler( String line){
		String initResName = line.substring(15,18).trim();
		String initChainId = line.substring(19,20);
		String initSeqNum  = line.substring(20,24).trim();
		String initICode   = line.substring(24,25);
		String endResName  = line.substring(26,29).trim();
		String endChainId  = line.substring(30,31);
		String endSeqNum   = line.substring(31,35).trim();
		String endICode    = line.substring(35,36);

		//System.out.println(initResName + " " + initChainId + " " + initSeqNum + " " + initICode + " " +
		//        endResName + " " + endChainId + " " + endSeqNum + " " + endICode);

		Map<String,String> m = new HashMap<String,String>();

		m.put("initResName",initResName);
		m.put("initChainId", initChainId);
		m.put("initSeqNum", initSeqNum);
		m.put("initICode", initICode);
		m.put("endResName", endResName);
		m.put("endChainId", endChainId);
		m.put("endSeqNum",endSeqNum);
		m.put("endICode",endICode);

		turnList.add(m);
	}

	/**
	 Handler for
	 REVDAT Record format:

	 COLUMNS       DATA TYPE      FIELD         DEFINITION
	 ----------------------------------------------------------------------------------
	 1 -  6       Record name    "REVDAT"
	 8 - 10       Integer        modNum        Modification number.
	 11 - 12       Continuation   continuation  Allows concatenation of multiple
	 records.
	 14 - 22       Date           modDate       Date of modification (or release for
	 new entries).  This is not repeated
	 on continuation lines.
	 24 - 28       String(5)      modId         Identifies this particular
	 modification.  It links to the
	 archive used internally by PDB.
	 This is not repeated on continuation
	 lines.
	 32            Integer        modType       An integer identifying the type of
	 modification.  In case of revisions
	 with more than one possible modType,
	 the highest value applicable will be
	 assigned.
	 40 - 45       LString(6)     record        Name of the modified record.
	 47 - 52       LString(6)     record        Name of the modified record.
	 54 - 59       LString(6)     record        Name of the modified record.
	 61 - 66       LString(6)     record        Name of the modified record.
	 */
	private void pdb_REVDAT_Handler(String line) {

		// only keep the first...
		String modDate = (String) header.get("modDate");

		if ( modDate.equals("0000-00-00") ) {
			// modDate is still initialized
			String modificationDate = line.substring (13, 22).trim() ;
			header.put("modDate",modificationDate);

			try {
				Date dep = dateFormat.parse(modificationDate);
				pdbHeader.setModDate(dep);
			} catch (ParseException e){
				e.printStackTrace();
			}

		}
	}

	/** @author Jules Jacobsen
	 * Handler for
	 * SEQRES record format
	 * SEQRES records contain the amino acid or nucleic acid sequence of residues in each chain of the macromolecule that was studied.
	 * <p/>
	 * Record Format
	 * <p/>
	 * COLUMNS        DATA TYPE       FIELD         DEFINITION
	 * ---------------------------------------------------------------------------------
	 * 1 -  6        Record name     "SEQRES"
	 * <p/>
	 * 9 - 10        Integer         serNum        Serial number of the SEQRES record
	 * for the current chain.  Starts at 1
	 * and increments by one each line.
	 * Reset to 1 for each chain.
	 * <p/>
	 * 12             Character       chainID       Chain identifier.  This may be any
	 * single legal character, including a
	 * blank which is used if there is
	 * only one chain.
	 * <p/>
	 * 14 - 17        Integer         numRes        Number of residues in the chain.
	 * This value is repeated on every
	 * record.
	 * <p/>
	 * 20 - 22        Residue name    resName       Residue name.
	 * <p/>
	 * 24 - 26        Residue name    resName       Residue name.
	 * <p/>
	 * 28 - 30        Residue name    resName       Residue name.
	 * <p/>
	 * 32 - 34        Residue name    resName       Residue name.
	 * <p/>
	 * 36 - 38        Residue name    resName       Residue name.
	 * <p/>
	 * 40 - 42        Residue name    resName       Residue name.
	 * <p/>
	 * 44 - 46        Residue name    resName       Residue name.
	 * <p/>
	 * 48 - 50        Residue name    resName       Residue name.
	 * <p/>
	 * 52 - 54        Residue name    resName       Residue name.
	 * <p/>
	 * 56 - 58        Residue name    resName       Residue name.
	 * <p/>
	 * 60 - 62        Residue name    resName       Residue name.
	 * <p/>
	 * 64 - 66        Residue name    resName       Residue name.
	 * <p/>
	 * 68 - 70        Residue name    resName       Residue name.
	 */
	private void pdb_SEQRES_Handler(String line)
	throws PDBParseException {
		//		System.out.println("PDBFileParser.pdb_SEQRES_Handler: BEGIN");
		//		System.out.println(line);

		//TODO: treat the following residues as amino acids?
		/*
        MSE Selenomethionine
        CSE Selenocysteine
        PTR Phosphotyrosine
        SEP Phosphoserine
        TPO Phosphothreonine
        HYP 4-hydroxyproline
        5HP Pyroglutamic acid; 5-hydroxyproline
        PCA Pyroglutamic Acid
        LYZ 5-hydroxylysine
        GLX Glu or Gln
        ASX Asp or Asn
        GLA gamma-carboxy-glutamic acid
                 1         2         3         4         5         6         7
        1234567890123456789012345678901234567890123456789012345678901234567890
        SEQRES   1 A  376  LYS PRO VAL THR VAL LYS LEU VAL ASP SER GLN ALA THR
        SEQRES   1 A   21  GLY ILE VAL GLU GLN CYS CYS THR SER ILE CYS SER LEU
        SEQRES   2 A   21  TYR GLN LEU GLU ASN TYR CYS ASN
        SEQRES   1 B   30  PHE VAL ASN GLN HIS LEU CYS GLY SER HIS LEU VAL GLU
        SEQRES   2 B   30  ALA LEU TYR LEU VAL CYS GLY GLU ARG GLY PHE PHE TYR
        SEQRES   3 B   30  THR PRO LYS ALA
        SEQRES   1 C   21  GLY ILE VAL GLU GLN CYS CYS THR SER ILE CYS SER LEU
        SEQRES   2 C   21  TYR GLN LEU GLU ASN TYR CYS ASN
        SEQRES   1 D   30  PHE VAL ASN GLN HIS LEU CYS GLY SER HIS LEU VAL GLU
        SEQRES   2 D   30  ALA LEU TYR LEU VAL CYS GLY GLU ARG GLY PHE PHE TYR
        SEQRES   3 D   30  THR PRO LYS ALA
		 */

		//System.out.println(line);
		String recordName = line.substring(0, 6).trim();
		String chainID    = line.substring(11, 12);
		String newLength   = line.substring(13,17).trim();
		String subSequence = line.substring(18, 70);

		//System.out.println("newLength " + newLength );

		if ( lengthCheck == -1 ){
			lengthCheck = Integer.parseInt(newLength);
		}

		//String residueNumber = line.substring(22, 27).trim() ;
		StringTokenizer subSequenceResidues = new StringTokenizer(subSequence);

		Character aminoCode1 = null;
		if (! recordName.equals(AminoAcid.SEQRESRECORD)) {
			// should not have been called
			return;
		}

		current_chain = isKnownChain(chainID, seqResChains);
		if ( current_chain == null) {

			current_chain = new ChainImpl();
			current_chain.setName(chainID);

		}

		while (subSequenceResidues.hasMoreTokens()) {

			String threeLetter = subSequenceResidues.nextToken();

			aminoCode1 = StructureTools.get1LetterCode(threeLetter);
			//System.out.println(aminoCode1);
			//if (aminoCode1 == null) {
			// could be a nucleotide...
			// but getNewGroup takes care of that and converts ATOM records with aminoCode1 == nnull to nucleotide...
			//System.out.println(line);
			// b
			//}
			current_group = getNewGroup("ATOM", aminoCode1);

			try {
				current_group.setPDBName(threeLetter);
			} catch (PDBParseException p){
				System.err.println(p.getMessage() );
			}
			if ( current_group instanceof AminoAcid){
				AminoAcid aa = (AminoAcid)current_group;
				aa.setRecordType(AminoAcid.SEQRESRECORD);
			}
			// add the current group to the new chain.
			current_chain.addGroup(current_group);

		}
		Chain test = isKnownChain(chainID, seqResChains);

		if ( test == null)
			seqResChains.add(current_chain);

		current_group = null;
		current_chain = null;

		//		 the current chain is finished!
		//if ( current_chain.getLength() != lengthCheck ){
		//	System.err.println("the length of chain " + current_chain.getName() + "(" +
		//			current_chain.getLength() + ") does not match the expected " + lengthCheck);
		//}

		lengthCheck = Integer.parseInt(newLength);

	}



	/** Handler for
	 TITLE Record Format

	 COLUMNS        DATA TYPE       FIELD          DEFINITION
	 ----------------------------------------------------------------------------------
	 1 -  6        Record name     "TITLE "
	 9 - 10        Continuation    continuation   Allows concatenation of multiple
	 records.
	 11 - 70        String          title          Title of the experiment.


	 */
	private void pdb_TITLE_Handler(String line) {
		String title;
		if ( line.length() > 69)
			title = line.substring(10,70).trim();
		else
			title = line.substring(10,line.length()).trim();

		String t= (String)header.get("title") ;
		if ( (t != null) && (! t.equals("")))
			t += " ";
		t += title;
		header.put("title",t);
		pdbHeader.setTitle(t);
	}

	/**
	 * JRNL handler.
	 * The JRNL record contains the primary literature citation that describes the experiment which resulted
	 * in the deposited coordinate set. There is at most one JRNL reference per entry. If there is no primary
	 * reference, then there is no JRNL reference. Other references are given in REMARK 1.

    Record Format

    COLUMNS       DATA TYPE     FIELD         DEFINITION
    -----------------------------------------------------------------------
    1 -  6       Record name   "JRNL  "

    13 - 70       LString        text         See Details below.

	 */
	private void pdb_JRNL_Handler(String line) {
		//add the strings to the journalLines
		//the actual JournalArticle is then built when the whole entry is being
		//finalized with triggerEndFileChecks()
		//JRNL        TITL   NMR SOLUTION STRUCTURE OF RECOMBINANT TICK           1TAP  10
		if (line.substring(line.length() - 8, line.length() - 4).equals(pdbId)) {
			//trim off the trailing PDB id from legacy files.
			//are we really trying to still cater for these museum pieces?
			if (DEBUG) {
				System.out.println("trimming legacy PDB id from end of JRNL section line");
			}
			line = line.substring(0, line.length() - 8);
			journalLines.add(line);
		} else {
			journalLines.add(line);
		}
	}

	/**
	 * This should not be accessed directly, other than by </code>makeCompounds</code>. It still deals with the same
	 * lines in a similar manner but if not accessed from </code>makeCompounds</code> the last element will be
	 * missing. Don't say I didn't warn you.
	 *
	 * @param line
	 */
	private void pdb_COMPND_Handler(String line) {

		String continuationNr = line.substring(9, 10).trim();
		if (DEBUG) {
			System.out.println("current continuationNo     is "
					+ continuationNr);
			System.out.println("previousContinuationField  is "
					+ previousContinuationField);
			System.out.println("current continuationField  is "
					+ continuationField);
			System.out.println("current continuationString is "
					+ continuationString);
			System.out.println("current compound           is "
					+ current_compound);
		}

		// in some PDB files the line ends with the PDB code and a serial number, chop those off!
		if (line.length() > 72) {
			line = line.substring(0, 72);
		}

		//String beginningOfLine = line.substring(0, 10);
		//line = line.replace(beginningOfLine, "");
		line = line.substring(10, line.length());

		if (DEBUG) {
			System.out.println("LINE: >" + line + "<");
		}
		String[] fieldList = line.split("\\s+");
		int fl = fieldList.length;
		if ((fl >0 ) && (!fieldList[0].equals(""))
				&& compndFieldValues.contains(fieldList[0])) {
			//			System.out.println("[PDBFileParser.pdb_COMPND_Handler] Setting continuationField to '" + fieldList[0] + "'");
			continuationField = fieldList[0];
			if (previousContinuationField.equals("")) {
				previousContinuationField = continuationField;
			}

		} else if ((fl >1 ) && compndFieldValues.contains(fieldList[1])) {
			//			System.out.println("[PDBFileParser.pdb_COMPND_Handler] Setting continuationField to '" + fieldList[1] + "'");
			continuationField = fieldList[1];
			if (previousContinuationField.equals("")) {
				previousContinuationField = continuationField;
			}

		} else {
			if (continuationNr.equals("")) {
				if (DEBUG) {
					System.out.println("looks like an old PDB file");
				}
				continuationField = "MOLECULE:";
				if (previousContinuationField.equals("")) {
					previousContinuationField = continuationField;
				}
			}

		}

		line = line.replace(continuationField, "").trim();

		StringTokenizer compndTokens = new StringTokenizer(line);

		//		System.out.println("PDBFileParser.pdb_COMPND_Handler: Tokenizing '" + line + "'");

		while (compndTokens.hasMoreTokens()) {
			String token = compndTokens.nextToken();

			if (previousContinuationField.equals("")) {
				previousContinuationField = continuationField;
			}

			if (previousContinuationField.equals(continuationField)
					&& compndFieldValues.contains(continuationField)) {
				if (DEBUG) {
					System.out.println("Still in field " + continuationField);
					System.out.println("token = " + token);
				}
				continuationString = continuationString.concat(token + " ");
				if (DEBUG) {
					System.out.println("continuationString = "
							+ continuationString);
				}
			}
			if (!continuationField.equals(previousContinuationField)) {

				if (continuationString.equals("")) {
					continuationString = token;

				} else {

					compndValueSetter(previousContinuationField,
							continuationString);
					previousContinuationField = continuationField;
					continuationString = token + " ";
				}
			} else if (ignoreCompndFieldValues.contains(token)) {
				// this field shall be ignored
				//continuationField = token;
			}
		}
		if (isLastCompndLine) {
			// final line in the section - finish off the compound
			//			System.out.println("[pdb_COMPND_Handler] Final COMPND line - Finishing off final MolID header.");
			compndValueSetter(continuationField, continuationString);
			continuationString = "";
			compounds.add(current_compound);
		}
	}

	/** set the value in the currrent molId object
	 *
	 * @param field
	 * @param value
	 */
	private void compndValueSetter(String field, String value) {

		value = value.trim().replace(";", "");
		if (field.equals("MOL_ID:")) {

			//todo: find out why an extra mol or chain gets added  and why 1H1J, 1J1H ATOM records are missing, but not 1H1H....
			if (DEBUG)
				System.out.println("molTypeCounter " + molTypeCounter + " "
						+ value);
			int i = -1;
			try {
				i = Integer.valueOf(value);
			} catch (NumberFormatException e){
				System.err.println(e.getMessage() + " while trying to parse COMPND line.");
			}
			if (molTypeCounter != i) {
				molTypeCounter++;

				compounds.add(current_compound);
				current_compound = null;
				current_compound = new Compound();

			}

			current_compound.setMolId(value);
		}
		if (field.equals("MOLECULE:")) {
			current_compound.setMolName(value);

		}
		if (field.equals("CHAIN:")) {
			//System.out.println(value);
			StringTokenizer chainTokens = new StringTokenizer(value, ",");
			List<String> chains = new ArrayList<String>();

			while (chainTokens.hasMoreTokens()) {
				String chainID = chainTokens.nextToken().trim();
				// NULL is used in old PDB files to represent empty chain DI
				if (chainID.equals("NULL"))
					chainID = " ";
				chains.add(chainID);
			}
			current_compound.setChainId(chains);

		}
		if (field.equals("SYNONYM:")) {

			StringTokenizer synonyms = new StringTokenizer(value, ",");
			List<String> names = new ArrayList<String>();

			while (synonyms.hasMoreTokens()) {
				names.add(synonyms.nextToken());

				current_compound.setSynonyms(names);
			}

		}

		if (field.equals("EC:")) {

			StringTokenizer ecNumTokens = new StringTokenizer(value, ",");
			List<String> ecNums = new ArrayList<String>();

			while (ecNumTokens.hasMoreTokens()) {
				ecNums.add(ecNumTokens.nextToken());

				current_compound.setEcNums(ecNums);
			}

		}
		if (field.equals("FRAGMENT:")) {

			current_compound.setFragment(value);

		}
		if (field.equals("ENGINEERED:")) {

			current_compound.setEngineered(value);

		}
		if (field.equals("MUTATION:")) {

			current_compound.setMutation(value);

		}
		if (field.equals("BIOLOGICAL_UNIT:")) {

			current_compound.setBiologicalUnit(value);

		}
		if (field.equals("OTHER_DETAILS:")) {

			current_compound.setDetails(value);

		}

	}


	/** Handler for
	 * SOURCE Record format
	 *
	 * The SOURCE record specifies the biological and/or chemical source of each biological molecule in the entry. Sources are described by both the common name and the scientific name, e.g., genus and species. Strain and/or cell-line for immortalized cells are given when they help to uniquely identify the biological entity studied.
Record Format

COLUMNS   DATA TYPE         FIELD          DEFINITION
-------------------------------------------------------------------------------
 1 -  6   Record name       "SOURCE"
 9 - 10   Continuation      continuation   Allows concatenation of multiple records.
11 - 70   Specification     srcName        Identifies the source of the macromolecule in
           list                            a token: value format.
	 * @param line the line to be parsed

	 */
	private void pdb_SOURCE_Handler(String line) {
		// works in the same way as the pdb_COMPND_Handler.
		boolean sourceDebug = false;

		String continuationNr = line.substring(9, 10).trim();

		if (sourceDebug) {
			System.out.println("current continuationNo     is "
					+ continuationNr);
			System.out.println("previousContinuationField  is "
					+ previousContinuationField);
			System.out.println("current continuationField  is "
					+ continuationField);
			System.out.println("current continuationString is "
					+ continuationString);
			System.out.println("current compound           is "
					+ current_compound);
		}

		// in some PDB files the line ends with the PDB code and a serial number, chop those off!
		if (line.length() > 72) {
			line = line.substring(0, 72);
		}

		line = line.substring(10, line.length());

		if (sourceDebug) {
			System.out.println("LINE: >" + line + "<");
		}
		String[] fieldList = line.split("\\s+");

		if (!fieldList[0].equals("")
				&& sourceFieldValues.contains(fieldList[0])) {
			//			System.out.println("[PDBFileParser.pdb_COMPND_Handler] Setting continuationField to '" + fieldList[0] + "'");
			continuationField = fieldList[0];
			if (previousContinuationField.equals("")) {
				previousContinuationField = continuationField;
			}

		} else if ((fieldList.length > 1) && ( sourceFieldValues.contains(fieldList[1]))) {
			//			System.out.println("[PDBFileParser.pdb_COMPND_Handler] Setting continuationField to '" + fieldList[1] + "'");
			continuationField = fieldList[1];
			if (previousContinuationField.equals("")) {
				previousContinuationField = continuationField;
			}

		} else {
			if (continuationNr.equals("")) {
				if (sourceDebug) {
					System.out.println("looks like an old PDB file");
				}
				continuationField = "MOLECULE:";
				if (previousContinuationField.equals("")) {
					previousContinuationField = continuationField;
				}
			}

		}

		line = line.replace(continuationField, "").trim();

		StringTokenizer compndTokens = new StringTokenizer(line);

		//		System.out.println("PDBFileParser.pdb_COMPND_Handler: Tokenizing '" + line + "'");

		while (compndTokens.hasMoreTokens()) {
			String token = compndTokens.nextToken();

			if (previousContinuationField.equals("")) {
				//				System.out.println("previousContinuationField is empty. Setting to : " + continuationField);
				previousContinuationField = continuationField;
			}

			if (previousContinuationField.equals(continuationField)
					&& sourceFieldValues.contains(continuationField)) {
				if (sourceDebug)
					System.out.println("Still in field " + continuationField);

				continuationString = continuationString.concat(token + " ");
				if (sourceDebug)
					System.out.println("continuationString = "
							+ continuationString);
			}
			if (!continuationField.equals(previousContinuationField)) {

				if (continuationString.equals("")) {
					continuationString = token;

				} else {

					sourceValueSetter(previousContinuationField,
							continuationString);
					previousContinuationField = continuationField;
					continuationString = token + " ";
				}
			} else if (ignoreCompndFieldValues.contains(token)) {
				// this field shall be ignored
				//continuationField = token;
			}
		}
		if (isLastSourceLine) {
			// final line in the section - finish off the compound
			//			System.out.println("[pdb_SOURCE_Handler] Final SOURCE line - Finishing off final MolID header.");
			sourceValueSetter(continuationField, continuationString);
			continuationString = "";
			//compounds.add(current_compound);
		}

	}


	/** set the value in the currrent molId object
	 *
	 * @param field
	 * @param value
	 */
	private void sourceValueSetter(String field, String value) {

		value = value.trim().replace(";", "");
		//		System.out.println("[sourceValueSetter] " + field);
		if (field.equals("MOL_ID:")) {

			try {
				current_compound = compounds.get(Integer.valueOf(value) - 1);
			} catch (Exception e){
				System.err.println("could not process SOURCE MOL_ID record correctly:" + e.getMessage());
				return;
			}


			//			System.out.println("[sourceValueSetter] Fetching compound " + value + " " + current_compound.getMolId());

		}
		if (field.equals("SYNTHETIC:")) {
			current_compound.setSynthetic(value);
		} else if (field.equals("FRAGMENT:")) {
			current_compound.setFragment(value);
		} else if (field.equals("ORGANISM_SCIENTIFIC:")) {
			current_compound.setOrganismScientific(value);
		} else if (field.equals("ORGANISM_TAXID:")) {
			current_compound.setOrganismTaxId(value);
		} else if (field.equals("ORGANISM_COMMON:")) {
			current_compound.setOrganismCommon(value);
		} else if (field.equals("STRAIN:")) {
			current_compound.setStrain(value);
		} else if (field.equals("VARIANT:")) {
			current_compound.setVariant(value);
		} else if (field.equals("CELL_LINE:")) {
			current_compound.setCellLine(value);
		} else if (field.equals("ATCC:")) {
			current_compound.setAtcc(value);
		} else if (field.equals("ORGAN:")) {
			current_compound.setOrgan(value);
		} else if (field.equals("TISSUE:")) {
			current_compound.setTissue(value);
		} else if (field.equals("CELL:")) {
			current_compound.setCell(value);
		} else if (field.equals("ORGANELLE:")) {
			current_compound.setOrganelle(value);
		} else if (field.equals("SECRETION:")) {
			current_compound.setSecretion(value);
		} else if (field.equals("GENE:")) {
			current_compound.setGene(value);
		} else if (field.equals("CELLULAR_LOCATION:")) {
			current_compound.setCellularLocation(value);
		} else if (field.equals("EXPRESSION_SYSTEM:")) {
			current_compound.setExpressionSystem(value);
		} else if (field.equals("EXPRESSION_SYSTEM_TAXID:")) {
			current_compound.setExpressionSystemTaxId(value);
		} else if (field.equals("EXPRESSION_SYSTEM_STRAIN:")) {
			current_compound.setExpressionSystemStrain(value);
		} else if (field.equals("EXPRESSION_SYSTEM_VARIANT:")) {
			current_compound.setExpressionSystemVariant(value);
		} else if (field.equals("EXPRESSION_SYSTEM_CELL_LINE:")) {
			current_compound.setExpressionSystemCellLine(value);
		} else if (field.equals("EXPRESSION_SYSTEM_ATCC_NUMBER:")) {
			current_compound.setExpressionSystemAtccNumber(value);
		} else if (field.equals("EXPRESSION_SYSTEM_ORGAN:")) {
			current_compound.setExpressionSystemOrgan(value);
		} else if (field.equals("EXPRESSION_SYSTEM_TISSUE:")) {
			current_compound.setExpressionSystemTissue(value);
		} else if (field.equals("EXPRESSION_SYSTEM_CELL:")) {
			current_compound.setExpressionSystemCell(value);
		} else if (field.equals("EXPRESSION_SYSTEM_ORGANELLE:")) {
			current_compound.setExpressionSystemOrganelle(value);
		} else if (field.equals("EXPRESSION_SYSTEM_CELLULAR_LOCATION:")) {
			current_compound.setExpressionSystemCellularLocation(value);
		} else if (field.equals("EXPRESSION_SYSTEM_VECTOR_TYPE:")) {
			current_compound.setExpressionSystemVectorType(value);
		} else if (field.equals("EXPRESSION_SYSTEM_VECTOR:")) {
			current_compound.setExpressionSystemVector(value);
		} else if (field.equals("EXPRESSION_SYSTEM_PLASMID:")) {
			current_compound.setExpressionSystemPlasmid(value);
		} else if (field.equals("EXPRESSION_SYSTEM_GENE:")) {
			current_compound.setExpressionSystemGene(value);
		} else if (field.equals("OTHER_DETAILS:")) {
			current_compound.setExpressionSystemOtherDetails(value);
		}

	}


	/** Handler for
	 REMARK  2

	 * For diffraction experiments:

	 COLUMNS        DATA TYPE       FIELD               DEFINITION
	 --------------------------------------------------------------------------------
	 1 -  6        Record name     "REMARK"
	 10             LString(1)      "2"
	 12 - 22        LString(11)     "RESOLUTION."
	 23 - 27        Real(5.2)       resolution          Resolution.
	 29 - 38        LString(10)     "ANGSTROMS."
	 */

	private void pdb_REMARK_2_Handler(String line) {
		//System.out.println(line);
		int i = line.indexOf("ANGSTROM");
		if ( i != -1) {
			// line contains ANGSTROM info...
			//get the chars between 22 and where Angstrom starts
			// this is for backwards compatibility
			// new PDB files start at 24!!!
			//System.out.println(i);

			String resolution = line.substring(22,i).trim();
			//System.out.println(resolution);
			// convert string to float
			float res = PDBHeader.DEFAULT_RESOLUTION ;
			try {
				res = Float.parseFloat(resolution);
			} catch (NumberFormatException e) {
				System.err.println(e.getMessage());
				System.err.println("could not parse resolution from line and ignoring it " + line);
				return ;
			}
			//System.out.println("got resolution:" +res);
			header.put("resolution",new Float(res));
			pdbHeader.setResolution(res);
		}

	}


	/** Handler for REMARK lines
	 */
	private void pdb_REMARK_Handler(String line) {
		// finish off the compound handler!



		String l = line.substring(0,11).trim();
		if (l.equals("REMARK   2"))pdb_REMARK_2_Handler(line);

	}


	/** Handler for
	 EXPDTA Record Format

	 COLUMNS       DATA TYPE      FIELD         DEFINITION
	 -------------------------------------------------------------------------------
	 1 -  6       Record name    "EXPDTA"
	 9 - 10       Continuation   continuation  Allows concatenation of multiple
	 records.
	 11 - 70       SList          technique     The experimental technique(s) with
	 optional comment describing the
	 sample or experiment.

	 allowed techniques are:
	 ELECTRON DIFFRACTION
	 FIBER DIFFRACTION
	 FLUORESCENCE TRANSFER
	 NEUTRON DIFFRACTION
	 NMR
	 THEORETICAL MODEL
	 X-RAY DIFFRACTION

	 */

	private void pdb_EXPDTA_Handler(String line) {

		String technique  ;
		if (line.length() > 69)
			technique = line.substring (10, 70).trim() ;
		else
			technique = line.substring(10).trim();

		String t =(String) header.get("technique");
		t += technique +" ";
		header.put("technique",t);
		pdbHeader.setTechnique(t);

		int nmr = technique.indexOf("NMR");
		if ( nmr != -1 ) structure.setNmr(true);  ;

	}




	/**
	 Handler for
	 ATOM Record Format
	 <pre>
	 COLUMNS        DATA TYPE       FIELD         DEFINITION
	 ---------------------------------------------------------------------------------
	 1 -  6        Record name     "ATOM  "
	 7 - 11        Integer         serial        Atom serial number.
	 13 - 16        Atom            name          Atom name.
	 17             Character       altLoc        Alternate location indicator.
	 18 - 20        Residue name    resName       Residue name.
	 22             Character       chainID       Chain identifier.
	 23 - 26        Integer         resSeq        Residue sequence number.
	 27             AChar           iCode         Code for insertion of residues.
	 31 - 38        Real(8.3)       x             Orthogonal coordinates for X in
	 Angstroms.
	 39 - 46        Real(8.3)       y             Orthogonal coordinates for Y in
	 Angstroms.
	 47 - 54        Real(8.3)       z             Orthogonal coordinates for Z in
	 Angstroms.
	 55 - 60        Real(6.2)       occupancy     Occupancy.
	 61 - 66        Real(6.2)       tempFactor    Temperature factor.
	 73 - 76        LString(4)      segID         Segment identifier, left-justified.
	 77 - 78        LString(2)      element       Element symbol, right-justified.
	 79 - 80        LString(2)      charge        Charge on the atom.
	 </pre>
	 */
	private void  pdb_ATOM_Handler(String line)
	throws PDBParseException
	{

		// build up chains first.
		// headerOnly just goes down to chain resolution.

		boolean startOfNewChain = false;

		String chain_id      = line.substring(21,22);

		if (current_chain == null) {
			current_chain = new ChainImpl();
			current_chain.setName(chain_id);
			startOfNewChain = true;
			current_model.add(current_chain);		
		}


		if ( ! chain_id.equals(current_chain.getName()) ) {

			startOfNewChain = true;

			// end up old chain...
			current_chain.addGroup(current_group);

			// see if old chain is known ...
			Chain testchain ;
			testchain = isKnownChain(current_chain.getName(),current_model);

			//System.out.println("trying to re-using known chain " + current_chain.getName() + " " + chain_id);		
			if ( testchain != null && testchain.getName().equals(chain_id)){
				//System.out.println("re-using known chain " + current_chain.getName() + " " + chain_id);				

			} else {

				testchain = isKnownChain(chain_id,current_model);
			}

			if ( testchain == null) {
				//System.out.println("unknown chain. creating new chain.");

				current_chain = new ChainImpl();
				current_chain.setName(chain_id);

			}   else {
				current_chain = testchain;
			}

			if ( ! current_model.contains(current_chain))
				current_model.add(current_chain);


		} 

		// process group data:
		// join residue numbers and insertion codes together
		String recordName     = line.substring (0, 6).trim ();
		String residueNumber  = line.substring(22,27).trim();
		String groupCode3     = line.substring(17,20);

		Character aminoCode1 = null;

		if ( recordName.equals("ATOM") ){
			aminoCode1 = StructureTools.get1LetterCode(groupCode3);
		} else {
			// HETATOM RECORDS are treated slightly differently
			// some modified amino acids that we want to treat as amino acids
			// can be found as HETATOM records
			aminoCode1 = StructureTools.get1LetterCode(groupCode3);
			if ( aminoCode1 != null)
				if ( aminoCode1.equals(StructureTools.UNKNOWN_GROUP_LABEL))
					aminoCode1 = null;
		}

		if (current_group == null) {

			current_group = getNewGroup(recordName,aminoCode1);
			current_group.setPDBCode(residueNumber);
			current_group.setPDBName(groupCode3);
		}


		if ( startOfNewChain) {
			//System.out.println("end of chain: "+current_chain.getName()+" >"+chain_id+"<");

			current_group = getNewGroup(recordName,aminoCode1);

			current_group.setPDBCode(residueNumber);
			current_group.setPDBName(groupCode3);			
		}


		// check if residue number is the same ...
		// insertion code is part of residue number
		if ( ! residueNumber.equals(current_group.getPDBCode())) {
			//System.out.println("end of residue: "+current_group.getPDBCode()+" "+residueNumber);
			current_chain.addGroup(current_group);

			current_group = getNewGroup(recordName,aminoCode1);

			current_group.setPDBCode(residueNumber);
			current_group.setPDBName(groupCode3);

		}

		if ( headerOnly)
			return;

		atomCount++;

		if ( atomCount == ATOM_CA_THRESHOLD ) {
			// throw away the SEQRES lines - too much to deal with...
			System.err.println("more than " + ATOM_CA_THRESHOLD + " atoms in this structure, ignoring the SEQRES lines");
			seqResChains.clear();

			switchCAOnly();

		}

		if ( atomCount == MAX_ATOMS){
			System.err.println("too many atoms (>"+MAX_ATOMS+"in this protein structure.");
			System.err.println("ignoring lines after: " + line);
			return;
		}
		if ( atomCount > MAX_ATOMS){
			//System.out.println("too many atoms in this protein structure.");
			//System.out.println("ignoring line: " + line);
			return;
		}

		//TODO: treat the following residues as amino acids?
		/*
		MSE Selenomethionine
		CSE Selenocysteine
		PTR Phosphotyrosine
		SEP Phosphoserine
		TPO Phosphothreonine
		HYP 4-hydroxyproline
		5HP Pyroglutamic acid; 5-hydroxyproline
		PCA Pyroglutamic Acid
		LYZ 5-hydroxylysine
		GLX Glu or Gln
		ASX Asp or Asn
		GLA gamma-carboxy-glutamic acid
		 */
		//          1         2         3         4         5         6
		//012345678901234567890123456789012345678901234567890123456789
		//ATOM      1  N   MET     1      20.154  29.699   5.276   1.0
		//ATOM    112  CA  ASP   112      41.017  33.527  28.371  1.00  0.00
		//ATOM     53  CA  MET     7      23.772  33.989 -21.600  1.00  0.00           C
		//ATOM    112  CA  ASP   112      37.613  26.621  33.571     0     0


		String fullname = line.substring (12, 16);

		// check for CA only if requested
		if ( parseCAOnly){
			// yes , user wants to get CA only
			// only parse CA atoms...
			if (! fullname.equals(" CA ")){
				//System.out.println("ignoring " + line);
				atomCount--;
				return;
			}
		}
		// create new atom

		int pdbnumber = Integer.parseInt (line.substring (6, 11).trim ());
		AtomImpl atom = new AtomImpl() ;
		atom.setPDBserial(pdbnumber) ;

		Character altLoc   = new Character(line.substring (16, 17).charAt(0));

		atom.setAltLoc(altLoc);
		atom.setFullName(fullname) ;
		atom.setName(fullname.trim());

		double x = Double.parseDouble (line.substring (30, 38).trim());
		double y = Double.parseDouble (line.substring (38, 46).trim());
		double z = Double.parseDouble (line.substring (46, 54).trim());

		double[] coords = new double[3];
		coords[0] = x ;
		coords[1] = y ;
		coords[2] = z ;
		atom.setCoords(coords);

		double occu  = 1.0;
		if ( line.length() > 59 ) {
			try {
				// occu and tempf are sometimes not used :-/
				occu = Double.parseDouble (line.substring (54, 60).trim());
			}  catch (NumberFormatException e){}
		}

		double tempf = 0.0;
		if ( line.length() > 65) {
			try {
				tempf = Double.parseDouble (line.substring (60, 66).trim());
			}  catch (NumberFormatException e){}
		}

		atom.setOccupancy(  occu  );
		atom.setTempFactor( tempf );



		
		// Parse element from the element field. If this field is
		// missing (i.e. misformatted PDB file), then parse the
		// name from the atom name.
		Element element = Element.R;
		if ( line.length() > 77 ) {
			// parse element from element field
			try {
				element = Element.valueOfIgnoreCase(line.substring (76, 78).trim());
			}  catch (IllegalArgumentException e){}
		} else {
			// parse the name from the atom name
			String elementSymbol = null;
			// for atom names with 4 characters, the element is
			// at the first position, example HG23 in Valine
			if (fullname.trim().length() == 4) {
				elementSymbol = fullname.substring(0, 1);
			} else if ( fullname.trim().length() > 1){
				elementSymbol = fullname.substring(0, 2).trim();
			} else {
				// unknown element...
				elementSymbol = "R";
			}
				
			try {
			element = Element.valueOfIgnoreCase(elementSymbol);
			}  catch (IllegalArgumentException e){}
		}
		atom.setElement(element);
		
		
		//see if chain_id is one of the previous chains ...
		current_group.addAtom(atom);
		//System.out.println(current_group);
	}


	private void switchCAOnly(){
		parseCAOnly = true;

		current_model = CAConverter.getCAOnly(current_model);

		for ( int i =0; i< structure.nrModels() ; i++){
			//  iterate over all known models ...
			List<Chain> model = structure.getModel(i);
			model = CAConverter.getCAOnly(model);
			structure.setModel(i,model);
		}

		current_chain = CAConverter.getCAOnly(current_chain);

	}


	/** safes repeating a few lines ... */
	private Integer conect_helper (String line,int start,int end) {
		String sbond = line.substring(start,end).trim();
		int bond  = -1 ;
		Integer b = null ;

		if ( ! sbond.equals("")) {
			bond = Integer.parseInt(sbond);
			b = new Integer(bond);
		}

		return b ;
	}

	/**
	 Handler for
	 CONECT Record Format

	 COLUMNS         DATA TYPE        FIELD           DEFINITION
	 ---------------------------------------------------------------------------------
	 1 -  6         Record name      "CONECT"
	 7 - 11         Integer          serial          Atom serial number
	 12 - 16         Integer          serial          Serial number of bonded atom
	 17 - 21         Integer          serial          Serial number of bonded atom
	 22 - 26         Integer          serial          Serial number of bonded atom
	 27 - 31         Integer          serial          Serial number of bonded atom
	 32 - 36         Integer          serial          Serial number of hydrogen bonded
	 atom
	 37 - 41         Integer          serial          Serial number of hydrogen bonded
	 atom
	 42 - 46         Integer          serial          Serial number of salt bridged
	 atom
	 47 - 51         Integer          serial          Serial number of hydrogen bonded
	 atom
	 52 - 56         Integer          serial          Serial number of hydrogen bonded
	 atom
	 57 - 61         Integer          serial          Serial number of salt bridged
	 atom
	 */
	private void pdb_CONECT_Handler(String line) {
		//System.out.println(line);
		// this try .. catch is e.g. to catch 1gte which has wrongly formatted lines...
		if ( atomOverflow) {
			return ;
		}
		try {
			int atomserial = Integer.parseInt (line.substring(6 ,11).trim());
			Integer bond1      = conect_helper(line,11,16);
			Integer bond2      = conect_helper(line,16,21);
			Integer bond3      = conect_helper(line,21,26);
			Integer bond4      = conect_helper(line,26,31);
			Integer hyd1       = conect_helper(line,31,36);
			Integer hyd2       = conect_helper(line,36,41);
			Integer salt1      = conect_helper(line,41,46);
			Integer hyd3       = conect_helper(line,46,51);
			Integer hyd4       = conect_helper(line,51,56);
			Integer salt2      = conect_helper(line,56,61);

			//System.out.println(atomserial+ " "+ bond1 +" "+bond2+ " " +bond3+" "+bond4+" "+
			//		   hyd1+" "+hyd2 +" "+salt1+" "+hyd3+" "+hyd4+" "+salt2);
			HashMap<String, Integer> cons = new HashMap<String, Integer>();
			cons.put("atomserial",new Integer(atomserial));

			if ( bond1 != null) cons.put("bond1",bond1);
			if ( bond2 != null) cons.put("bond2",bond2);
			if ( bond3 != null) cons.put("bond3",bond3);
			if ( bond4 != null) cons.put("bond4",bond4);
			if ( hyd1  != null) cons.put("hydrogen1",hyd1);
			if ( hyd2  != null) cons.put("hydrogen2",hyd2);
			if ( salt1 != null) cons.put("salt1",salt1);
			if ( hyd3  != null) cons.put("hydrogen3",hyd3);
			if ( hyd4  != null) cons.put("hydrogen4",hyd4);
			if ( salt2 != null) cons.put("salt2",salt2);

			connects.add(cons);
		} catch (Exception e){
			System.err.println("could not parse CONECT line correctly.");
			System.err.println(e.getMessage() + " at line " + line);
			return;
		}
	}

	/*
	 Handler for
	 MODEL Record Format

	 COLUMNS       DATA TYPE      FIELD         DEFINITION
	 ----------------------------------------------------------------------
	 1 -  6       Record name    "MODEL "
	 11 - 14       Integer        serial        Model serial number.
	 */

	private void pdb_MODEL_Handler(String line) {
		// check beginning of file ...
		if (current_chain != null) {
			if (current_group != null) {
				current_chain.addGroup(current_group);
			}
			//System.out.println("starting new model "+(structure.nrModels()+1));

			Chain ch = isKnownChain(current_chain.getName(),current_model) ;
			if ( ch == null ) {
				current_model.add(current_chain);
			}
			structure.addModel(current_model);
			current_model = new ArrayList<Chain>();
			current_chain = null;
			current_group = null;
		}

	}


	/**
    COLUMNS       DATA TYPE          FIELD          DEFINITION
    ----------------------------------------------------------------
     1 - 6        Record name        "DBREF "
     8 - 11       IDcode             idCode         ID code of this entry.
    13            Character          chainID        Chain identifier.
    15 - 18       Integer            seqBegin       Initial sequence number
                                                    of the PDB sequence segment.
    19            AChar              insertBegin    Initial insertion code
                                                    of the PDB sequence segment.
    21 - 24       Integer            seqEnd         Ending sequence number
                                                    of the PDB sequence segment.
    25            AChar              insertEnd      Ending insertion code
                                                    of the PDB sequence segment.
    27 - 32       LString            database       Sequence database name.
    34 - 41       LString            dbAccession    Sequence database accession code.
    43 - 54      LString            dbIdCode        Sequence database
                                                    identification code.
    56 - 60      Integer            dbseqBegin      Initial sequence number of the
                                                    database seqment.
    61           AChar              idbnsBeg        Insertion code of initial residue
                                                    of the segment, if PDB is the
                                                    reference.
    63 - 67      Integer            dbseqEnd        Ending sequence number of the
                                                    database segment.
    68           AChar              dbinsEnd        Insertion code of the ending
                                                    residue of the segment, if PDB is
                                                    the reference.
	 */
	private void pdb_DBREF_Handler(String line){
		if (DEBUG) {
			logger.info("Parsing DBREF " + line);
		}
		DBRef dbref = new DBRef();
		String idCode      = line.substring(7,11);
		String chainId     = line.substring(12,13);
		String seqBegin    = line.substring(14,18);
		String insertBegin = line.substring(18,19);
		String seqEnd      = line.substring(20,24);
		String insertEnd   = line.substring(24,25);
		String database    = line.substring(26,32);
		String dbAccession = line.substring(33,41);
		String dbIdCode    = line.substring(42,54);
		String dbseqBegin  = line.substring(55,60);
		String idbnsBeg    = line.substring(60,61);
		String dbseqEnd    = line.substring(62,67);
		String dbinsEnd    = line.substring(67,68);

		dbref.setIdCode(idCode);
		dbref.setChainId(chainId.charAt(0));
		dbref.setSeqBegin(intFromString(seqBegin));
		dbref.setInsertBegin(insertBegin.charAt(0));
		dbref.setSeqEnd(intFromString(seqEnd));
		dbref.setInsertEnd(insertEnd.charAt(0));
		dbref.setDatabase(database.trim());
		dbref.setDbAccession(dbAccession.trim());
		dbref.setDbIdCode(dbIdCode.trim());
		dbref.setDbSeqBegin(intFromString(dbseqBegin));
		dbref.setIdbnsBegin(idbnsBeg.charAt(0));
		dbref.setDbSeqEnd(intFromString(dbseqEnd));
		dbref.setIdbnsEnd(dbinsEnd.charAt(0));

		//System.out.println(dbref.toPDB());
		dbrefs.add(dbref);
	}

	/* process the disulfid bond info provided by an SSBOND record
	 *
	 *
	COLUMNS        DATA TYPE       FIELD         DEFINITION
	-------------------------------------------------------------------
	 1 -  6        Record name     "SSBOND"
	 8 - 10        Integer         serNum       Serial number.
	12 - 14        LString(3)      "CYS"        Residue name.
	16             Character       chainID1     Chain identifier.
	18 - 21        Integer         seqNum1      Residue sequence number.
	22             AChar           icode1       Insertion code.
	26 - 28        LString(3)      "CYS"        Residue name.
	30             Character       chainID2     Chain identifier.
	32 - 35        Integer         seqNum2      Residue sequence number.
	36             AChar           icode2       Insertion code.
	60 - 65        SymOP           sym1         Symmetry oper for 1st resid
	67 - 72        SymOP           sym2         Symmetry oper for 2nd resid
	 */
	private void pdb_SSBOND_Handler(String line){
		String chain1      = line.substring(15,16);
		String seqNum1     = line.substring(18,21).trim();
		String icode1      = line.substring(21,22);
		String chain2      = line.substring(29,30);
		String seqNum2     = line.substring(31,35).trim();
		String icode2      = line.substring(35,36);

		if (icode1.equals(" "))
			icode1 = "";
		if (icode2.equals(" "))
			icode2 = "";

		SSBond ssbond = new SSBond();

		ssbond.setChainID1(chain1);
		ssbond.setResnum1(seqNum1);
		ssbond.setChainID2(chain2);
		ssbond.setResnum2(seqNum2);
		ssbond.setInsCode1(icode1);
		ssbond.setInsCode2(icode2);
		structure.addSSBond(ssbond);


	}


	private int intFromString(String intString){
		int val = Integer.MIN_VALUE;
		try {
			val = Integer.parseInt(intString.trim());
		} catch (NumberFormatException ex){
			//ex.printStackTrace();
			System.err.println("NumberformatException: " + ex.getMessage());
		}
		return val;
	}



	/** test if the chain is already known (is in current_model
	 * ArrayList) and if yes, returns the chain
	 * if no -> returns null
	 */
	private Chain isKnownChain(String chainID, List<Chain> chains){

		for (int i = 0; i< chains.size();i++){
			Chain testchain =  chains.get(i);
			//System.out.println("comparing chainID >"+chainID+"< against testchain " + i+" >" +testchain.getName()+"<");
			if (chainID.equals(testchain.getName())) {
				//System.out.println("chain "+ chainID+" already known ...");
				return testchain;
			}
		}

		return null;
	}



	private BufferedReader getBufferedReader(InputStream inStream)
	throws IOException {

		BufferedReader buf ;
		if (inStream == null) {
			throw new IOException ("input stream is null!");
		}

		buf = new BufferedReader (new InputStreamReader (inStream));
		return buf ;

	}



	/** parse a PDB file and return a datastructure implementing
	 * PDBStructure interface.
	 *
	 * @param inStream  an InputStream object
	 * @return a Structure object
	 * @throws IOException
	 */
	public Structure parsePDBFile(InputStream inStream)
	throws IOException
	{

		//System.out.println("preparing buffer");
		BufferedReader buf ;
		try {
			buf = getBufferedReader(inStream);

		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException ("error initializing BufferedReader");
		}
		//System.out.println("done");

		return parsePDBFile(buf);

	}

	/** parse a PDB file and return a datastructure implementing
	 * PDBStructure interface.
	 *
	 * @param buf  a BufferedReader object
	 * @return the Structure object
	 * @throws IOException ...
	 */

	public Structure parsePDBFile(BufferedReader buf)
	throws IOException
	{
		// (re)set structure

		structure     = new StructureImpl() ;
		current_model = new ArrayList<Chain>();
		seqResChains  = new ArrayList<Chain>();
		current_chain = null           ;
		current_group = null           ;
		header        = init_header();
		pdbHeader     = new PDBHeader();
		connects      = new ArrayList<Map<String,Integer>>();
		continuationField = "";
		continuationString = "";
		current_compound = new Compound();
		sourceLines.clear();
		compndLines.clear();
		isLastCompndLine = false;
		isLastSourceLine = false;
		molTypeCounter = 1;
		compounds.clear();
		helixList.clear();
		strandList.clear();
		turnList.clear();
		lengthCheck = -1;
		atomCount = 0;
		atomOverflow = false;
		String line = null;
		try {

			line = buf.readLine ();
			String recordName = "";

			// if line is null already for the first time, the buffered Reader had a problem
			if ( line == null ) {
				throw new IOException ("could not parse PDB File, BufferedReader returns null!");
			}



			while (line != null) {

				// System.out.println (">"+line+"<");

				// ignore empty lines
				if ( line.equals("") ||
						(line.equals(NEWLINE))){

					line = buf.readLine ();
					continue;
				}


				// ignore short TER and END lines
				if ( (line.startsWith("TER")) ||
						(line.startsWith("END"))) {

					line = buf.readLine ();
					continue;
				}

				if ( line.length() < 6) {
					System.err.println("found line length < 6. ignoring it. >" + line +"<" );
					line = buf.readLine ();
					continue;
				}

				try {
					recordName = line.substring (0, 6).trim ();

				} catch (StringIndexOutOfBoundsException e){

					System.err.println("StringIndexOutOfBoundsException at line >" + line + "<" + NEWLINE +
					"this does not look like an expected PDB file") ;
					e.printStackTrace();
					throw new StringIndexOutOfBoundsException(e.getMessage());

				}

				//System.out.println(recordName);

				try {
					if (recordName.equals("ATOM"))
						pdb_ATOM_Handler(line);
					else if (recordName.equals("SEQRES"))
						pdb_SEQRES_Handler(line);
					else if (recordName.equals("HETATM"))
						pdb_ATOM_Handler(line);
					else if (recordName.equals("MODEL"))
						pdb_MODEL_Handler(line);
					else if (recordName.equals("HEADER"))
						pdb_HEADER_Handler(line);
					else if (recordName.equals("AUTHOR"))
						pdb_AUTHOR_Handler(line);
					else if (recordName.equals("TITLE"))
						pdb_TITLE_Handler(line);
					else if (recordName.equals("SOURCE"))
						sourceLines.add(line); //pdb_SOURCE_Handler
					else if (recordName.equals("COMPND"))
						compndLines.add(line); //pdb_COMPND_Handler
					else if (recordName.equals("JRNL"))
						pdb_JRNL_Handler(line);
					else if (recordName.equals("EXPDTA"))
						pdb_EXPDTA_Handler(line);
					else if (recordName.equals("REMARK"))
						pdb_REMARK_Handler(line);
					else if (recordName.equals("CONECT"))
						pdb_CONECT_Handler(line);
					else if (recordName.equals("REVDAT"))
						pdb_REVDAT_Handler(line);
					else if (recordName.equals("DBREF"))
						pdb_DBREF_Handler(line);
					else if (recordName.equals("SSBOND"))
						pdb_SSBOND_Handler(line);
					else if ( parseSecStruc) {
						if ( recordName.equals("HELIX") ) pdb_HELIX_Handler (  line ) ;
						else if (recordName.equals("SHEET")) pdb_SHEET_Handler(line ) ;
						else if (recordName.equals("TURN")) pdb_TURN_Handler(   line ) ;
					}
					else {
						// this line type is not supported, yet.
						// we ignore it
					}
				} catch (Exception e){
					// the line is badly formatted, ignore it!
					e.printStackTrace();
					System.err.println("badly formatted line ... " + line);
				}
				line = buf.readLine ();
			}

			makeCompounds(compndLines, sourceLines);

			triggerEndFileChecks();

		} catch (Exception e) {
			System.err.println(line);
			e.printStackTrace();
			throw new IOException ("Error parsing PDB file");
		}

		if ( parseSecStruc)
			setSecStruc();


		return structure;

	}

	/**
	 * This is the new method for building the COMPND and SOURCE records. Now each method is self-contained.
	 * @author Jules Jacobsen
	 * @param  compoundList
	 * @param  sourceList
	 */
	private void makeCompounds(List<String> compoundList,
			List<String> sourceList) {
		//		System.out.println("[makeCompounds] making compounds from compoundLines");

		for (String line : compoundList) {
			if (compoundList.indexOf(line) + 1 == compoundList.size()) {
				//				System.out.println("[makeCompounds] Final line in compoundLines.");
				isLastCompndLine = true;
			}
			pdb_COMPND_Handler(line);

		}
		//		System.out.println("[makeCompounds] adding sources to compounds from sourceLines");
		// since we're starting again from the first compound, reset it here
		if ( compounds.size() == 0){
			current_compound = new Compound();
		} else {
			current_compound = compounds.get(0);
		}
		for (String line : sourceList) {
			if (sourceList.indexOf(line) + 1 == sourceList.size()) {
				//				System.out.println("[makeCompounds] Final line in sourceLines.");
				isLastSourceLine = true;
			}
			pdb_SOURCE_Handler(line);
		}

	}


	private void triggerEndFileChecks(){

		// finish and add ...

		String modDate = (String) header.get("modDate");
		if ( modDate.equals("0000-00-00") ) {
			// modification date = deposition date
			String depositionDate = (String) header.get("depDate");
			header.put("modDate",depositionDate) ;

			if (! depositionDate.equals(modDate)){
				// depDate is 0000-00-00

				try {
					Date dep = dateFormat.parse(depositionDate);
					pdbHeader.setDepDate(dep);
				} catch (ParseException e){
					e.printStackTrace();
				}
			}

		}

		// a problem occurred earlier so current_chain = null ...
		// most likely the buffered reader did not provide data ...
		if ( current_chain != null ) {			
			current_chain.addGroup(current_group);

			if (isKnownChain(current_chain.getName(),current_model) == null) {
				current_model.add(current_chain);
			}
		}

		//set the JournalArticle, if there is one
		if (!journalLines.isEmpty()) {
			buildjournalArticle();
			structure.setJournalArticle(journalArticle);
		}

		structure.addModel(current_model);
		structure.setHeader(header);
		structure.setPDBHeader(pdbHeader);
		structure.setConnections(connects);
		structure.setCompounds(compounds);
		structure.setDBRefs(dbrefs);

		if ( alignSeqRes ){

			SeqRes2AtomAligner aligner = new SeqRes2AtomAligner();
			aligner.align(structure,seqResChains);
		}


		linkChains2Compound(structure);

	}


	private void setSecStruc(){

		setSecElement(helixList,  PDB_AUTHOR_ASSIGNMENT, HELIX  );
		setSecElement(strandList, PDB_AUTHOR_ASSIGNMENT, STRAND );
		setSecElement(turnList,   PDB_AUTHOR_ASSIGNMENT, TURN   );

	}

	private void setSecElement(List<Map<String,String>> secList, String assignment, String type){


		Iterator<Map<String,String>> iter = secList.iterator();
		nextElement:
			while (iter.hasNext()){
				Map<String,String> m = iter.next();

				// assign all residues in this range to this secondary structure type
				// String initResName = (String)m.get("initResName");
				String initChainId = (String)m.get("initChainId");
				String initSeqNum  = (String)m.get("initSeqNum" );
				String initICode   = (String)m.get("initICode" );
				// String endResName  = (String)m.get("endResName" );
				String endChainId  = (String)m.get("endChainId" );
				String endSeqNum   = (String)m.get("endSeqNum");
				String endICode    = (String)m.get("endICode");

				if (initICode.equals(" "))
					initICode = "";
				if (endICode.equals(" "))
					endICode = "";



				GroupIterator gi = new GroupIterator(structure);
				boolean inRange = false;
				while (gi.hasNext()){
					Group g = (Group)gi.next();
					Chain c = g.getParent();

					if (c.getName().equals(initChainId)){

						String pdbCode = initSeqNum + initICode;
						if ( g.getPDBCode().equals(pdbCode)  ) {
							inRange = true;
						}
					}
					if ( inRange){
						if ( g instanceof AminoAcid) {
							AminoAcid aa = (AminoAcid)g;

							Map<String,String> assignmentMap = new HashMap<String,String>();
							assignmentMap.put(assignment,type);
							aa.setSecStruc(assignmentMap);
						}

					}
					if ( c.getName().equals(endChainId)){
						String pdbCode = endSeqNum + endICode;
						if (pdbCode.equals(g.getPDBCode())){
							inRange = false;
							continue nextElement;
						}
					}

				}

			}
	}


	/** After the parsing of a PDB file the {@link Chain} and  {@link Compound}
	 * objects need to be linked to each other.
	 *
	 * @param s the structure
	 */
	public void linkChains2Compound(Structure s){
		List<Compound> compounds = s.getCompounds();

		for(Compound comp : compounds){
			List<Chain> chains = new ArrayList<Chain>();
			List<String> chainIds = comp.getChainId();
			if ( chainIds == null)
				continue;
			for ( String chainId : chainIds) {
				if ( chainId.equals("NULL"))
					chainId = " ";
				try {

					Chain c = s.findChain(chainId);
					chains.add(c);

				} catch (StructureException e){
					// usually if this happens something is wrong with the PDB header
					// e.g. 2brd - there is no Chain A, although it is specified in the header
					e.printStackTrace();
				}
			}
			comp.setChains(chains);
		}

		if ( compounds.size() == 1) {
			Compound comp = compounds.get(0);
			if ( comp.getChainId() == null){
				List<Chain> chains = s.getChains(0);
				if ( chains.size() == 1) {
					// this is an old style PDB file - add the ChainI
					Chain ch = chains.get(0);
					List <String> chainIds = new ArrayList<String>();
					chainIds.add(ch.getName());
					comp.setChainId(chainIds);
					comp.addChain(ch);
				}
			}
		}

		for (Compound comp: compounds){
			if ( comp.getChainId() == null) {
				// could not link to chain
				// TODO: should this be allowed to happen?
				continue;
			}
			for ( String chainId : comp.getChainId()){
				if ( chainId.equals("NULL"))
					continue;
				try {
					Chain c = s.getChainByPDB(chainId);
					c.setHeader(comp);
				} catch (StructureException e){
					e.printStackTrace();
				}
			}
		}

	}
	private void buildjournalArticle() {
		if (DEBUG) {
			System.out.println("building new JournalArticle");
			//            for (String line : journalLines) {
			//                System.out.println(line);
			//            }
		}
		this.journalArticle = new JournalArticle();
		//        JRNL        AUTH   M.HAMMEL,G.SFYROERA,D.RICKLIN,P.MAGOTTI,
		//        JRNL        AUTH 2 J.D.LAMBRIS,B.V.GEISBRECHT
		//        JRNL        TITL   A STRUCTURAL BASIS FOR COMPLEMENT INHIBITION BY
		//        JRNL        TITL 2 STAPHYLOCOCCUS AUREUS.
		//        JRNL        REF    NAT.IMMUNOL.                  V.   8   430 2007
		//        JRNL        REFN                   ISSN 1529-2908
		//        JRNL        PMID   17351618
		//        JRNL        DOI    10.1038/NI1450
		StringBuffer auth = new StringBuffer();
		StringBuffer titl = new StringBuffer();
		StringBuffer edit = new StringBuffer();
		StringBuffer ref = new StringBuffer();
		StringBuffer publ = new StringBuffer();
		StringBuffer refn = new StringBuffer();
		StringBuffer pmid = new StringBuffer();
		StringBuffer doi = new StringBuffer();

		for (String line : journalLines) {
			if ( line.length() < 19 ) {
				System.err.println("can not process Journal line: " + line);
				continue;
			}
			//            System.out.println("'" + line + "'");
			String subField = line.substring(12, 16);
			//            System.out.println("'" + subField + "'");
			if (subField.equals("AUTH")) {
				auth.append(line.substring(19, line.length()).trim());
				if (DEBUG) {
					System.out.println("AUTH '" + auth.toString() + "'");
				}
			}
			if (subField.equals("TITL")) {
				//add a space to the end of a line so that when wrapped the
				//words on the join won't be concatenated
				titl.append(line.substring(19, line.length()).trim() + " ");
				if (DEBUG) {
					System.out.println("TITL '" + titl.toString() + "'");
				}
			}
			if (subField.equals("EDIT")) {
				edit.append(line.substring(19, line.length()).trim());
				if (DEBUG) {
					System.out.println("EDIT '" + edit.toString() + "'");
				}
			}
			//        JRNL        REF    NAT.IMMUNOL.                  V.   8   430 2007
			if (subField.equals("REF ")) {
				ref.append(line.substring(19, line.length()).trim() + " ");
				if (DEBUG) {
					System.out.println("REF '" + ref.toString() + "'");
				}
			}
			if (subField.equals("PUBL")) {
				publ.append(line.substring(19, line.length()).trim() + " ");
				if (DEBUG) {
					System.out.println("PUBL '" + publ.toString() + "'");
				}
			}
			//        JRNL        REFN                   ISSN 1529-2908
			if (subField.equals("REFN")) {
				if ( line.length() < 35 ) {
					System.err.println("can not process Journal REFN line: " + line);
					continue;
				}
				refn.append(line.substring(35, line.length()).trim());
				if (DEBUG) {
					System.out.println("REFN '" + refn.toString() + "'");
				}
			}
			//        JRNL        PMID   17351618
			if (subField.equals("PMID")) {
				pmid.append(line.substring(19, line.length()).trim());
				if (DEBUG) {
					System.out.println("PMID '" + pmid.toString() + "'");
				}
			}
			//        JRNL        DOI    10.1038/NI1450
			if (subField.equals("DOI ")) {
				doi.append(line.substring(19, line.length()).trim());
				if (DEBUG) {
					System.out.println("DOI '" + doi.toString() + "'");
				}
			}
		}

		//now set the parts of the JournalArticle
		journalArticle.setAuthorList(authorBuilder(auth.toString()));
		journalArticle.setEditorList(authorBuilder(edit.toString()));
		journalArticle.setRef(ref.toString());
		JournalParser journalParser = new JournalParser(ref.toString());
		journalArticle.setJournalName(journalParser.getJournalName());
		if (!journalArticle.getJournalName().equals("TO BE PUBLISHED")) {
			journalArticle.setIsPublished(true);
		}
		journalArticle.setVolume(journalParser.getVolume());
		journalArticle.setStartPage(journalParser.getStartPage());
		journalArticle.setPublicationDate(journalParser.getPublicationDate());
		journalArticle.setPublisher(publ.toString().trim());
		journalArticle.setTitle(titl.toString().trim());
		journalArticle.setRefn(refn.toString().trim());
		journalArticle.setPmid(pmid.toString().trim());
		journalArticle.setDoi(doi.toString().trim());

	}

	//inner class to deal with all the journal info
	private class JournalParser {

		private String journalName;
		private String volume;
		private String startPage;
		private int publicationDate;


		public JournalParser(String ref) {
			if (DEBUG) {
				System.out.println("JournalParser init '" + ref + "'");
			}

			if (ref.equals("TO BE PUBLISHED ")) {
				journalName = ref.trim();
				return;
			}

			//check the line is the correct length
			if (ref.length() != 48) {
				logger.warning(pdbId + " REF line not of correct length. Found " + ref.length() + ", should be 48. Returning dummy JRNL object.");
				journalName = "TO BE PUBLISHED";
				return;
			}


			//REF    NUCLEIC ACIDS RES.                         2009
			//REF    MOL.CELL                                   2009
			//REF    NAT.STRUCT.MOL.BIOL.          V.  16   238 2009
			//REF    ACTA CRYSTALLOGR.,SECT.F      V.  65   199 2009
			//check if the date is present at the end of the line.
			//                             09876543210987654321
			//'J.AM.CHEM.SOC.                V. 130 16011 2008 '
			//'NAT.STRUCT.MOL.BIOL.          V.  16   238 2009'
			String dateString = ref.substring(ref.length() - 5 , ref.length() - 1).trim();
			String startPageString = ref.substring(ref.length() - 11 , ref.length() - 6).trim();
			String volumeString = ref.substring(ref.length() - 14 , ref.length() - 12).trim();
			String journalString = ref.substring(0 , ref.length() - 18).trim();
			if (DEBUG) {
				System.out.println("JournalParser found volumeString " + volumeString);
				System.out.println("JournalParser found startPageString " + startPageString);
				System.out.println("JournalParser found dateString " + dateString);
			}

			if (!dateString.equals("    ")) {
				publicationDate = Integer.valueOf(dateString);
				if (DEBUG) {
					System.out.println("JournalParser set date " + publicationDate);
				}
			}

			if (!startPageString.equals("    ")) {
				startPage = startPageString;
				if (DEBUG) {
					System.out.println("JournalParser set startPage " + startPage);
				}
			}

			if (!volumeString.equals("    ")) {
				volume = volumeString;
				if (DEBUG) {
					System.out.println("JournalParser set volume " + volume);
				}
			}

			if (!journalString.equals("    ")) {
				journalName = journalString;
				if (DEBUG) {
					System.out.println("JournalParser set journalName " + journalName);
				}
			}
		}

		private String getJournalName() {
			return journalName;
		}

		private int getPublicationDate() {
			return publicationDate;
		}

		private String getStartPage() {
			return startPage;
		}

		private String getVolume() {
			return volume;
		}
	}

	private List<Author> authorBuilder(String authorString) {
		ArrayList<Author> authorList = new ArrayList<Author>();

		if (authorString.equals("")) {
			return authorList;
		}

		String[] authors = authorString.split(",");
		//        if (DEBUG) {
		//            for (int i = 0; i < authors.length; i++) {
		//                String string = authors[i];
		//                System.out.println("authorBuilder author: '" + string + "'");
		//            }
		//        }
		//        AUTH   SEATTLE STRUCTURAL GENOMICS CENTER FOR INFECTIOUS
		//        AUTH 2 DISEASE (SSGCID)
		//        or
		//        AUTH   E.DOBROVETSKY,A.DONG,A.SEITOVA,B.DUNCAN,L.CROMBET,
		//        AUTH 2 M.SUNDSTROM,C.H.ARROWSMITH,A.M.EDWARDS,C.BOUNTRA,
		//        AUTH 3 A.BOCHKAREV,D.COSSAR,
		//        AUTH 4 STRUCTURAL GENOMICS CONSORTIUM (SGC)
		//        or
		//        AUTH   T.-C.MOU,S.R.SPRANG,N.MASADA,D.M.F.COOPER
		if (authors.length == 1) {
			//only one element means it's a consortium only
			Author author = new Author();
			author.setSurname(authors[0]);
			if (DEBUG) {
				System.out.println("Set consortium author name " + author.getSurname());
			}
			authorList.add(author);
		} else {
			for (int i = 0; i < authors.length; i++) {
				String authorFullName = authors[i];
				if (DEBUG) {
					System.out.println("Building author " + authorFullName);
				}
				Author author = new Author();
				String regex = "\\.";
				String[] authorNames = authorFullName.split(regex);
				//                if (DEBUG) {
				//                    System.out.println("authorNames size " + authorNames.length);
				//                    for (int j = 0; j < authorNames.length; j++) {
				//                        String name = authorNames[j];
				//                        System.out.println("split authName '" + name + "'");
				//
				//                    }
				//                }
				if (authorNames.length == 0) {
					author.setSurname(authorFullName);
					if (DEBUG) {
						System.out.println("Unable to split using '" + regex + "' Setting whole name " + author.getSurname());
					}
				}
				//again there might be a consortium name so there may be no elements
				else if (authorNames.length == 1) {
					author.setSurname(authorNames[0]);
					if (DEBUG) {
						System.out.println("Set consortium author name in multiple author block " + author.getSurname
								());
					}
				} else {
					String initials = "";
					for (int j = 0; j < authorNames.length - 1; j++) {
						String initial = authorNames[j];
						//                        if (DEBUG) {
						//                            System.out.println("adding initial '" + initial + "'");
						//                        }
						//build the initials back up again
						initials += initial + ".";
					}
					if (DEBUG) {
						System.out.println("built initials '" + initials + "'");
					}
					author.setInitials(initials);
					//surname is always last
					int lastName = authorNames.length - 1;
					String surname = authorNames[lastName];
					if (DEBUG) {
						System.out.println("built author surname " + surname);
					}
					author.setSurname(surname);

				}
				authorList.add(author);
			}
		}
		return authorList;
	}
	public void setHeaderOnly(boolean headerOnly) {
		this.headerOnly = headerOnly;

	}

	public boolean isHeaderOnly(){
		return headerOnly;
	}


}
