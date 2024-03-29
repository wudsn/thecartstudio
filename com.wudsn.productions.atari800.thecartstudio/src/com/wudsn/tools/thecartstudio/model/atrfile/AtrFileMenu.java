/**
 * Copyright (C) 2013 - 2020 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of The!Cart Studio distribution.
 * 
 * The!Cart Studio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * The!Cart Studio distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with The!Cart Studio. If not, see <https://www.gnu.org/licenses/>.
 */

package com.wudsn.tools.thecartstudio.model.atrfile;

import static com.wudsn.tools.base.common.ByteArrayUtility.MASK_FF;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.wudsn.tools.base.atari.AtrFile;
import com.wudsn.tools.base.atari.AtrFile.AtrException;
import com.wudsn.tools.base.common.FileUtility;
import com.wudsn.tools.base.common.StringUtility;
import com.wudsn.tools.thecartstudio.model.AtrLoader;
import com.wudsn.tools.thecartstudio.model.AtrLoader.PatchRange;
import com.wudsn.tools.thecartstudio.model.ExportFormat.FileExtensions;
import com.wudsn.tools.thecartstudio.model.ImportableMenu;
import com.wudsn.tools.thecartstudio.model.WorkbookEntry.Parameter;

public final class AtrFileMenu extends ImportableMenu {

	/**
	 * Types of Mike Langer's BootManagers
	 *
	 */
	private static enum BootManagerType {
		NONE, V1, V2
	}

	private static final String PICONAME_TXT = "PICONAME.TXT";

	private AtrFile atrFile;

	/**
	 * Creation is public.
	 * 
	 * @param content
	 *                    The file content, not <code>null</code>.
	 */
	public AtrFileMenu(byte[] content) {
		super(content, "MyPicoDOS or Bootmanager");
		try {
			atrFile = AtrFile.createInstance(content);
		} catch (AtrException ex) {

		}
	}

