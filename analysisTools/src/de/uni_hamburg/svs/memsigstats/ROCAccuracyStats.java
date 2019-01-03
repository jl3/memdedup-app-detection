package de.uni_hamburg.svs.memsigstats;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
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
public class ROCAccuracyStats extends AccuracyStats {
	double[] _limit;
	
	/**
	 * Creates a new AccuracyStats instance and calculates the accuracy statistics, cf.
	 * @see #ROCAccuracyStats(int[], File[], File[], int, int[]). This constructor
	 * uses the same dataset for training and testing.
	 * 
	 * @param numPages how many pages were contained in the signatures used for the measurements
	 * @param ddfstr name of the @{link File}s containing the measurements for the fully deduplicated case
	 * @param nonddfstr name of the Files containing the measurements for the non-deduplicated case
	 * @param numSets how many sets of measurements are to be sampled for each configuration 
	 * @param numMeasurements the size of the sample sets
	 * @param numDataPoints number of data points to generate for the ROC curve
	 * @param outputdir output directory for statistics
	 */
	public ROCAccuracyStats(int[] numPages, String[] ddfstr, String[] nonddfstr, int numSets, int[] numMeasurements, int numDataPoints, File outputdir) {
		File[] ddf = initFileArray(ddfstr);
		File nonddf[] = initFileArray(nonddfstr);
		
		new ROCAccuracyStats(numPages, ddf, nonddf, numSets, numMeasurements, numDataPoints, outputdir);
	}
	
	/**
	 * Creates a new AccuracyStats instance and calculates the accuracy statistics, cf.
	 * @see #ROCAccuracyStats(int[], File[], File[], int, int[]). This constructor
	 * uses the same dataset for training and testing.
	 * 
	 * @param numPages how many pages were contained in the signatures used for the measurements
	 * @param ddf @{link File}s containing the measurements for the fully deduplicated case
	 * @param nonddf @{link File}s containing the measurements for the non-deduplicated case
	 * @param numSets how many sets of measurements are to be sampled for each configuration
	 * @param numMeasurements the size of the sample sets
	 * @param numDataPoints number of data points to generate for the ROC curve
	 * @param outputdir output directory for statistics
	 */
	public ROCAccuracyStats(int[] numPages, File[] ddf, File[] nonddf, int numSets, int[] numMeasurements, int numDataPoints, File outputdir) {
		new ROCAccuracyStats(numPages, ddf, nonddf, null, null, numSets, numMeasurements, numDataPoints, outputdir);
	}
	
