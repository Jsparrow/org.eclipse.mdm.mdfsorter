<!--
Copyright (c) 2016 Audi AG
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/epl-v10.html
-->

# Documentation for MDFSorter
Tobias Leemann, AUDI AG

## Introduction
This documentation will provide a small "quick reference"-guide to the MDFSorter Application.

## Fields of Usage
The MDFSorter is used to preprocess MDF3.x and MDF4.x measurement data files for usage with an ASAM ODS Server.  
MDF-Files can contain so called "unsorted" data blocks. A data block is unsorted, if it contains records of multiple channel groups. In a sorted MDF-File, each channel group has its own data block, only containing records for this specific collection of channels.  
MDF4-Files can further contain other structures that may cause problems, or slow down the process of accessing the file contents. These structures are explained more detailed in the next section.

## Refactored Structures

### Unsorted Data
In an MDF-File each data section (either a single data block (DTBLOCK) or a list containing data blocks (DLBLOCK)) is referenced by a datagroup block (DGBLOCK). This datagroup block also references a list of channel groups (CGBLOCK). The records for these channels will be stored in the data group's data block. If a data group block's channelgroup list consists of more than one CGBLOCK, it is unsorted, because the records of all the CGBLOCKs in the list will be stored in a single data section.  
Such a "mixed" data block cannot be parsed by the ODS Server, and has therefore to be sorted in advance. Once this tool encounters a DGBLOCK with more than one channel group (CGBLOCK) it creates a new DGBLOCK (and the associated data section) for each channel group found. After processing, each DGBLOCK will only contain one channel group and a its data group will only contain the records of this channel group.

### VLSD records (Variable Length Signal Data)
Normal channelgroups' records normally have a fixed length. This must not be the case for VLSD channelgroups. Theses groups contain data from a channel of another channelgroup, that has no specified fixed length. The variable length records are mixed into the data section with the other channel groups. In a sorted MDF-File, this is not allowed, because each channel group should contain only fixed length records. If a channel group contains a VLSD channel, the channel itself should have a link to a signal data block (SDBLOCK), containing the variable length records. Its fixed length part of the channel groups record should only contain offsets to the data in the signal data block. The contained data will be stored in a new SDBLOCK referenced in the corresponding channel.

### Zipped Data
An MDF4-File of version 4.1 or above can contain data compressed with the deflate algorithm. Such data is stored in a zipped data block (DZBLOCK), and cannot be read by the ODS Server. Blocks which can be compressed are data blocks (DTBLOCK), signal data blocks (SDBLOCK) and reduction data blocks (RDBLOCK). If the *-zip* flag is not set the output file will not contain any DZBLOCKS, and the data will be stored in an equivalent uncompressed block.

### Linked Data List
An MDF4-File may also contain a data section consisting of multiple blocks. Therefore the datalist block (DLBLOCK) is used, which stores an array of links to data blocks. Apart from that, it can also store a link to another datalist block with an array of data blocks and so on. This results in a linked list of data block arrays.  
In general this tool tries to merge the data section into as few blocks as possible (size of blocks is limited by the *maxblocksize*-flag). If the whole section can be merged into one block the list block will be omitted. If a list block is needed, all data blocks in the list except the last one will contain *maxblocksize* bytes of data. This will also be specified in the maxblocksize attibute of the parent data list.  
A data list is not touched, if splitting up the data into parts of *maxblocksize* bytes each would result in more blocks to be in the output, than were there before.

## Interfaces
This tool provides two interfaces: A Command Line Interface (CLI), and an Java API, for usage in Java applications.

### Command Line Interface
The JAR-File of this tool can be executed from the command line (use: *java -jar <nameOfJar>.jar*). The program needs the following arguments to work:  
First, a general command has to be given. This can be either *check*, to see if processing a file is needed or *process* to resort a file.

### Usage of the *process*-Command
Syntax:	process <Input file> <Output file> [<Flags>]

