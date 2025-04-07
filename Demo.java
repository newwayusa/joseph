SELECT 
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema NOT IN ('information_schema', 'pg_catalog')) AS total_tables,
  (SELECT COUNT(*) FROM pg_indexes WHERE schemaname NOT IN ('information_schema', 'pg_catalog')) AS total_indexes,
  (SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema NOT IN ('information_schema', 'pg_catalog')) AS total_stored_procedures,
  (SELECT COUNT(*) FROM information_schema.views WHERE table_schema NOT IN ('information_schema', 'pg_catalog')) AS total_views,
  (SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema NOT IN ('information_schema', 'pg_catalog')) AS total_triggers,
  (SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name NOT IN ('information_schema', 'pg_catalog')) AS total_schemas
;

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.ubs.pirum.rm6</groupId>
  <artifactId>PirumRecordManagement</artifactId>
  <packaging>jar</packaging>
  <version>1.0-SNAPSHOT</version>
  <name>PirumRecordManagement</name>
  <url>http://maven.apache.org</url>
  <dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
    
    <!-- Jackson JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.0</version>
    </dependency>

    <!-- JavaMail for Sending Emails -->
    <dependency>
        <groupId>com.sun.mail</groupId>
        <artifactId>jakarta.mail</artifactId>
        <version>2.0.1</version>
    </dependency>

    <!-- Apache Commons for File Handling -->
    <dependency>
        <groupId>commons-io</groupId>
        <artifactId>commons-io</artifactId>
        <version>2.11.0</version>
    </dependency>
  </dependencies>
</project>



