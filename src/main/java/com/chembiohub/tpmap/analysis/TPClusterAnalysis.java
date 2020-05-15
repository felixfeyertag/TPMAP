/*
 * Copyright (C) 2020 Felix Feyertag <felix.feyertag@ndm.ox.ac.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.chembiohub.tpmap.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chembiohub.tpmap.dstruct.Protein;
import javafx.collections.ObservableList;
//import org.rosuda.REngine.REXP;
//import org.rosuda.REngine.REXPMismatchException;
//import org.rosuda.REngine.Rserve.RConnection;
//import org.rosuda.REngine.Rserve.RserveException;
import com.chembiohub.tpmap.dstruct.Proteome;
import com.chembiohub.tpmap.dstruct.Protein2D;

/**
 * TPClusterAnalysis
 *
 * TODO: implement cluster analysis
 *
 * @author felixfeyertag
 */
public class TPClusterAnalysis {

    /**
     *
     */
    public TPClusterAnalysis() {
        
    }
    
    /**
     *
     * @param exp Thermal profiling experiment
     */
    public void analyseProtein(Proteome exp)  {
        try {
            //RConnection c = new RConnection();
            
            ObservableList<Protein> proteins = exp.getProteins();
            
            Double[][] dists = new Double[proteins.size()][proteins.size()];
            for(int i=0;i<dists.length;i++) {
                for(int j=0;j<dists[0].length;j++) {
                    dists[i][j] = 0.0;
                }
            }
            String[] accessions = new String[proteins.size()];
            
            //Integer counter = 0;
            
            for (int i=0;i<proteins.size();i++) {
                Protein2D p1 = (Protein2D)proteins.get(i);
                accessions[i] = p1.getAccession();
                for (int j=0;j<proteins.size();j++) {
                    Protein2D p2 = (Protein2D)proteins.get(j);
                    Integer counter = 0;
                    for (int k=0;k<p1.getAbundancesConcRatioNormalised().length;k++) {
                        for (int l=0;l<p1.getAbundancesConcRatioNormalised()[0].length;l++) {
                            if(!p1.getAbundancesConcRatioNormalised()[k][l].isNaN() && !p2.getAbundancesConcRatioNormalised()[k][l].isNaN()) {
                                //System.out.println("p1: " + p1.getAbundancesRatioNormalised()[k][l] + "\tp2:" + p2.getAbundancesRatioNormalised()[k][l] + "\tdist: " + dists[i][j]);
                                dists[i][j] += Math.abs(p1.getAbundancesConcRatioNormalised()[k][l]-p2.getAbundancesConcRatioNormalised()[k][l]);
                            }
                            else {
                                dists[i][j] += 1;
                            }
                            counter ++;
                        }
                    }
                    dists[i][j] /= counter;
                }
            }
            
            //REXP x = c.eval("R.version.string");
            //System.out.println(x.asString());
            
            //File tempFile = File.createTempFile("tpmap-dist", ".tab");
            //System.out.println("")
            File tempFile = new File("/Users/felixfeyertag/.tmp/test.txt");
            tempFile.createNewFile();
            
            OutputStream os = new FileOutputStream(tempFile);
            
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));

            for (String accession : accessions) {
                bw.write("\t" + accession);
            }
            bw.write("\n");
            
            for(int i=0;i<dists.length;i++) {
                bw.write(accessions[i]);
                for(int j=0;j<dists[0].length;j++) {
                    bw.write("\t" + dists[i][j].toString());
                }
                bw.write("\n");
            }
            
            bw.close();
            
            //c.voidEval(string);
            
            
            
        } catch (IOException ex /* | RserveException ex */ ) {
            Logger.getLogger(TPClusterAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