__Input file:__ The path to the MDF3 / MDF4 file to be processed.  
__Output file:__ A valid file name, where the output should be written. If this file already exists, it is overwritten without prior warning.  
__Flags:__ The flags for this program call. The order of flags is not important. If none of these flags is passed, the program will run with its default values. The following flags are valid:

 - __*-maxblocksize=<value>*:__ The maximum size of data blocks in the output (in bytes). If data blocks from lists are merged, the resulting blocks will have this size. Blocks with a larger sizes than specified and will not be touched. Default value is 2GB. The value can be passed using the decimal prefixes, e.g.*-maxblocksize=1k*, *-maxblocksize=32m*, *-maxblocksize=2g*. Fractions are not allowed. If the *-zip*-flag is set, the maximum allowed size is 4MB, which will be automatically set, if a large size is passed to the program.
 - __*-zip* / *-unzip*:__ If the *-zip*-flag is set, all data blocks will be zipped. If the *-unzip*-flag is set, all zipped blocks will be unzipped. Only one of those two flags can be passed, passing both will result in an error. Default value is unzip. 
 - __*-overridesize*:__ Makes the program split up larger data blocks to parts of maxblocksize bytes, even if they were larger before. This can be useful if a file contains blocks that are too large to be handled by some application or if all data blocks should have an equal size. Default value: Not set.
 - __*-verbose*:__ The *-verbose*-flag causes the programm to print more detailed output. This can be useful when debugging or when processing larger files (to make sure the program is still working). Default value: Not set.

Example: *process C:\\file1.mf4 C:\\file2.mf4 -unzip -maxblocksize=800k*

#### Usage of the *check*-Command
Syntax: check <Input file> <MaxBlockSize> [<ZipFlag>]

__Input file:__ The path to the MDF3 / MDF4 file to be checked.  
__MaxBlockSize:__ The maximum size of data blocks in the output (in bytes). Default value is 2GB. The value can be passed using the decimal prefixes, e.g. *1k*, *32m*, *2g*. Decimal values are not allowed.  
__ZipFlag:__ *-zip* or *-unzip*. If the *-zip*-flag is set the file is also checked for unzipped data. If it is not set, it is checked for zipped data. Default value: unzip. This flag is not compulsory. 

Example: *check C:\\file1.mf4 800k -zip*

### Java API
The MDFSorter \versionnum also provides a Java-Interface, for usage in other programs. It consists of two methods from the *MDFSorter*-Class:

 - __*sortMDF()*__: Processes an MDF-File and sorts it if needed.
 - __*checkForProblems()*__: Check if processing a file is needed. Returns *true* only if this is the case.

Please refer to the JavaDoc documentation of the *MDFSorter*-Class for more detailed information, about arguments, semantics and return values.

## Known Limitations
The following limits of this tool are known:

 - __Recursion:__ Recursion is used to parse data lists linking to other data lists. Every time a function recurses, it uses some memory on the program stack. If too many levels of recursion occur the stack will be full and the program will crash with a *StackOverflowError*. This will happen after about 5000 recursive calls, e.g. if a data list has a link to another data list and so on, for 5000 times. 
 - __UINT64 Values:__ Due to the lack of a larger basic type than *long* in Java, this application will not be able to work with any values larger than *Long.MAX_VALUE*, even if the MDF-Standard defines the UINT64 data type. If the values don't need to be processed, they will be written out in the same form as they were before, so no damage to the file will be expected.
 - __Number of Links:__ Even though the number of links to child block is stored as in UINT64 value in the MDF-File, this application uses an array to access them, which is *int*-based in Java. The number of children a block can have is therefore limited to *Integer.MAX_VALUE*.
 - __The Channel Array Block (CABLOCK):__ In MDF4 a special block to store a group (array) of Channels exists. This Block is not fully supported by MDFSorter 0.2.0, and will only be copied as it was before. If it has links to any channels stored in an unsorted data group, that are resorted during processing, the behavior is not defined.
