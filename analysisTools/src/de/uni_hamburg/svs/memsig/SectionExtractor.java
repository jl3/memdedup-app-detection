package de.uni_hamburg.svs.memsig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import nl.lxtreme.binutils.elf.Elf;
import nl.lxtreme.binutils.elf.ProgramHeader;
import nl.lxtreme.binutils.elf.SectionHeader;
import nl.lxtreme.binutils.elf.SectionType;
import nl.lxtreme.binutils.elf.SegmentType;

/**
 * This class extracts the individual code sections from a binary. It relies
 * on the java-binutils available at https://github.com/jawi/java-binutils
 * 
 * @author Jens Lindemann
 */
public class SectionExtractor {
	private Elf elf;
	private int _pageSize;
	
	/**
	 * Creates a new SectionExtractor.
	 * 
	 * @param elfFilename filename of the binary
	 */
	public SectionExtractor(String elfFilename) {
		this(elfFilename, 4096);
	}
	
	public SectionExtractor(String elfFilename, int pageSize) {
		this(new File(elfFilename), pageSize);
	}
	
	/**
	 * Creates a new SectionExtractor.
	 * 
	 * @param binary the binary to process
	 */
	public SectionExtractor(File binary) {
		this(binary, 4096);
	}
	
	public SectionExtractor(File binary, int pageSize) {
		try {
			this.elf = new Elf(binary);
			this._pageSize = pageSize;
		} catch (IOException e) {
			System.err.println("I/O error");
			e.printStackTrace();
		}
	}
	
	/**
	 * Extracts all sections containing program bits from the binary into individual files.
	 * 
	 * @param outputPath path that the files will be saved to
	 */
	public void split(File outputPath) {
		split(outputPath, false, true);
	}
	
	/**
	 * Extracts all sections containing program bits from the binary.
	 * Sections can be saved in individual files or in a merged file containing
	 * all sections in one file. In the latter case, however, no padding will be
	 * applied to individual sections.
	 * 
	 * @param outputPath path that the files will be saved to
	 * @param mergedFile specifies whether a merged file containing all program bit sections should be created 
	 * @param indivFiles specifies whether individual files should be created for all sections
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
			
			/*
			for(int i = 0; i < elf.sectionHeaders.length; i++) {
				if(elf.sectionHeaders[i].type == SectionType.PROGBITS) {
					byte[] secBytes = getSectionBytes(elf.sectionHeaders[i]);
					
					if(mergedFile) {
						mos.write(secBytes);
					}
					
					if(indivFiles) {
						String sectOutFilename = new String(outputPath + "/" + elf.sectionHeaders[i].getName());
						FileOutputStream sos = new FileOutputStream(sectOutFilename);
						sos.write(secBytes);
						sos.close();
					}
				}
			}
			
			if(mergedFile) {
				mos.close();
			}*/
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
	 * @param sh {@link ProgramHeader} of the section to be retrieved
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
	
	private long pagestart(long addr) {
		long paddr = addr & ~(_pageSize - 1);
		return paddr;
	}
	
	private long pageoffset(long addr) {
		long poff = addr & (_pageSize - 1);
		return poff;
	}
	
	
	/**
	 * Gets the contents of the section identified by a specific {@link SectionHeader}.
	 * 
	 * @param sh {@link SectionHeader} of the section to be retrieved
	 * @return contents of the section
	 */
	private byte[] getSectionBytes(SectionHeader sh) {
		ByteBuffer secBuffer;
		try {
			secBuffer = elf.getSection(sh);
			int buflen = secBuffer.remaining();
			byte[] sArray = new byte[buflen];
			secBuffer.get(sArray, 0, buflen);
			return sArray;
		} catch (IOException e) {
			System.err.println("I/O error " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets the contents of all Sections of the specified SectionType from an ELF file.
	 * 
	 * @return ArrayList<byte[]> of the contents of all sections matching the specified type
	 */
	public ArrayList<byte[]> getSections(File elfFile, SectionType type) {
		try {
			Elf elf = new Elf(elfFile);
			ArrayList<byte[]> sections = new ArrayList<byte[]>();
			// elf.getSectionHeaderByType(SectionType.PROGBITS); - won't work, only returns first header of that type
			for(int i = 0; i < elf.sectionHeaders.length; i++) {
				// We only want program code, so we can skip everything else
				if(elf.sectionHeaders[i].type != type) continue;
				
				ByteBuffer secBuffer = elf.getSection(elf.sectionHeaders[i]);
				int buflen = secBuffer.remaining();
				int arlen = ((buflen / 4096) + 1) * 4096;
				byte[] sArray = new byte[arlen];
				secBuffer.get(sArray, 0, buflen);
				sections.add(sArray);
			}
			
			elf.close();
			return sections;
		} catch (IOException e) {
			System.err.println("I/O error " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Main method that allows this class to be used as a stand-alone CLI tool.
	 * The filename of the binary must be passed as first argument, the output path
	 * as the second argument.
	 * 
	 * @param args elf_filename output_path
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length >= 2) {
			SectionExtractor se = new SectionExtractor(args[0]);
			se.split(new File(args[1]));
		} else {
			System.err.println("Usage: java SectionExtractor <elfFile> <output_path>");
		}
	}

}
