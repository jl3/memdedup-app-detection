package de.uni_hamburg.svs.memsigstats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This class can be used to create statistics on the accuracy
 * of classifying measurements of the memory deduplication
 * side-channel attack for sets of pages that either fully consist
 * of (1) deduplicated pages or (2) non-deduplicated pages.
 * 
 * @author Jens Lindemann
 */
public class NaiveAccuracyStats extends AccuracyStats {
	double[] _limit;
	
	/**
	 * Creates a new AccuracyStats instance and calculates the accuracy statistics, cf.
	 * @see #AccuracyStats(int[], File[], File[], int, int[]).
	 * 
	 * @param numPages how many pages were contained in the signatures used for the measurements
	 * @param ddfstr name of the @{link File}s containing the measurements for the fully deduplicated case
	 * @param nonddfstr name of the Files containing the measurements for the non-deduplicated case
	 * @param numSets how many sets of measurements are to be sampled for each configuration 
	 * @param numMeasurements the size of the sample sets
	 * @param outputdir output directory for statistics
	 */
	public NaiveAccuracyStats(int[] numPages, String[] ddfstr, String[] nonddfstr, int numSets, int[] numMeasurements, File outputdir) {
		File[] ddf = initFileArray(ddfstr);
		File nonddf[] = initFileArray(nonddfstr);
		
		new NaiveAccuracyStats(numPages, ddf, nonddf, numSets, numMeasurements, outputdir);
	}
	
	/**
	 * Creates a new AccuracyStats instance and calculates the accuracy statistics, cf.
	 * @see #AccuracyStats(int[], File[], File[], File[], File[], int, int[], File).
	 * 
	 * @param numPages how many pages were contained in the signatures used for the measurements
	 * @param ddf @{link File}s containing the measurements for the fully deduplicated case
	 * @param nonddf Files containing the measurements for the non-deduplicated case
	 * @param numSets how many sets of measurements are to be sampled for each configuration 
	 * @param numMeasurements the size of the sample sets
	 * @param outputdir output directory for statistics
	 */
	public NaiveAccuracyStats(int[] numPages, File[] ddf, File[] nonddf, int numSets, int[] numMeasurements, File outputdir) {
		new NaiveAccuracyStats(numPages, ddf, nonddf, null, null, numSets, numMeasurements, outputdir);
	}
	
	/**
	 * Creates a new AccuracyStats instance and calculates the accuracy statistics, cf.
	 * @see #AccuracyStats(int[], File[], File[], File[], File[], int, int[], File).
	 * 
	 * @param numPages how many pages were contained in the signatures used for the measurements
	 * @param ddfTrainStr names of the @{link File}s containing the training measurements for the fully deduplicated case
	 * @param nonddfTrainStr names of the files containing the training measurements for the non-deduplicated case
	 * @param ddfTest names of the files containing the test measurements for the fully deduplicated case
	 * @param nonddfTest names of the files containing the test measurements for the non-deduplicated case
	 * @param numSets how many sets of measurements are to be sampled for each configuration 
	 * @param numMeasurements the size of the sample sets
	 * @param outputdir output directory for statistics
	 */
	public NaiveAccuracyStats(int[] numPages, String[] ddfTrainStr, String[] nonddfTrainStr, String[] ddfTestStr, String[] nonddfTestStr, int numSets, int[] numMeasurements, File outputdir) {
		File[] ddfTrain = initFileArray(ddfTrainStr);
		File[] nonddfTrain = initFileArray(nonddfTrainStr);
		File[] ddfTest = initFileArray(ddfTestStr);
		File[] nonddfTest = initFileArray(nonddfTestStr);
		
		new NaiveAccuracyStats(numPages, ddfTrain, nonddfTrain, ddfTest, nonddfTest, numSets, numMeasurements, outputdir);
	}
	