	@Override
	public boolean hasMenuEntries() {
		if (atrFile != null) {
			if (!getLongFileNames().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int collectMenuEntries(Object owner, Collector collector) {
		if (owner == null) {
			throw new IllegalArgumentException("Parameter 'owner' must not be null.");
		}
		if (collector == null) {
			throw new IllegalArgumentException("Parameter 'collector' must not be null.");
		}

		if (atrFile == null) {
			return Result.NO_MENU_FOUND;
		}

		List<String> longFileNames = getLongFileNames();
		if (longFileNames.isEmpty()) {
			return Result.NO_MENU_ENTRIES_FOUND;
		}

		// Use menu version 0 as nothing is actually startable.
		int menuVersion = 0;
		for (int j = 0; j < longFileNames.size(); j++) {
			collector.collectMenuEntry(owner, menuVersion, j, longFileNames.get(j));
		}

		// Returns MENU_ENTRIES_FOUND_AND_STARTABLE to prevent warning that a
		// newer menu version would solve the issue.
		return Result.MENU_ENTRIES_FOUND_AND_STARTABLE;
	}

	/**
	 * File menus for ATRs use a real DOS or a simple game DOS only. The direct SIO
	 * calls (JSR/JMP $E459) for them will only be present in the boot area in the
	 * disk. Hence the ATR patching can be limited to that area to increase
	 * accuracy.
	 * 
	 * @return The modifiable list of relevant patch ranges, may be empty, not
	 *         <code>null</code>.
	 */
	public List<PatchRange> getPatchRanges() {
		List<PatchRange> patchRanges = new ArrayList<PatchRange>();

		try {
			AtrFile atrFile = AtrFile.createInstance(content);
			List<Integer> usedSectors = new ArrayList<Integer>();
			switch (addBootmanagerFileNames(atrFile, null)) {
			case V1:
				patchRanges.add(new PatchRange(0, 0x11d));
				break;
			case V2:
				patchRanges.add(new PatchRange(0, 0x121));
				break;

			default:

				if (atrFile.hasDirectory() && atrFile.getFileContent("DOS.SYS", usedSectors) != null) {
					usedSectors.add(Integer.valueOf(1));
					usedSectors.add(Integer.valueOf(2));
					usedSectors.add(Integer.valueOf(3));
					addPatchedSectors(atrFile, usedSectors, patchRanges);
				} else {
					patchRanges.add(new PatchRange(0, Integer.MAX_VALUE));
				}
			}
		} catch (AtrException ex) {
			throw new RuntimeException(ex);
		}
		return patchRanges;
	}

	/**
	 * Gets the default patch parameters for the ATR.
	 * 
	 * @return The modifiable list of default patch parameters, may be empty, not
	 *         <code>null</code>.
	 */
	public List<Parameter> getPatchParameters() {
		List<Parameter> result = new ArrayList<Parameter>();
		int offset;

		// See "Patch-Bootmanager.txt" for details.
		switch (addBootmanagerFileNames(atrFile, null)) {
		case NONE:
			break;

		case V1:
			try {
				offset = atrFile.getSectorStartOffset(364) + 0x71; // Address $0971
			} catch (AtrException ex) {
				throw new RuntimeException(ex);
			}
			// $0971 LDX $nnnn for SELECTED_ITEM_NUMBER
			result.add(new Parameter(offset++, 0xae));
			result.add(new Parameter(offset++, AtrLoader.Constants.SELECTED_ITEM_NUMBER)); // Word
			offset++;

			// $0974 JMP $098B
			result.add(new Parameter(offset++, 0x4c));
			result.add(new Parameter(offset++, 0x8b));
			result.add(new Parameter(offset++, 0x09));
			break;

		case V2:
			try {
				offset = atrFile.getSectorStartOffset(364) + 0xbb; // Address $093B
			} catch (AtrException ex) {
				throw new RuntimeException(ex);
			}
			// $093B LDX $nnnn for SELECTED_ITEM_NUMBER
			result.add(new Parameter(offset++, 0xae));
			result.add(new Parameter(offset++, AtrLoader.Constants.SELECTED_ITEM_NUMBER)); // Word
			offset++;

			// $093E JMP $0958
			result.add(new Parameter(offset++, 0x4c));
			result.add(new Parameter(offset++, 0x58));
			result.add(new Parameter(offset++, 0x09));
			break;
		}
		return result;
	}

	private static void addPatchedSectors(AtrFile atrFile, List<Integer> usedSectors, List<PatchRange> patchRanges) {
		if (atrFile == null) {
			throw new IllegalArgumentException("Parameter 'atrFile' must not be null.");
		}
		if (usedSectors == null) {
			throw new IllegalArgumentException("Parameter 'usedSectors' must not be null.");
		}
		if (patchRanges == null) {
			throw new IllegalArgumentException("Parameter 'patchRanges' must not be null.");
		}
		for (Integer sector : usedSectors) {
			try {
				int startOffset = atrFile.getSectorStartOffset(sector.intValue());
				int sectorSize = atrFile.getSectorSize(sector.intValue());
				patchRanges.add(new PatchRange(startOffset, startOffset + sectorSize - 1));
			} catch (AtrException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	private List<String> getLongFileNames() {
		if (atrFile == null) {
			throw new IllegalStateException("Field 'atrFile' must not be null.");
		}
		List<String> result = new ArrayList<String>();

		addPicoNames(atrFile, result);
		addBootmanagerFileNames(atrFile, result);
		return result;

	}

	/**
	 * Collections the file names from a boot manager disk.
	 * 
	 * @param atrFile
	 *                      The ATR file, not <code>null</code>.
	 * @param fileNames
	 *                      The modifiable list to collect the file names in or
	 *                      <code>null</code> if the file names are not requested.
	 * @return <code>true</code> if there is a boot manager menu with at least one
	 *         entry present.
	 */
	private static BootManagerType addBootmanagerFileNames(AtrFile atrFile, List<String> fileNames) {
		if (atrFile == null) {
			throw new IllegalArgumentException("Parameter 'atrFile' must not be null.");
		}

		// Read Mike Langer's Bootmanager menu.
		// Code is sector 363 to 365. File names are in sector 366 to 368. Up to
		// 16 entries are possible but only as much entries as are in the real
		// directory are relevant.
		// Every entry has 0x18 bytes. $00+$00 means end. $7f means empty screen
		// line, >$80 means empty entry. First two bytes of entry are not
		// relevant here.
		BootManagerType result = BootManagerType.NONE;
		try {

			byte[] bootManagerSector = atrFile.getSector(365);
			if (containsStringAt(bootManagerSector, 0x4f, "Bootmanager (c)1996 by Mike Langer")) {
				result = BootManagerType.V1;
			} else if (containsStringAt(bootManagerSector, 0x40, "Bootmanager (c)1996,2022 by Homesoft")) {
				result = BootManagerType.V2;
			}

			// Directory structure is the same for V1 and V2
			if (result != BootManagerType.NONE) {
				if (fileNames != null) {
					// im V1 only 128 bytes are used per directory sector with long
					// file names.
					byte[] sectors = atrFile.getSectors(366, 368,
							result == BootManagerType.V1 ? AtrFile.SECTOR_SIZE_SD : AtrFile.SECTOR_SIZE_DD);
					int offset = 0;
					int line = 0;
					// The first line is the disk title and must be skipped.
					// Then there are at most 16 entries, but never more than
					// files on the disk.
					int maxLine = 1 + Math.min(16, atrFile.getDirectory().size());
					int entrySize = 0x18;
					StringBuilder builder = new StringBuilder();
					while (offset < sectors.length && line < maxLine) {
						int b = sectors[offset] & MASK_FF;
						if (b == 0 && sectors[offset + 1] == 0) {
							break;
						}
						if (b < 0x80) {
							if (b != 0x7f) {
								builder.setLength(0);
								for (int i = 2; i < entrySize; i++) {
									b = sectors[offset + i] & MASK_FF;
									char c = ATASCII[b];
									builder.append(c);
								}
								// Skip disk title
								if (line > 0) {
									fileNames.add(builder.toString().trim());
								}
							}
							line++;
						}

						offset += entrySize;
					}
				}
			}
		} catch (AtrException ex) {
			result = BootManagerType.NONE; // Inconsistent image
		}
		return result;
	}

	private static boolean containsStringAt(byte[] bootManagerSector, int offset, String identifier) {
		StringBuilder bootString = new StringBuilder(identifier.length());
		for (int i = 0; i < identifier.length(); i++) {
			int b = bootManagerSector[offset + i] & MASK_FF;
			bootString.append(ATASCII[b]);
		}
		boolean found = identifier.equals(bootString.toString());
		return found;
	}

	private static boolean addPicoNames(AtrFile atrFile, List<String> result) {
		if (atrFile == null) {
			throw new IllegalArgumentException("Parameter 'atrFile' must not be null.");
		}

		// Read PICODOS text file.
		try {
			byte[] picoNameFileContent;
			picoNameFileContent = atrFile.getFileContent(PICONAME_TXT, null);
			if (picoNameFileContent != null) {
				if (result != null) {
					int count = 0;
					StringBuilder builder = new StringBuilder();
					for (int i = 0; i < picoNameFileContent.length; i++) {

						int b = picoNameFileContent[i] & MASK_FF;
						if (b == 0x9b) {
							// Ignore first line which contains the disk title.
							if (count > 0) {
								String longFileName = builder.toString();
								if (StringUtility.isSpecified(longFileName)) {
									longFileName = longFileName.substring(11);
									longFileName = longFileName.trim();
									result.add(longFileName);
								}
							}
							builder.setLength(0);
							count++;
						} else {
							builder.append((char) (b & 0x7f));
						}
					}
				}
				return true;
			}
		} catch (AtrException ex) {
		}
		return false;
	}

	/**
	 * Call with directory containing ATR files are single argument to recursively
	 * scan them.
	 */
	public static void main(String[] args) {
		if (args == null) {
			throw new IllegalArgumentException("Parameter 'args' must not be null.");
		}
		if (args.length == 0) {
			throw new IllegalArgumentException("Parameter 'args' must not contain a file name.");
		}
		String fileName = args[0];
		File inputFile = new File(fileName);
		scanRecursively(inputFile);
	}

	private static void scanRecursively(File inputFile) {
		println("INFO: Scanning '" + inputFile.getAbsolutePath() + "'.");

		if (!inputFile.exists()) {
			println("ERROR: '" + inputFile.getAbsolutePath() + "' does not exist.");
			return;
		}
		File[] files;
		if (inputFile.isDirectory()) {
			files = inputFile.listFiles();
		} else {
			files = new File[] { inputFile };
		}
		for (File file : files) {
			if (file.isDirectory()) {
				scanRecursively(file);
			} else if (file.isFile() && file.getName().toLowerCase().endsWith(FileExtensions.ATR_IMAGE)) {
				try {
					byte[] atrData = FileUtility.readBytes(file, AtrFile.MAXIMUM_SIZE, true);
					println(file.getName() + ": ");
					AtrFile atrFile = AtrFile.createInstance(atrData);
					AtrFileMenu menu = new AtrFileMenu(atrData);
					println(atrFile.toString());
					if (atrFile.hasDirectory()) {
						println(atrFile.getDirectory().toString());

						List<String> longFileNames = menu.getLongFileNames();
						println(longFileNames.toString());
						List<PatchRange> patchRanges = menu.getPatchRanges();
						println(patchRanges.toString());

					}
					println("");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	private static void println(String text) {
		System.out.println(text);

	}
}
