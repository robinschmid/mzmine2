﻿/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * ------------------------------------------------------------------------------
 *
 * This program binds to the XRawfile2.dll provided by the MSFileReader library
 * (http://sjsupport.thermofinnigan.com/public/detail.asp?id=703) and dumps the
 * contents of a given RAW file as text+binary data. To compile this source, you
 * can use Microsoft Visual Studio 2013.
 *
 * Notes:
 * 
 * 1) The libraries XRawfile2.dll, Fileio.dll, and fregistry.dll come from Thermo 
 * MSFileReader (32-bit version)
 * 
 * 2) The .NET version of the native XRawFile2.dll file was generated by running 
 * "C:\Program Files\Microsoft SDKs\Windows\v7.0A\bin\tlbimp.exe" XRawfile2.dll
 * (named automatically as MSFileReaderLib.dll)
 * 
 * 3) The app.manifest file was generated as described in the file 
 *
 * 4) We use Console.Write and add \n (UNIX-style end of line) in the end, which 
 * is what the MZmine import module expects. If we use Console.WriteLine, it would
 * add \r\n (Windows-style end of line).

 NUMBER OF SCANS: int
 SCAN NUMBER: int
 SCAN ID: string
 POLARITY: char
 MS LEVEL: int
 RETENTION TIME: double
 MZ RANGE: double - double
 PRECURSOR: double int
 MASS VALUES: int x int BYTES
 INTENSITY VALUES: int x int BYTES

 notes: 
 RT is in minutes
 polarity is + or - or ?
 precursor corresponds to m/z and charge (0 if unknown)
 
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using MSFileReaderLib;

