/*
 *
 *  * Copyright 2003-2022 Beijing XinRong Meridian Limited.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.adaptiveMQ.example.blob;

import com.adaptiveMQ.client.ClientConnectionFactory;
import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.ConnectionException;
import com.adaptiveMQ.client.IClientConnection;
import com.adaptiveMQ.client.IClientSession;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.example.ExampleConfiguration;
import com.adaptiveMQ.message.Destination;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.MessageBody;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class PingBlobClient
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PingBlobClient.class);
    private static final String blobStr = "Backstory\n" + "The story focuses on the activities of three species in a part of the Milky Way known as the Koprulu Sector. Millennia before any of the events of the games, a species known as the Xel'Naga genetically engineer the Protoss and later the Zerg in attempts to create pure beings.[11][12] These experiments backfire and the Xel'Naga are largely destroyed by the Zerg.[12] Centuries before the beginning of StarCraft in 2499, the hardline international government of Earth, the United Earth Directorate (UED), commissions a colonization program as part of a solution to overpopulation. On the way, the computers automating the colony ships malfunction, propelling the Terran colonists far off course to the edge of Protoss space.[13] Out of contact with Earth, they form various factions to maintain their interests. Intrigued by the behavior and mentality of the Terrans, the Protoss remain hidden to examine the humans, while protecting them from other threats without their knowledge. The Zerg, however, target the Terrans for assimilation to harness their psionic potential,[12] forcing the Protoss to destroy tainted Terran colonies to contain the Zerg infestation.[14]\n" + "\n" + "StarCraft\n" + "StarCraft begins days after the first of these attacks, where the predominant Terran government, the Terran Confederacy, falls into a state of panic as it comes under attack by both the Zerg and the Protoss, in addition to increasing rebel activity led by Arcturus Mengsk against its rule. The Confederacy eventually succumbs to Mengsk's rebels when they use Confederate technology to lure the Zerg into attacking the Confederate capital, Tarsonis. In the consequent power vacuum, Mengsk crowns himself emperor of a new Terran Dominion. During the assault on Tarsonis, Mengsk allows the Zerg to capture and infest his psion second-in-command, Sarah Kerrigan. This betrayal prompts Mengsk's other commander, Jim Raynor, to desert him with a small army. Having retreated with Kerrigan to their primary hive clusters, the Zerg are assaulted by Protoss forces commanded by Tassadar and the dark templar Zeratul. Through assassinating a Zerg cerebrate, Zeratul inadvertently allows the Overmind to learn the location of the Protoss homeworld, Aiur. The Overmind quickly launches an invasion to assimilate the Protoss and gain genetic perfection. Pursued by his own people as a heretic for siding with the dark templar Zeratul, Tassadar returns to Aiur and, with the assistance of Raynor and the templar Fenix, launches an attack on the Overmind and ultimately sacrifices himself to kill the creature.[14]\n" + "\n" + "Brood War\n" + "In Brood War, the Protoss are led by Zeratul and Artanis. They begin to evacuate the surviving population of Aiur to the dark templar homeworld of Shakuras under a fragile alliance between the two untrusting branches of the Protoss. On Shakuras, they are misled by Kerrigan into attacking the Zerg to advance Kerrigan's quest to securing power over the Zerg. This deception comes after she reveals that a new Overmind has entered incubation. Meanwhile, Earth decides to take action in the sector, sending a fleet to conquer the Terran Dominion and capture the new Overmind. Although successfully taking the Dominion capital Korhal and enslaving the Overmind, the UED's efforts to capture Mengsk are thwarted by a double agent working for Kerrigan, Samir Duran. Kerrigan, allying with Mengsk, Fenix, and Raynor, launches a campaign against the UED, recapturing Korhal. She turns against her allies, with Fenix and Duke both perishing in the ensuing attacks. Kerrigan later extorts Zeratul into killing the new Overmind, giving her full control over the entire Zerg Swarm. After defeating a retaliatory attack by the Protoss, Dominion, and the UED (consequently destroying the last of the UED fleet), Kerrigan and her Zerg broods become the dominant power in the sector. However, Zeratul secretly uncovers a plot by Duran to create Protoss-Zerg hybrids and learns that Duran is a servant of not Kerrigan, but a \"far greater power\".[15]\n" + "\n" + "Wings of Liberty\n" + "Four years later, in Wings of Liberty, Kerrigan and the Zerg vanish from the Koprulu Sector, allowing the Protoss to once again take on a passive role in the galaxy. Meanwhile, Raynor forms a revolutionary group named Raynor's Raiders in order to overthrow Mengsk. On Mar Sara, Raynor liberates the local population from Dominion control and also discovers a component of a mysterious Xel'Naga artifact. The Zerg reappear and overrun Mar Sara, forcing Raynor to arrange an evacuation to his battlecruiser, the Hyperion. The Raiders embark on a series of missions to undermine Mengsk, stop frequent Zerg infestations on Terran worlds, gather psychic individuals for military assets, and find the remaining pieces of the Xel'Naga artifact, which they sell to the enigmatic Moebius Foundation in order to fund their revolution. Soon after, Zeratul delivers a psychic crystal that allows Raynor to share visions involving an ominous prophecy where Zerg-Protoss hybrids and an enslaved Zerg swarm wipe out the Terrans and the Protoss. The vision reveals that only Kerrigan has the power to prevent the eradication of all life in the sector and beyond. After collecting more artifact pieces, the Raiders forge an alliance with Valerian Mengsk, Arcturus' son, who is their secret benefactor from Moebius Foundation. After recovering the final artifact piece, Valerian and Raynor work together to invade the Zerg world of Char and use the artifact to restore Kerrigan's humanity, thus weakening the Zerg at the cost of much of the Dominion fleet. An agent of Arcturus makes an attempt on Kerrigan's life, and Raynor defends her and takes her in for medical examination.\n" + "\n" + "Heart of the Swarm\n" + "In Heart of the Swarm, the Dominion discovers where Raynor and Kerrigan are hiding and launch an attack on them. Kerrigan manages to escape, but is cut off from Raynor and upon hearing news that he was captured and executed, she returns to Zerg territory to retake control of the swarm and exact revenge on Mengsk. During her quest, she has an encounter with Zeratul, who advises her to travel to Zerus, the original homeworld of the Zerg, where she regains her powers as the Queen of Blades, returning stronger than ever, and learns that a fallen Xel'Naga named Amon was responsible for making the Zerg what they are: a warring swarm, bound to a single overriding will. After confronting a legion of servants of Amon, including a breed of Protoss-Zerg hybrids, Mengsk informs Kerrigan that Raynor is still alive and uses him as a leverage against her, keeping the location where he is imprisoned a secret, until she joins forces with the Hyperion to locate and rescue him. Seeing that she discarded her humanity after all the effort he took to restore it, Raynor rejects her, despite her confession that she loves him, and parts ways with her. Kerrigan turns her attention to Korhal and sends her forces to bring down Mengsk once and for all. During their showdown, Mengsk uses the artifact to immobilize her, but Raynor appears to protect her, and Mengsk is ultimately killed by Kerrigan. With the Dominion under control of Mengsk's son Valerian, Kerrigan bids farewell to Raynor and departs with the Zerg Swarm to confront Amon and his forces.\n" + "\n" + "Legacy of the Void\n" + "In Legacy of the Void, Zeratul invades a Terran installation under control of Amon in order to pinpoint the exact location of his resurrection, taking advantage of a sudden attack by Kerrigan and the Zerg swarm. After obtaining the exact location, he departs to an ancient Xel'Naga temple where he has a vision of Tassadar, who prompts him to claim the artifact in possession of the Terrans. Zeratul returns to warn Artanis of Amon's return, but he decides to proceed with his plans of leading his army to reclaim Aiur. Amon awakens on Aiur and takes control of the majority of the Protoss race through the Khala, the telepathic bond that unites all emotions for the Khalai faction of the Protoss. Only Zeratul and the Nerazim, the Dark Templar, are immune due to their lack of connection to the Khala, and the Nerazim proceed to save as many Khalai as they can by severing their nerve cords, which connect them to the Khala, with Zeratul sacrificing himself to save Artanis in the occasion. After escaping the planet with an ancient vessel, the Spear of Adun, Artanis reclaims the artifact as Zeratul suggested and gathers allies among the many Protoss tribes scattered across the galaxy in order to remake his army and launch another assault on Aiur. Using the artifact, Artanis' forces restrain Amon's essence, time enough for the other Khalai Protoss who were still under his control to sever their nerve cords and banish Amon to the Void.\n" + "\n" + "In a short epilogue after the end of Legacy of the Void, Kerrigan calls for Artanis and Raynor's help to confront Amon inside the Void to defeat him once and for all. In the occasion, they meet Ouros, the last of the Xel'Naga who reveals that to confront Amon on equal terms, Kerrigan must inherit Ouros' essence and become a Xel'Naga herself, as Ouros himself is at the last of his strength. Assisted by the Zerg, Terran and Protoss forces, the empowered Kerrigan vanquishes Amon, before disappearing without a trace. Two years later, Kerrigan appears before Raynor in human form and he departs with her to never be heard from again, while the Zerg, the Terran and the Protoss civilizations begin to rebuild in an age of peace and prosperity.";
    public static void main(String[] args) throws Exception
    {
        PingBlobClient requestClient = new PingBlobClient();
        ClientInfo clientInfo = createClientInfo();
        requestClient.start(clientInfo);
    }
    private static final byte[] blobFileBytes = PingBlobClient.
            loadResourceFileAsBytes("rfc793.txt");

    public static ClientInfo createClientInfo() throws Exception
    {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setUser(ExampleConfiguration.LOGIN_USER_NAME, ExampleConfiguration.LOGIN_PASSWORD);
        clientInfo.setAddress(ExampleConfiguration.SERVER_HOST, ExampleConfiguration.SERVER_PORT);
        clientInfo.setProtocol(ExampleConfiguration.protocolType);
        clientInfo.setLocalHost(ExampleConfiguration.CLIENT_HOST);
        clientInfo.setLocalPort(ExampleConfiguration.CLIENT_PORT);
        clientInfo.setShareMemoryPath(ExampleConfiguration.SHM_BASE_PATH + ExampleConfiguration.PONG_SERVER_NAME);
        return clientInfo;
    }

    private IClientConnection createConnection(ClientInfo clientInfo) throws IOException, ConnectionException
    {
        IClientConnection conn = ClientConnectionFactory.createConnection(clientInfo);
        conn.addEventListener((int nCode) ->
        {
            switch (nCode) {
                case IEventListener.CONNECTION_CONNECTING:
                    logger.info("Event: CONNECTION_CONNECTING");
                    break;
                case IEventListener.CONNECTION_CONNECTED:
                    logger.info("Event: CONNECTION_CONNECTED");
                    break;
                case IEventListener.CONNECTION_CLOSED:
                    logger.info("Event: CONNECTION_CLOSED");
                    break;
                case IEventListener.CONNECTION_RECONNECT:
                    logger.info("Event: CONNECTION_RECONNECTED");
                    break;
                case IEventListener.CONNECTION_LOGINING:
                    logger.info("Event: CONNECTION_LOGINING");
                    break;
                case IEventListener.CONNECTION_LOGIN_SUCCESS:
                    logger.info("Event: CONNECTION_LOGIN_SUCCESS");
                    break;
                case IEventListener.CONNECTION_IO_EXCEPTION:
                    logger.info("Event: CONNECTION_IO_EXCEPTION");
                    break;
                case IEventListener.CONNECTION_LOST:
                    logger.info("Event: CONNECTION_LOST");
                    break;
                case IEventListener.CONNECTION_TIMEOUT:
                    logger.info("Event: CONNECTION_TIMEOUT");
                    break;
            }
        });
        conn.start();
        return conn;
    }

    public static byte[] loadResourceFileAsBytes(String fileName)   {
        try {
            // Use the class loader to get the resource as an InputStream
            InputStream inputStream = FileUtils.class.getResourceAsStream("/" + fileName);

            if (inputStream != null) {
                // Read the contents of the file into a byte array
                return readInputStreamToByteArray(inputStream);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] readInputStreamToByteArray(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int bytesRead;
            byte[] data = new byte[1024]; // You can adjust the buffer size based on your requirements

            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }

            return buffer.toByteArray();
        } finally {
            inputStream.close();
        }
    }


    public void start(ClientInfo clientInfo) throws Exception
    {
        IClientConnection conn = createConnection(clientInfo);
        IClientSession session = conn.createSession();

        for (int count = 0; count < 10; ++count) {
            Message msgRequest = new Message();
            msgRequest.setDestination(new Destination("CEDA.RPC.PING"));
            MessageBody record = msgRequest.getMessageBody();
            record.setBlobField((short) 1, blobFileBytes);
            record.addString((short) 5, "ping");
            long requestTimeStamp = System.currentTimeMillis();
            record.addLong((short) 6, requestTimeStamp);
            Message reply = session.sendRequest(msgRequest, 5000);
            if (reply != null) {
                long diff = System.currentTimeMillis() - requestTimeStamp;
                logger.warn(String.format("Receive reply : id=%d, topic=%s, result=%s, elapsed=%d(ms)",
                        reply.getMessageID(), reply.getDestination().getName(), reply.getMessageBody().getString((short) 3), diff));
                String blobstr = new String(reply.getMessageBody().getBlobField(), java.nio.charset.StandardCharsets.UTF_8);
                String[] lines = blobstr.split("\n");
                for(String line : lines) {
                    logger.warn(line);
                }
            }
            else {
                logger.info("Timeout, can't receieve the reply");
            }
            try {
                Thread.sleep(5000);
            }
            catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }
}
