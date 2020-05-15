package com.chembiohub.tpmap.test

import com.chembiohub.tpmap.dstruct.Protein1D
import com.chembiohub.tpmap.dstruct.Protein2D
import com.chembiohub.tpmap.dstruct.Proteome
import com.chembiohub.tpmap.normalisation.TPNormalisation
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import junit.framework.Test
import junit.textui.TestRunner

class Tests {
    static Test suite() {
        def allTests = new GroovyTestSuite()
        allTests.addTestSuite(Protein1DTest.class)
        allTests.addTestSuite(Protein2DTest.class)
        allTests.addTestSuite(ProteomeTest.class)
        return allTests
    }

    /**
     * Protein1DTest
     *
     * Tests for Protein1D class
     *
     * @author felixfeyertag
     */
    static class Protein1DTest extends GroovyTestCase {

        void testProtein1D() {

            def protein1D = new Protein1D();

            protein1D.setAccession "Test1"
            assertEquals "Test1", protein1D.getAccession()

            protein1D.setDescription "Test1 Protein"
            assertEquals "Test1 Protein", protein1D.getDescription()

            protein1D.setOrganismName "Test1 Organism"
            assertEquals "Test1 Organism", protein1D.getOrganismName()

            protein1D.setOrganismIdentifier "Test1 OrganismIdentifier"
            assertEquals "Test1 OrganismIdentifier", protein1D.getOrganismIdentifier()

            protein1D.setGeneName "Test1 GeneName"
            assertEquals "Test1 GeneName", protein1D.getGeneName()

            protein1D.setProteinExistence "Test1 ProteinExistence"
            assertEquals "Test1 ProteinExistence", protein1D.getProteinExistence()

            protein1D.setSequenceVersion "Test1 SequenceVersion"
            assertEquals "Test1 SequenceVersion", protein1D.getSequenceVersion()

            protein1D.setAttempts 100
            assertEquals 100, protein1D.getAttempts()

            protein1D.setMaxIterations 100
            assertEquals 100, protein1D.getMaxIterations()

            def tempReference = [ 10.0, 10.0, 10.0, 10.0 ]

            protein1D.setTempReference((Double[]) tempReference.toArray())
            assertArrayEquals(tempReference.toArray(), protein1D.getTempReference())

            def tempLabels = FXCollections.observableArrayList()
            tempLabels.addAll([ "37.0", "38.1", "39.2", "40.3", "41.4", "42.5", "43.6", "44.7", "45.8", "46.9" ])

            def concLabels = FXCollections.observableArrayList()
            concLabels.addAll([ "V1", "V2", "T1", "T2" ])

            def abundances =
                    [ [ 10.0, 10.0, 10.0, 10.0 ].toArray() ,
                      [  9.0,  9.0,  9.5,  9.5 ].toArray() ,
                      [  8.0,  8.0,  8.5,  8.5 ].toArray() ,
                      [  7.0,  7.0,  7.5,  7.5 ].toArray() ,
                      [  6.0,  6.0,  6.5,  6.5 ].toArray() ,
                      [  5.0,  5.0,  5.5,  5.5 ].toArray() ,
                      [  4.0,  4.0,  4.5,  4.5 ].toArray() ,
                      [  3.0,  3.0,  3.5,  3.5 ].toArray() ,
                      [  2.0,  2.0,  2.5,  2.5 ].toArray() ,
                      [  1.0,  1.0,  1.0,  1.0 ].toArray()  ]

            protein1D.setAbundances((Double[][]) abundances.toArray(), (ObservableList<String>)tempLabels, (ObservableList<String>)concLabels)
            assertArrayEquals abundances.toArray(), protein1D.getAbundances()

            def abundancesRatio =
                    [[ 1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1 ].toArray() ,
                     [ 1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1 ].toArray(),
                     [ 1.0, 0.95, 0.85, 0.75, 0.65, 0.55, 0.45, 0.35, 0.25, 0.1 ].toArray(),
                     [ 1.0, 0.95, 0.85, 0.75, 0.65, 0.55, 0.45, 0.35, 0.25, 0.1  ].toArray() ]

            protein1D.calculateRatios(tempLabels, concLabels)
            assertArrayEquals((Double[][])abundancesRatio.toArray(), protein1D.getAbundancesTempRatio())

            assertTrue(true)

        }



    }

