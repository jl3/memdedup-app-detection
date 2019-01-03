/**
 * Main class that offers a CLI.
 * 
 * @author Jens Lindemann
 */

package de.uni_hamburg.svs.memsig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;

public class MemSigs {
	static final String sep = ";";

	/**
	 * 
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		Options opt = new Options();
		
		// define CLI options
		Option swnameOpt = Option.builder("s")
										.longOpt("swname")
										.hasArg()
										.argName("name")
										.required()
										.desc("name of software")
										.build();
		
		Option swpathOpt = Option.builder("d")
								.longOpt("swpath")
								.hasArg()
								.argName("path")
								.required()
								.desc("path to software")
								.build();
		
		Option binnameOpt = Option.builder("b")
								.longOpt("bin")
								.hasArg()
								.argName("file")
								.required()
								.desc("name of program binary")
								.build();
		
		Option vsigsOpt = Option.builder("vsigs")
								.desc("creates version signatures for all versions in the specified subdirectory of swpath (default: swpath/vsigs).")
								.optionalArg(true)
								.numberOfArgs(1)
								.argName("directory")
								.build();
		
		Option cmpVersOpt = Option.builder("c")
								.longOpt("compv")
								.desc("compares all versions (equal, unique and internal duplicate pages). Output directory can be specified (default: swpath/comp).")
								.optionalArg(true)
								.numberOfArgs(1)
								.argName("directory")
								.build();
		
		Option findGroupsOpt = Option.builder("f")
								.longOpt("findgrp")
								.desc("determines the optimal group configuration for group signatures (according to parameters -t and -md). Output directory can be specified (default: swpath/groups).")
								.optionalArg(true)
								.numberOfArgs(1)
								.argName("output_dir")
								.build();
		
		Option findGroupsAlgOpt = Option.builder("falg")
								   .longOpt("findgrpalg")
								   .desc("specifies the algorithm used for identifying groups: similarity-maxsigsize, neighbour-maxgrpsize (Default if falg is not specified: similarity-maxsigsize)")
								   .hasArg()
								   .argName("algorithm")
								   .build();
		
		Option matchpagesOpt = Option.builder("m")
								.longOpt("matchpgs")
								.desc("find individual pages matching across versions. Output directory can be specified (Default: swpath/matchpgs)")
								.optionalArg(true)
								.numberOfArgs(1)
								.argName("directory")
								.build();
		
		Option threshOpt = Option.builder("t")
								.longOpt("threshold")
								.desc("threshold for single-version signatures to be considered for group signatures (default: 0.4)")
								.hasArg()
								.argName("threshold")
								.build();
		
		Option maxDistOpt = Option.builder("md")
								.longOpt("maxdist")
								.desc("maximum distance between first and last version in a group (default: 10)")
								.hasArg()
								.argName("distance")
								.build();
		
		Option helpOpt = Option.builder("h")
								.longOpt("help")
								.desc("print this message")
								.build();
		
		Option psizeOpt = Option.builder("p")
								.longOpt("pagesize")
								.hasArg()
								.argName("bytes")
								.desc("set page size (default=4096)")
								.build();
								
		// add CLI options
		opt.addOption(swnameOpt);
		opt.addOption(swpathOpt);
		opt.addOption(binnameOpt);
		opt.addOption(vsigsOpt);
		opt.addOption(cmpVersOpt);
		opt.addOption(findGroupsOpt);
		opt.addOption(findGroupsAlgOpt);
		opt.addOption(matchpagesOpt);
		opt.addOption(helpOpt);
		opt.addOption(psizeOpt);
		opt.addOption(threshOpt);
		opt.addOption(maxDistOpt);
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(opt, args);
			
			if(cmd.hasOption(helpOpt.getOpt())) {
				printHelp(opt);
			}
			
			String swname = cmd.getOptionValue(swnameOpt.getOpt());
			String swpathStr = cmd.getOptionValue(swpathOpt.getOpt());
			String binname = cmd.getOptionValue(binnameOpt.getOpt());
			
			// Get pagesize from CLI arg. If not specified, use 4096 as default.
			int pagesize = 4096;
			if(cmd.hasOption(psizeOpt.getOpt())) {
				try {
					pagesize = Integer.parseInt(cmd.getOptionValue(psizeOpt.getOpt()));
				} catch (NumberFormatException e) {
					System.err.println("Invalid page size -- must be a number.");
					//e.printStackTrace();
					System.exit(1);
				}
			}
			
			File swpath = new File(swpathStr);
			File versionsPath = new File(swpath, "versions");
			
			Software sw = new Software(swname, versionsPath, binname, pagesize);
			
			// Generate version signatures (and statistics) if the appropriate CLI
			// option has been set.
			if(cmd.hasOption(vsigsOpt.getOpt())) {
				VersionSignature[] sigs = sw.generateVersionSignatures(pagesize);
				
				// Unless no versions have been provided, this should return signatures...
				if(sigs.length == 0) {
					System.err.println("Error: No signatures have been generated. Please check whether swpath was set correctly.");
					return;
				}
				
				// Get the name for the signature dir from CLI arguments. If none was specified, use vsigs as default.
				String vsigdirname = cmd.getOptionValue(vsigsOpt.getOpt());
				if(vsigdirname == null) vsigdirname = "vsigs";
				File vsigdir = new File(swpath, vsigdirname);
				vsigdir.mkdir();
				
				// Write software and page size information to file
				File vsiginfofile = new File(vsigdir, "info.txt");
				FileOutputStream infoOs = new FileOutputStream(vsiginfofile);
				PrintWriter infoWriter = new PrintWriter(infoOs);
				infoWriter.write("Software: " + swname + "\n");
				infoWriter.write("Page size: " + pagesize + "\n");
				
				// Open log file for signature details (i.e. which pages from which sections are included in the signature)
				File detailFile = new File(vsigdir, "details.txt");
				FileOutputStream detailOs = new FileOutputStream(detailFile);
				PrintWriter detailWriter = new PrintWriter(detailOs);
				
				// Open log file for signature statistics
				File vsigstatfile = new File(vsigdir, "sigstats.csv");
				FileOutputStream statOs = new FileOutputStream(vsigstatfile);
				PrintWriter statWriter = new PrintWriter(statOs);
				statWriter.write("version" + sep + "binSize" + sep + "sigSize" + sep + "all01" + sep + "intDup" + sep + "dupsOtherVersions\n");
				
				// save signatures and statistics about the signatures to files
				for(VersionSignature sig : sigs) {
					SoftwareVersion[] svers = sig.getSoftwareVersions();
					String filename = new String();
					
					if(svers.length == 0) {
						// This should never happen...
						System.err.println("Error: signature does not contain version information.");
						break;
					} else if(svers.length == 1) {
						SoftwareVersion sv = sig.getSoftwareVersions()[0];
						String versionString = sv.toString();
						filename += sv.getSoftware().getName();
						filename += "-";
						filename += versionString;
						filename += ".sig";
						
						statWriter.write(versionString + sep);
						statWriter.write(sv.numberOfPages(sig.getPageSize()) + sep);
						statWriter.write(sig.numberOfPages() + sep);
						statWriter.write(sig.getAll01Count() + sep);
						statWriter.write(sig.getIntDupCount() + sep);
						statWriter.write(sig.getOtherVersionDups() + "\n");
						
						detailWriter.write("Version: " + versionString + "\nPages:\n");
						for(int i = 0; i < sig.numberOfPages(); i++) {
							Page p = sig.getPage(i);
							String sectName = p.getPart().getName();
							detailWriter.write("Section: " + sectName + "; page: " + p.getPageNumber() + "\n");
						}
						detailWriter.write("---------------\n");
					} else {
						System.err.println("Saving to files not implemented yet for multi-version signatures!");
						break;
						// TODO implement
					}
					
					File sigfile = new File(vsigdir, filename);
					try {
						sig.writeToFile(sigfile);
					} catch (FileNotFoundException e) {
						System.err.println("Error: Could not write to signature file " + sigfile.getAbsolutePath());
						e.printStackTrace();
					}
				}
				
				// close output streams and writers
				infoWriter.flush();
				infoWriter.close();
				infoOs.flush();
				infoOs.close();
				
				detailWriter.flush();
				detailWriter.close();
				detailOs.flush();
				detailOs.close();
				
				statWriter.flush();
				statWriter.close();
				statOs.flush();
				statOs.close();
			}
			
			// Calculate similarities between versions (if appropriate CLI option is set)
			if(cmd.hasOption(cmpVersOpt.getOpt())) {
				TreeMap<SoftwareVersion,HashMap<SoftwareVersion, VersionComparisonResult>> cmpres = sw.compareAllVersions(pagesize);
				
				// Create subdirectory for comparison stats (if no name is specified in CLI options, use comp as default
				String cmpdirname = cmd.getOptionValue(cmpVersOpt.getOpt());
				if(cmpdirname == null) cmpdirname = "comp";
				File cmpdir = new File(swpath, cmpdirname);
				if(!cmpdir.exists()) {
					cmpdir.mkdir();
				}
				
				// Create detailed stat file (unique pages, internal duplicates, matching pages between two versions)
				File cmpFile = new File(cmpdir, "comp.csv");
				FileOutputStream cmpOs = new FileOutputStream(cmpFile);
				PrintWriter cmpWriter = new PrintWriter(cmpOs);
				
				// Create stat file containing only number of pages matching between a pair of versions
				File duplFile = new File(cmpdir, "dupl.csv");
				FileOutputStream duplOs = new FileOutputStream(duplFile);
				PrintWriter duplWriter = new PrintWriter(duplOs);
				
				// Create stat file containing only relative proportion of pages matching between a pair of versions
				File duplRelFile = new File(cmpdir, "dupl-rel.csv");
				FileOutputStream duplRelOs = new FileOutputStream(duplRelFile);
				PrintWriter duplRelWriter = new PrintWriter(duplRelOs);
				
				SoftwareVersion[] versions = cmpres.keySet().toArray(new SoftwareVersion[0]);
				
				// Write headers to stat files
				cmpWriter.write(sep);
				cmpWriter.write("#pages" + sep);
				duplWriter.write(sep);
				duplWriter.write("#pages" + sep);
				for(int i = 0; i < versions.length; i++) {
					cmpWriter.write(versions[i].toString() + "-uniq");
					cmpWriter.write(sep);
					cmpWriter.write(versions[i].toString() + "-dupl");
					cmpWriter.write(sep);
					cmpWriter.write(versions[i].toString() + "-intdup");
					if((i+1) < versions.length) {
						cmpWriter.write(sep);
					}
					
					duplWriter.write(versions[i].toString());
					if((i+1) < versions.length) {
						duplWriter.write(sep);
					}
				}
				cmpWriter.write("\n");
				duplWriter.write("\n");
				
				// write stats to stat files
				for(int i = 0; i < versions.length; i++) {
					int numPages = versions[i].numberOfPages(pagesize);
					cmpWriter.write(versions[i].toString());
					cmpWriter.write(sep);
					cmpWriter.write(Integer.toString(numPages));
					
					duplWriter.write(versions[i].toString());
					duplWriter.write(sep);
					duplWriter.write(Integer.toString(numPages));
					
					duplRelWriter.write(versions[i].toString());
					duplRelWriter.write(sep);
					duplRelWriter.write(Integer.toString(numPages));
					
					HashMap<SoftwareVersion, VersionComparisonResult> vmap = cmpres.get(versions[i]);
					for(int j = 0; j < versions.length; j++) {
						VersionComparisonResult res = vmap.get(versions[j]);
						if(!versions[i].equals(versions[j])) {
							cmpWriter.write(sep);
							cmpWriter.write(Integer.toString(res.numberOfUniques()));
							cmpWriter.write(sep);
							cmpWriter.write(Integer.toString(res.numberOfMatches()));
							cmpWriter.write(sep);
							cmpWriter.write(Integer.toString(res.numberOfInternalDuplicates()));
							
							duplWriter.write(sep);
							duplWriter.write(Integer.toString(res.numberOfMatches()));
							
							double relDupl = ((double)res.numberOfMatches()) / numPages * 100;
							duplRelWriter.write(sep);
							duplRelWriter.write(Double.toString(relDupl));
							
						} else {
							// If a version is compared with itself, all pages will match.
							cmpWriter.write(sep + sep + sep);
							duplWriter.write(sep);
							duplWriter.write(Integer.toString(versions[j].numberOfPages(pagesize)));
							duplRelWriter.write(sep + "100");
						}
					}
					
					cmpWriter.write("\n");
					duplWriter.write("\n");
					duplRelWriter.write("\n");
				}
				
				// close output streams and writers
				cmpWriter.flush();
				cmpWriter.close();
				cmpOs.flush();
				cmpOs.close();
				
				duplWriter.flush();
				duplWriter.close();
				duplOs.flush();
				duplOs.close();
				
				duplRelWriter.flush();
				duplRelWriter.close();
				duplRelOs.flush();
				duplRelOs.close();
			}
			
			if(cmd.hasOption(matchpagesOpt.getOpt())) {
				// Create subdirectory for comparison stats (if no name is specified in CLI options, use comp as default
				String mpfdirname = cmd.getOptionValue(matchpagesOpt.getOpt());
				if(mpfdirname == null) mpfdirname = "matchpgs";
				File mpfdir = new File(swpath, mpfdirname);
				if(!mpfdir.exists()) {
					mpfdir.mkdir();
				}
				MatchingPageFinder mpf = new MatchingPageFinder(sw, pagesize, mpfdir);
				mpf.findMatchingPages();
			}
			
			if(cmd.hasOption(findGroupsOpt.getOpt())) {
				// get threshold from CLI
				String threshstr = cmd.getOptionValue(threshOpt.getOpt());
				if(threshstr == null) threshstr = "0.4";
				Double thresh = new Double(threshstr);
				
				// get maximum distance from CLI
				String maxDistStr = cmd.getOptionValue(maxDistOpt.getOpt());
				if(maxDistStr == null) maxDistStr = "10";
				Integer maxDist = new Integer(maxDistStr);
				
				//RecursiveGroupFinder grpf = new RecursiveGroupFinder(sw, pagesize, thresh, maxDist);
				//GroupFinder grpf = new IterativeGroupFinder(sw, pagesize, thresh, maxDist);
				String algstring = "similarity-maxsigsize"; // default
				if(cmd.hasOption(findGroupsAlgOpt.getOpt())) {
					algstring = cmd.getOptionValue(findGroupsAlgOpt.getOpt());
				}
				
				GroupFinder grpf = null;
				if(algstring.equals("similarity-maxsigsize")) {
					grpf = new IterativeSimilarityGroupFinder(sw, pagesize, thresh, maxDist);
				} else if (algstring.equals("neighbour-maxgrpsize")) {
					grpf = new IterativeNeighbouringGroupFinder(sw, pagesize, thresh, maxDist);
				} else {
					System.err.print("ERROR: Incorrect group finding algorithm specified.");
					MemSigs.printHelp(opt);
					System.exit(1);
				}
				SoftwareVersionGroup[] bestGroups = grpf.findSignatureGroups();
				
				// Create String for group configuration and stats file
				String grpcfg = new String();
				grpcfg += "avgsize" + sep + grpf.getBestGroupConfigAvgSigsize() + "\n";
				for(int i = 0; i < bestGroups.length; i++) {
					grpcfg += ("Group" + i + sep);
					SoftwareVersion[] currGrp = bestGroups[i].toArray();
					
					for(int j = 0; j < currGrp.length; j++) {
						grpcfg += (currGrp[j]);
						if((j + 1) < currGrp.length) { 
							grpcfg += sep;
						}
					}
					grpcfg += ("\n");
				}
				
				System.out.println(grpcfg);
				
				// Create subdirectory for comparison stats (if no name is specified in CLI options, use comp as default
				String grpdirname = cmd.getOptionValue(findGroupsOpt.getOpt());
				if(grpdirname == null) grpdirname = "groups";
				File grpdir = new File(swpath, grpdirname);
				if(!grpdir.exists()) {
					grpdir.mkdir();
				}
				
				// write group config to file in grpdir
				File grpcfgFile = new File(grpdir, "groupconfig.csv");
				FileOutputStream grpCfgOs = new FileOutputStream(grpcfgFile);
				PrintWriter grpCfgWriter = new PrintWriter(grpCfgOs);
				grpCfgWriter.write(grpcfg);
				grpCfgWriter.flush();
				grpCfgWriter.close();
				grpCfgOs.flush();
				grpCfgOs.close();
				
				// write individual group stats to file in grpdir
				// Create detailed stat file (unique pages, internal duplicates, matching pages between two groups)
				File grpstatsFile = new File(grpdir, "groupstats.csv");
				FileOutputStream grpstatsOs = new FileOutputStream(grpstatsFile);
				PrintWriter grpstatsWriter = new PrintWriter(grpstatsOs);
				grpstatsWriter.write("group" + sep + "binSize" + sep + "sigSize" + sep + "all01" + sep + "intDup" + sep + "dupsOtherVersions" + sep + "notMatchingInGroup" + sep + "avgDist" + sep + "lowHigDist" + sep + "skippedVer");
				
				
				// write group signatures to files in grpdir
				for(int i = 0; i < bestGroups.length; i++) {
					SoftwareVersion[] versions = bestGroups[i].toArray();
					//SoftwareVersion[] versions = new SoftwareVersion[currGrpArray.length];
					String grpname = "g" + i;
					for(int j = 0; j < versions.length; j++) {
						grpname += "__" + versions[j].toString();
					}
					//grpname += ".sig";
					System.out.println("Generating "  + grpname + "...");
					
					VersionSignature sig = sw.generateVersionsSignature(versions, pagesize);
					
					grpstatsWriter.write("\n");
					grpstatsWriter.write(grpname + sep);
					// TODO For now, we'll just use the size of the first binary.
					// Other options: - smallest binary (as this is the upper bound)?
					// - number of pages present in all versions?
					grpstatsWriter.write(versions[0].numberOfPages(pagesize) + sep);
					grpstatsWriter.write(sig.numberOfPages() + sep);
					grpstatsWriter.write(sig.getAll01Count() + sep);
					grpstatsWriter.write(sig.getIntDupCount() + sep);
					grpstatsWriter.write(sig.getOtherVersionDups() + sep);
					grpstatsWriter.write(sig.getNotMatchingInGroupCount() + sep);
					
					SoftwareVersion[] idvVer = sw.getVersions().toArray(new SoftwareVersion[0]);
					
					// avgDist
					/*long distSum = 0;
					int distNum = 0;
					for(int j = 0; j < versions.length; j++) {
						int pos1 = ArrayUtils.indexOf(idvVer, versions[j]);
						for(int k = j+1; k < versions.length; k++) {
							int pos2 = ArrayUtils.indexOf(idvVer, versions[k]);
							int dist = Math.abs(pos2 - pos1);
							distSum += dist;
							distNum++;
						}
					}
					double avgDist = (double)distSum / distNum;*/
					double avgDist = bestGroups[i].getAvgVersionDistance();
					grpstatsWriter.write(avgDist + sep);
					
					// lowHiDist
					//int lowPos = ArrayUtils.indexOf(idvVer, versions[0]);
					//int hiPos = ArrayUtils.indexOf(idvVer, versions[versions.length-1]);
					//int lowHiDist = Math.abs(hiPos - lowPos);
					int lowHiDist = bestGroups[i].getMaxVersionDistance();
					grpstatsWriter.write(lowHiDist + sep);
					
					// skippedVer
					int skippedVer = bestGroups[i].getSkippedVersionCount();
					grpstatsWriter.write(skippedVer + "");
					
					File sigFile = new File(grpdir, grpname + ".sig");
					sig.writeToFile(sigFile);
				}
				
				grpstatsWriter.flush();
				grpstatsWriter.close();
				grpstatsOs.flush();
				grpstatsOs.close();
			}
		} catch (MissingOptionException e) {
			printHelp(opt);
			System.exit(1);
		} catch (ParseException e) {
			System.err.println("Error: Could not parse CLI options.");
			e.printStackTrace();
			System.exit(1);
		} catch (FileNotFoundException e1) {
			System.err.println("Error: File not found.");
			e1.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err.println("I/O Error");
			e.printStackTrace();
			System.exit(1);
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