	/**
	 * Creates a new AccuracyStats instance and calculates the accuracy statistics.
	 * 
	 * Statistics will be calculated for all combinations of numbers of (1) pages
	 * and (2) measurements specified. For each number of pages separate {@link File}s
	 * containing the corresponding measurements for fully deduplicated and non-deduplicated
	 * sets of pages have to be supplied. Their position in the arrays ddf and nonddf must
	 * match the position of the corresponding number of pages in the numPages array.
	 * 
	 * Statistics will be created by randomly sampling sets of measurements from
	 * the measurement files and checking whether they would be classified correctly
	 * by our naive classification rule (i.e. by comparing them to the mean of the means
	 * of the deduplicated and non-deduplicated measurements).
	 * 
	 * @param numPages how many pages were contained in the signatures used for the measurements
	 * @param ddfTrain @{link File}s containing the training measurements for the fully deduplicated case
	 * @param nonddfTrain Files containing the training measurements for the non-deduplicated case
	 * @param ddfTest Files containing the test measurements for the fully deduplicated case
	 * @param nonddfTest Files containing the test measurements for the non-deduplicated case
	 * @param numSets how many sets of measurements are to be sampled for each configuration 
	 * @param numMeasurements the size of the sample sets
	 * @param outputdir output directory for statistics
	 */
	public NaiveAccuracyStats(int[] numPages, File[] ddfTrain, File[] nonddfTrain, File[] ddfTest, File[] nonddfTest, int numSets, int[] numMeasurements, File outputdir) {
		_numPages = numPages;
		_limit = new double[_numPages.length];
		
		initArrays(ddfTrain, nonddfTrain);
		findLimits();
		
		// re-initialise measurement arrays if we have a separate test set
		if(!((ddfTest == null) || (nonddfTest == null))) {
			initArrays(ddfTest, nonddfTest);
		}
		
		// initialize results arrays
		int[][] correctDD = new int[_numPages.length][numMeasurements.length];
		int[][] correctNonDD = new int[_numPages.length][numMeasurements.length];
		double[][] percCorrDD = new double[_numPages.length][numMeasurements.length];
		double[][] percCorrNonDD = new double[_numPages.length][numMeasurements.length];
		
		for(int i = 0; i < _numPages.length; i++) {
			for(int j = 0; j < numMeasurements.length; j++) {
				correctDD[i][j] = test(_dd[i], numMeasurements[j], numSets, true, _limit[i]);
				correctNonDD[i][j] = test(_nondd[i], numMeasurements[j], numSets, false, _limit[i]);
				percCorrDD[i][j] = (double)correctDD[i][j] / numSets * 100;
				percCorrNonDD[i][j] = (double)correctNonDD[i][j] / numSets * 100;
				
				System.out.println("numPages: " + _numPages[i]);
				System.out.println("numMeasurements: " + numMeasurements[j]);
				System.out.println("numSets: " + numSets);
				System.out.println("#correct DD: " + correctDD[i][j]);
				System.out.println("%correct DD: " + percCorrDD[i][j]);
				System.out.println("#correct non-DD: " + correctNonDD[i][j]);
				System.out.println("%correct non-DD: " + percCorrNonDD[i][j]);
				System.out.println("-----------");
			}
		}
		
		System.out.println("Summary:");
		System.out.println("numMeasurements");
		String numPagesLine = "numPages";
		for(int i = 0; i < numMeasurements.length; i++) {
			numPagesLine += ";";
			numPagesLine += numMeasurements[i] + " measurements";
		}
		System.out.println(numPagesLine);
		
		try {
			File corrDDFile = new File(outputdir, "corrDD.csv");
			FileOutputStream corrDDOS = new FileOutputStream(corrDDFile);
			PrintWriter corrDDWriter = new PrintWriter(corrDDOS);
			corrDDWriter.write(numPagesLine + "\n");
			
			System.out.println("\n%correctDD:");
			for(int p = 0; p < _numPages.length; p++) {
				for(int i = 0; i < percCorrDD[p].length; i++) {
					if(i == 0) {
						System.out.print(_numPages[p] + ";");
						corrDDWriter.write(_numPages[p] + ";");
					} else {
						System.out.print(";");
						corrDDWriter.write(";");
					}
					System.out.print(percCorrDD[p][i]);
					corrDDWriter.write(Double.toString(percCorrDD[p][i]));
				}
				System.out.print("\n");
				corrDDWriter.write("\n");
			}
			
			corrDDWriter.close();
			corrDDOS.close();
		
			
			File nonddFile = new File(outputdir, "corrNonDD.csv");
			FileOutputStream nonddOS = new FileOutputStream(nonddFile);
			PrintWriter nonddWriter = new PrintWriter(nonddOS);
			nonddWriter.write(numPagesLine + "\n");
			
			System.out.println("-----------");
			System.out.println("\n%correctNonDD");
			for(int p = 0; p < _numPages.length; p++) {
				for(int i = 0; i < percCorrNonDD[p].length; i++) {
					if(i == 0) {
						System.out.print(_numPages[p] + ";");
						nonddWriter.print(_numPages[p] + ";");
					} else {
						System.out.print(";");
						nonddWriter.write(";");
					}
					System.out.print(percCorrNonDD[p][i]);
					nonddWriter.write(Double.toString(percCorrNonDD[p][i]));
				}
				System.out.print("\n");
				nonddWriter.write("\n");
			}
			
			nonddWriter.close();
			nonddOS.close();
			
			
			File avgFile = new File(outputdir, "corrAvg.csv");
			FileOutputStream avgOS = new FileOutputStream(avgFile);
			PrintWriter avgWriter = new PrintWriter(avgOS);
			avgWriter.write(numPagesLine + "\n");
			
			System.out.println("-----------");
			System.out.println("\n%avgCorrect(50/50)");
			for(int p = 0; p < _numPages.length; p++) {
				for(int i = 0; i < percCorrNonDD[p].length; i++) {
					if(i == 0) {
						System.out.print(_numPages[p] + ";");
						avgWriter.write(_numPages[p] + ";");
					} else {
						System.out.print(";");
						avgWriter.write(";");
					}
					System.out.print((percCorrNonDD[p][i] + percCorrDD[p][i]) / 2);
					avgWriter.write(Double.toString((percCorrNonDD[p][i] + percCorrDD[p][i]) / 2));
				}
				System.out.print("\n");
				avgWriter.write("\n");
			}
			
			avgWriter.close();
			avgOS.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * This will find the classification limit for all numbers of pages.
	 * The underlying classification rule is the naive rule described in the paper:
	 * First, we calculate the means of all measurements for the fully deduplicated and
	 * the non-deduplicated case. Then, we use the mean of these means as our limit. 
	 */
	private void findLimits() {
		for(int i = 0; i < _numPages.length; i++) {
			double ddavg  = avg(_dd[i]);
			double nonddavg = avg(_nondd[i]);
			
			// naive implementation: just take average of averages of dd- and non-dd baselines
			_limit[i] = (ddavg + nonddavg) / 2;
		}
	}
	
	/**
	 * Returns the number of correct classifications for the specified
	 * experiment configuration. 
	 * 
	 * @param m the measurements
	 * @param setSize the size of the sample sets
	 * @param numSets how many sets are to be sampled
	 * @param setsDD true if the experiment is for the fully deduplicated case,
	 * false if it is for the non-deduplicated case
	 * @return number of correct classifications
	 */
	private int test(int[] m, int setSize, int numSets, boolean setDD, double limit) {
		// Take numSets samples of size setSize from set m (Bootstrap-recycle).
		// If setDD: Set is correctly classified if avg(set) >= _limit.
		// If !setDD: Set is correctly classified if avg(set) < _limit.
		// If avg(set) == _limit: Decide randomly
		
		int correct = 0;
		
		for(int s = 0; s < numSets; s++) {
			Random rnd = new Random();
			
			int[] set = new int[setSize];
			for(int i = 0; i < setSize; i++) {
				int rndidx = rnd.nextInt(m.length);
				set[i] = m[rndidx];
			}
			
			double setavg = avg(set);
			if(setavg > limit) {
				if(setDD) {
					correct++;
				}
			} else if(setavg < limit) {
				if(!setDD) {
					correct++;
				}
			} else {
				// decide randomly in case of avg(set) == _limit
				if(rnd.nextBoolean()) {
					correct++;
				}
			}
		}
		
		return correct;
	}
	
	/**
	 * Calculates the average of the values in a.
	 * 
	 * @param a values to calculate average for
	 * @return average of a
	 */
	private double avg(int[] a) {
		int sum = 0;
		for(int i : a) {
			sum += i;
		}
		double avg = (double)sum / a.length;
		return avg;
	}
	
	/**
	 * Main method that provides a CLI for generating naive accuracy statistics.
	 * 
	 * @param args CLI arguments
	 */
	public static void main(String[] args) {
		Options opt = new Options();
		
		Option pagesOpt = Option.builder("p")
				.longOpt("pages")
				.hasArg()
				.argName("pages")
				.required()
				.desc("number of pages for set of files. Can occur multiple times and must be (immediately) followed by the appropriate -n and -d arguments")
				.build();
		
		Option nonddFileOpt = Option.builder("n")
				.longOpt("nonddfile")
				.hasArg()
				.argName("nonddfile")
				.required()
				.desc("file containing stats for non-dedup case (training set, if separate sets are used)")
				.build();
		
		Option ddFileOpt = Option.builder("d")
				.longOpt("ddfile")
				.hasArg()
				.argName("ddfile")
				.required()
				.desc("file containing stats for dedup case (training set, if separate sets are used)")
				.build();
		
		Option nonddTestFileOpt = Option.builder("nt")
				.longOpt("nonddtest")
				.hasArg()
				.argName("nonddtest")
				.required()
				.desc("file containing stats for non-dedup case (test set, if separate sets are used)")
				.build();
		
		Option ddTestFileOpt = Option.builder("dt")
				.longOpt("ddtest")
				.hasArg()
				.argName("ddtest")
				.required()
				.desc("file containing stats for dedup case (test set, if separate sets are used)")
				.build();
		
		Option numMeasurementsOpt = Option.builder("m")
				.longOpt("nummeasurements")
				.hasArg()
				.argName("nummeasurements")
				.required()
				.desc("number of measurements in test sets")
				.build();
		
		Option setsOpt = Option.builder("s")
				.longOpt("sets")
				.hasArg()
				.argName("sets")
				.required()
				.desc("number of sets for bootstrap-resample")
				.build();
		
		Option outputPathOpt = Option.builder("o")
				.longOpt("outputpath")
				.hasArg()
				.argName("path")
				.desc("output path (default: <current_dir>/rocstats)")
				.build();
		
		Option helpOpt = Option.builder("h")
				.longOpt("help")
				.desc("print this message")
				.build();
		
		opt.addOption(pagesOpt);
		opt.addOption(nonddFileOpt);
		opt.addOption(ddFileOpt);
		opt.addOption(nonddTestFileOpt);
		opt.addOption(ddTestFileOpt);
		opt.addOption(numMeasurementsOpt);
		opt.addOption(outputPathOpt);
		opt.addOption(helpOpt);
		opt.addOption(setsOpt);
		
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd;
		try {
			cmd = parser.parse(opt, args);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
			return; // Somehow, this is needed to avoid compiler errors here... Not in PageSizeStats though?!?
		}
		
		if(cmd.hasOption(helpOpt.getOpt())) {
			printHelp(opt);
		}
		
		String[] pagesStr = cmd.getOptionValues(pagesOpt.getOpt());
		int[] numPages = new int[pagesStr.length];
		for(int i = 0; i < pagesStr.length; i++) {
			numPages[i] = new Integer(pagesStr[i]);
		}
		
		String[] nonddfstr = cmd.getOptionValues(nonddFileOpt.getOpt());
		String[] ddfstr = cmd.getOptionValues(ddFileOpt.getOpt());
		
		if(pagesStr.length != nonddfstr.length || pagesStr.length != ddfstr.length) {
			System.err.println("Error: Number of -p, -n, -d arguments does not match.");
			System.exit(1);
		}
		
		String[] nonddfteststr = cmd.getOptionValues(nonddTestFileOpt.getOpt());
		String[] ddfteststr = cmd.getOptionValues(ddTestFileOpt.getOpt());
		if(!((nonddfteststr == null) && (ddfteststr == null))) {
			if((nonddfteststr == null) || (ddfteststr == null) 
					|| (pagesStr.length != nonddfteststr.length) || (pagesStr.length != ddfteststr.length)) {
				System.err.println("Error: Number of -p, -n, -d, -ntest, -dtest arguments does not match.");
				System.exit(1);
			}
		}
		
		String numSetsStr = cmd.getOptionValue(setsOpt.getOpt());
		int numSets = new Integer(numSetsStr);
		
		String numMeasurementsStr = cmd.getOptionValue(numMeasurementsOpt.getOpt());
		String[] measurementsStrArray = numMeasurementsStr.split(",");
		int measurementsIntArray[] = new int[measurementsStrArray.length];
		for(int i = 0; i < measurementsIntArray.length; i++) {
			measurementsIntArray[i] = new Integer(measurementsStrArray[i]);
		}
		
		String outputPathStr = cmd.getOptionValue(outputPathOpt.getOpt());
		if(outputPathStr == null) outputPathStr = "rocstats";
		File outputdir = new File(outputPathStr);
		outputdir.mkdir();
		
		if(nonddfteststr == null) {
			new NaiveAccuracyStats(numPages, ddfstr, nonddfstr, numSets, measurementsIntArray, outputdir);
		} else {
			new NaiveAccuracyStats(numPages, ddfstr, nonddfstr, ddfteststr, nonddfteststr, numSets, measurementsIntArray, outputdir);
		}
	}
	
	/**
	 * Prints the help message containing information about the CLI options.
	 * 
	 * @param opt Options object containing CLI options.
	 */
	private static void printHelp(Options opt) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("MemSigs", opt);
	}
}
