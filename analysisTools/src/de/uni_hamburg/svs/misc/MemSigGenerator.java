package de.uni_hamburg.svs.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a signature of a memory image. This is done by comparing it to
 * other memory images and removing any duplicate memory pages, leaving only
 * the ones being unique to the memory image for which a signature is to be
 * created. 
 * 
 * This class is only useful for experimentation with individual versions and
 * cannot be used for generating signatures for large-scale datasets.
 * 
 * @author Jens Lindemann
 */

@Deprecated
public class MemSigGenerator {
	FileOutputStream _log; // TODO implement logging
	
	int _totalUnique;
	int _totalDuplicate;
	
	/**
	 * Constructor for generating signatures by comparing two files.
	 * 
	 * @param filename1 file to create signature for
	 * @param filename2 file to compare to
	 * @param outFile file to write signature to
	 * @param logFilename file to write logs to
	 */
	public MemSigGenerator(String filename1, String filename2, String outFile, String logFilename) {
		File f1 = new File(filename1);
		File f2 = new File(filename2);
		initLog(logFilename);
		createSignature(f1, f2, outFile, null);
		closeLog();
	}
	
	/**
	 * Constructor for generating signatures by comparing two directories
	 * containing 
	 * 
	 * @param dirname1
	 * @param dirname2
	 * @param logFilename
	 */
	public MemSigGenerator(String dirname1, String dirname2, String logFilename) {
		initLog(logFilename);
		createDirSignatures(dirname1, dirname2);
		closeLog();
	}
	
