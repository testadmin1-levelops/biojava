/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.biojava3.core.sequence.io;

import java.util.ArrayList;

import org.biojava3.core.sequence.AccessionID;
import org.biojava3.core.sequence.DataSource;
import org.biojava3.core.sequence.ProteinSequence;
import org.biojava3.core.sequence.compound.AminoAcidCompound;
import org.biojava3.core.sequence.io.template.FastaHeaderParserInterface;
import org.biojava3.core.sequence.template.AbstractSequence;
import org.biojava3.core.sequence.template.Compound;
import org.biojava3.core.sequence.template.AbstractSequence.AnnotationType;

/**
 * GenBank                           gi|gi-number|gb|accession|locus
 * EMBL Data Library                 gi|gi-number|emb|accession|locus
 * DDBJ, DNA Database of Japan       gi|gi-number|dbj|accession|locus
 * NBRF PIR                          pir||entry
 * Protein Research Foundation       prf||name
 * SWISS-PROT                        sp|accession|name
 * Brookhaven Protein Data Bank (1)  pdb|entry|chain
 * Brookhaven Protein Data Bank (2)  entry:chain|PDBID|CHAIN|SEQUENCE
 * PDB EBI                           PDB:1ECY_A mol:protein length:142  ECOTIN
 * Patents                           pat|country|number
 * GenInfo Backbone Id               bbs|number
 * General database identifier       gnl|database|identifier
 * NCBI Reference Sequence           ref|accession|locus
 * Local Sequence identifier         lcl|identifier
 *
 * @author Scooter Willis <willishf at gmail dot com>
 */
public class GenericFastaHeaderParser<S extends AbstractSequence<C>, C extends Compound> implements FastaHeaderParserInterface<S,C> {

    public String[] getHeaderValues(String header) {
        String[] data = new String[0];
        ArrayList<String> values = new ArrayList<String>();
        StringBuffer sb = new StringBuffer();
        if(header.indexOf("length=") != -1){
            data = new String[1];
            int index = header.indexOf("length=");
            data[0] = header.substring(0, index).trim();
    //        System.out.println("accession=" + data[0]);
            return data;
        } else if (header.startsWith("PDB:") == false) {
            for (int i = 0; i < header.length(); i++) {
                if (header.charAt(i) == '|') {
                    values.add(sb.toString());
                    sb = new StringBuffer();
                } else if (i == header.length() - 1) {
                    sb.append(header.charAt(i));
                    values.add(sb.toString());
                } else {
                    sb.append(header.charAt(i));
                }

                data = new String[values.size()];
                values.toArray(data);
            }
        } else {
            data = header.split(" ");
        }
        return data;
    }