package com.ubs.pirum.rm6;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ubs.pirum.rm6.models.ConversationUpdate;
import com.ubs.pirum.rm6.models.Root;
import com.ubs.pirum.rm6.models.Update;
import com.ubs.pirum.rm6.models.UpdateMetadata;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class RM6Processor {
    private static final String OUTPUT_DIR = "output_json/";

    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            // Ensure output directory exists
            Files.createDirectories(Paths.get(OUTPUT_DIR));

            // Read JSON file
            Root root = objectMapper.readValue(new File("src/main/resources/newmessages.json"), Root.class);

            // Process conversation updates
            for (ConversationUpdate update : root.payload.conversationUpdates) {
                String conversationId = update.conversationId;

                // Generate JSON file
                String jsonFileName = OUTPUT_DIR + conversationId + ".json";
                objectMapper.writeValue(new File(jsonFileName), update);
                System.out.println("Generated JSON: " + jsonFileName);

                // Generate HTML table content
                String emailBody = generateHtmlTable(update.updates);

                // Send Email
                sendEmailWithAttachment(conversationId, emailBody, jsonFileName);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates an HTML table from the update metadata
     */
    private static String generateHtmlTable(List<Update> updates) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h2>Conversation Updates</h2>");
        sb.append("<table border='1' cellpadding='5' cellspacing='0'>");
        sb.append(
                "<tr><th>Sender</th><th>Time</th><th>Message</th><th>Email</th><th>User ID</th><th>Client ID</th></tr>");

        for (Update update : updates) {
            UpdateMetadata meta = update.metadata;
            sb.append("<tr>");
            sb.append("<td>").append(meta.fullName).append("</td>");
            sb.append("<td>").append(meta.timestamp).append("</td>");
            sb.append("<td>").append(meta.content).append("</td>");
            sb.append("<td>").append(meta.email).append("</td>");
            sb.append("<td>").append(meta.userId).append("</td>");
            sb.append("<td>").append(meta.clientCd).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table></body></html>");

        return sb.toString();
    }

    /**
     * Sends an email with the generated JSON file as an attachment
     */
    private static void sendEmailWithAttachment(String conversationId, String emailBody, String jsonFilePath) {
        final String senderEmail = "your_email@example.com";
        final String senderPassword = "your_password";
        final String recipientEmail = "recipient@example.com";
        final String smtpHost = "smtp.example.com";
        final int smtpPort = 587;

        // Set mail properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);

        // Authenticate and create session
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            // Create email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Conversation Update: " + conversationId);

            // Create Multipart
            Multipart multipart = new MimeMultipart();

            // HTML Body Part
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(emailBody, "text/html");
            multipart.addBodyPart(htmlPart);

            // Attachment Part
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new File(jsonFilePath));
            multipart.addBodyPart(attachmentPart);

            // Set Content
            message.setContent(multipart);

            // Send Email
            Transport.send(message);
            System.out.println("Email sent successfully with attachment: " + jsonFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


package com.ubs.pirum.rm6.models;

public class Context {
    public String level;
    public String levelId;
}
package com.ubs.pirum.rm6.models;

import java.util.List;

public class ConversationUpdate {
    public String conversationId;
    public String cparty;
    public String created;
    public String updated;
    public String product;
    public List<Context> contexts;
    public List<Update> updates;
}
package com.ubs.pirum.rm6.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

public class Metadata {
    public String timestamp;
    public String messageSchemaVersion;
    public String messageType;
    public String messageId;
    public long sequenceId;
    public int itemTotal;
    public String client;
    public String sourceSystem;
}
package com.ubs.pirum.rm6.models;


import java.util.List;

public class Payload {
    public String from;
    public String to;
    public List<ConversationUpdate> conversationUpdates;
}

package com.ubs.pirum.rm6.models;


public class Root {
    public Metadata metadata;
    public Payload payload;
}

package com.ubs.pirum.rm6.models;

public class Update {
    public UpdateMetadata metadata;
    public boolean deleted;
}


package com.ubs.pirum.rm6.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

public class UpdateMetadata {
    public String clientCd;
    public String content;
    public String email;
    public String userId;
    public String fullName;
    public String messageId;
    public String timestamp;
    public String visibility;
}



{
    "metadata": {
        "timestamp": "2025-01-15T14:51:59.370689Z",
        "messageSchemaVersion": "1.0.0-RC1",
        "messageType": "CommentsFeed",
        "messageId": "dfd56d97-c5f5-49e3-955a-e73852507ef4",
        "sequenceId": 1736952719370,
        "itemTotal": 5,
        "client": "UBSLNAG",
        "sourceSystem": "Pirum"
    },
    "payload": {
        "from": "2025-01-14T00:00:00",
        "to": "2025-01-14T23:59:59",
        "conversationUpdates": [
            {
                "conversationId": "c67c5126-4633-4d58-83ec-26760f370850",
                "cparty": "BNYMEL",
                "created": "2025-01-14T17:22:51.549",
                "updated": "2025-01-14T17:22:51.549",
                "product": "TRM",
                "contexts": [
                    {
                        "level": "trade",
                        "levelId": "11000:192531558"
                    },
                    {
                        "level": "trade",
                        "levelId": "26000:40575269"
                    }
                ],
                "updates": [
                    {
                        "metadata": {
                            "clientCd": "PIRUM",
                            "content": "tests",
                            "email": "robert.keane@pirum.com",
                            "userId": "10005457",
                            "fullName": "Robert Keane",
                            "messageId": "8eb75d2f-258b-4795-81c7-7c590a1b7359",
                            "timestamp": "2025-01-14T17:24:08.325",
                            "visibility": "Public"
                        },
                        "deleted": false
                    },
                    {
                        "metadata": {
                            "clientCd": "422794",
                            "content": "What is the status of the trade?",
                            "email": "augustine.selvaraj@ubs.com",
                            "userId": "10005457",
                            "fullName": "Augustine Selvaraj",
                            "messageId": "8eb75d2f-258b-4795-81c7-7c590a1b7359",
                            "timestamp": "2025-01-14T17:24:08.325",
                            "visibility": "Public"
                        },
                        "deleted": false
                    }
                ]
            },
            {
                "conversationId": "c67c5126-4633-4d58-83ec-xxxxxxxxxxxxxx",
                "cparty": "BNYMEL",
                "created": "2024-01-14T17:22:51.549",
                "updated": "2024-01-14T17:22:51.549",
                "product": "TRM",
                "contexts": [
                    {
                        "level": "trade",
                        "levelId": "11000:192531558"
                    },
                    {
                        "level": "trade",
                        "levelId": "26000:40575269"
                    }
                ],
                "updates": [
                    {
                        "metadata": {
                            "clientCd": "PIRUM",
                            "content": "Message from Alex",
                            "email": "alex@pirum.com",
                            "userId": "10005457",
                            "fullName": "Alex",
                            "messageId": "8eb75d2f-258b-4795-81c7-7c590a1b7359",
                            "timestamp": "2025-01-14T17:24:08.325",
                            "visibility": "Public"
                        },
                        "deleted": false
                    },
                    {
                        "metadata": {
                            "clientCd": "422794",
                            "content": "Message from Augustine Selvaraj",
                            "email": "augustine.selvaraj@ubs.com",
                            "userId": "10005457",
                            "fullName": "Augustine Selvaraj",
                            "messageId": "8eb75d2f-258b-4795-81c7-7c590a1b7359",
                            "timestamp": "2025-01-14T17:24:08.325",
                            "visibility": "Public"
                        },
                        "deleted": false
                    }
                ]
            }
        ]
    }
}


