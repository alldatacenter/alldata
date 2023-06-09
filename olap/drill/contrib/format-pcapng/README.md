## Overview

A core component of any network security program is analyzing raw data that is coming over the wire. This raw network data is captured in a format called Packet Capture (PCAP) or PCAP Next Generation (PCAP­NG) and can be challenging to analyze because it is a binary format. One of the most common tools for analyzing PCAP data is called Wireshark. Even though Wireshark is a capable tool, it is limited in that it can only analyze data that fits in your system’s memory.

<i> -- taken from "Learning Apache Drill" Book.</i>

Drill can query a PCAP or PCAP­NG file and retrieve fields including the following:

 - Protocol type (TCP/UDP)

 - Source/destination IP address Source/destination port

 - Source/destination MAC address

 - Date and time of packet creation

 - Packet length

 - TCP session number and flags

 - The packet data in binary form
 
Querying PCAP or PCAP­NG requires no additional configuration settings, so out of the box, Drill installation can query them both.

## Attributes

The following table lists configuration attributes:

Attribute|Default Value|Description
---------|-------------|-----------
stat|false|return the statistics data about the each pcapng file if true

## ToDo
We plan to refactor the NG parser because of the `pcapngdecoder` so bad to process huge pcapng file. (Neither efficient nor support parser packet as stream)