    public void parseHeader(String header, S sequence) {
        //uniptrot
        // tr|Q0TET7|Q0TET7_ECOL5 Putative uncharacterized protein OS=Escherichia coli O6:K15:H31 (strain 536 / UPEC) GN=ECP_2553 PE=4 SV=1
        sequence.setOriginalHeader(header);
        String[] data = getHeaderValues(header);

        if (data.length == 1) {
            sequence.setAccession(new AccessionID(data[0]));
        } else  if (data[0].equalsIgnoreCase("sp") || data[0].equalsIgnoreCase("tr")) {
            if (data[0].equalsIgnoreCase("sp")) {
                sequence.setAnnotationType(AnnotationType.CURATED);
            } else {
                sequence.setAnnotationType(AnnotationType.PREDICTED);
            }

            sequence.setAccession(new AccessionID(data[1], DataSource.UNIPROT));
            if (data.length > 1) {
                sequence.setDescription(data[2]);
            }

        } else if (data[0].equalsIgnoreCase("gi")) {
            DataSource giSource = DataSource.UNKNOWN;
            if (data.length >= 3) {
                if (data[2].equalsIgnoreCase("gb")) {
                    giSource = DataSource.GENBANK;
                } else if (data[2].equalsIgnoreCase("emb")) {
                    giSource = DataSource.EMBL;
                } else if (data[2].equalsIgnoreCase("dbj")) {
                    giSource = DataSource.DDBJ;
                }
                sequence.setAccession(new AccessionID(data[3], giSource));
            } else {
                sequence.setAccession(new AccessionID(header, giSource));
            }
        } else if (data[0].equalsIgnoreCase("pir")) {
            sequence.setAccession(new AccessionID(data[2], DataSource.NBRF));
        } else if (data[0].equalsIgnoreCase("prf")) {
            sequence.setAccession(new AccessionID(data[2], DataSource.PRF));
        } else if (data[0].equalsIgnoreCase("pdb")) {
            sequence.setAccession(new AccessionID(data[1] + ":" + data[2], DataSource.PDB1));
        } else if (data[0].startsWith("PDB")) {
            String[] pdbe = data[0].split(" ");
            String[] pdbaccession = pdbe[0].split(":");
            sequence.setAccession(new AccessionID(pdbaccession[1], DataSource.PDBe));
        } else if (data[0].indexOf(":") != -1 && data.length > 1 && data[1].equals("PDBID")) {
            sequence.setAccession(new AccessionID(data[0], DataSource.PDB2));
        } else if (data[0].equalsIgnoreCase("pat")) {
            sequence.setAccession(new AccessionID(data[2], DataSource.PATENTS));
        } else if (data[0].equalsIgnoreCase("bbs")) {
            sequence.setAccession(new AccessionID(data[1], DataSource.GENINFO));
        } else if (data[0].equalsIgnoreCase("gnl")) {
            sequence.setAccession(new AccessionID(data[2], DataSource.GENERAL));
        } else if (data[0].equalsIgnoreCase("ref")) {
            sequence.setAccession(new AccessionID(data[1], DataSource.NCBI));
        } else if (data[0].equalsIgnoreCase("lcl")) {
            sequence.setAccession(new AccessionID(data[1], DataSource.LOCAL));
        } else {
            sequence.setAccession(new AccessionID(header));
        }


    }

    public static void main(String[] args) {

        System.out.println("parseHeader");
        String header = "";
        ProteinSequence sequence = new ProteinSequence("");
        GenericFastaHeaderParser<ProteinSequence,AminoAcidCompound> instance =
          new GenericFastaHeaderParser<ProteinSequence,AminoAcidCompound>();

        header = "gi|gi-number|gb|accession|locus";
        instance.parseHeader(header, sequence);
        System.out.println("accession" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.GENBANK);

        header = "gi|gi-number|emb|accession|locus";
        instance.parseHeader(header, sequence);
        System.out.println("accession" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.EMBL);

        header = "gi|gi-number|dbj|accession|locus";
        instance.parseHeader(header, sequence);
        System.out.println("accession" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.DDBJ);

        header = "pir||entry";
        instance.parseHeader(header, sequence);
        System.out.println("entry" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.NBRF);

        header = "prf||name";
        instance.parseHeader(header, sequence);
        System.out.println("name" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.PRF);

        header = "sp|accession|name";
        instance.parseHeader(header, sequence);
        System.out.println("accession" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.UNIPROT);

        header = "pdb|entry|chain";
        instance.parseHeader(header, sequence);
        System.out.println("entry:chain" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.PDB1);

        header = "entry:chain|PDBID|CHAIN|SEQUENCE";
        instance.parseHeader(header, sequence);
        System.out.println("entry:chain" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.PDB2);
        header = "PDB:1ECY_A mol:protein length:142  ECOTIN";
        instance.parseHeader(header, sequence);
        System.out.println("1ECY_A" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.PDBe);

        header = "pat|country|number";
        instance.parseHeader(header, sequence);
        System.out.println("number" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.PATENTS);

        header = "bbs|number";
        instance.parseHeader(header, sequence);
        System.out.println("number" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.GENINFO);

        header = "gnl|database|identifier";
        instance.parseHeader(header, sequence);
        System.out.println("identifier" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.GENERAL);

        header = "ref|accession|locus";

        instance.parseHeader(header, sequence);
        System.out.println("accession" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.NCBI);

        header = "lcl|identifier";
        instance.parseHeader(header, sequence);
        System.out.println("identifier" + "=" + sequence.getAccession());
        System.out.println(sequence.getAccession().getDataSource() + "=" + DataSource.LOCAL);
    }
}