	/**
	 * Initialises the log.
	 * 
	 * @param logFilename filename of log
	 */
	private void initLog(String logFilename) {
		File logfile = new File(logFilename);
		try {
			logfile.createNewFile();
			_log = new FileOutputStream(logfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Closes the log.
	 */
	private void closeLog() {
		try {
			_log.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads a File from disk and creates a List of byte arrays, where each
	 * array corresponds to one memory page.
	 * @param file File to be loaded
	 * @param list List to be filled
	 */
	private void loadFile(File file, ArrayList<byte[]> list) {
		try {
			FileInputStream is = new FileInputStream(file);
			int bytesRead;
			do {
				byte[] b = new byte[4096];
				bytesRead = is.read(b);
				if(bytesRead > 0) {
					list.add(b);
				}
			} while (bytesRead == 4096);
			is.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Compares two byte arrays to check whether the values contained are
	 * equal.
	 * 
	 * WARNING: This is optimised for efficiency, NOT for resistance against
	 * timing attacks.
	 * 
	 * @param b1 first array to compare
	 * @param b2 second array to compare
	 * @return true if values are equal, false otherwise
	 */
	private boolean compareByteArrays(byte[] b1, byte[] b2) {
		if(b1.length != b2.length) {
			return false;
		}
		
		for(int i = 0; i < b1.length; i++) {
			 if(b1[i] != b2[i]) {
				 return false;
			 }
		}
		
		return true;
	}
	
	/**
	 * Compares two files (in the form of Lists of byte arrays corresponding
	 * to memory pages) and replaces duplicate pages by null in byteList.
	 * 
	 * @param byteList List containing file for which signature is to be generated
	 * @param removeByteList List containing file to be compared against
	 */
	private void removePages(List<byte[]> byteList, List<byte[]> removeByteList) {
		for (int i = 0; i < byteList.size(); i++) {
			byte[] b1 = byteList.get(i);
			if (b1 == null) continue;
			
			for(int j = 0; j < removeByteList.size(); j++) {
				byte[] b2 = removeByteList.get(j);
				if(compareByteArrays(b1, b2)) {
					byteList.set(i, null);
					System.out.println("Found duplicate of page " + i);
					break;
				}
			}
		}
	}
	
	/**
	 * Creates the signature.
	 * 
	 * @param sigFile {@link File} to create signature for
	 * @param cmpFile {@link File} to compare sigFile to
	 * @param outFilename {@link File} to ouput signature to
	 * @param mos FileOutputStream for merged signature. null, if no merged signature is to be created
	 */
	private void createSignature(File sigFile, File cmpFile, String outFilename, FileOutputStream mos) {
		ArrayList<byte[]> sigBytes = new ArrayList<byte[]>();
		ArrayList<byte[]> bytes2 = new ArrayList<byte[]>();
		loadFile(sigFile, sigBytes);
		loadFile(cmpFile, bytes2);
		
		removePages(sigBytes, bytes2);
		
		try {
			File outFile = new File(outFilename);
			if(!outFile.exists()){
				outFile.createNewFile();
			}

			FileOutputStream os = new FileOutputStream(outFile);
			
			int unique = 0;
			int duplicate = 0;
			for(int i = 0; i < sigBytes.size(); i++) {
				byte[] b = sigBytes.get(i);
				if(b == null) {
					duplicate++;
					_totalDuplicate++;
				} else {
					unique++;
					_totalUnique++;
					
					os.write(b);
					
					if(mos != null) {
						mos.write(b);
					}
				}
			}
			os.close();
			
			System.out.println("Signature created");
			_log.write(new String("Comparing " + sigFile.getName() + " and " + cmpFile.getName() + "\n").getBytes());
			_log.write(new String("Unique pages: " + unique + "\n").getBytes());
			System.out.println("Unique pages: " + unique);
			_log.write(new String("Duplicate pages: " + duplicate + "\n").getBytes());
			System.out.println("Duplicate pages: " + duplicate);
			int total = unique + duplicate;
			_log.write(new String("Total pages for section: " + total + "\n\n").getBytes());
			System.out.println("Total pages: " + total);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Compare the individual sections of a binary. The sections must be placed
	 * in individual files in two directories. Unique pages in sections in
	 * dirname1 are determined by comparing the sections to those in dirname2.
	 * The comparison is on a section-by-section base only -- to create real
	 * signatures, the existence of a page should be checked for *all* sections
	 * of the other binary.
	 * 
	 * @param dirname1 directory containing files for all sections of the first binary
	 * @param dirname2 directory containing files for all sections of the binary to compare the first binary to
	 */
	@Deprecated
	private void createDirSignatures(String dirname1, String dirname2) {
		try {
			File dir1 = new File(dirname1);
			File dir2 = new File(dirname2);
			
			_log.write(new String("Creating signatures for " + dir1.getName() + " compared to" + dir2.getName() + "\n\n").getBytes());
		
			// Create subdirectory for signatures
			String dir2last = dir2.getName();
			String outdirname = new String(dirname1 + "/" + dir2last);
			File outdir = new File(outdirname);
			outdir.mkdir();
			
			String mosFilename = new String(outdir.getAbsolutePath() + "/merged.sig");
			File mosFile = new File(mosFilename);
			if(!mosFile.exists()){
				mosFile.createNewFile();
			}
	
			FileOutputStream mos = new FileOutputStream(mosFile);
			
			File[] dir1Files = dir1.listFiles();
			for(int i = 0; i < dir1Files.length; i++) {
				File f = dir1Files[i];
				
				// Directories are irrelevant, so skip them.
				if(f.isDirectory()) {
					continue;
				}
				
				String f2name = new String(dirname2 + "/" + f.getName());
				File f2 = new File(f2name);
				
				// If the file does not exist, all of the section's pages are unique.
				if(!f2.exists()) {
					int bytesRead;
					FileInputStream is = new FileInputStream(f);
					FileOutputStream cos = new FileOutputStream(new String(outdir.getAbsolutePath() + "/sigcopy-" + f.getName()));
					do {
						byte[] b = new byte[4096];
						bytesRead = is.read(b);
						if(bytesRead > 0) {
							mos.write(b);
							cos.write(b); // Copy is padded to multiple of 4096 bytes
						}
					} while (bytesRead == 4096);
					is.close();
					cos.close();
					
					continue;
				}
				
				// Otherwise, we have to take a closer look.
				String outfilename = new String(outdir.getAbsolutePath() + "/sig-" + f.getName());
				createSignature(f, f2, outfilename, mos);
			}
			
			_log.write(new String("Total stats\n").getBytes());
			_log.write(new String("Unique pages: " + _totalUnique + "\n").getBytes());
			_log.write(new String("Duplicate pages: " + _totalDuplicate + "\n").getBytes());
			int total = _totalUnique + _totalDuplicate;
			_log.write(new String("Total pages: " + total + "\n").getBytes());
		} catch(IOException e) {
			e.printStackTrace();
			// TODO handle error
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length >= 4) {
			if(args[0].equals("-f")) {
				new MemSigGenerator(args[1], args[2], args[3]);
			} else {
				new MemSigGenerator(args[1], args[2], args[0], args[3]);
			}
		} else {
			System.err.println("Syntax: java MemSigGenerator <outputFile> <file1> <file2> <logfile>");
			System.err.println("java -f <dir1> <dir2> <logfile>");
		}
	}
}
