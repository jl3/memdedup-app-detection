package de.uni_hamburg.svs.memsigstats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
 * This class can create statistics on the potential for memory
 * deduplication at different page sizes. It supports calculation
 * of separate statistics based on how much sharing potential a
 * {@link SoftwareVersion} exhibits at the base page size.
 * As input, it requires statistics about the individual signatures
 * at the two different page sizes that are to be compared.
 * 
 * Statistics that will be output include: average percentage of shared pages,
 * average percentage of low-sharing and high-sharing pages (according to
 * configureable threshold) and average size of signatures.
 * 
 * @author Jens Lindemann
 */
public class PageSizeStats {
	private String[] _versions;
	private int[] _sizeBase;
	private int[] _sizeCmp;
	private double[][] _basePerc;
	private double[][] _cmpPerc;
	private double _thresh;
	private int _basePS;
	private int _cmpPS;
	
	/**
	 * Creates a new PageSizeStats instance and initialises it.
	 * 
	 * @param basefileStr base stats file (typically page size of 4096 bytes)
	 * @param cmpfileStr stats file to compare base to
	 * @param threshold threshold for low- vs. high-sharing pages
	 * @param basePS page size used for base stats
	 * @param cmpPS page size used for comparison stats
	 */
	public PageSizeStats(String basefileStr, String cmpfileStr, double threshold, int basePS, int cmpPS) {
		File baseFile = new File(basefileStr);
		File cmpFile = new File(cmpfileStr);
		new PageSizeStats(baseFile, cmpFile, threshold, basePS, cmpPS);
	}
	

	/**
	 * Creates a new PageSizeStats instance and initialises it.
	 * 
	 * @param baseFile base stats file (typically page size of 4096 bytes)
	 * @param cmpFile stats file to compare base to
	 * @param threshold threshold for low- vs. high-sharing pages
	 * @param basePS page size used for base stats
	 * @param cmpPS page size used for comparison stats
	 */
	public PageSizeStats(File baseFile, File cmpFile, double threshold, int basePS, int cmpPS) {
		_thresh = threshold;
		_basePS = basePS;
		_cmpPS = cmpPS;
		initArrays(baseFile, cmpFile);
		createStats();
	}

	/**
	 * Calculates the statistics.
	 */
	private void createStats() {
		double baseLowSum = 0;
		int baseLowNum = 0;
		double baseHiSum = 0;
		int baseHiNum = 0;
		double baseSum = 0;
		double cmpLowSum = 0;
		double cmpHiSum = 0;
		double cmpSum = 0;
		
		for(int i = 0; i < _versions.length; i++) {
			for(int j = 0; j < _versions.length; j++) {
				double basev = _basePerc[i][j];
				double cmpv = _cmpPerc[i][j];
				
				baseSum += basev;
				cmpSum += cmpv;
				
				if(basev < _thresh) {
					baseLowNum++;
					baseLowSum += basev;
					cmpLowSum += cmpv;
				} else {
					baseHiNum++;
					baseHiSum += basev;
					cmpHiSum += cmpv;
				}
			}
		}
		
		double baseavg = baseSum / (baseLowNum + baseHiNum);
		double cmpavg = cmpSum / (baseLowNum + baseHiNum);
		
		double baseLowAvg = baseLowSum / baseLowNum;
		double cmpLowAvg = cmpLowSum / baseLowNum;
		
		double baseHiAvg = baseHiSum / baseHiNum;
		double cmpHiAvg = cmpHiSum / baseHiNum;
		
		double basePagesAvg = avg(_sizeBase);
		double cmpPagesAvg = avg(_sizeCmp);
		
		double baseSizeAvg = basePagesAvg * _basePS;
		double cmpSizeAvg = cmpPagesAvg * _cmpPS;
		
		System.out.println("pageSize;avg;avgLow;avgHigh;avgSize");
		System.out.println(_basePS + ";" + baseavg + ";" + baseLowAvg + ";" + baseHiAvg + ";" + baseSizeAvg);
		System.out.println(_cmpPS + ";" + cmpavg + ";" + cmpLowAvg + ";" + cmpHiAvg + ";" + cmpSizeAvg);
	}
	
