import javax.xml.soap.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.*;

public class EwsFindFolderClient {
    private static final Logger logger = Logger.getLogger(EwsFindFolderClient.class.getName());

    private static final String DEFAULT_ENDPOINT = "http://localhost:8080/ews/FindFolder";
    private static final String SIMULATED_RESPONSE_FILE = "simulated-response.xml";

    private String endpoint;
    private boolean simulationMode;

    public EwsFindFolderClient(String endpoint, boolean simulationMode) {
        this.endpoint = endpoint != null ? endpoint : DEFAULT_ENDPOINT;
        this.simulationMode = simulationMode;
    }

    // Run the FindFolder operation
    public void runFindFolder(String parentFolderId, String folderShape) throws Exception {
        try {

            SOAPMessage soapResponse;
            if (simulationMode) {
                soapResponse = getSimulatedResponse();
            } else {
                SOAPMessage soapRequest = buildSoapEnvelope(parentFolderId, folderShape);
                logger.info("SOAP Request message :" + soapRequest);
                soapResponse = sendSoapRequest(soapRequest);
            }

            parseAndPrintResponse(soapResponse);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error occurred during runFindFolder: ", e);
            throw e;
        }
    }

    // Builds the SOAP envelope for FindFolder command
    private static SOAPMessage buildSoapEnvelope(String parentFolderId, String folderShape) throws SOAPException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();

        // Create SOAP envelope
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("t", "http://schemas.microsoft.com/exchange/services/2006/types");
        envelope.addNamespaceDeclaration("m", "http://schemas.microsoft.com/exchange/services/2006/messages");

        // Create SOAP body
        SOAPBody soapBody = envelope.getBody();
        SOAPElement findFolderElement = soapBody.addChildElement("FindFolder", "m");
        findFolderElement.setAttribute("Traversal", "Shallow");

        // FolderShape
        SOAPElement folderShapeElement = findFolderElement.addChildElement("FolderShape", "m");
        SOAPElement baseShapeElement = folderShapeElement.addChildElement("BaseShape", "t");
        baseShapeElement.addTextNode(folderShape != null ? folderShape : "Default");

        // ParentFolderIds
        SOAPElement parentFolderIdsElement = findFolderElement.addChildElement("ParentFolderIds", "m");
        SOAPElement folderIdElement = parentFolderIdsElement.addChildElement("FolderId", "t");
        folderIdElement.setAttribute("Id", parentFolderId != null ? parentFolderId : "root");

        // Save changes
        soapMessage.saveChanges();
        return soapMessage;
    }

    // Send SOAP request
    private SOAPMessage sendSoapRequest(SOAPMessage soapRequest) throws IOException {
        URL url = new URL(this.endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set request properties
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "\"http://schemas.microsoft.com/exchange/services/2006/messages/FindFolder\"");

        // Write the SOAP request body
        try (OutputStream os = conn.getOutputStream()) {
            soapRequest.writeTo(os);
        }

        // Get the response code
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP error code: " + responseCode);
        }

        // Read and parse SOAP response
        try (InputStream is = conn.getInputStream()) {
            MessageFactory messageFactory = MessageFactory.newInstance();
            return messageFactory.createMessage(null, is);
        }
    }

    // Read static XML file for response
    private static SOAPMessage getSimulatedResponse() throws Exception {
        InputStream is = EwsFindFolderClient.class.getClassLoader().getResourceAsStream(SIMULATED_RESPONSE_FILE);
        if (is == null) throw new FileNotFoundException("Simulation file not found.");
            
        MessageFactory messageFactory = MessageFactory.newInstance();
        return messageFactory.createMessage(null, is);
    }

    // Parse SOAP response and log FolderId, DisplayName, TotalCount
    private static void parseAndPrintResponse(SOAPMessage soapResponse) throws Exception {
        SOAPBody soapBody = soapResponse.getSOAPBody();
        
        // Check for errors
        if (soapBody.getFault() != null) {
            logger.severe("SOAP Fault encountered:");
            logger.severe("Fault Code: " + soapBody.getFault().getFaultCode());
            logger.severe("Fault String: " + soapBody.getFault().getFaultString());
            return;
        }

        // Find the response element
        javax.xml.soap.NodeList responseList = soapBody.getElementsByTagNameNS(
            "http://schemas.microsoft.com/exchange/services/2006/messages", "FindFolderResponse");
        
        if (responseList.getLength() == 0) {
            logger.info("No FindFolderResponse found in SOAP body.");
            return;
        }

        SOAPElement responseElement = (SOAPElement) responseList.item(0);
        SOAPElement responseMessage = (SOAPElement) responseElement.getFirstChild();

        // Get ResponseCode
        SOAPElement responseCodeElement = (SOAPElement) responseMessage.getElementsByTagNameNS(
            "http://schemas.microsoft.com/exchange/services/2006/messages", "ResponseCode").item(0);
        logger.info("Response Code: " + responseCodeElement.getValue());

        // Get folders
        javax.xml.soap.NodeList folders = soapBody.getElementsByTagNameNS(
            "http://schemas.microsoft.com/exchange/services/2006/types", "Folder");

        if (folders.getLength() == 0) {
            logger.info("No folders found in response.");
            return;
        }

        for (int i = 0; i < folders.getLength(); i++) {
            SOAPElement folder = (SOAPElement) folders.item(i);
            
            String folderId = getElementValue(folder, "FolderId", "Id");
            String displayName = getElementValue(folder, "DisplayName");
            String totalCount = getElementValue(folder, "TotalCount");

            logger.info(String.format("Folder %d:\n  FolderId: %s\n  DisplayName: %s\n  ParentId: %s",
                    i + 1,
                    folderId != null ? folderId : "(missing)",
                    displayName != null ? displayName : "(missing)",
                    totalCount != null ? totalCount : "(missing)"
            ));
        }
    }

    private String getElementValue(SOAPElement parent, String localName) {
        return getElementValue(parent, localName, null);
    }

    private String getElementValue(SOAPElement parent, String localName, String attribute) {
        javax.xml.soap.NodeList nodes = parent.getElementsByTagNameNS(
            "http://schemas.microsoft.com/exchange/services/2006/types", localName);
        
        if (nodes.getLength() > 0) {
            SOAPElement element = (SOAPElement) nodes.item(0);
            if (attribute != null) {
                return element.getAttribute(attribute);
            }
            return element.getValue();
        }
        return "N/A";
    }

    public static void main(String[] args) {
        configureLogger();

        boolean simulationMode = true;
        EWSFindFolderClient client = new EWSFindFolderClient(null, simulationMode);
                
        String parentFolderId = "root"; // Root of the mailbox
        String folderShape = "AllProperties";

        try {
            client.runFindFolder(parentFolderId, folderShape);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid input Error: " + e.getMessage());
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Simulation file not found: ", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO Error while reading the simulation file: ", e);
        } catch (SOAPException e) {
            logger.log(Level.SEVERE, "Error while building the SOAP envelope for FindFolder command: ", e);
        }} catch (Exception e) {
            logger.log(Level.SEVERE, "Error occurred during FindFolder operation: ", e);
        }
    }

    //  Configure Logging
    private static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        rootLogger.addHandler(consoleHandler);
        rootLogger.setLevel(Level.INFO);
    }

    //  Validate inputs
    private static void validateInputs(String parentFolderId, String folderShape) {
        if (parentFolderId == null || parentFolderId.isEmpty())
            throw new IllegalArgumentException("Parent folder id cannot be null or empty.");
        if (folderShape == null || folderShape.isEmpty())
            throw new IllegalArgumentException("Folder shape cannot be null or empty.");
    }
}