namespace ThermoRawDump
{
    class ThermoRawDump
    {
        static void Main(string[] args)
        {

            try
            {

                if (args.Length != 1)
                {
                    Console.Write("ERROR: This program accepts exactly 1 argument: a RAW file path\n");
                    Environment.Exit(1);
                }

                string filename = args[0];

                // Test whether the file exists, because the rawFile.Open()
                // function does not check that
                if (!File.Exists(filename))
                {
                    Console.Write("ERROR: Unable to read RAW file " + filename + "\n");
                    Environment.Exit(1);
                }

                // Open the raw file
                IXRawfile4 rawFile = (IXRawfile4)new MSFileReader_XRawfile();
                rawFile.Open(filename);

                // Set the controller number, otherwise no data can be accessed
                int nControllerType = 0; // 0 == mass spec device
                int nContorllerNumber = 1; // first MS device
                rawFile.SetCurrentController(nControllerType, nContorllerNumber);

                // Number of scans
                int totalNumScans = 0;
                rawFile.GetNumSpectra(ref totalNumScans);
                Console.Write("NUMBER OF SCANS: " + totalNumScans + "\n");

                // Number of first and last scan
                int firstScanNumber = 0, lastScanNumber = 0;
                rawFile.GetFirstSpectrumNumber(ref firstScanNumber);
                rawFile.GetLastSpectrumNumber(ref lastScanNumber);

                // Open the stdout stream and prepare a buffer
                Stream stdout = Console.OpenStandardOutput();
                byte[] byteBuffer = new byte[1000000];

                for (int curScanNum = firstScanNumber; curScanNum <= lastScanNumber; curScanNum++)
                {

                    // Scan number
                    Console.Write("SCAN NUMBER: " + curScanNum + "\n");

                    // Scan filter line
                    string scanFilter = null;
                    rawFile.GetFilterForScanNum(curScanNum, ref scanFilter);
                    if (scanFilter == null)
                    {
                        Console.Write("ERROR: Could not extract scan filter line for scan #" + curScanNum + "\n");
                        Environment.Exit(1);
                    }
                    Console.Write("SCAN ID: " + scanFilter + "\n");

                    // Polarity
                    char polarity;
                    if (scanFilter.Contains(" - ")) polarity = '-';
                    else if (scanFilter.Contains(" + ")) polarity = '+';
                    else polarity = '?';
                    Console.Write("POLARITY: " + polarity + "\n");

                    // MS level
                    int msLevel = -1;
                    rawFile.GetMSOrderForScanNum(curScanNum, ref msLevel);
                    if (msLevel < -1) msLevel = 2; // e.g., neutral gain scan returns -3, see MSFileReader doc
                    if (msLevel < 1) msLevel = 1; // e.g., parent scan scan returns -1, see MSFileReader doc
                    Console.Write("MS LEVEL: " + msLevel + "\n");

                    int numDataPoints = -1; // points in both the m/z and intensity arrays
                    double retentionTimeInMinutes = -1;
                    double minObservedMZ = -1;
                    double maxObservedMZ = -1;
                    double totalIonCurrent = -1;
                    double basePeakMZ = -1;
                    double basePeakIntensity = -1;
                    int channel = 0; // unused
                    int uniformTime = 0; // unused
                    double frequency = 0; // unused
                    rawFile.GetScanHeaderInfoForScanNum(curScanNum,
                                                        ref numDataPoints,
                                                        ref retentionTimeInMinutes,
                                                        ref minObservedMZ,
                                                        ref maxObservedMZ,
                                                        ref totalIonCurrent,
                                                        ref basePeakMZ,
                                                        ref basePeakIntensity,
                                                        ref channel, // unused
                                                        ref uniformTime, // unused
                                                        ref frequency // unused
                                                        );

                    // Retention time
                    Console.Write("RETENTION TIME: " + retentionTimeInMinutes + "\n");

                    // m/z range
                    Console.Write("MZ RANGE: " + minObservedMZ + " - " + maxObservedMZ + "\n");

                    // Precursor
                    if (msLevel > 1)
                    {
                        object precursorMz = null;
                        object precursorCharge = null;
                        rawFile.GetTrailerExtraValueForScanNum(curScanNum, "Monoisotopic M/Z:", ref precursorMz);
                        rawFile.GetTrailerExtraValueForScanNum(curScanNum, "Charge State:", ref precursorCharge);
                        Console.Write("PRECURSOR: " + precursorMz + " " + precursorCharge + "\n");
                    }

                    // Scan raw data points
                    int arraySize = -1;
                    object rawData = null; // rawData wil come as Double[,]
                    object peakFlags = null;
                    int scanNum = curScanNum;
                    string szFilter = null;        // No filter
                    int intensityCutoffType = 0;        // No cutoff
                    int intensityCutoffValue = 0;    // No cutoff
                    int maxNumberOfPeaks = 0;        // 0 : return all data peaks
                    double centroidPeakWidth = 0;        // No centroiding
                    int centroidThisScan = 0; // No centroiding

                    rawFile.GetMassListFromScanNum(
                                                    ref scanNum,
                                                    szFilter,             // filter
                                                    intensityCutoffType, // intensityCutoffType
                                                    intensityCutoffValue, // intensityCutoffValue
                                                    maxNumberOfPeaks,     // maxNumberOfPeaks
                                                    centroidThisScan,        // centroid result?
                                                    ref centroidPeakWidth,    // centroidingPeakWidth
                                                    ref rawData,        // daw data
                                                    ref peakFlags,        // peakFlags
                                                    ref arraySize);        // array size

                    // Print data points
                    Console.Write("MASS VALUES: " + arraySize + " x " + sizeof(double) + " BYTES\n");


                    // Calculate the byte size of rawData.
                    // rawData contains arraySize of doubles for mz values,
                    // followed by arraySize of doubles for intensity values
                    int numOfBytes = sizeof(double) * arraySize;

                    // Make sure our buffer is big enough
                    if (byteBuffer.Length < numOfBytes)
                        byteBuffer = new byte[numOfBytes * 2];

                    // Dump the binary data
                    Buffer.BlockCopy((Array)rawData, 0, byteBuffer, 0, numOfBytes);
                    stdout.Write(byteBuffer, 0, numOfBytes);

                    Console.Write("INTENSITY VALUES: " + arraySize + " x " + sizeof(double) + " BYTES\n");

                    // Dump the binary data
                    Buffer.BlockCopy((Array)rawData, numOfBytes, byteBuffer, 0, numOfBytes);
                    stdout.Write(byteBuffer, 0, numOfBytes);


                }

                rawFile.Close();

            }
            catch (Exception e)
            {
                Console.Write("ERROR: " + e.ToString() + "\n");
            }
        }

    }

}