    /**
     * Protein2DTest
     *
     * Tests for Protein2D class
     *
     * @author felixfeyertag
     */
    static class Protein2DTest extends GroovyTestCase {

        void testProtein2D() {

            def protein2D = new Protein2D();

            protein2D.setAccession "Test1"
            assertEquals "Test1", protein2D.getAccession()

            protein2D.setDescription "Test1 Protein"
            assertEquals "Test1 Protein", protein2D.getDescription()

            protein2D.setOrganismName "Test1 Organism"
            assertEquals "Test1 Organism", protein2D.getOrganismName()

            protein2D.setOrganismIdentifier "Test1 Organism Identifier"
            assertEquals "Test1 Organism Identifier", protein2D.getOrganismIdentifier()

            protein2D.setGeneName "Test1 Gene Name"
            assertEquals "Test1 Gene Name", protein2D.getGeneName()

            protein2D.setProteinExistence "Test1 Protein Existence"
            assertEquals "Test1 Protein Existence", protein2D.getProteinExistence()

            protein2D.setSequenceVersion "Test1 Sequence Version"
            assertEquals "Test1 Sequence Version", protein2D.getSequenceVersion()

            def concReference = [ 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0 ]

            protein2D.setConcReference((Double[]) concReference.toArray())
            assertArrayEquals(concReference.toArray(), protein2D.getConcReference())

            def tempReference = [ 10.0, 10.0, 10.0, 10.0 ]

            protein2D.setTempReference((Double[]) tempReference.toArray())
            assertArrayEquals(tempReference.toArray(), protein2D.getTempReference())

            ObservableList<String> tempLabels = FXCollections.observableArrayList()
            tempLabels.addAll([ "42.0", "44.0", "46.0", "48.0", "50.0", "52.0", "54.0", "56.0", "58.0", "60.0" ].toArray())

            ObservableList<String> concLabels = FXCollections.observableArrayList()
            concLabels.addAll([ "0.0", "4.0", "28.0", "35.0" ].toArray())

            def abundances =
                    [ [ 10.0, 20.0, 30.0, 40.0 ].toArray() ,
                      [ 10.0, 30.0, 40.0, 50.0 ].toArray() ,
                      [ 10.0, 40.0, 50.0, 60.0 ].toArray() ,
                      [ 10.0, 50.0, 60.0, 70.0 ].toArray() ,
                      [ 10.0, 60.0, 70.0, 80.0 ].toArray() ,
                      [ 10.0, 70.0, 80.0, 90.0 ].toArray() ,
                      [ 10.0, 50.0, 60.0, 70.0 ].toArray() ,
                      [ 10.0, 40.0, 50.0, 60.0 ].toArray() ,
                      [ 10.0, 30.0, 40.0, 50.0 ].toArray() ,
                      [ 10.0, 20.0, 30.0, 40.0 ].toArray()  ]

            protein2D.setAbundances((Double[][]) abundances.toArray(), (ObservableList<String>)tempLabels, (ObservableList<String>)concLabels)
            assertArrayEquals abundances.toArray(), protein2D.getAbundances()

            def abundancesRatio =
                    [ [ 1.0, 2.0, 3.0, 4.0 ].toArray() ,
                      [ 1.0, 3.0, 4.0, 5.0 ].toArray() ,
                      [ 1.0, 4.0, 5.0, 6.0 ].toArray() ,
                      [ 1.0, 5.0, 6.0, 7.0 ].toArray() ,
                      [ 1.0, 6.0, 7.0, 8.0 ].toArray() ,
                      [ 1.0, 7.0, 8.0, 9.0 ].toArray() ,
                      [ 1.0, 5.0, 6.0, 7.0 ].toArray() ,
                      [ 1.0, 4.0, 5.0, 6.0 ].toArray() ,
                      [ 1.0, 3.0, 4.0, 5.0 ].toArray() ,
                      [ 1.0, 2.0, 3.0, 4.0 ].toArray()  ]

            protein2D.setNormalisationMethod(TPNormalisation.Normalisation.NONE, tempLabels, concLabels)
            assertArrayEquals(abundancesRatio.toArray(), protein2D.getAbundancesConcRatio())

            protein2D.updateScores(1.0, 1.0)

            assertEquals (1.0, protein2D.getScore())

        }



    }