	/**
	 * Computes the average of the values in an int[].
	 * 
	 * @param a int[] to compare average of
	 * @return average of values in a
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
	 * Reads the two input files into the internal arrays.
	 * 
	 * @param baseFile base stats file
	 * @param cmpFile comparison stats file
	 */
	private void initArrays(File baseFile, File cmpFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(baseFile));
			String line = br.readLine();
			if(line == null) {
				System.exit(1);
				// TODO error handling
			}
			String[] s = line.split(";");
			int numVersions = line.split(";").length - 2;
			_versions = new String[numVersions];
			_sizeBase = new int[numVersions];
			_sizeCmp = new int[numVersions];
			_basePerc = new double[numVersions][numVersions];
			_cmpPerc = new double[numVersions][numVersions];
			
			for(int i = 0; i < numVersions; i++) {
				if(i!=0) {
					line = br.readLine();
					if(line == null) {
						break;
					}
					s = line.split(";");
				}
				
				_versions[i] = s[0];
				_sizeBase[i] = new Integer(s[1]);
				
				for(int j = 0; j < numVersions; j++) {
					_basePerc[i][j] = new Double(s[j+2]);
				}
			}
			br.close();
			
			BufferedReader cr = new BufferedReader(new FileReader(cmpFile));
			
			for(int i = 0; i < numVersions; i++) {
				line = cr.readLine();
				if(line == null) {
					break;
				}
				s = line.split(";");
				
				if(!_versions[i].equals(s[0])) {
					System.err.println("Error: Data files do not belong to the same dataset.");
					System.exit(1);
				}
				_sizeCmp[i] = new Integer(s[1]);
				
				for(int j = 0; j < numVersions; j++) {
					_cmpPerc[i][j] = new Double(s[j+2]);
				}
			}
			cr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Main method that provided a CLI for the page size stat generation.
	 * 
	 * @param args CLI arguments
	 */
	public static void main(String[] args) {
		Options opt = new Options();
		
		Option baseFileOpt = Option.builder("b")
				.longOpt("basefile")
				.hasArg()
				.argName("basefilefile")
				.required()
				.desc("file containing stats for base case (typically page size of 4096 bytes)")
				.build();
		
		Option basePSOpt = Option.builder("p")
				.longOpt("bp")
				.hasArg()
				.argName("basePageSize")
				.required()
				.desc("page size of the base dataset (in bytes)")
				.build();
		
		Option cmpFileOpt = Option.builder("c")
				.longOpt("cmpfile")
				.hasArg()
				.argName("cmpfile")
				.required()
				.desc("file containing stats to compare to")
				.build();
		
		Option cmpPSOpt = Option.builder("s")
				.longOpt("cp")
				.hasArg()
				.argName("cmpPageSize")
				.required()
				.desc("page size of the comparison dataset (in bytes)")
				.build();
		
		Option thresholdOpt = Option.builder("t")
				.longOpt("threshold")
				.hasArg()
				.argName("threshold")
				.required()
				.desc("threshold between low- and high-sharing pages")
				.build();
		
		Option helpOpt = Option.builder("h")
				.longOpt("help")
				.desc("print this message")
				.build();
		
		opt.addOption(baseFileOpt);
		opt.addOption(basePSOpt);
		opt.addOption(cmpFileOpt);
		opt.addOption(cmpPSOpt);
		opt.addOption(thresholdOpt);
		opt.addOption(helpOpt);
		
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd;
		try {
			cmd = parser.parse(opt, args);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
			return;
		}
		
		if(cmd.hasOption(helpOpt.getOpt())) {
			printHelp(opt);
		}
		
		String basefileStr = cmd.getOptionValue(baseFileOpt.getOpt());
		String cmpfileStr = cmd.getOptionValue(cmpFileOpt.getOpt());
		
		String thresholdStr = cmd.getOptionValue(thresholdOpt.getOpt());
		double threshold = new Double(thresholdStr);
		
		int basePS = new Integer(cmd.getOptionValue(basePSOpt.getOpt()));
		int cmpPS = new Integer(cmd.getOptionValue(cmpPSOpt.getOpt()));
		
		new PageSizeStats(basefileStr, cmpfileStr, threshold, basePS, cmpPS);
	}
	
	/**
	 * Prints the help output to the command line.
	 */
	private static void printHelp(Options opt) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("MemSigs", opt);
	}
}