	/**
	 * Creates a new AccuracyStats instance and calculates the accuracy statistics, cf.
	 * @see #ROCAccuracyStats(int[], File[], File[], int, int[]).
	 * 
	 * @param numPages how many pages were contained in the signatures used for the measurements
	 * @param ddfTrainStr name of the @{link File}s containing the training measurements for the fully deduplicated case
	 * @param nonddfTrainStr name of the @{link File}s containing the training measurements for the non-deduplicated case
	 * @param ddfTestStr name of the @{link File}s containing the test measurements for the fully deduplicated case
	 * @param nonddfTestStr name of the @{link File}s containing the test measurements for the non-deduplicated case
	 * @param numSets how many sets of measurements are to be sampled for each configuration
	 * @param numMeasurements the size of the sample sets
	 * @param numDataPoints number of data points to generate for the ROC curve
	 * @param outputdir output directory for statistics
	 */
	public ROCAccuracyStats(int[] numPages, String[] ddfTrainStr, String[] nonddfTrainStr, String[] ddfTestStr, String[] nonddfTestStr, int numSets, int[] numMeasurements, int numDataPoints, File outputdir) {
		File[] ddfTrain = initFileArray(ddfTrainStr);
		File[] nonddfTrain = initFileArray(nonddfTrainStr);
		File[] ddfTest = initFileArray(ddfTestStr);
		File[] nonddfTest = initFileArray(nonddfTestStr);
		
		new ROCAccuracyStats(numPages, ddfTrain, nonddfTrain, ddfTest, nonddfTest, numSets, numMeasurements, numDataPoints, outputdir);
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
	 * @param numDataPoints number of data points to generate for the ROC curve
	 * @param outputdir output directory for statistics
	 */
	public ROCAccuracyStats(int[] numPages, File[] ddfTrain, File[] nonddfTrain, File[] ddfTest, File[] nonddfTest, int numSets, int[] numMeasurements, int numDataPoints, File outputdir) {
		try {
			_numPages = numPages;
			_limit = new double[_numPages.length];
			
			initArrays(ddfTrain, nonddfTrain);
			
			File optFile = new File(outputdir, "opt.csv");
			FileOutputStream optOS = new FileOutputStream(optFile);
			PrintWriter optWriter = new PrintWriter(optOS);
			// header
			optWriter.write("numPages;numMeasurements;limit;optDDPerc;optNonDDPerc;optPercComb");
			
			// Initialise output for summary of avg accuracy stats.
			File avgFile = new File(outputdir, "opt-avg.csv");
			FileOutputStream avgOS = new FileOutputStream(avgFile);
			PrintWriter avgWriter = new PrintWriter(avgOS);
			avgWriter.print("numPages;");
			for(int i = 0; i < numMeasurements.length; i++) {
				if(i != 0) {
					avgWriter.print(";");
				}
				
				avgWriter.print(numMeasurements[i]);
			}
			
			// create array for storing optimal limit for each signature size
			double[][] optLimits = new double[_numPages.length][numMeasurements.length];
			
			// For each set (dd, nondd) and each num measurements:
			// 			generate numSets results (mean of sampled measurements)
			//			limits for ROC generation: min: mean of nondd; max: mean of dd?
			//			generate ROC: 100-200(?) data points between min and max
			//			find optimal point on ROC curve -> log for analysed config
			for(int i = 0; i < _numPages.length; i++) {
				avgWriter.write("\n" + Integer.toString(_numPages[i]));
				
				for(int j = 0; j < numMeasurements.length; j++) {
					// Get sorted arrays of samples.
					double[] ddSetMeans = calculateSampleSetMeans(_dd[i], numMeasurements[j], numSets);
					double[] nonddSetMeans = calculateSampleSetMeans(_nondd[i], numMeasurements[j], numSets);
					
					double rocMin = avg(_nondd[i]);
					double rocMax = avg(_dd[i]);
					rocMax += 2*(rocMax-rocMin); // triple the interval to generate the lower end of the ROC curve...
					
	
					// generate ROC data points
					double[] limits = new double[numDataPoints];
					int[] correctDD = new int[numDataPoints];
					int[] correctNonDD = new int[numDataPoints];
					
					int ddpos = 0;
					int nonddpos = 0;
					for(int k = 0; k < numDataPoints; k++) {
						double limit = (double)rocMin+((rocMax-rocMin)*((double)k/(numDataPoints-1)));
						limits[k] = limit;
						while((ddpos < ddSetMeans.length) && (ddSetMeans[ddpos] < limit)) {
							ddpos++;
						}
						
						while((nonddpos < nonddSetMeans.length) && (nonddSetMeans[nonddpos] <= limit)) {
							nonddpos++;
						}
						
						correctDD[k] = ddSetMeans.length - ddpos;
						correctNonDD[k] = nonddpos;
					}
					
					// find optimal point
					int optDP = -1;
					int optCorrect = -1;
					for(int k = 0; k < numDataPoints; k++) {
						int corrSum = correctDD[k] + correctNonDD[k];
						if(corrSum > optCorrect) {
							optCorrect = corrSum;
							optDP = k;
						}
					}
					optLimits[i][j] = limits[optDP];
					
					// output to file
					String filename = _numPages[i] + "p-" + numMeasurements[j] + "m.csv";
					File outFile = new File(outputdir, filename);
					FileOutputStream outOS = new FileOutputStream(outFile);
					PrintWriter outWriter = new PrintWriter(outOS);
					
					// info about optimum
					double optPercDD = (double)correctDD[optDP] / numSets * 100;
					double optPercNonDD = (double)correctNonDD[optDP] / numSets * 100;
					double optPercComb = ((double)correctDD[optDP]+correctNonDD[optDP])/ (numSets*2) * 100;
					double limitPerc = ((limits[optDP]-rocMin) / (rocMax-rocMin));
					outWriter.write("opt:" + limits[optDP] + ";" + optPercDD + ";" + optPercNonDD + "\n\n");
					optWriter.write("\n" + _numPages[i] + ";" + numMeasurements[j] + ";" + limitPerc + ";" + optPercDD + ";" + optPercNonDD + ";" + optPercComb);
					avgWriter.write(";" + optPercComb);
					
					// header
					outWriter.write("limit;correctDD;correctNonDD;correctComb");
					
					for(int k = 0; k < numDataPoints; k++) {
						outWriter.write("\n");
						outWriter.write(limits[k] + ";");
						double correctPercDD = (double)correctDD[k] / numSets * 100;
						double correctPercNonDD = (double)correctNonDD[k] / numSets * 100;
						double correctPercComb = ((double)correctDD[k]+correctNonDD[k]) / (numSets*2) * 100;
						outWriter.write(correctPercDD + ";" + correctPercNonDD + ";" + correctPercComb);
					}
					
					outWriter.close();
					outOS.close();
				}
			}
			
			optWriter.close();
			optOS.close();
			
			avgWriter.close();
			avgOS.close();
			
			if(!((ddfTest == null) || (nonddfTest == null))) {
				// Load test measurements if there is a separate test set
				int[][] ddTest = initArray(ddfTest);
				int[][] nonddTest = initArray(nonddfTest);
				
				// file for detailed stats
				File optTestFile = new File(outputdir, "opt-test.csv");
				FileOutputStream optTestOS = new FileOutputStream(optTestFile);
				PrintWriter optTestWriter = new PrintWriter(optTestOS);
				// header
				optTestWriter.write("numPages;numMeasurements;limit;numSets;ddCorrect;nonDDCorrect;totalCorrect;DDPerc;NonDDPerc;TotalPerc"); // TODO write to file
				
				// file for avg stats
				File avgTestFile = new File(outputdir, "opt-test-avg.csv");
				FileOutputStream avgTestOS = new FileOutputStream(avgTestFile);
				PrintWriter avgTestWriter = new PrintWriter(avgTestOS);
				// header
				avgTestWriter.print("numPages;");
				for(int i = 0; i < numMeasurements.length; i++) {
					if(i != 0) {
						avgTestWriter.print(";");
					}
					avgTestWriter.print(numMeasurements[i]);
				}
				
				// calculate stats on test set
				for(int i = 0; i < _numPages.length; i++) {
					avgTestWriter.write("\n" + Integer.toString(_numPages[i]));

					for(int j = 0; j < numMeasurements.length; j++) {
						// Get sorted arrays of samples.
						double[] ddTestSetMeans = calculateSampleSetMeans(_dd[i], numMeasurements[j], numSets);
						double[] nonddTestSetMeans = calculateSampleSetMeans(_nondd[i], numMeasurements[j], numSets);
						
						// determine number of correct deduplicated classifications
						int correctTestDD = 0;
						for(int k = 0; k < ddTestSetMeans.length; k++) {
							if(ddTestSetMeans[k] > optLimits[i][j]) {
								correctTestDD++;
							}
						}
						
						// determine number of correct non-deduplicated classifications
						int correctTestNonDD = 0;
						for(int k = 0; k < nonddTestSetMeans.length; k++) {
							if(nonddTestSetMeans[k] <= optLimits[i][j]) {
								correctTestNonDD++;
							}
						}
						
						int totalCorrect = correctTestDD + correctTestNonDD;
						
						// calculate percentages
						double corrDDTestPerc = (double)correctTestDD / numSets * 100;
						double corrNonDDTestPerc = (double)correctTestNonDD / numSets * 100;
						double corrTotalPerc = (double)totalCorrect / (numSets*2) * 100;
						
						// write stats to files
						avgTestWriter.write(";" + corrTotalPerc);
						String out = "\n" + Integer.toString(_numPages[i]);
						out += ";" + numMeasurements[j];
						out += ";" + optLimits[i][j];
						out += ";" + numSets;
						out += ";" + correctTestDD;
						out += ";" + correctTestNonDD;
						out += ";" + totalCorrect;
						out += ";" + corrDDTestPerc;
						out += ";" + corrNonDDTestPerc;
						out += ";" + corrTotalPerc;
						optTestWriter.write(out);
						System.out.println(out);
					}
				}
				
				optTestWriter.close();
				optTestOS.close();
				avgTestWriter.close();
				avgTestOS.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Generates the a number of sample sets by bootstrap-resample from
	 * the provided set of measurements and returns the mean of the sample
	 * set. The size of the sample sets can be specified.
	 * 
	 * @param m the measurements
	 * @param setSize the size of the sample sets
	 * @param numSets how many sets are to be sampled
	 */
	private double[] calculateSampleSetMeans(int[] m, int setSize, int numSets) {
		// Take numSets samples of size setSize from set m (Bootstrap-recycle).
		// If setDD: Set is correctly classified if avg(set) >= _limit.
		// If !setDD: Set is correctly classified if avg(set) < _limit.
		// If avg(set) == _limit: Decide randomly
		
		ArrayList<Double> setMeans = new ArrayList<Double>(numSets);
		
		for(int s = 0; s < numSets; s++) {
			Random rnd = new Random();
			
			int[] set = new int[setSize];
			for(int i = 0; i < setSize; i++) {
				int rndidx = rnd.nextInt(m.length);
				set[i] = m[rndidx];
			}
			
			double setavg = avg(set);
			setMeans.add(setavg);
		}
		
		Collections.sort(setMeans);
		Double[] setMeansArray = setMeans.toArray(new Double[0]);
		
		return ArrayUtils.toPrimitive(setMeansArray);
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
		
		Option numDataPointsOpt = Option.builder("r")
				.longOpt("resolution")
				.hasArg()
				.argName("number of data points")
				.desc("Sets the resolution of the ROC stats, i.e. the number of data points to calculate (default: 300)")
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
		opt.addOption(helpOpt);
		opt.addOption(outputPathOpt);
		opt.addOption(numDataPointsOpt);
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
		
		int numDataPoints = 300;
		if(cmd.hasOption(numDataPointsOpt.getOpt())) {
			try {
				numDataPoints = Integer.parseInt(cmd.getOptionValue(numDataPointsOpt.getOpt()));
			} catch (NumberFormatException e) {
				System.err.println("Invalid resolution -- must be a number.");
				//e.printStackTrace();
				System.exit(1);
			}
		}
		
		if(nonddfteststr == null) {
			new ROCAccuracyStats(numPages, ddfstr, nonddfstr, numSets, measurementsIntArray, numDataPoints, outputdir);
		} else {
			new ROCAccuracyStats(numPages, ddfstr, nonddfstr, ddfteststr, nonddfteststr, numSets, measurementsIntArray, numDataPoints, outputdir);
		}
	}
	
	/**
	 * Prints the help message containing information about the CLI options.
	 * @param opt Options object containing CLI options.
	 */
	private static void printHelp(Options opt) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("MemSigs", opt);
	}
}