    /**
     * ProteomeTest
     *
     * Tests for Proteome class.
     *
     * @author felixfeyertag
     */
    static class ProteomeTest extends GroovyTestCase {

        void testProteome2D() {

            //Application.launch()

            def protein1 = new Protein2D();

            protein1.setAccession "Test1"
            assertEquals "Test1", protein1.getAccession()

            protein1.setDescription "Test1 Protein"
            assertEquals "Test1 Protein", protein1.getDescription()

            protein1.setOrganismName "Test1 Organism"
            assertEquals "Test1 Organism", protein1.getOrganismName()

            protein1.setOrganismIdentifier "Test1 Organism Identifier"
            assertEquals "Test1 Organism Identifier", protein1.getOrganismIdentifier()

            protein1.setGeneName "Test1 Gene Name"
            assertEquals "Test1 Gene Name", protein1.getGeneName()

            protein1.setProteinExistence "Test1 Protein Existence"
            assertEquals "Test1 Protein Existence", protein1.getProteinExistence()

            protein1.setSequenceVersion "Test1 Sequence Version"
            assertEquals "Test1 Sequence Version", protein1.getSequenceVersion()

            def concReference = [ 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0 ]

            protein1.setConcReference((Double[]) concReference.toArray())
            assertArrayEquals(concReference.toArray(), protein1.getConcReference())

            def tempReference = [ 10.0, 10.0, 10.0, 10.0 ]

            protein1.setTempReference((Double[]) tempReference.toArray())
            assertArrayEquals(tempReference.toArray(), protein1.getTempReference())

            ObservableList<String> tempLabels = FXCollections.observableArrayList()
            tempLabels.addAll([ "42.0", "44.0", "46.0", "48.0", "50.0", "52.0", "54.0", "56.0", "58.0", "60.0" ].toArray())

            ObservableList<String> concLabels = FXCollections.observableArrayList()
            concLabels.addAll([ "0.0", "4.0", "28.0", "35.0" ].toArray())

            def abundances =
                    [ [ 10.0, 20.0, 30.0, 40.0 ].toArray() ,
                      [ 10.0, 30.0, 40.0, 50.0 ].toArray() ,
                      [ 10.0, 40.0, 50.0, 60.0 ].toArray() ,
                      [ 10.0, 50.0, 60.0, 70.0 ].toArray() ,
                      [ 10.0, 60.0, 70.0, 80.0 ].toArray() ,
                      [ 10.0, 70.0, 80.0, 90.0 ].toArray() ,
                      [ 10.0, 50.0, 60.0, 70.0 ].toArray() ,
                      [ 10.0, 40.0, 50.0, 60.0 ].toArray() ,
                      [ 10.0, 30.0, 40.0, 50.0 ].toArray() ,
                      [ 10.0, 20.0, 30.0, 40.0 ].toArray()  ]

            protein1.setAbundances((Double[][]) abundances.toArray(), (ObservableList<String>)tempLabels, (ObservableList<String>)concLabels)
            assertArrayEquals abundances.toArray(), protein1.getAbundances()

            def abundancesRatio =
                    [ [ 1.0, 2.0, 3.0, 4.0 ].toArray() ,
                      [ 1.0, 3.0, 4.0, 5.0 ].toArray() ,
                      [ 1.0, 4.0, 5.0, 6.0 ].toArray() ,
                      [ 1.0, 5.0, 6.0, 7.0 ].toArray() ,
                      [ 1.0, 6.0, 7.0, 8.0 ].toArray() ,
                      [ 1.0, 7.0, 8.0, 9.0 ].toArray() ,
                      [ 1.0, 5.0, 6.0, 7.0 ].toArray() ,
                      [ 1.0, 4.0, 5.0, 6.0 ].toArray() ,
                      [ 1.0, 3.0, 4.0, 5.0 ].toArray() ,
                      [ 1.0, 2.0, 3.0, 4.0 ].toArray()  ]

            protein1.setNormalisationMethod(TPNormalisation.Normalisation.NONE, tempLabels, concLabels)
            assertArrayEquals(abundancesRatio.toArray(), protein1.getAbundancesConcRatio())

            protein1.updateScores(1.0, 1.0)

            assertEquals (1.0, protein1.getScore())


            def protein2 = new Protein2D();

            protein2.setAccession "Test2"
            assertEquals "Test2", protein2.getAccession()

            protein2.setDescription "Test2 Protein"
            assertEquals "Test2 Protein", protein2.getDescription()

            protein2.setOrganismName "Test2 Organism"
            assertEquals "Test2 Organism", protein2.getOrganismName()

            protein2.setOrganismIdentifier "Test2 Organism Identifier"
            assertEquals "Test2 Organism Identifier", protein2.getOrganismIdentifier()

            protein2.setGeneName "Test2 Gene Name"
            assertEquals "Test2 Gene Name", protein2.getGeneName()

            protein2.setProteinExistence "Test2 Protein Existence"
            assertEquals "Test2 Protein Existence", protein2.getProteinExistence()

            protein2.setSequenceVersion "Test2 Sequence Version"
            assertEquals "Test2 Sequence Version", protein2.getSequenceVersion()

            concReference = [ 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0 ]

            protein2.setConcReference((Double[]) concReference.toArray())
            assertArrayEquals(concReference.toArray(), protein2.getConcReference())

            tempReference = [ 10.0, 10.0, 10.0, 10.0 ]

            protein2.setTempReference((Double[]) tempReference.toArray())
            assertArrayEquals(tempReference.toArray(), protein2.getTempReference())

            tempLabels = FXCollections.observableArrayList()
            tempLabels.addAll([ "42.0", "44.0", "46.0", "48.0", "50.0", "52.0", "54.0", "56.0", "58.0", "60.0" ].toArray())

            concLabels = FXCollections.observableArrayList()
            concLabels.addAll([ "0.0", "4.0", "28.0", "35.0" ].toArray())

            abundances =
                    [ [ 10.0, 9.0, 8.0, 7.0 ].toArray() ,
                      [ 10.0, 8.0, 7.0, 6.0 ].toArray() ,
                      [ 10.0, 7.0, 6.0, 5.0 ].toArray() ,
                      [ 10.0, 6.0, 5.0, 4.0 ].toArray() ,
                      [ 10.0, 5.0, 4.0, 3.0 ].toArray() ,
                      [ 10.0, 4.0, 3.0, 2.0 ].toArray() ,
                      [ 10.0, 5.0, 4.0, 3.0 ].toArray() ,
                      [ 10.0, 6.0, 5.0, 4.0 ].toArray() ,
                      [ 10.0, 7.0, 6.0, 5.0 ].toArray() ,
                      [ 10.0, 8.0, 7.0, 6.0 ].toArray()  ]

            protein2.setAbundances((Double[][]) abundances.toArray(), (ObservableList<String>)tempLabels, (ObservableList<String>)concLabels)
            assertArrayEquals abundances.toArray(), protein2.getAbundances()

            abundancesRatio =
                    [ [ 1.0, 0.9, 0.8, 0.7 ].toArray() ,
                      [ 1.0, 0.8, 0.7, 0.6 ].toArray() ,
                      [ 1.0, 0.7, 0.6, 0.5 ].toArray() ,
                      [ 1.0, 0.6, 0.5, 0.4 ].toArray() ,
                      [ 1.0, 0.5, 0.4, 0.3 ].toArray() ,
                      [ 1.0, 0.4, 0.3, 0.2 ].toArray() ,
                      [ 1.0, 0.5, 0.4, 0.3 ].toArray() ,
                      [ 1.0, 0.6, 0.5, 0.4 ].toArray() ,
                      [ 1.0, 0.7, 0.6, 0.5 ].toArray() ,
                      [ 1.0, 0.8, 0.7, 0.6 ].toArray()  ]

            protein2.setNormalisationMethod(TPNormalisation.Normalisation.NONE, tempLabels, concLabels)
            assertArrayEquals(abundancesRatio.toArray(), protein2.getAbundancesConcRatio())

            protein2.updateScores(1.0, 1.0)

            assertEquals (-1.0, protein2.getScore())



            def protein3 = new Protein2D();

            protein3.setAccession "Test3"
            assertEquals "Test3", protein3.getAccession()

            protein3.setDescription "Test3 Protein"
            assertEquals "Test3 Protein", protein3.getDescription()

            protein3.setOrganismName "Test3 Organism"
            assertEquals "Test3 Organism", protein3.getOrganismName()

            protein3.setOrganismIdentifier "Test3 Organism Identifier"
            assertEquals "Test3 Organism Identifier", protein3.getOrganismIdentifier()

            protein3.setGeneName "Test3 Gene Name"
            assertEquals "Test3 Gene Name", protein3.getGeneName()

            protein3.setProteinExistence "Test3 Protein Existence"
            assertEquals "Test3 Protein Existence", protein3.getProteinExistence()

            protein3.setSequenceVersion "Test3 Sequence Version"
            assertEquals "Test3 Sequence Version", protein3.getSequenceVersion()

            concReference = [ 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0 ]

            protein3.setConcReference((Double[]) concReference.toArray())
            assertArrayEquals(concReference.toArray(), protein3.getConcReference())

            tempReference = [ 10.0, 10.0, 10.0, 10.0 ]

            protein3.setTempReference((Double[]) tempReference.toArray())
            assertArrayEquals(tempReference.toArray(), protein3.getTempReference())

            tempLabels = FXCollections.observableArrayList()
            tempLabels.addAll([ "42.0", "44.0", "46.0", "48.0", "50.0", "52.0", "54.0", "56.0", "58.0", "60.0" ].toArray())

            concLabels = FXCollections.observableArrayList()
            concLabels.addAll([ "0.0", "4.0", "28.0", "35.0" ].toArray())

            abundances =
                    [ [ 10.0, 10.0, 10.0, 10.0 ].toArray() ,
                      [ 10.0, 10.0, 10.0, 10.0 ].toArray() ,
                      [ 10.0, 10.0, 10.0, 10.0 ].toArray() ,
                      [ 10.0, 10.0, 10.0, 10.0 ].toArray() ,
                      [ 10.0, 10.0, 10.0, 10.0 ].toArray() ,
                      [ 10.0, 10.0, 10.0, 10.0 ].toArray() ,
                      [ 10.0, 10.0, 10.0, 10.0 ].toArray() ,
                      [ 10.0, 10.0, 10.0, 10.0 ].toArray() ,
                      [ 10.0, 10.0, 10.0, 10.0 ].toArray() ,
                      [ 10.0, 10.0, 10.0, 10.0 ].toArray()  ]

            protein3.setAbundances((Double[][]) abundances.toArray(), (ObservableList<String>)tempLabels, (ObservableList<String>)concLabels)
            assertArrayEquals abundances.toArray(), protein3.getAbundances()

            abundancesRatio =
                    [ [ 1.0, 1.0, 1.0, 1.0 ].toArray() ,
                      [ 1.0, 1.0, 1.0, 1.0 ].toArray() ,
                      [ 1.0, 1.0, 1.0, 1.0 ].toArray() ,
                      [ 1.0, 1.0, 1.0, 1.0 ].toArray() ,
                      [ 1.0, 1.0, 1.0, 1.0 ].toArray() ,
                      [ 1.0, 1.0, 1.0, 1.0 ].toArray() ,
                      [ 1.0, 1.0, 1.0, 1.0 ].toArray() ,
                      [ 1.0, 1.0, 1.0, 1.0 ].toArray() ,
                      [ 1.0, 1.0, 1.0, 1.0 ].toArray() ,
                      [ 1.0, 1.0, 1.0, 1.0 ].toArray()  ]

            protein3.setNormalisationMethod(TPNormalisation.Normalisation.NONE, tempLabels, concLabels)
            assertArrayEquals(abundancesRatio.toArray(), protein3.getAbundancesConcRatio())

            protein3.updateScores(1.0, 1.0)

            assertEquals (0.0, protein3.getScore())

            //Stage stage = new Stage()
            //TabPane tabPane = new TabPane()

            //Proteome<Protein2D> proteome = new Proteome<>(stage, tabPane)
            Proteome<Protein2D> proteome = new Proteome<>(null, null)

            proteome.addProtein(protein1)
            proteome.addProtein(protein2)
            proteome.addProtein(protein3)

            proteome.updateProteinCount()

            assertEquals(3, proteome.getProteinCount())
            assertEquals(3, proteome.getProteinCountProperty().get())

        }

        void testProteome1D() {



        }


    }
}

TestRunner.run(Tests.suite())



