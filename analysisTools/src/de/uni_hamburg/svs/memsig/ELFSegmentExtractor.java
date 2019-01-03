package de.uni_hamburg.svs.memsig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import nl.lxtreme.binutils.elf.Elf;
import nl.lxtreme.binutils.elf.ProgramHeader;
import nl.lxtreme.binutils.elf.SegmentType;

/**
 * This class extracts the individual code segments from an ELF binary. It relies
 * on the java-binutils available at https://github.com/jawi/java-binutils
 * 
 * @author Jens Lindemann
 */
public class ELFSegmentExtractor {
	private Elf elf;
	private int _pageSize;
	
	/**
	 * Creates a new ELFSegmentExtractor.
	 * 
	 * @param elfFilename filename of the binary
	 */
	public ELFSegmentExtractor(String elfFilename) {
		this(elfFilename, 4096);
	}
	
	/**
	 * Creates a new ELFSegmentExtractor.
	 * 
	 * @param elfFilename filename of the binary
	 * @param pageSize page size in bytes
	 */
	public ELFSegmentExtractor(String elfFilename, int pageSize) {
		this(new File(elfFilename), pageSize);
	}
	
	/**
	 * Creates a new ELFSegmentExtractor.
	 * 
	 * @param binary the binary to process
	 */
	public ELFSegmentExtractor(File binary) {
		this(binary, 4096);
	}
	
	/**
	 * Creates a new ELFSegmentExtractor.
	 * 
	 * @param binary the binary to process
	 * @param pageSize page size in bytes
	 */
	public ELFSegmentExtractor(File binary, int pageSize) {
		try {
			this.elf = new Elf(binary);
			this._pageSize = pageSize;
		} catch (IOException e) {
			System.err.println("I/O error");
			e.printStackTrace();
		}
	}
	
	/**
	 * Extracts all loadable segments from the binary into individual files.
	 * 
	 * @param outputPath path that the files will be saved to
	 */
	public void split(File outputPath) {
		split(outputPath, false, true);
	}
	
	/**
	 * Extracts all loadable segments from the binary.
	 * Segments can be saved in individual files or in a merged file containing
	 * all segments in one file. In the latter case, however, no padding will be
	 * applied to individual segments.
	 * 
	 * @param outputPath path that the files will be saved to
	 * @param mergedFile specifies whether a merged file containing all program bit segments should be created 
	 * @param indivFiles specifies whether individual files should be created for all segments
	 */
	public void split(File outputPath, boolean mergedFile, boolean indivFiles) {
		try {
			outputPath.mkdirs();
			FileOutputStream mos = null;
			if(mergedFile) {
				String mergedOutFilename = new String(outputPath + "/merged.dat");
				mos = new FileOutputStream(mergedOutFilename);
			}
			
			for(int i = 0; i < elf.programHeaders.length; i++) {
				ProgramHeader ph = elf.programHeaders[i];
				if(!ph.type.equals(SegmentType.LOAD)) {
					continue;
				}
				
				byte[] segBytes = getSegmentBytes(ph);
				
				if(mergedFile) {
					mos.write(segBytes);
				}
				
				if(indivFiles) {
					String sectOutFilename = new String(outputPath + "/" + i + ".seg");
					FileOutputStream sos = new FileOutputStream(sectOutFilename);
					sos.write(segBytes);
					sos.close();
				}
			}
			
			if(mergedFile) {
				mos.close();
			}
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O error " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets the contents of the segment identified by a specific {@link ProgramHeader}.
	 * 
	 * @param sh {@link ProgramHeader} of the segment to be retrieved
	 * @return contents of the segment
	 */
	private byte[] getSegmentBytes(ProgramHeader ph) {
		ByteBuffer segBuffer;
		try {
			long addr = pagestart(ph.virtualAddress);
			long off = ph.offset - pageoffset(ph.virtualAddress);
			
			segBuffer = elf.getSegment(ph);
			int buflen = segBuffer.remaining();
			
			// ensure padding to page size
			int missingBytes = 0;
			int mod = buflen % _pageSize;
			if(mod > 0) {
				missingBytes = _pageSize - mod;
			}
			int arlen = buflen + missingBytes;
			
			byte[] sArray = new byte[arlen];
			segBuffer.get(sArray, 0, buflen);
			return sArray;
		} catch (IOException e) {
			System.err.println("I/O error " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Returns the address of the page boundary before the specified address.
	 * 
	 * @param addr address to calculate page start for
	 * @return start of page containing addr
	 */
	private long pagestart(long addr) {
		long paddr = addr & ~(_pageSize - 1);
		return paddr;
	}
	
	/**
	 * Returns the offset of addr to the page boundary.
	 * 
	 * @param addr address to calculate page offset for
	 * @return page offset for addr
	 */
	private long pageoffset(long addr) {
		long poff = addr & (_pageSize - 1);
		return poff;
	}

	/**
	 * Main method that allows this class to be used as a stand-alone CLI tool.
	 * The filename of the binary must be passed as first argument, the output path
	 * as the second argument.
	 * 
	 * @param args elf_filename output_path
	 */
	public static void main(String[] args) {
		if(args.length >= 2) {
			ELFSegmentExtractor se = new ELFSegmentExtractor(args[0]);
			se.split(new File(args[1]));
		} else {
			System.err.println("Usage: java ELFSegmentExtractor <elfFile> <output_path>");
		}
	}

}
