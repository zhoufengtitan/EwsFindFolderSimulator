# EwsFindFolderSimulator

EWS - FindFolder Simulation

Task: Create a Java client that constructs and sends a SOAP request to simulate the
EWS FindFolder operation.
Requirements:
• Construct a SOAP envelope for the FindFolder request.
• Use HTTP POST to send to a mock or test EWS endpoint.
• Parse the SOAP response and extract:
o FolderId
o DisplayName
o TotalCount
Sample tools/libraries:
• Apache HttpClient or HttpURLConnection
• Java XML APIs (javax.xml.soap, DOM, or JAXB)

Overview

• Constructs a SOAP request.

• Uses HttpURLConnection to send the request (to a simulated or placeholder endpoint).

• Parses a static SOAP XML response from a file.

• Extracts and logs: FolderId, DisplayName, and TotalCount

1, For simulation mode:

• Open a terminal in that folder
• Compile: javac EwsFindFolderClient.java
• Run: java EwsFindFolderClient

Sample Output:

May 22, 2025 10:00:00 PM EwsFindFolderClient parseAndPrintResponse
INFO: Folder 1:
FolderId: folder1-id
DisplayName: Inbox
TotalCount: 217

May 22, 2025 10:00:00 PM EwsFindFolderClient parseAndPrintResponse
INFO: Folder 2:
FolderId: folder2-id
DisplayName: Sent Items
TotalCount: 28

2, For real HTTP mode:

Set simulationMode = false in the main method

And provide a real EWS endpoint URL
